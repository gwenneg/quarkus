package io.quarkus.cache.impl.caffeine;

import static io.quarkus.cache.impl.NullValueConverter.fromCacheValue;
import static io.quarkus.cache.impl.NullValueConverter.toCacheValue;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

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
        if (executor != null) {
            builder.executor(executor);
        }
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

    @SuppressWarnings("unchecked")
    private <K, V> CompletionStage<Object> getFromCaffeine(K key, Function<K, V> valueLoader) {
        // Most of the code in this method is there to prevent Caffeine from logging unwanted warnings.
        CompletableFuture<Object> cacheValue = cache.get(key, new BiFunction<Object, Executor, CompletableFuture<Object>>() {
            @Override
            public CompletableFuture<Object> apply(Object k, Executor executor) {
                // This future is needed to prevent any exception throw during the Caffeine computation.
                return CompletableFuture.supplyAsync(new Supplier<Object>() {
                    @Override
                    public Object get() {
                        try {
                            Object value = valueLoader.apply((K) k);
                            return toCacheValue(value);
                        } catch (CacheException e) {
                            throw e;
                        } catch (Exception e) {
                            throw new CacheException(e);
                        }
                    }
                }, executor).exceptionally(new Function<Throwable, Object>() {
                    public Object apply(Throwable cause) {
                        // Instead of throwing the exception, it is returned encapsulated into a normal object.
                        return new CaffeineComputationThrowable(cause);
                    }
                });
            }
        });
        // Now, it's time to rethrow any encapsulated exception or return the Caffeine computation result.
        return cacheValue.thenApply(new Function<Object, Object>() {
            @Override
            @SuppressWarnings("finally")
            public Object apply(Object value) {
                if (value instanceof CaffeineComputationThrowable) {
                    try {
                        // The cache entry needs to be removed from Caffeine explicitly (this would usually happen automatically).
                        cache.asMap().remove(key, cacheValue);
                    } finally {
                        Throwable cause = ((CaffeineComputationThrowable) value).getCause();
                        if (cause instanceof RuntimeException) {
                            throw (RuntimeException) cause;
                        } else {
                            throw new CacheException(cause);
                        }
                    }
                } else {
                    return fromCacheValue(value);
                }
            }
        });
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
}
