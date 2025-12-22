package xyz.overdyn.dynconfig.backend;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Backend abstraction for configuration storage.
 * Handles loading and saving configuration data with comment support.
 */
public interface ConfigBackend {

    /**
     * Loads configuration data from file.
     *
     * @param path file path
     * @return flattened configuration data (dotted keys)
     * @throws IOException if file cannot be read
     */
    @NotNull Map<String, Object> load(@NotNull Path path) throws IOException;

    /**
     * Saves configuration data to file with comments.
     *
     * @param path     file path
     * @param data     flattened configuration data (dotted keys)
     * @param comments comments for each key
     * @throws IOException if file cannot be written
     */
    void save(@NotNull Path path, 
              @NotNull Map<String, Object> data, 
              @NotNull Map<String, List<String>> comments) throws IOException;

    /**
     * Gets a value from the data map using a key.
     * Default implementation just does a map lookup.
     *
     * @param data the data map
     * @param key  the key
     * @return the value or null if not found
     */
    default @Nullable Object get(@NotNull Map<String, Object> data, @NotNull String key) {
        return data.get(key);
    }

    /**
     * Sets a value in the data map using a key.
     * Default implementation just does a map put.
     *
     * @param data  the data map
     * @param key   the key
     * @param value the value
     */
    default void set(@NotNull Map<String, Object> data, @NotNull String key, @Nullable Object value) {
        data.put(key, value);
    }
}
