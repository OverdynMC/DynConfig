package xyz.overdyn.dynconfig;

import org.jetbrains.annotations.NotNull;
import xyz.overdyn.dynconfig.annotation.ConfigResource;
import xyz.overdyn.dynconfig.backend.ConfigBackend;
import xyz.overdyn.dynconfig.backend.ConfigurateYamlBackend;
import xyz.overdyn.dynconfig.format.ConfigFormat;
import xyz.overdyn.dynconfig.processor.ConfigProcessor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Main configuration manager providing a simple API for loading, saving, and managing configurations.
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * // Initialize
 * ConfigManager.init(Paths.get("./config"));
 * 
 * // Load config
 * MyConfig config = ConfigManager.get(MyConfig.class);
 * 
 * // Update config
 * ConfigManager.update(MyConfig.class, cfg -> cfg.setValue(42));
 * 
 * // Reload from file
 * config = ConfigManager.reload(MyConfig.class);
 * }</pre>
 */
public final class ConfigManager {

    private static Path baseDirectory;
    private static final Map<ConfigFormat, ConfigBackend> backends = new EnumMap<>(ConfigFormat.class);
    private static final Map<Class<?>, Object> configCache = new HashMap<>();

    private ConfigManager() {}

    static {
        // Register default backends
        registerBackend(ConfigFormat.YAML, new ConfigurateYamlBackend());
    }

    /**
     * Initializes the ConfigManager with the base directory for configuration files.
     * This must be called before using any other methods.
     *
     * @param baseDirectory the base directory where config files will be stored
     * @throws IllegalArgumentException if baseDirectory is null
     */
    public static void init(@NotNull Path baseDirectory) {
        if (baseDirectory == null) {
            throw new IllegalArgumentException("Base directory cannot be null");
        }
        ConfigManager.baseDirectory = baseDirectory;
    }

    /**
     * Registers a backend for a specific configuration format.
     *
     * @param format  the configuration format
     * @param backend the backend implementation
     * @throws IllegalArgumentException if format or backend is null
     */
    public static void registerBackend(@NotNull ConfigFormat format, @NotNull ConfigBackend backend) {
        if (format == null) {
            throw new IllegalArgumentException("Format cannot be null");
        }
        if (backend == null) {
            throw new IllegalArgumentException("Backend cannot be null");
        }
        backends.put(format, backend);
    }

    /**
     * Gets a configuration instance, loading it from file if not already cached.
     * If the file doesn't exist, it will be created with default values.
     *
     * @param configClass the configuration class annotated with @ConfigResource
     * @param <T>         the configuration type
     * @return the configuration instance
     * @throws IllegalStateException    if ConfigManager is not initialized or config class is invalid
     * @throws RuntimeException         if loading fails
     */
    @SuppressWarnings("unchecked")
    public static <T> @NotNull T get(@NotNull Class<T> configClass) {
        if (configClass == null) {
            throw new IllegalArgumentException("Config class cannot be null");
        }

        return (T) configCache.computeIfAbsent(configClass, clazz -> {
            try {
                return loadConfigInternal(clazz);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load configuration " + clazz.getName(), e);
            }
        });
    }

    /**
     * Reloads a configuration from file, updating the cached instance.
     * The current instance is saved before reloading.
     *
     * @param configClass the configuration class
     * @param <T>         the configuration type
     * @return the reloaded configuration instance
     * @throws IllegalStateException if ConfigManager is not initialized or config class is invalid
     * @throws RuntimeException      if reloading fails
     */
    public static <T> @NotNull T reload(@NotNull Class<T> configClass) {
        if (configClass == null) {
            throw new IllegalArgumentException("Config class cannot be null");
        }

        try {
            // Save current instance if it exists
            Object currentInstance = configCache.get(configClass);
            if (currentInstance != null) {
                saveConfigInternal(currentInstance);
            }

            // Load fresh instance
            T newInstance = loadConfigInternal(configClass);
            configCache.put(configClass, newInstance);
            return newInstance;
        } catch (Exception e) {
            throw new RuntimeException("Failed to reload configuration " + configClass.getName(), e);
        }
    }

