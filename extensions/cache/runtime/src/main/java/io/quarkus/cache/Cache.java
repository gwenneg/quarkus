package io.quarkus.cache;

import java.util.concurrent.Callable;

import io.quarkus.cache.runtime.caffeine.CaffeineCache;
import io.smallrye.mutiny.Uni;

/**
 * This interface can be used to interact with a cache programmatically and store, retrieve or delete cache values.
 * Most of its operations are reactive.
 */
public interface Cache {

    /**
     * Returns the unique and immutable default key for the current cache. This key is used by the annotations caching API when
     * a no-args method annotated with {@link CacheResult} or {@link CacheInvalidate} is invoked. It can also be used with the
     * programmatic caching API.
     * 
     * @return default cache key
     */
    Object getDefaultKey();

    /**
     * Returns a lazy asynchronous action that will emit the cache value identified by {@code key}, obtaining that value from
     * {@code valueLoader} if necessary.
     * 
     * @param <T> cache value type
     * @param key cache key
     * @param valueLoader value loader
     * @return a lazy asynchronous action that will emit a cache value
     * @throws NullPointerException if the key is {@code null}
     */
    <T> Uni<T> get(Object key, Callable<T> valueLoader);

    /**
     * Removes the cache entry identified by {@code key} from the cache. If the key does not identify any cache entry, nothing
     * will happen.
     * 
     * @param key cache key
     * @throws NullPointerException if the key is {@code null}
     */
    Uni<Void> invalidate(Object key);

    /**
     * Removes all entries from the cache.
     */
    Uni<Void> invalidateAll();

    /**
     * Returns this cache as a {@link CaffeineCache} if possible.
     *
     * @return {@link CaffeineCache} instance
     * @throws UnsupportedOperationException if this cache is not a {@link CaffeineCache}
     */
    CaffeineCache asCaffeineCache();
}
