package io.quarkus.cache.runtime.caffeine;

import com.github.benmanes.caffeine.cache.AsyncCache;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class CaffeineCacheMetricsInitializer {

    public static void recordMetrics(AsyncCache<Object, Object> cache, String cacheName, String[] tags) {
        if (tags == null) {
            CaffeineCacheMetrics.monitor(Metrics.globalRegistry, cache, cacheName);
        } else {
            CaffeineCacheMetrics.monitor(Metrics.globalRegistry, cache, cacheName, tags);
        }
    }
}
