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
            // If the method doesn't have any parameter, then a unique default key is generated and used.
            return cache.getDefaultKey();
        } else if (cacheKeyParameterPositions.length == 1) {
            // TODO: Comment
            Object key = methodParameterValues[cacheKeyParameterPositions[0]];
            return Objects.requireNonNull(key, NULL_KEYS_NOT_SUPPORTED_MSG);
        } else if (cacheKeyParameterPositions.length >= 2) {
            // If at least two of the method parameters are annotated with @CacheKey, then the key is composed of all
            // @CacheKey-annotated parameters that were identified at build time.
            List<Object> keyElements = new ArrayList<>();
            for (int i = 0; i < cacheKeyParameterPositions.length; i++) {
                keyElements.add(methodParameterValues[cacheKeyParameterPositions[i]]);
            }
            return new CompositeCacheKey(keyElements.toArray(new Object[0]));
        } else if (methodParameterValues.length == 1) {
            // TODO: Comment
            Object key = methodParameterValues[0];
            return Objects.requireNonNull(key, NULL_KEYS_NOT_SUPPORTED_MSG);
        } else {
            // Otherwise, the key is composed of all of the method parameters.
            return new CompositeCacheKey(methodParameterValues);
        }
    }
}
