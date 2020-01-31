package io.quarkus.cache.runtime;

import static io.quarkus.cache.runtime.NullValueConverter.fromCacheValue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.jboss.logging.Logger;

import io.quarkus.cache.Cache;

@CacheResultInterceptorBinding
@Interceptor
@Priority(CacheInterceptor.BASE_PRIORITY + 2)
public class CacheResultInterceptor extends CacheInterceptor {

    private static final Logger LOGGER = Logger.getLogger(CacheResultInterceptor.class);

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        CacheResultInterceptorBinding binding = getInterceptorBinding(context, CacheResultInterceptorBinding.class);

        Cache cache = cacheManager.getCache(binding.cacheName()).get();
        Object key = getCacheKey(cache, binding.cacheKeyParameterPositions(), context.getParameters());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debugf("Loading entry with key [%s] from cache [%s]", key, binding.cacheName());
        }

        if (binding.lockTimeout() <= 0) {
            CompletionStage<Object> cacheValue = cache.get(key, () -> context.proceed());
            return cacheValue.toCompletableFuture().get();
        } else {

            // The lock timeout logic starts here.

            /*
             * If the current key is not already associated with a value in the Caffeine cache, there's no way to know if the
             * current thread or another one started the missing value computation. The following variable will be used to
             * determine whether or not a timeout should be triggered during the computation depending on which thread started
             * it.
             */
            boolean[] isCurrentThreadComputation = { false };

            CompletionStage<CompletableFuture<Object>> cacheValue = cache.get(key, () -> {
                isCurrentThreadComputation[0] = true;
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        return context.proceed();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            });
            CompletableFuture<Object> future = cacheValue.toCompletableFuture().get();

            if (isCurrentThreadComputation[0]) {
                // The value is missing and its computation was started from the current thread.
                // We'll wait for the result no matter how long it takes.
                return fromCacheValue(future.get());
            } else {
                // The value is either already present in the cache or missing and its computation was started from another thread.
                // We want to retrieve it from the cache within the lock timeout delay.
                try {
                    return fromCacheValue(future.get(binding.lockTimeout(), TimeUnit.MILLISECONDS));
                } catch (TimeoutException e) {
                    // Timeout triggered! We don't want to wait any longer for the value computation and we'll simply invoke the
                    // cached method and return its result without caching it.
                    // TODO: Add statistics here to monitor the timeout.
                    return context.proceed();
                }
            }
        }
    }
}
