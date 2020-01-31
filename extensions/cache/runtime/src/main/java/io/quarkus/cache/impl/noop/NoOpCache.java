package io.quarkus.cache.impl.noop;

import java.util.function.Function;
import java.util.function.Supplier;

import io.quarkus.cache.impl.AbstractCache;
import io.smallrye.mutiny.Uni;

public class NoOpCache extends AbstractCache {

    @Override
    protected String getName() {
        return NoOpCache.class.getName();
    }

    @Override
    public <K, V> Uni<V> get(K key, Function<K, V> valueLoader) {
        return Uni.createFrom().item(new Supplier<V>() {
            @Override
            public V get() {
                return valueLoader.apply(key);
            }
        });
    }

    @Override
    public Uni<Void> invalidate(Object key) {
        return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<Void> invalidateAll() {
        return Uni.createFrom().voidItem();
    }
}
