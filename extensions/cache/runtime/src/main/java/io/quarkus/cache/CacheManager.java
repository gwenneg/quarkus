package io.quarkus.cache;

import java.util.Collection;
import java.util.Optional;

/**
 * <p>
 * Quarkus programmatic caching API entry point.
 * </p>
 * <p>
 * This API can be used to access a {@link Cache} programmatically and store, retrieve or delete cache values. It shares the
 * same caches collection than the Quarkus annotations caching API. Most {@link Cache} operations are reactive.
 * </p>
 * <p>
 * Example code:
 * 
 * <pre>
 * {@literal @}ApplicationScoped
 * public class CachedService {
 * 
 *     {@literal @}Inject
 *     CacheManager cacheManager;

 *     String getExpensiveValue(Object key) {
 *         Cache cache = cacheManager.getCache("my-cache");
 *         {@code Uni<String>} cacheValue = cache.get(key, () -> expensiveService.getValue(key));
 *         return cacheValue.await().indefinitely();
 *     }
 * }
 * </pre>
 * </p>
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
     * @param name cache name
     * @return an {@link Optional} containing the identified cache if it exists, or an empty {@link Optional} otherwise
     */
    Optional<Cache> getCache(String name);
}
