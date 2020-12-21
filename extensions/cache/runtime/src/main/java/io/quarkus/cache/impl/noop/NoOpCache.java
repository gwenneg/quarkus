package io.quarkus.cache.impl.noop;

import java.util.function.Function;
import java.util.function.Supplier;

import io.quarkus.cache.impl.AbstractCache;
import io.smallrye.mutiny.Uni;

/**
 * This class is an internal Quarkus cache implementation. Do not use it explicitly from your Quarkus application. The public
 * methods signatures may change without prior notice.
 */
public class NoOpCache extends AbstractCache {

    private static final String NAME = NoOpCache.class.getName();

    @Override
    protected String getName() {
        return NAME;
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
