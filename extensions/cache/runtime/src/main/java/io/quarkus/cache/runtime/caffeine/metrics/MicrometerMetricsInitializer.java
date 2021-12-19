package io.quarkus.cache.runtime.caffeine.metrics;

import com.github.benmanes.caffeine.cache.AsyncCache;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;

public class MicrometerMetricsInitializer implements MetricsInitializer {

    @Override
    public boolean metricsEnabled() {
        return true;
    }

    @Override
    public void recordMetrics(AsyncCache<Object, Object> cache, String cacheName, String[] tags) {
        if (tags == null) {
            CaffeineCacheMetrics.monitor(Metrics.globalRegistry, cache, cacheName);
        } else {
            CaffeineCacheMetrics.monitor(Metrics.globalRegistry, cache, cacheName, tags);
        }
    }
}
