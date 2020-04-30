package io.quarkus.cache.test.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CompositeCacheKey;
import io.quarkus.cache.impl.CacheInterceptor;
import io.smallrye.mutiny.Uni;

public class CacheInterceptorTest {

    private static final Object DEFAULT_KEY = new Object();

    @Test
    public void testDefaultKey() {
        Object key = getCacheKey(new short[] {}, new Object[] {});
        assertEquals(DEFAULT_KEY, key);
    }

    @Test
    public void testExplicitSimpleKey() {
        Object expectedKey = new Object();
        Object actualKey = getCacheKey(new short[] { 1 }, new Object[] { new Object(), expectedKey });
        assertEquals(expectedKey, actualKey);
    }

    @Test
    public void testExplicitCompositeKey() {
        Object keyElement1 = new Object();
        Object keyElement2 = new Object();
        Object expectedKey = new CompositeCacheKey(keyElement1, keyElement2);
        Object actualKey = getCacheKey(new short[] { 0, 2 }, new Object[] { keyElement1, new Object(), keyElement2 });
        assertEquals(expectedKey, actualKey);
    }

    @Test
    public void testImplicitSimpleKey() {
        Object expectedKey = new Object();
        Object actualKey = getCacheKey(new short[] {}, new Object[] { expectedKey });
        assertEquals(expectedKey, actualKey);
    }

    @Test
    public void testImplicitCompositeKey() {
        Object keyElement1 = new Object();
        Object keyElement2 = new Object();
        Object expectedKey = new CompositeCacheKey(keyElement1, keyElement2);
        Object actualKey = getCacheKey(new short[] {}, new Object[] { keyElement1, keyElement2 });
        assertEquals(expectedKey, actualKey);
    }

    private Object getCacheKey(short[] cacheKeyParameterPositions, Object[] methodParameterValues) {
        return new MyCacheInterceptor().getCacheKey(new MyCache(), cacheKeyParameterPositions, methodParameterValues);
    }

    private static class MyCacheInterceptor extends CacheInterceptor {
        @Override
        protected Object getCacheKey(Cache cache, short[] cacheKeyParameterPositions, Object[] methodParameterValues) {
            return super.getCacheKey(cache, cacheKeyParameterPositions, methodParameterValues);
        }
    }

    private static class MyCache implements Cache {

        @Override
        public Object getDefaultKey() {
            return DEFAULT_KEY;
        }

        @Override
        public <T extends Cache> T asSpecializedCache(Class<T> cacheType) {
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
    }
}
