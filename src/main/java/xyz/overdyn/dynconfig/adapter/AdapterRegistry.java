package xyz.overdyn.dynconfig.adapter;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for {@link ConfigAdapter} instances.
 *
 * <p>The registry maps Java types to adapters responsible for converting
 * instances of those types to and from configuration values.</p>
 */
public final class AdapterRegistry {

    private static final Map<Class<?>, ConfigAdapter<?>> ADAPTERS = new HashMap<>();

    private AdapterRegistry() {
    }

    /**
     * Registers an adapter for the given type.
     *
     * @param type    target Java type
     * @param adapter adapter implementation
     * @param <T>     generic type parameter
     */
    public static <T> void register(Class<T> type, ConfigAdapter<T> adapter) {
        ADAPTERS.put(type, adapter);
    }

    /**
     * Retrieves the adapter for the given type if present.
     *
     * @param type target Java type
     * @param <T>  generic type parameter
     * @return adapter instance or {@code null} if none is registered
     */
    @SuppressWarnings("unchecked")
    public static <T> @Nullable ConfigAdapter<T> get(Class<T> type) {
        return (ConfigAdapter<T>) ADAPTERS.get(type);
    }
}
