package io.quarkus.cache;

import java.util.Collection;
import java.util.Optional;

/**
 * Quarkus programmatic caching API.
 */
public interface CacheManager {

    /**
     * Gets a collection of all cache names.
     * 
     * @return names of all caches
     */
    Collection<String> getCacheNames();

    /**
     * Gets the cache identified by the given name.
     * 
     * @param name cache name - must be not {@code null}
     * @return an {@link Optional} containing the identified cache if it exists, or an empty {@link Optional} otherwise
     */
    Optional<Cache> getCache(String name);
}
