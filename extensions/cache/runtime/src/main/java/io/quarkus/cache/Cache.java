package io.quarkus.cache;

import java.util.concurrent.Callable;

import io.quarkus.cache.runtime.caffeine.CaffeineCache;

/**
 * 
 *
 */
public interface Cache {

    /**
     * Gets the unique and immutable default key for the current cache. This key is used by the annotations caching API when a
     * no-args method annotated with {@link CacheResult} or {@link CacheInvalidate} is invoked. It can also be used with the
     * programmatic caching API.
     * 
     * @return default cache key
     */
    Object getDefaultKey();

    /**
     * 
     * @param <T>
     * @param key
     * @param valueLoader
     * @return
     * @throws Exception
     */
    <T> T get(Object key, Callable<T> valueLoader) throws Exception;

    /**
     * 
     * @param <T>
     * @param key
     * @param valueLoader
     * @param lockTimeout
     * @return
     * @throws Exception
     */
    <T> T get(Object key, Callable<T> valueLoader, long lockTimeout) throws Exception;

    /**
     * 
     * @param key
     */
    void invalidate(Object key);

    /**
     * 
     */
    void invalidateAll();

    /**
     * 
     * @return
     */
    CaffeineCache asCaffeineCache();

    // TODO: Get if present API?
}