    /**
     * Atomically updates a configuration and immediately saves it to file.
     *
     * @param configClass the configuration class
     * @param mutator     the function to modify the configuration
     * @param <T>         the configuration type
     * @throws IllegalStateException if ConfigManager is not initialized or config class is invalid
     * @throws RuntimeException      if updating fails
     */
    public static <T> void update(@NotNull Class<T> configClass, @NotNull Consumer<T> mutator) {
        if (configClass == null) {
            throw new IllegalArgumentException("Config class cannot be null");
        }
        if (mutator == null) {
            throw new IllegalArgumentException("Mutator cannot be null");
        }

        try {
            T config = get(configClass);
            mutator.accept(config);
            saveConfigInternal(config);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update configuration " + configClass.getName(), e);
        }
    }

    /**
     * Saves all cached configuration instances to their respective files.
     *
     * @throws RuntimeException if saving fails for any configuration
     */
    public static void saveAll() {
        for (Object configInstance : configCache.values()) {
            try {
                saveConfigInternal(configInstance);
            } catch (Exception e) {
                throw new RuntimeException("Failed to save configuration " + configInstance.getClass().getName(), e);
            }
        }
    }

    /**
     * Clears the configuration cache. Configurations will be reloaded from file on next access.
     */
    public static void clearCache() {
        configCache.clear();
    }

    /**
     * Internal method to load a configuration from file.
     */
    private static <T> T loadConfigInternal(Class<T> configClass) throws IOException {
        validateInitialized();
        validateConfigClass(configClass);

        ConfigResource annotation = configClass.getAnnotation(ConfigResource.class);
        ConfigFormat format = annotation.format();
        ConfigBackend backend = getBackend(format);
        Path filePath = resolveConfigPath(annotation);

        return ConfigProcessor.load(configClass, backend, filePath);
    }

    /**
     * Internal method to save a configuration to file.
     */
    private static void saveConfigInternal(Object configInstance) throws IOException {
        validateInitialized();

        Class<?> configClass = configInstance.getClass();
        validateConfigClass(configClass);

        ConfigResource annotation = configClass.getAnnotation(ConfigResource.class);
        ConfigFormat format = annotation.format();
        ConfigBackend backend = getBackend(format);
        Path filePath = resolveConfigPath(annotation);

        ConfigProcessor.save(configInstance, backend, filePath);
    }

    /**
     * Validates that ConfigManager has been initialized.
     */
    private static void validateInitialized() {
        if (baseDirectory == null) {
            throw new IllegalStateException("ConfigManager not initialized. Call ConfigManager.init(baseDirectory) first.");
        }
    }

    /**
     * Validates that a class is a valid configuration class.
     */
    private static void validateConfigClass(Class<?> configClass) {
        ConfigResource annotation = configClass.getAnnotation(ConfigResource.class);
        if (annotation == null) {
            throw new IllegalStateException("Class " + configClass.getName() + 
                                          " must be annotated with @ConfigResource");
        }
    }

    /**
     * Gets the backend for a specific format.
     */
    private static ConfigBackend getBackend(ConfigFormat format) {
        ConfigBackend backend = backends.get(format);
        if (backend == null) {
            throw new IllegalStateException("No backend registered for format " + format + 
                                          ". Register one using ConfigManager.registerBackend()");
        }
        return backend;
    }

    /**
     * Resolves the full path to a configuration file.
     */
    private static Path resolveConfigPath(ConfigResource annotation) {
        String pathTemplate = annotation.path();
        
        // Replace placeholders
        String resolvedPath = pathTemplate.replace("{lang}", LangContext.get());
        
        return baseDirectory.resolve(resolvedPath);
    }
}