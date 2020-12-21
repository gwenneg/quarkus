package io.quarkus.cache.impl;

import java.time.Duration;
import java.util.function.Function;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.jboss.logging.Logger;

import io.quarkus.cache.CacheException;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;

@CacheResultInterceptorBinding
@Interceptor
@Priority(CacheInterceptor.BASE_PRIORITY + 2)
public class CacheResultInterceptor extends CacheInterceptor {

    private static final Logger LOGGER = Logger.getLogger(CacheResultInterceptor.class);

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        CacheResultInterceptorBinding binding = getInterceptorBinding(context, CacheResultInterceptorBinding.class);

        AbstractCache cache = (AbstractCache) cacheManager.getCache(binding.cacheName()).get();
        short[] cacheKeyParameterPositions = getCacheKeyParameterPositions(context);
        Object key = getCacheKey(cache, cacheKeyParameterPositions, context.getParameters());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debugf("Loading entry with key [%s] from cache [%s]", key, binding.cacheName());
        }

        try {

            Uni<Object> cacheValue = cache.get(key, new Function<Object, Object>() {
                @Override
                public Object apply(Object t) {
                    try {
                        return context.proceed();
                    } catch (Exception e) {
                        throw new CacheException(e);
                    }
                }
            });

            if (binding.lockTimeout() <= 0) {
                return cacheValue.await().indefinitely();
            } else {
                try {
                    /*
                     * If the current thread started the cache value computation, then the computation is already finished since
                     * it was done synchronously and the following call will never time out.
                     */
                    return cacheValue.await().atMost(Duration.ofMillis(binding.lockTimeout()));
                } catch (TimeoutException e) {
                    // TODO: Add statistics here to monitor the timeout.
                    return context.proceed();
                }
            }

        } catch (CacheException e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            } else {
                throw e;
            }
        }
    }
}
