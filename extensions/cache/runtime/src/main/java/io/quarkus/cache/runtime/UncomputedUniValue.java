package io.quarkus.cache.runtime;

import io.smallrye.mutiny.Uni;

public class UncomputedUniValue {

    private final Uni<Object> uni;

    public UncomputedUniValue(Uni<Object> uni) {
        this.uni = uni;
    }

    public Uni<Object> getUni() {
        return uni;
    }
}
