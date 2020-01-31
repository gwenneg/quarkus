package io.quarkus.cache;

import java.util.Arrays;

// TODO: Javadoc
public class CompositeCacheKey {

    private final Object[] keyElements;

    // TODO: Javadoc
    public CompositeCacheKey(Object... keyElements) {
        if (keyElements.length < 2) {
            throw new IllegalArgumentException(
                    "At least two key elements are required to create a composite cache key instance");
        }
        this.keyElements = keyElements;
    }

    public Object[] get() {
        return keyElements;
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(keyElements);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (CompositeCacheKey.class.isInstance(obj)) {
            final CompositeCacheKey other = (CompositeCacheKey) obj;
            return Arrays.deepEquals(keyElements, other.keyElements);
        }
        return false;
    }
}
