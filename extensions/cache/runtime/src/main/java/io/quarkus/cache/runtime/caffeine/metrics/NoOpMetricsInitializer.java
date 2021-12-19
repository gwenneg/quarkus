package io.quarkus.cache.runtime.caffeine.metrics;

import com.github.benmanes.caffeine.cache.AsyncCache;

public class NoOpMetricsInitializer implements MetricsInitializer {

    @Override
    public void recordMetrics(AsyncCache<Object, Object> cache, String cacheName, String[] tags) {
        // No op.
    }
}
