package io.quarkus.cache;

import java.util.Collection;

// TODO: Javadoc
public interface CacheManager {

    // TODO: Javadoc
    Collection<String> getCacheNames();

    // TODO: Javadoc
    Cache getCache(String name);
}
