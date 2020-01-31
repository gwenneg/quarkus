package io.quarkus.cache.runtime.caffeine;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.cache.Cache;
import io.quarkus.cache.runtime.CacheManagerImpl;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class CaffeineCacheBuildRecorder {

    private static final Logger LOGGER = Logger.getLogger(CaffeineCacheBuildRecorder.class);

    public void buildCaches(Set<CaffeineCacheInfo> cacheInfos, boolean managedExecutorInitialized, Executor defaultExecutor,
            BeanContainer beanContainer) {
        // The number of caches is known at build time so we can use fixed initialCapacity and loadFactor for the caches map.
        Map<String, Cache> caches = new HashMap<>(cacheInfos.size() + 1, 1.0F);

        Executor executor;
        if (managedExecutorInitialized) {
            executor = beanContainer.instance(ManagedExecutor.class);
        } else {
            executor = defaultExecutor;
        }

        for (CaffeineCacheInfo cacheInfo : cacheInfos) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debugf(
                        "Building Caffeine cache [%s] with [initialCapacity=%s], [maximumSize=%s], [expireAfterWrite=%s] and [expireAfterAccess=%s]",
                        cacheInfo.name, cacheInfo.initialCapacity, cacheInfo.maximumSize, cacheInfo.expireAfterWrite,
                        cacheInfo.expireAfterAccess);
            }
            CaffeineCache cache = new CaffeineCache(cacheInfo, executor);
            caches.put(cacheInfo.name, cache);
        }

        beanContainer.instance(CacheManagerImpl.class).setCaches(caches);
    }
}
