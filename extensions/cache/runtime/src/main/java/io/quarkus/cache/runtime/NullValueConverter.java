package io.quarkus.cache.runtime;

/**
 * This class is used to allow the storage of {@code null} values in the Quarkus cache while it is forbidden by the underlying
 * caching provider.
 */
public class NullValueConverter {

    private static final class NullValue {
        public static Object INSTANCE = new NullValue();
    }

    public static Object toCacheValue(Object value) {
        return value == null ? NullValue.INSTANCE : value;
    }

    public static Object fromCacheValue(Object value) {
        return value == NullValue.INSTANCE ? null : value;
    }
}

// https://github.com/spring-projects/spring-framework/blob/master/spring-context/src/main/java/org/springframework/cache/support/AbstractValueAdaptingCache.java
// https://github.com/spring-projects/spring-framework/blob/master/spring-context-support/src/main/java/org/springframework/cache/caffeine/CaffeineCache.java
