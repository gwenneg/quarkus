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
        // Action: value retrieval from the cache with the same key as STEP 1.
        // Expected effect: value loader not invoked.
        // Verified by: same object reference between STEPS 1 and 2 results.
        String value2 = cache.get(KEY_1, () -> new String());
        assertTrue(value1 == value2);

        // STEP 3
        // Action: value retrieval from the cache with a new key.
        // Expected effect: value loader invoked and result cached.
        // Verified by: STEP 4.
        String value3 = cache.get(KEY_2, () -> new String());
        assertTrue(value2 != value3);

        // STEP 4
        // Action: @CacheResult-annotated method call with the same key as STEP 3.
        // Expected effect: method not invoked and result coming from the cache.
        // Verified by: same object reference between STEPS 3 and 4 results.
        String value4 = cachedService.cachedMethod(KEY_2);
        assertTrue(value3 == value4);

        // STEP 5
        // Action: cache entry invalidation.
        // Expected effect: STEP 2 cache entry removed.
        // Verified by: STEP 6.
        cache.invalidate(KEY_1);

        // STEP 6
        // Action: value retrieval from the cache with the same key as STEP 2.
        // Expected effect: value loader invoked because of STEP 5 and result cached.
        // Verified by: different objects references between STEPS 2 and 6 results.
        String value6 = cache.get(KEY_1, () -> new String());
        assertTrue(value2 != value6);

        // STEP 7
        // Action: value retrieval from the cache with the same key as STEP 4.
        // Expected effect: value loader not invoked.
        // Verified by: same object reference between STEPS 4 and 7 results.
        String value7 = cache.get(KEY_2, () -> new String());
        assertTrue(value4 == value7);

        // STEP 8
        // Action: full cache invalidation.
        // Expected effect: empty cache.
        // Verified by: STEPS 9 and 10.
        cache.invalidateAll();

        // STEP 9
        // Action: same call as STEP 6.
        // Expected effect: value loader invoked because of STEP 8 and result cached.
        // Verified by: different objects references between STEPS 6 and 9 results.
        String value9 = cache.get(KEY_1, () -> new String());
        assertTrue(value6 != value9);

        // STEP 10
        // Action: same call as STEP 7.
        // Expected effect: value loader invoked because of STEP 8 and result cached.
        // Verified by: different objects references between STEPS 7 and 10 results.
        String value10 = cache.get(KEY_2, () -> new String());
        assertTrue(value7 != value10);
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
