package io.quarkus.cache.runtime.caffeine;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.quarkus.cache.runtime.CacheException;
import io.quarkus.cache.runtime.DefaultCacheKey;
import io.quarkus.cache.runtime.NullValueConverter;

public class CaffeineCache {

    public static final String NULL_KEYS_NOT_SUPPORTED_MSG = "Null keys are not supported by the Quarkus application data cache";

    private AsyncCache<Object, Object> cache;

    private String name;

    private Integer initialCapacity;

    private Long maximumSize;

    private Duration expireAfterWrite;

    private Duration expireAfterAccess;

    private Object defaultKey;

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

    /**
     * Returns a {@link CompletableFuture} holding the cache value identified by {@code key}, obtaining that value from
     * {@code valueLoader} if necessary. The value computation is done synchronously on the calling thread and the
     * {@link CompletableFuture} is immediately completed before being returned.
     * 
     * @param key cache key
     * @param valueLoader function used to compute a cache value if {@code key} is not already associated with a value
     * @return a {@link CompletableFuture} holding a cache value
     * @throws CacheException if an exception is thrown during a cache value computation
     */
    public CompletableFuture<Object> getWithSyncComputation(Object key, Function<Object, Object> valueLoader) {
        if (key == null) {
            throw new NullPointerException(NULL_KEYS_NOT_SUPPORTED_MSG);
        }
        CompletableFuture<Object> newCacheValue = new CompletableFuture<Object>();
        CompletableFuture<Object> existingCacheValue = cache.asMap().putIfAbsent(key, newCacheValue);
        if (existingCacheValue == null) {
            try {
                Object value = valueLoader.apply(key);
                newCacheValue.complete(NullValueConverter.toCacheValue(value));
            } catch (CacheException e) {
                newCacheValue.complete(new CaffeineComputationThrowable(e));
            }
            return rethrowThrowableOrUnwrapCacheValue(key, newCacheValue);
        } else {
            return rethrowThrowableOrUnwrapCacheValue(key, existingCacheValue);
        }
    }

    /**
     * Returns a {@link CompletableFuture} holding the cache value identified by {@code key}, obtaining that value from
     * {@code valueLoader} if necessary. The value computation is done asynchronously using a thread from the Quarkus
     * {@link ManagedExecutor} instance if available, or {@link java.util.concurrent.ForkJoinPool#commonPool()
     * ForkJoinPool.commonPool()} otherwise.
     * 
     * @param key cache key
     * @param valueLoader function used to compute a cache value if {@code key} is not already associated with a value
     * @return a {@link CompletableFuture} holding a cache value
     * @throws CacheException if an exception is thrown during a cache value computation
     */
    public CompletableFuture<Object> getWithAsyncComputation(Object key, Function<Object, Object> valueLoader) {
        if (key == null) {
            throw new NullPointerException(NULL_KEYS_NOT_SUPPORTED_MSG);
        }
        CompletableFuture<Object> cacheValue = cache.get(key, new BiFunction<Object, Executor, CompletableFuture<Object>>() {
            @Override
            public CompletableFuture<Object> apply(Object k, Executor executor) {
                return CompletableFuture.supplyAsync(new Supplier<Object>() {
                    @Override
                    public Object get() {
                        Object value = valueLoader.apply(key);
                        return NullValueConverter.toCacheValue(value);
                    }
                }, executor).exceptionally(new Function<Throwable, Object>() {
                    @Override
                    public Object apply(Throwable cause) {
                        // This is required to prevent Caffeine from logging unwanted warnings.
                        return new CaffeineComputationThrowable(cause);
                    }
                });
            }
        });
        return rethrowThrowableOrUnwrapCacheValue(key, cacheValue);
    }

    private CompletableFuture<Object> rethrowThrowableOrUnwrapCacheValue(Object key, CompletableFuture<Object> cacheValue) {
        return cacheValue.thenApply(new Function<Object, Object>() {
            @Override
            @SuppressWarnings("finally")
            public Object apply(Object value) {
                // If there's a throwable encapsulated into a CaffeineComputationThrowable, it must be rethrown.
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
                    return NullValueConverter.fromCacheValue(value);
                }
            }
        });
    }

    public void invalidate(Object key) {
        if (key == null) {
            throw new NullPointerException(NULL_KEYS_NOT_SUPPORTED_MSG);
        }
        cache.synchronous().invalidate(key);
    }

    public void invalidateAll() {
        cache.synchronous().invalidateAll();
    }

    public String getName() {
        return name;
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

    /**
     * Returns the unique and immutable default key for the current cache. This key is used by the annotations caching API when
     * a no-args method annotated with {@link io.quarkus.cache.CacheResult CacheResult} or
     * {@link io.quarkus.cache.CacheInvalidate CacheInvalidate} is invoked.
     * 
     * @return default cache key
     */
    public Object getDefaultKey() {
        if (defaultKey == null) {
            defaultKey = new DefaultCacheKey(getName());
        }
        return defaultKey;
    }
}
