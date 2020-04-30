package io.quarkus.cache.impl;

import java.util.Objects;

import io.quarkus.cache.Cache;

public abstract class AbstractCache implements Cache {

    public static final String NULL_KEYS_NOT_SUPPORTED_MSG = "Null keys are not supported by the Quarkus application data cache";

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
    @SuppressWarnings("unchecked")
    public <T extends Cache> T asSpecializedCache(Class<T> type) {
        if (type.isInstance(this)) {
            return (T) this;
        } else {
            throw new IllegalStateException("This cache is not an instance of " + type.getName());
        }
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
