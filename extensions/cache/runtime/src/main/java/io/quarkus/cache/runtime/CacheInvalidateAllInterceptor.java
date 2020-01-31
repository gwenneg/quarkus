package io.quarkus.cache.runtime;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.jboss.logging.Logger;

import io.quarkus.cache.Cache;

@CacheInvalidateAllInterceptorBinding
@Interceptor
@Priority(CacheInterceptor.BASE_PRIORITY)
public class CacheInvalidateAllInterceptor extends CacheInterceptor {

    private static final Logger LOGGER = Logger.getLogger(CacheInvalidateAllInterceptor.class);

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        for (CacheInvalidateAllInterceptorBinding binding : getInterceptorBindings(context,
                CacheInvalidateAllInterceptorBinding.class)) {
            Cache cache = cacheManager.getCache(binding.cacheName());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debugf("Invalidating all entries from cache [%s]", binding.cacheName());
            }
            cache.invalidateAll();
        }
        return context.proceed();
    }
}
