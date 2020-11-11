package io.quarkus.cache.runtime;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.jboss.logging.Logger;

import io.quarkus.cache.runtime.caffeine.CaffeineCache;

@CacheResultInterceptorBinding
@Interceptor
@Priority(CacheInterceptor.BASE_PRIORITY + 2)
public class CacheResultInterceptor extends CacheInterceptor {

    private static final Logger LOGGER = Logger.getLogger(CacheResultInterceptor.class);

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        CacheResultInterceptorBinding binding = getInterceptorBinding(context, CacheResultInterceptorBinding.class);

        CaffeineCache cache = cacheRepository.getCache(binding.cacheName());
        Object key = getCacheKey(cache, binding.cacheKeyParameterPositions(), context.getParameters());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debugf("Loading entry with key [%s] from cache [%s]", key, cache.getName());
        }

        if (binding.lockTimeout() <= 0) {
            CompletableFuture<Object> cacheValue = cache.getWithSyncComputation(key, new Function<Object, Object>() {
                @Override
                public Object apply(Object k) {
                    return invokeInterceptedMethod(context);
                }
            });
            try {
                return cacheValue.get();
            } catch (ExecutionException e) {
                throw getExceptionToThrow(e);
            }
        } else {

            // The lock timeout logic starts here.

            /*
             * If the current key is not already associated with a value in the Caffeine cache, there's no way to know if the
             * current thread or another one started the missing value computation. The following variable will be used to
             * determine whether or not a timeout should be triggered during the computation depending on which thread started
             * it.
             */
            boolean[] isCurrentThreadComputation = { false };

            CompletableFuture<Object> cacheValue = cache.getWithAsyncComputation(key, new Function<Object, Object>() {
                @Override
                public Object apply(Object k) {
                    isCurrentThreadComputation[0] = true;
                    return invokeInterceptedMethod(context);
                }
            });

            if (isCurrentThreadComputation[0]) {
                // The value is missing and its computation was started from the current thread.
                // We'll wait for the result no matter how long it takes.
                try {
                    return cacheValue.get();
                } catch (ExecutionException e) {
                    throw getExceptionToThrow(e);
                }
            } else {
                // The value is either already present in the cache or missing and its computation was started from another thread.
                // We want to retrieve it from the cache within the lock timeout delay.
                try {
                    return cacheValue.get(binding.lockTimeout(), TimeUnit.MILLISECONDS);
                } catch (ExecutionException e) {
                    throw getExceptionToThrow(e);
                } catch (TimeoutException e) {
                    // Timeout triggered! We don't want to wait any longer for the value computation and we'll simply invoke the
                    // cached method and return its result without caching it.
                    // TODO: Add statistics here to monitor the timeout.
                    return context.proceed();
                }
            }
        }
    }

    private Object invokeInterceptedMethod(InvocationContext context) {
        try {
            return context.proceed();
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    private Exception getExceptionToThrow(ExecutionException e) {
        if (e.getCause() instanceof CacheException && e.getCause().getCause() instanceof Exception) {
            return (Exception) e.getCause().getCause();
        } else {
            /*
             * If:
             * - the cause is not a CacheException
             * - the cause is a CacheException which doesn't have a cause itself
             * - the cause is a CacheException which was caused itself by an Error
             * ... then we'll throw the original ExecutionException.
             */
            return e;
        }
    }
}
