package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.quarkus.cache.CompositeCacheKey;

public class CompositeCacheKeyTest {

    private static final String KEY_ELEMENT_1 = "test";
    private static final long KEY_ELEMENT_2 = 123L;

    @Test
    public void testEquality() {

        // Two composite keys built from the same elements and in the same order should be equal.
        CompositeCacheKey key1 = new CompositeCacheKey(KEY_ELEMENT_1, KEY_ELEMENT_2);
        CompositeCacheKey key2 = new CompositeCacheKey(KEY_ELEMENT_1, KEY_ELEMENT_2);
        assertEquals(key1, key2);
        assertEquals(key1.hashCode(), key2.hashCode());

        // If the same elements are used but their order is changed, the keys should not be equal.
        CompositeCacheKey key3 = new CompositeCacheKey(KEY_ELEMENT_2, KEY_ELEMENT_1);
        assertNotEquals(key2, key3);

        // If at least one element is different, the keys should not be equal.
        CompositeCacheKey key4 = new CompositeCacheKey(KEY_ELEMENT_1, 456L);
        assertNotEquals(key2, key4);

        // If the elements numbers are different, the keys should not be equal.
        CompositeCacheKey key5 = new CompositeCacheKey(KEY_ELEMENT_1, KEY_ELEMENT_2, new Object());
        assertNotEquals(key2, key5);
    }

    @Test
    public void testInvalidKey() {

        assertThrows(IllegalArgumentException.class, () -> {
            new CompositeCacheKey();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new CompositeCacheKey("One key element is not enough!");
        });
    }
}
