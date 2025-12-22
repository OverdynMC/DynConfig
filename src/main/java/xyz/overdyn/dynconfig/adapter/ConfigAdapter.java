package xyz.overdyn.dynconfig.adapter;

/**
 * Adapter interface that defines how a specific type is read from and written
 * to configuration values.
 *
 * <p>An adapter converts between raw configuration representations and
 * strongly typed Java objects.</p>
 *
 * @param <T> target Java type
 */
public interface ConfigAdapter<T> {

    /**
     * Converts a raw configuration value into a strongly typed Java object.
     *
     * @param raw raw value obtained from configuration
     * @return converted value
     * @throws IllegalArgumentException if the raw value cannot be converted
     */
    T fromConfig(Object raw) throws IllegalArgumentException;

    /**
     * Converts a strongly typed Java object into a value that can be stored in
     * the underlying configuration implementation.
     *
     * @param value strongly typed value
     * @return raw value suitable for configuration storage
     */
    Object toConfig(T value);
}
