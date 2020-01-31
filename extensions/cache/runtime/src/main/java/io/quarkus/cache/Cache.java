package io.quarkus.cache;

import java.util.concurrent.Callable;

import io.quarkus.cache.runtime.caffeine.CaffeineCache;
import io.smallrye.mutiny.Uni;

/**
 * TODO
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
     * TODO
     * 
     * @param <T>
     * @param key
     * @param valueLoader
     * @return
     */
    <T> Uni<T> get(Object key, Callable<T> valueLoader);

    /**
     * TODO
     * 
     * @param key
     */
    Uni<Void> invalidate(Object key);

    /**
     * TODO
     */
    Uni<Void> invalidateAll();

    /**
     * TODO
     * 
     * @return
     */
    CaffeineCache asCaffeineCache();
}
