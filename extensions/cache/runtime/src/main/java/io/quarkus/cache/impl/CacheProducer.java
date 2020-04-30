package io.quarkus.cache.impl;

import java.lang.annotation.Annotation;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheManager;
import io.quarkus.cache.CacheName;

@ApplicationScoped
public class CacheProducer {

    @Inject
    CacheManager cacheManager;

    @Produces
    @CacheName("") // The value is @Nonbinding, its value here is not important.
    Cache produce(InjectionPoint injectionPoint) {
        for (Annotation qualifier : injectionPoint.getQualifiers()) {
            if (qualifier instanceof CacheName) {
                return cacheManager.getCache(((CacheName) qualifier).value()).get();
            }
        }
        // This will never be returned.
        return null;
    }
}
