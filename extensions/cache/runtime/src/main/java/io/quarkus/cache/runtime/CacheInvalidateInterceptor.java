package io.quarkus.cache.runtime;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.jboss.logging.Logger;

import io.quarkus.cache.Cache;

@CacheInvalidateInterceptorBinding
@Interceptor
@Priority(CacheInterceptor.BASE_PRIORITY + 1)
public class CacheInvalidateInterceptor extends CacheInterceptor {

    private static final Logger LOGGER = Logger.getLogger(CacheInvalidateInterceptor.class);

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        Object key = null;
        for (CacheInvalidateInterceptorBinding binding : getInterceptorBindings(context,
                CacheInvalidateInterceptorBinding.class)) {
            Cache cache = cacheManager.getCache(binding.cacheName()).get();
            if (key == null) {
                key = getCacheKey(cache, binding.cacheKeyParameterPositions(), context.getParameters());
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debugf("Invalidating entry with key [%s] from cache [%s]", key, binding.cacheName());
            }
            cache.invalidate(key).await().indefinitely();
        }
        return context.proceed();
    }
}
