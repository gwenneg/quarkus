package io.quarkus.cache.runtime;

import java.util.Objects;

import io.quarkus.cache.Cache;
import io.quarkus.cache.runtime.caffeine.CaffeineCache;

public abstract class AbstractCache implements Cache {

    private Object defaultKey;

    protected abstract String getName();

    @Override
    public Object getDefaultKey() {
        if (defaultKey == null) {
            defaultKey = new DefaultCacheKey(getName());
        }
        return defaultKey;
    }

    @Override
    public CaffeineCache asCaffeineCache() {
        throw new IllegalArgumentException("This cache is not a Caffeine cache");
    }

    private static class DefaultCacheKey {

        private final String cacheName;

        public DefaultCacheKey(String cacheName) {
            this.cacheName = cacheName;
        }

        @Override
        public int hashCode() {
            return Objects.hash(cacheName);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof DefaultCacheKey) {
                DefaultCacheKey other = (DefaultCacheKey) obj;
                return Objects.equals(cacheName, other.cacheName);
            }
            return false;
        }
    }
}
