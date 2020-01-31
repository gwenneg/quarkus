package io.quarkus.cache.runtime.caffeine;

import static io.quarkus.cache.runtime.NullValueConverter.fromCacheValue;
import static io.quarkus.cache.runtime.NullValueConverter.toCacheValue;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.quarkus.cache.runtime.AbstractCache;

public class CaffeineCache extends AbstractCache {

    private AsyncCache<Object, Object> cache;

    private String name;

    private Integer initialCapacity;

    private Long maximumSize;

    private Duration expireAfterWrite;

    private Duration expireAfterAccess;

    public CaffeineCache(CaffeineCacheInfo cacheInfo) {
        this.name = cacheInfo.name;
        Caffeine<Object, Object> builder = Caffeine.newBuilder();

        if (cacheInfo.initialCapacity != null) {
            this.initialCapacity = cacheInfo.initialCapacity;
            builder.initialCapacity(cacheInfo.initialCapacity);
        }
        if (cacheInfo.maximumSize != null) {
            this.maximumSize = cacheInfo.maximumSize;
            builder.maximumSize(cacheInfo.maximumSize);
        }
        if (cacheInfo.expireAfterWrite != null) {
            this.expireAfterWrite = cacheInfo.expireAfterWrite;
            builder.expireAfterWrite(cacheInfo.expireAfterWrite);
        }
        if (cacheInfo.expireAfterAccess != null) {
            this.expireAfterAccess = cacheInfo.expireAfterAccess;
            builder.expireAfterAccess(cacheInfo.expireAfterAccess);
        }
        cache = builder.buildAsync();
    }

    @Override
    protected String getName() {
        return name;
    }

    @Override
    public CaffeineCache asCaffeineCache() {
        return (CaffeineCache) this;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) throws Exception {
        return get(key, valueLoader, 0L);
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader, long lockTimeout) throws Exception {
        Object value = null;

        if (lockTimeout <= 0) {
            value = fromCacheValue(cache.synchronous().get(key, k -> new MappingSupplier(valueLoader).get()));
        } else {

            // The lock timeout logic starts here.

            /*
             * If the current key is not already associated with a value in the Caffeine cache, there's no way to know if the
             * current thread or another one started the missing value computation. The following variable will be used to
             * determine whether or not a timeout should be triggered during the computation depending on which thread started
             * it.
             */
            boolean[] isCurrentThreadComputation = { false };

            CompletableFuture<Object> future = cache.get(key, (k, executor) -> {
                isCurrentThreadComputation[0] = true;
                return CompletableFuture.supplyAsync(new MappingSupplier(valueLoader), executor);
            });

            if (isCurrentThreadComputation[0]) {
                // The value is missing and its computation was started from the current thread.
                // We'll wait for the result no matter how long it takes.
                value = fromCacheValue(future.get());
            } else {
                // The value is either already present in the cache or missing and its computation was started from another thread.
                // We want to retrieve it from the cache within the lock timeout delay.
                try {
                    value = fromCacheValue(future.get(lockTimeout, TimeUnit.MILLISECONDS));
                } catch (TimeoutException e) {
                    // Timeout triggered! We don't want to wait any longer for the value computation and we'll simply invoke the
                    // cached method and return its result without caching it.
                    // TODO: Add statistics here to monitor the timeout.
                    return valueLoader.call();
                }
            }

        }

        return cast(value);
    }

    @SuppressWarnings("unchecked")
    private <T> T cast(Object value) {
        try {
            return (T) value;
        } catch (ClassCastException e) {
            throw new IllegalStateException(
                    "An existing cached value type does not match the type returned by the loading function", e);
        }
    }

    @Override
    public void invalidate(Object key) {
        cache.synchronous().invalidate(key);
    }

    @Override
    public void invalidateAll() {
        cache.synchronous().invalidateAll();
    }

    // For testing purposes only.
    public Integer getInitialCapacity() {
        return initialCapacity;
    }

    // For testing purposes only.
    public Long getMaximumSize() {
        return maximumSize;
    }

    // For testing purposes only.
    public Duration getExpireAfterWrite() {
        return expireAfterWrite;
    }

    // For testing purposes only.
    public Duration getExpireAfterAccess() {
        return expireAfterAccess;
    }

    private static class MappingSupplier implements Supplier<Object> {

        private final Callable<?> valueLoader;

        public MappingSupplier(Callable<?> valueLoader) {
            this.valueLoader = valueLoader;
        }

        @Override
        public Object get() {
            try {
                return toCacheValue(valueLoader.call());
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
