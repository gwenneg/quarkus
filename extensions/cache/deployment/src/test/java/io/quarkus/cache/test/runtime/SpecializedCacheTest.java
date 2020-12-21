package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheManager;
import io.quarkus.cache.CacheResult;
import io.quarkus.cache.impl.caffeine.CaffeineCache;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class SpecializedCacheTest {

    private static final String CACHE_NAME = "test-cache";

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class).addClass(CachedService.class));

    @Inject
    CachedService cachedService;

    @Inject
    CacheManager cacheManager;

    @Test
    public void testSuccessfulAsSpecializedCache() {
        cacheManager.getCache(CACHE_NAME).get().asSpecializedCache(CaffeineCache.class);
    }

    @Test
    public void testFailedAsSpecializedCache() {
        assertThrows(IllegalStateException.class, () -> {
            cacheManager.getCache(CACHE_NAME).get().asSpecializedCache(MyCache.class);
        });
    }

    @Singleton
    static class CachedService {

        @CacheResult(cacheName = CACHE_NAME)
        public Object cachedMethod(Object key) {
            return new Object();
        }
    }

    static class MyCache implements Cache {

        @Override
        public Object getDefaultKey() {
            throw new UnsupportedOperationException("This method is not tested here");
        }

        @Override
        public <K, V> Uni<V> get(K key, Function<K, V> valueLoader) {
            throw new UnsupportedOperationException("This method is not tested here");
        }

        @Override
        public Uni<Void> invalidate(Object key) {
            throw new UnsupportedOperationException("This method is not tested here");
        }

        @Override
        public Uni<Void> invalidateAll() {
            throw new UnsupportedOperationException("This method is not tested here");
        }

        @Override
        public <T extends Cache> T asSpecializedCache(Class<T> cacheType) {
            throw new UnsupportedOperationException("This method is not tested here");
        }
    }
}
