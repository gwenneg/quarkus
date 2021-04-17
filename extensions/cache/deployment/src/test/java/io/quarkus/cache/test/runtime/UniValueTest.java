package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.CacheResult;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

/**
 * Tests the {@link CacheResult} annotation on methods returning {@link Uni}.
 */
public class UniValueTest {

    private static final String KEY = "key";

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(CachedService.class).addAsResource(
                    new StringAsset("quarkus.log.category.\"io.quarkus.cache\".level=DEBUG"), "application.properties"));

    @Inject
    CachedService cachedService;

    @Test
    public void test() {
         // STEP 1
         // Action: a method annotated with @CacheResult and returning a Uni is called.
         // Expected effect: the method is invoked and its result (a Uni) is wrapped into an UncomputedUniValue which is cached.
         // Verified by: invocations counter and CacheResultInterceptor log.
        Uni<String> uni1 = cachedService.cachedMethod(KEY);
        assertEquals(1, cachedService.getInvocations());

         // STEP 2
         // Action: same call as STEP 1.
         // Expected effect: the method is not invoked and the cached UncomputedUniValue from STEP 1 is used to produce a result.
         // Verified by: invocations counter and CacheResultInterceptor log.
        Uni<String> uni2 = cachedService.cachedMethod(KEY);
        assertEquals(1, cachedService.getInvocations());

         // STEP 3
         // Action: the Uni returned in STEP 1 is subscribed to and we wait for an item event to be fired.
         // Expected effect: the UncomputedUniValue wrapper cached during STEP 1 is replaced with the emitted item from this step in the cache.
         // Verified by: CaffeineCache log.
        String emittedItem1 = uni1.await().indefinitely();

         // STEP 4
         // Action: the Uni returned in STEP 2 is subscribed to and we wait for an item event to be fired.
         // Expected effect: the emitted item from STEP 3 is replaced with the emitted item from this step in the cache.
         // Verified by: CaffeineCache log and different objects references between STEPS 3 and 4 emitted items.
        String emittedItem2 = uni2.await().indefinitely();
        assertTrue(emittedItem1 != emittedItem2);

         // STEP 5
         // Action: same call as STEP 2 but we immediately subscribe to the returned Uni and wait for an item event to be fired.
         // Expected effect: the method is not invoked and the emitted item cached during STEP 4 is returned.
         // Verified by: invocations counter and same object reference between STEPS 4 and 5 emitted items.
        String emittedItem3 = cachedService.cachedMethod(KEY).await().indefinitely();
        assertEquals(1, cachedService.getInvocations());
        assertTrue(emittedItem2 == emittedItem3);

         // STEP 6
         // Action: same call as STEP 5 with a different key.
         // Expected effect: the method is invoked and its result (a Uni) is wrapped into an UncomputedUniValue which is cached.
         // Verified by: invocations counter, CacheResultInterceptor log and different objects references between STEPS 5 and 6 emitted items.
        String emittedItem4 = cachedService.cachedMethod("another-key").await().indefinitely();
        assertEquals(2, cachedService.getInvocations());
        assertTrue(emittedItem3 != emittedItem4);
    }

    @ApplicationScoped
    static class CachedService {

        private int invocations;

        @CacheResult(cacheName = "test-cache")
        public Uni<String> cachedMethod(String key) {
            invocations++;
            return Uni.createFrom().item(() -> new String());
        }

        public int getInvocations() {
            return invocations;
        }
    }
}
