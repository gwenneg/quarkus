package io.quarkus.cache.runtime.caffeine.metrics;

import com.github.benmanes.caffeine.cache.AsyncCache;

public interface MetricsInitializer {

    void recordMetrics(AsyncCache<Object, Object> cache, String cacheName, String[] tags);
}
