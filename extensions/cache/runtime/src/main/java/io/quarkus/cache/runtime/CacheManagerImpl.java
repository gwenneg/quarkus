package io.quarkus.cache.runtime;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheManager;

@ApplicationScoped
public class CacheManagerImpl implements CacheManager {

    // There's no need for concurrency here since the map is created at build time and never modified after that.
    private Map<String, Cache> caches;
    private Set<String> cacheNames;

    public void setCaches(Map<String, Cache> caches) {
        if (this.caches != null) {
            throw new IllegalStateException("The caches map must only be set once at build time");
        }
        this.caches = Collections.unmodifiableMap(caches);
        this.cacheNames = Collections.unmodifiableSet(caches.keySet());
    }

    @Override
    public Set<String> getCacheNames() {
        return this.cacheNames;
    }

    @Override
    public Cache getCache(String name) {
        return caches.get(name);
    }
}
