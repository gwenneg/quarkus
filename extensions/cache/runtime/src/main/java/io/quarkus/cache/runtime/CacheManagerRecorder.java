package io.quarkus.cache.runtime;

import static io.quarkus.cache.runtime.CacheConfig.CAFFEINE_CACHE_TYPE;

import java.util.Set;
import java.util.function.Supplier;

import javax.enterprise.inject.spi.DeploymentException;

import io.quarkus.cache.CacheManager;
import io.quarkus.cache.runtime.caffeine.CaffeineCacheManagerBuilder;
import io.quarkus.cache.runtime.noop.NoOpCacheManagerBuilder;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class CacheManagerRecorder {

    private final CacheConfig cacheConfig;

    public CacheManagerRecorder(CacheConfig cacheConfig) {
        this.cacheConfig = cacheConfig;
    }

    public Supplier<CacheManager> getCacheManagerSupplier(Set<String> cacheNames, boolean micrometerSupported) {
        if (cacheConfig.enabled) {
            switch (cacheConfig.type) {
                case CAFFEINE_CACHE_TYPE:
                    return CaffeineCacheManagerBuilder.build(cacheNames, cacheConfig, micrometerSupported);
                default:
                    throw new DeploymentException("Unknown cache type: " + cacheConfig.type);
            }
        } else {
            return NoOpCacheManagerBuilder.build(cacheNames);
        }
    }
}
