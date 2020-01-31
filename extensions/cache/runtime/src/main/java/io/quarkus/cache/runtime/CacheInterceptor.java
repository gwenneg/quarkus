package io.quarkus.cache.runtime;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.interceptor.Interceptor.Priority;
import javax.interceptor.InvocationContext;

import io.quarkus.arc.runtime.InterceptorBindings;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheManager;
import io.quarkus.cache.CompositeCacheKey;

public abstract class CacheInterceptor {

    public static final int BASE_PRIORITY = Priority.PLATFORM_BEFORE;
    public static final String NULL_KEYS_NOT_SUPPORTED_MSG = "Null keys are not supported by the Quarkus application data cache";

    @Inject
    CacheManager cacheManager;

    @SuppressWarnings("unchecked")
    protected <T> List<T> getInterceptorBindings(InvocationContext context, Class<T> bindingClass) {
        List<T> bindings = new ArrayList<>();
        for (Annotation binding : InterceptorBindings.getInterceptorBindings(context)) {
            if (bindingClass.isInstance(binding)) {
                bindings.add((T) binding);
            }
        }
        return bindings;
    }

    protected <T> T getInterceptorBinding(InvocationContext context, Class<T> bindingClass) {
        return getInterceptorBindings(context, bindingClass).get(0);
    }

    protected Object getCacheKey(Cache cache, short[] cacheKeyParameterPositions, Object[] methodParameterValues) {
        if (methodParameterValues.length == 0) {
            // If the intercepted method doesn't have any parameter, then the default cache key will be used.
            return cache.getDefaultKey();
        } else if (cacheKeyParameterPositions.length == 1) {
            // If exactly one @CacheKey-annotated parameter was identified for the intercepted method at build time, then this
            // parameter will be used as the cache key.
            Object key = methodParameterValues[cacheKeyParameterPositions[0]];
            return Objects.requireNonNull(key, NULL_KEYS_NOT_SUPPORTED_MSG);
        } else if (cacheKeyParameterPositions.length >= 2) {
            // If two or more @CacheKey-annotated parameters were identified for the intercepted method at build time, then a
            // composite cache key built from all these parameters will be used.
            List<Object> keyElements = new ArrayList<>();
            for (int i = 0; i < cacheKeyParameterPositions.length; i++) {
                keyElements.add(methodParameterValues[cacheKeyParameterPositions[i]]);
            }
            return new CompositeCacheKey(keyElements.toArray(new Object[0]));
        } else if (methodParameterValues.length == 1) {
            // If the intercepted method has exactly one parameter, then this parameter will be used as the cache key.
            Object key = methodParameterValues[0];
            return Objects.requireNonNull(key, NULL_KEYS_NOT_SUPPORTED_MSG);
        } else {
            // If the intercepted method has two or more parameters, then a composite cache key built from all these parameters
            // will be used.
            return new CompositeCacheKey(methodParameterValues);
        }
    }
}
