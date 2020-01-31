package io.quarkus.cache;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;

/**
 * <p>
 * Use this annotation to inject a {@link Cache} and interact with it programmatically e.g. store, retrieve or delete cache
 * values.
 * </p>
 * <p>
 * Code example:
 * 
 * <pre>
 * {@literal @}ApplicationScoped
 * public class CachedService {
 * 
 *     {@literal @}CacheName("my-cache")
 *     Cache cache;

 *     String getExpensiveValue(Object key) {
 *         {@code Uni<String>} cacheValue = cache.get(key, () -> expensiveService.getValue(key));
 *         return cacheValue.await().indefinitely();
 *     }
 * }
 * </pre>
 * </p>
 * 
 * @see CacheManager
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ FIELD, METHOD, PARAMETER })
public @interface CacheName {

    /**
     * The name of the cache.
     */
    @Nonbinding
    String value();
}
