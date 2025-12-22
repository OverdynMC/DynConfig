package xyz.overdyn.dynconfig.processor;

import org.jetbrains.annotations.NotNull;
import xyz.overdyn.dynconfig.LangContext;
import xyz.overdyn.dynconfig.annotation.Comment;
import xyz.overdyn.dynconfig.annotation.ConfigIgnore;
import xyz.overdyn.dynconfig.annotation.ConfigKey;
import xyz.overdyn.dynconfig.annotation.ConfigResource;
import xyz.overdyn.dynconfig.backend.ConfigBackend;
import xyz.overdyn.dynconfig.policy.MissingKeyPolicy;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.*;

/**
 * Processes configuration classes and handles annotation-based mapping.
 */
public final class ConfigProcessor {

    private ConfigProcessor() {}

    /**
     * Loads a configuration instance from file.
     */
    public static <T> @NotNull T load(@NotNull Class<T> configClass, 
                                      @NotNull ConfigBackend backend, 
                                      @NotNull Path filePath) throws IOException {
        
        // Create instance
        T instance = createInstance(configClass);
        
        // Load data from file
        Map<String, Object> data = backend.load(filePath);
        
        // Get config metadata
        ConfigResource meta = configClass.getAnnotation(ConfigResource.class);
        MissingKeyPolicy policy = meta != null ? meta.missingKeyPolicy() : MissingKeyPolicy.WRITE_DEFAULT;
        
        // Process fields
        Map<String, List<String>> comments = new HashMap<>();
        boolean hasChanges = false;
        boolean isNewFile = data.isEmpty(); // File didn't exist or was empty
        
        for (Field field : getAllFields(configClass)) {
            if (isIgnored(field)) continue;
            
            field.setAccessible(true);
            String key = resolveKey(field);
            
            // Always process comments for all fields
            Comment commentAnnotation = field.getAnnotation(Comment.class);
            if (commentAnnotation != null) {
                List<String> commentLines = resolveComment(commentAnnotation, LangContext.get());
                if (!commentLines.isEmpty()) {
                    comments.put(key, commentLines);
                }
            }
            
            // Handle field value
            Object rawValue = data.get(key);
            Object currentValue = getFieldValue(instance, field);
            
            if (rawValue != null) {
                // Value exists in file - deserialize and set
                Object deserializedValue = FieldMapper.deserialize(rawValue, field.getType());
                setFieldValue(instance, field, deserializedValue);
            } else {
                // Value missing from file - handle according to policy
                if (policy == MissingKeyPolicy.WRITE_DEFAULT && currentValue != null) {
                    Object serializedValue = FieldMapper.serialize(currentValue, field.getType());
                    data.put(key, serializedValue);
                    hasChanges = true;
                } else if (policy == MissingKeyPolicy.ERROR) {
                    throw new IllegalStateException("Missing required key '" + key + "' in config file " + filePath);
                } else if (isNewFile && currentValue != null) {
                    // For new files, always write default values regardless of policy
                    Object serializedValue = FieldMapper.serialize(currentValue, field.getType());
                    data.put(key, serializedValue);
                    hasChanges = true;
                }
                // For USE_FIELD_DEFAULT, we just keep the current field value
            }
        }
        
        // Always save if it's a new file, has changes, or has comments
        if (isNewFile || hasChanges || !comments.isEmpty()) {
            backend.save(filePath, data, comments);
        }
        
        return instance;
    }
    
    /**
     * Saves a configuration instance to file.
     */
    public static void save(@NotNull Object instance, 
                           @NotNull ConfigBackend backend, 
                           @NotNull Path filePath) throws IOException {
        
        Class<?> configClass = instance.getClass();
        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, List<String>> comments = new HashMap<>();
        
        for (Field field : getAllFields(configClass)) {
            if (isIgnored(field)) continue;
            
            field.setAccessible(true);
            String key = resolveKey(field);
            
            // Get field value and serialize
            Object value = getFieldValue(instance, field);
            Object serializedValue = FieldMapper.serialize(value, field.getType());
            data.put(key, serializedValue);
            
            // Process comments
            Comment commentAnnotation = field.getAnnotation(Comment.class);
            if (commentAnnotation != null) {
                List<String> commentLines = resolveComment(commentAnnotation, LangContext.get());
                if (!commentLines.isEmpty()) {
                    comments.put(key, commentLines);
                }
            }
        }
        
        backend.save(filePath, data, comments);
    }
    
    /**
     * Resolves the configuration key for a field.
     */
    private static String resolveKey(Field field) {
        ConfigKey annotation = field.getAnnotation(ConfigKey.class);
        return annotation != null ? annotation.value() : field.getName();
    }
    
    /**
     * Checks if a field should be ignored.
     */
    private static boolean isIgnored(Field field) {
        return field.isAnnotationPresent(ConfigIgnore.class) || 
               Modifier.isStatic(field.getModifiers()) ||
               Modifier.isTransient(field.getModifiers());
    }
    
    /**
     * Resolves comment text for the current language.
     */
    private static List<String> resolveComment(Comment comment, String language) {
        if (language == null || language.isBlank()) {
            language = "en";
        }
        language = language.toLowerCase(Locale.ROOT);
        
        Comment.Entry fallback = null;
        
        // Look for exact language match
        for (Comment.Entry entry : comment.value()) {
            if (entry.lang().equalsIgnoreCase(language)) {
                return Arrays.asList(entry.lines());
            }
            // Keep track of fallback (en or default)
            if (entry.lang().equalsIgnoreCase("en") || entry.lang().equalsIgnoreCase("default")) {
                fallback = entry;
            }
        }
        
        // Use fallback if no exact match
        if (fallback != null) {
            return Arrays.asList(fallback.lines());
        }
        
        return Collections.emptyList();
    }
    
    /**
     * Gets all fields from a class, including inherited ones.
     */
    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        
        return fields;
    }
    
    /**
     * Creates a new instance of the configuration class.
     */
    private static <T> T createInstance(Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Cannot create instance of " + clazz.getName() + 
                                     ". Make sure it has a public no-args constructor.", e);
        }
    }
    
    /**
     * Gets field value via reflection.
     */
    private static Object getFieldValue(Object instance, Field field) {
        try {
            return field.get(instance);
        } catch (Exception e) {
            throw new RuntimeException("Cannot read field " + field.getName() + 
                                     " from " + instance.getClass().getName(), e);
        }
    }
    
    /**
     * Sets field value via reflection.
     */
    private static void setFieldValue(Object instance, Field field, Object value) {
        try {
            field.set(instance, value);
        } catch (Exception e) {
            throw new RuntimeException("Cannot set field " + field.getName() + 
                                     " in " + instance.getClass().getName() + 
                                     " to value " + value, e);
        }
    }
}