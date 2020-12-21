package io.quarkus.cache.impl.caffeine;

import static io.quarkus.cache.impl.NullValueConverter.fromCacheValue;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.quarkus.cache.CacheException;
import io.quarkus.cache.impl.AbstractCache;
import io.quarkus.cache.impl.NullValueConverter;
import io.smallrye.mutiny.Uni;

/**
 * This class is an internal Quarkus cache implementation. Do not use it explicitly from your Quarkus application. The public
 * methods signatures may change without prior notice.
 */
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
    public <K, V> Uni<V> get(K key, Function<K, V> valueLoader) {
        Objects.requireNonNull(key, NULL_KEYS_NOT_SUPPORTED_MSG);
        // We need to defer the CompletionStage eager computation.
        return Uni.createFrom().deferred(new Supplier<Uni<? extends V>>() {
            @Override
            public Uni<? extends V> get() {
                CompletionStage<Object> caffeineValue = getFromCaffeine(key, valueLoader);
                return Uni.createFrom().completionStage(caffeineValue).map(new Function<Object, V>() {
                    @Override
                    public V apply(Object cacheValue) {
                        return cast(cacheValue);
                    }
                });
            }
        });
    }

    /**
     * Returns a {@link CompletableFuture} holding the cache value identified by {@code key}, obtaining that value from
     * {@code valueLoader} if necessary. The value computation is done synchronously on the calling thread and the
     * {@link CompletableFuture} is immediately completed before being returned.
     * 
     * @param key cache key
     * @param valueLoader function used to compute the cache value if {@code key} is not already associated with a value
     * @return a {@link CompletableFuture} holding the cache value
     * @throws CacheException if an exception is thrown during the cache value computation
     */
    public <K, V> CompletableFuture<Object> getFromCaffeine(K key, Function<K, V> valueLoader) {
        if (key == null) {
            throw new NullPointerException(NULL_KEYS_NOT_SUPPORTED_MSG);
        }
        CompletableFuture<Object> newCacheValue = new CompletableFuture<>();
        CompletableFuture<Object> existingCacheValue = cache.asMap().putIfAbsent(key, newCacheValue);
        if (existingCacheValue == null) {
            try {
                Object value = valueLoader.apply(key);
                newCacheValue.complete(NullValueConverter.toCacheValue(value));
            } catch (Throwable t) {
                cache.asMap().remove(key, newCacheValue);
                newCacheValue.complete(new CaffeineComputationThrowable(t));
            }
            return unwrapCacheValueOrThrowable(newCacheValue);
        } else {
            return unwrapCacheValueOrThrowable(existingCacheValue);
        }
    }

    private CompletableFuture<Object> unwrapCacheValueOrThrowable(CompletableFuture<Object> cacheValue) {
        return cacheValue.thenApply(new Function<Object, Object>() {
            @Override
            public Object apply(Object value) {
                // If there's a throwable encapsulated into a CaffeineComputationThrowable, it must be rethrown.
                if (value instanceof CaffeineComputationThrowable) {
                    Throwable cause = ((CaffeineComputationThrowable) value).getCause();
                    if (cause instanceof RuntimeException) {
                        throw (RuntimeException) cause;
                    } else {
                        throw new CacheException(cause);
                    }
                } else {
                    return NullValueConverter.fromCacheValue(value);
                }
            }
        });
    }

    @Override
    public Uni<Void> invalidate(Object key) {
        Objects.requireNonNull(key, NULL_KEYS_NOT_SUPPORTED_MSG);
        return Uni.createFrom().item(new Supplier<Void>() {
            @Override
            public Void get() {
                cache.synchronous().invalidate(key);
                return null;
            }
        });
    }

    @Override
    public Uni<Void> invalidateAll() {
        return Uni.createFrom().item(new Supplier<Void>() {
            @Override
            public Void get() {
                cache.synchronous().invalidateAll();
                return null;
            }
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
                    "An existing cached value type does not match the type returned by the value loading function", e);
        }
    }
}
