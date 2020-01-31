package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheManager;
import io.quarkus.cache.CacheResult;
import io.quarkus.test.QuarkusUnitTest;

public class ProgrammaticApiTest {

    private static final String CACHE_NAME = "test-cache";
    private static final Object KEY_1 = new Object();
    private static final Object KEY_2 = new Object();

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class).addClass(CachedService.class));

    @Inject
    CachedService cachedService;

    @Inject
    CacheManager cacheManager;

    @Test
    public void test() throws Exception {

        Cache cache = cacheManager.getCache(CACHE_NAME).get();

        // STEP 1
        // Action: @CacheResult-annotated method call.
        // Expected effect: method invoked and result cached.
        // Verified by: STEP 2.
        String value1 = cachedService.cachedMethod(KEY_1);

        // STEP 2
        // Action: 
        String value2 = cache.get(KEY_1, () -> new String());

        // STEP 1
        // Actions: @CacheResult-annotated method call and programmatic 
        // Expected effect: method invoked and result cached.
        // Verified by: STEP 2.

        // d'abord on check le partage du cache entre les 2 API dans un sens
        assertTrue(value1 == value2);

        cache.invalidate(KEY_1);

        cache.invalidateAll();

        /*
         * cache.get(key, valueLoader, lockTimeout);
         * 
         * String value3 = cache.get(KEY_2, () -> new String());
         * String value4 = cachedService.cachedMethod(KEY_2, new Object());
         * assertTrue(value3 == value4);
         * 
         * // STEP 2
         * // Action: same call as STEP 1.
         * // Expected effect: method not invoked and result coming from the cache.
         * // Verified by: same object reference between STEPS 1 and 2 results.
         * String value2 = cachedService.cachedMethod(KEY_1, new Object());
         * assertTrue(value1 == value2);
         * 
         * // STEP 3
         * // Action: same call as STEP 2 with a new key.
         * // Expected effect: method invoked and result cached.
         * // Verified by: different objects references between STEPS 2 and 3 results.
         * String value3 = cachedService.cachedMethod(KEY_2, new Object());
         * assertTrue(value2 != value3);
         * 
         * // STEP 4
         * // Action: cache entry invalidation.
         * // Expected effect: STEP 2 cache entry removed.
         * // Verified by: STEP 5.
         * cache.invalidate(KEY_1);
         * //cache.invalidate(CacheKeyBuilder.build(KEY_1));
         * //cachedService.invalidate(KEY_1, new Object());
         * 
         * // STEP 5
         * // Action: same call as STEP 2.
         * // Expected effect: method invoked because of STEP 4 and result cached.
         * // Verified by: different objects references between STEPS 2 and 5 results.
         * String value5 = cachedService.cachedMethod(KEY_1, new Object());
         * assertTrue(value2 != value5);
         * 
         * // STEP 6
         * // Action: same call as STEP 3.
         * // Expected effect: method not invoked and result coming from the cache.
         * // Verified by: same object reference between STEPS 3 and 6 results.
         * String value6 = cachedService.cachedMethod(KEY_2, new Object());
         * assertTrue(value3 == value6);
         * 
         * // STEP 7
         * // Action: full cache invalidation.
         * // Expected effect: empty cache.
         * // Verified by: STEPS 8 and 9.
         * cachedService.invalidateAll();
         * 
         * // STEP 8
         * // Action: same call as STEP 5.
         * // Expected effect: method invoked because of STEP 7 and result cached.
         * // Verified by: different objects references between STEPS 5 and 8 results.
         * String value8 = cachedService.cachedMethod(KEY_1);
         * assertTrue(value5 != value8);
         * 
         * // STEP 9
         * // Action: same call as STEP 6.
         * // Expected effect: method invoked because of STEP 7 and result cached.
         * // Verified by: different objects references between STEPS 6 and 9 results.
         * String value9 = cachedService.cachedMethod(KEY_2);
         * assertTrue(value6 != value9);
         */
    }

    @Dependent
    static class CachedService {

        @CacheResult(cacheName = CACHE_NAME)
        public String cachedMethod(Object key) {
            return new String();
        }

        @CacheInvalidate(cacheName = CACHE_NAME)
        public void invalidate(Object key) {
        }

        @CacheInvalidateAll(cacheName = CACHE_NAME)
        public void invalidateAll() {
        }
    }
}
