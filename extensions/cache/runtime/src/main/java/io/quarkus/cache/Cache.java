package io.quarkus.cache;

import java.util.concurrent.Callable;

import io.quarkus.cache.runtime.caffeine.CaffeineCache;

// TODO: Javadoc
public interface Cache {

    /**
     * Gets the default key of the current cache. This key is used by the annotations caching API for no-args methods annotated
     * with {@link CacheResult} or {@link CacheInvalidate}. It can also be used from the programmatic caching API.
     * 
     * @return default key
     */
    Object getDefaultKey();

    // TODO: Javadoc
    <T> T get(Object key, Callable<T> valueLoader) throws Exception;

    // TODO: Javadoc
    <T> T get(Object key, Callable<T> valueLoader, long lockTimeout) throws Exception;

    // TODO: Javadoc
    void invalidate(Object key);

    // TODO: Javadoc
    void invalidateAll();

    CaffeineCache asCaffeineCache();

    // TODO: Get if present API?
}
