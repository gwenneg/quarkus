package io.quarkus.cache.runtime.caffeine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import org.jboss.logging.Logger;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheManager;
import io.quarkus.cache.runtime.CacheManagerImpl;
import io.quarkus.cache.runtime.caffeine.metrics.MetricsInitializer;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class CaffeineCacheBuildRecorder {

    private static final Logger LOGGER = Logger.getLogger(CaffeineCacheBuildRecorder.class);

    public Supplier<CacheManager> getCacheManagerSupplier(Set<CaffeineCacheInfo> cacheInfos, boolean micrometerAvailable,
            MetricsInitializer init) {
        Objects.requireNonNull(cacheInfos);
        return new Supplier<CacheManager>() {
            @Override
            public CacheManager get() {
                if (cacheInfos.isEmpty()) {
                    return new CacheManagerImpl(Collections.emptyMap());
                } else {
                    // The number of caches is known at build time so we can use fixed initialCapacity and loadFactor for the caches map.
                    Map<String, Cache> caches = new HashMap<>(cacheInfos.size() + 1, 1.0F);
                    for (CaffeineCacheInfo cacheInfo : cacheInfos) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debugf(
                                    "Building Caffeine cache [%s] with [initialCapacity=%s], [maximumSize=%s], [expireAfterWrite=%s] and [expireAfterAccess=%s]",
                                    cacheInfo.name, cacheInfo.initialCapacity, cacheInfo.maximumSize,
                                    cacheInfo.expireAfterWrite, cacheInfo.expireAfterAccess);
                        }
                        // TODO improve
                        if (cacheInfo.metricsEnabled) {
                            if (micrometerAvailable) {
                                CaffeineCacheImpl cache = new CaffeineCacheImpl(cacheInfo, true);
                                caches.put(cacheInfo.name, cache);

                                if (cacheInfo.metricsTags == null) {
                                    CaffeineCacheMetrics.monitor(Metrics.globalRegistry, cache.cache, cacheInfo.name);
                                } else {
                                    CaffeineCacheMetrics.monitor(Metrics.globalRegistry, cache.cache, cacheInfo.name, cacheInfo.metricsTags);
                                }

                                //init.recordMetrics(cache.cache, cacheInfo.name,
                                        //cacheInfo.metricsTags);
                            } else {
                                CaffeineCacheImpl cache = new CaffeineCacheImpl(cacheInfo, false);
                                caches.put(cacheInfo.name, cache);
                                LOGGER.warnf(
                                        "Metrics won't be recorded for cache '%s' because the application does not depend on a Micrometer "
                                                + "extension. This warning can be fixed by disabling the cache metrics in the configuration or by "
                                                +
                                                "adding a Micrometer extension to the pom.xml file.",
                                        cacheInfo.name);
                            }
                        } else {
                            CaffeineCacheImpl cache = new CaffeineCacheImpl(cacheInfo, false);
                            caches.put(cacheInfo.name, cache);
                        }
                    }
                    return new CacheManagerImpl(caches);
                }
            }
        };
    }
}
