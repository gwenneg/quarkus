package io.quarkus.cache.impl.caffeine;

import static io.quarkus.cache.impl.NullValueConverter.fromCacheValue;
import static io.quarkus.cache.impl.NullValueConverter.toCacheValue;

import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.quarkus.cache.CacheException;
import io.quarkus.cache.impl.AbstractCache;
import io.smallrye.mutiny.Uni;

public class CaffeineCache extends AbstractCache {

    private AsyncCache<Object, Object> cache;

    private String name;

    private Integer initialCapacity;

    private Long maximumSize;

    private Duration expireAfterWrite;

    private Duration expireAfterAccess;

    public CaffeineCache(CaffeineCacheInfo cacheInfo, Executor executor) {
        this.name = cacheInfo.name;
        Caffeine<Object, Object> builder = Caffeine.newBuilder();
        builder.executor(executor);
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
    public <K, V> Uni<V> get(K key, Function<K, V> valueLoader) {
        if (key == null) {
            throw new NullPointerException(NULL_KEYS_NOT_SUPPORTED_MSG);
        }
        return Uni.createFrom().deferred(() -> {
            @SuppressWarnings("unchecked")
            CompletionStage<Object> cacheValueCompletionStage = cache.get(key, k -> {
                try {
                    return toCacheValue(valueLoader.apply((K) k));
                } catch (Exception e) {
                    throw new CacheException(e);
                }
            });
            return Uni.createFrom().completionStage(cacheValueCompletionStage).map(cacheValue -> {
                return cast(fromCacheValue(cacheValue));
            });
        });
    }

    @Override
    public Uni<Void> invalidate(Object key) {
        if (key == null) {
            throw new NullPointerException(NULL_KEYS_NOT_SUPPORTED_MSG);
        }
        return Uni.createFrom().item(() -> {
            cache.synchronous().invalidate(key);
            return (Void) null;
        });
    }

    @Override
    public Uni<Void> invalidateAll() {
        return Uni.createFrom().item(() -> {
            cache.synchronous().invalidateAll();
            return (Void) null;
        });
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
            throw new CacheException(
                    "An existing cached value type does not match the type returned by the loading function", e);
        }
    }
}
