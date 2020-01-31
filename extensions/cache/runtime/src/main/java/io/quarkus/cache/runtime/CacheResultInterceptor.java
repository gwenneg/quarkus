package io.quarkus.cache.runtime;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.jboss.logging.Logger;

import io.quarkus.cache.Cache;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;

@CacheResultInterceptorBinding
@Interceptor
@Priority(CacheInterceptor.BASE_PRIORITY + 2)
public class CacheResultInterceptor extends CacheInterceptor {

    private static final Logger LOGGER = Logger.getLogger(CacheResultInterceptor.class);

    @Inject
    CacheExecutor cacheExecutor;

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        CacheResultInterceptorBinding binding = getInterceptorBinding(context, CacheResultInterceptorBinding.class);

        Cache cache = cacheManager.getCache(binding.cacheName()).get();
        Object key = getCacheKey(cache, binding.cacheKeyParameterPositions(), context.getParameters());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debugf("Loading entry with key [%s] from cache [%s]", key, binding.cacheName());
        }

        if (binding.lockTimeout() <= 0) {
            Uni<Object> cacheValue = cache.get(key, () -> context.proceed());
            return cacheValue.await().indefinitely();
        } else {

            // The lock timeout logic starts here.

            /*
             * If the current key is not already associated with a value in the Caffeine cache, there's no way to know if the
             * current thread or another one started the missing value computation. The following variable will be used to
             * determine whether or not a timeout should be triggered during the computation depending on which thread started
             * it.
             */
            AtomicBoolean isCurrentThreadComputation = new AtomicBoolean();

            Uni<Object> cacheValue = cache.get(key, () -> {
                isCurrentThreadComputation.set(true);
                return context.proceed();
            });

            if (isCurrentThreadComputation.get()) {
                // The value is missing and its computation was started from the current thread.
                // We'll wait for the result no matter how long it takes.
                return cacheValue.await().indefinitely();
            } else {
                // The value is either already present in the cache or missing and its computation was started from another thread.
                // We want to retrieve it from the cache within the lock timeout delay.
                try {
                    return cacheValue.await().atMost(Duration.ofMillis(binding.lockTimeout()));
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
