package io.quarkus.cache.runtime.caffeine;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.quarkus.cache.runtime.AbstractCache;
import io.quarkus.cache.runtime.NullValueConverter;
import io.smallrye.mutiny.Uni;

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
    public <T> Uni<T> get(Object key, Callable<T> valueLoader) {
        if (key == null) {
            throw new NullPointerException(NULL_KEYS_NOT_SUPPORTED_MSG);
        }
        CompletionStage<T> cacheValue = cache.get(key, unused -> new MappingSupplier(valueLoader).get())
                .thenApply(value -> cast(NullValueConverter.fromCacheValue(value)));
        return Uni.createFrom().completionStage(cacheValue);
    }

    @Override
    public Uni<Void> invalidate(Object key) {
        if (key == null) {
            throw new NullPointerException(NULL_KEYS_NOT_SUPPORTED_MSG);
        }
        return Uni.createFrom().completionStage(CompletableFuture.runAsync(() -> cache.synchronous().invalidate(key)));
    }

    @Override
    public Uni<Void> invalidateAll() {
        return Uni.createFrom().completionStage(CompletableFuture.runAsync(() -> cache.synchronous().invalidateAll()));
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

    @SuppressWarnings("unchecked")
    private <T> T cast(Object value) {
        try {
            return (T) value;
        } catch (ClassCastException e) {
            throw new IllegalStateException(
                    "An existing cached value type does not match the type returned by the loading function", e);
        }
    }

    private static class MappingSupplier implements Supplier<Object> {

        private final Callable<?> valueLoader;

        public MappingSupplier(Callable<?> valueLoader) {
            this.valueLoader = valueLoader;
        }

        @Override
        public Object get() {
            try {
                return NullValueConverter.toCacheValue(valueLoader.call());
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
