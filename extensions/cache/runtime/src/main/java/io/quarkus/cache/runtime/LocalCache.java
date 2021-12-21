package io.quarkus.cache.runtime;

import io.quarkus.cache.Cache;

public interface LocalCache extends Cache {

    /**
     * Removes the cache entry identified by {@code key} from the cache. If the key does not identify any cache entry, nothing
     * will happen.
     *
     * @param key cache key
     * @throws NullPointerException if the key is {@code null}
     */
    void invalidateLocal(Object key);

    /**
     * Removes all entries from the cache.
     */
    void invalidateAllLocal();
}
