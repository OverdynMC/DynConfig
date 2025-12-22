package xyz.overdyn.dynconfig.processor;

import xyz.overdyn.dynconfig.adapter.AdapterRegistry;
import xyz.overdyn.dynconfig.adapter.ConfigAdapter;

import java.util.*;

/**
 * Handles serialization and deserialization of field values.
 */
public final class FieldMapper {

    private FieldMapper() {}

    /**
     * Deserializes a raw value to the target type.
     */
    @SuppressWarnings("unchecked")
    public static Object deserialize(Object rawValue, Class<?> targetType) {
        if (rawValue == null) {
            return null;
        }

        // Check for custom adapter first
        ConfigAdapter<Object> adapter = (ConfigAdapter<Object>) AdapterRegistry.get(targetType);
        if (adapter != null) {
            try {
                return adapter.fromConfig(rawValue);
            } catch (Exception e) {
                throw new RuntimeException("Adapter failed to deserialize value " + rawValue + 
                                         " to type " + targetType.getName(), e);
            }
        }

        // If value is already the correct type, return as-is
        if (targetType.isInstance(rawValue)) {
            return rawValue;
        }

        // Handle primitive types and their wrappers
        if (isPrimitiveOrWrapper(targetType)) {
            return convertPrimitive(rawValue, targetType);
        }

        // Handle enums
        if (targetType.isEnum()) {
            return convertEnum(rawValue, (Class<? extends Enum>) targetType);
        }

        // Handle collections
        if (List.class.isAssignableFrom(targetType)) {
            return convertToList(rawValue);
        }

        if (Set.class.isAssignableFrom(targetType)) {
            return convertToSet(rawValue);
        }

        if (Map.class.isAssignableFrom(targetType)) {
            return convertToMap(rawValue);
        }

        // Handle arrays
        if (targetType.isArray()) {
            return convertToArray(rawValue, targetType);
        }

        // For other types, try to return as-is or convert to string
        if (targetType == String.class) {
            return rawValue.toString();
        }

        return rawValue;
    }

    /**
     * Serializes a value for storage.
     */
    @SuppressWarnings("unchecked")
    public static Object serialize(Object value, Class<?> sourceType) {
        if (value == null) {
            return null;
        }

        // Check for custom adapter first
        ConfigAdapter<Object> adapter = (ConfigAdapter<Object>) AdapterRegistry.get(sourceType);
        if (adapter != null) {
            try {
                return adapter.toConfig(value);
            } catch (Exception e) {
                throw new RuntimeException("Adapter failed to serialize value " + value + 
                                         " of type " + sourceType.getName(), e);
            }
        }

        // Handle primitive types, strings, and enums - return as-is
        if (isPrimitiveOrWrapper(sourceType) || sourceType == String.class || sourceType.isEnum()) {
            return value;
        }

        // Handle collections - serialize recursively
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            List<Object> result = new ArrayList<>();
            for (Object item : list) {
                result.add(item != null ? serialize(item, item.getClass()) : null);
            }
            return result;
        }

        if (value instanceof Set) {
            Set<?> set = (Set<?>) value;
            List<Object> result = new ArrayList<>();
            for (Object item : set) {
                result.add(item != null ? serialize(item, item.getClass()) : null);
            }
            return result;
        }

        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = entry.getKey() != null ? entry.getKey().toString() : "null";
                Object val = entry.getValue();
                result.put(key, val != null ? serialize(val, val.getClass()) : null);
            }
            return result;
        }

        // Handle arrays
        if (value.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(value);
            List<Object> result = new ArrayList<>();
            for (int i = 0; i < length; i++) {
                Object item = java.lang.reflect.Array.get(value, i);
                result.add(item != null ? serialize(item, item.getClass()) : null);
            }
            return result;
        }

        // For other types, return as-is
        return value;
    }

    /**
     * Converts a raw value to a primitive type or wrapper.
     */
    private static Object convertPrimitive(Object rawValue, Class<?> targetType) {
        String stringValue = rawValue.toString();

        try {
            if (targetType == int.class || targetType == Integer.class) {
                return Integer.parseInt(stringValue);
            }
            if (targetType == long.class || targetType == Long.class) {
                return Long.parseLong(stringValue);
            }
            if (targetType == double.class || targetType == Double.class) {
                return Double.parseDouble(stringValue);
            }
            if (targetType == float.class || targetType == Float.class) {
                return Float.parseFloat(stringValue);
            }
            if (targetType == boolean.class || targetType == Boolean.class) {
                if (rawValue instanceof Boolean) {
                    return rawValue;
                }
                return Boolean.parseBoolean(stringValue);
            }
            if (targetType == byte.class || targetType == Byte.class) {
                return Byte.parseByte(stringValue);
            }
            if (targetType == short.class || targetType == Short.class) {
                return Short.parseShort(stringValue);
            }
            if (targetType == char.class || targetType == Character.class) {
                return stringValue.isEmpty() ? '\0' : stringValue.charAt(0);
            }
        } catch (NumberFormatException e) {
            throw new RuntimeException("Cannot convert '" + stringValue + "' to " + targetType.getName(), e);
        }

        return rawValue;
    }

    /**
     * Converts a raw value to an enum.
     */
    private static Object convertEnum(Object rawValue, Class<? extends Enum> enumType) {
        String enumName = rawValue.toString();
        try {
            return Enum.valueOf(enumType, enumName);
        } catch (IllegalArgumentException e) {
            // Provide helpful error message with valid values
            String validValues = Arrays.toString(enumType.getEnumConstants());
            throw new RuntimeException("Invalid enum value '" + enumName + "' for type " + 
                                     enumType.getName() + ". Valid values: " + validValues, e);
        }
    }

    /**
     * Converts a raw value to a List.
     */
    private static List<Object> convertToList(Object rawValue) {
        if (rawValue instanceof List) {
            return new ArrayList<>((List<?>) rawValue);
        }
        // Single value - wrap in list
        return new ArrayList<>(Collections.singletonList(rawValue));
    }

    /**
     * Converts a raw value to a Set.
     */
    private static Set<Object> convertToSet(Object rawValue) {
        if (rawValue instanceof List) {
            return new LinkedHashSet<>((List<?>) rawValue);
        }
        if (rawValue instanceof Set) {
            return new LinkedHashSet<>((Set<?>) rawValue);
        }
        // Single value - wrap in set
        return new LinkedHashSet<>(Collections.singletonList(rawValue));
    }

    /**
     * Converts a raw value to a Map.
     */
    private static Map<String, Object> convertToMap(Object rawValue) {
        if (rawValue instanceof Map) {
            Map<?, ?> sourceMap = (Map<?, ?>) rawValue;
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : sourceMap.entrySet()) {
                String key = entry.getKey() != null ? entry.getKey().toString() : "null";
                result.put(key, entry.getValue());
            }
            return result;
        }
        // Cannot convert non-map to map
        throw new RuntimeException("Cannot convert " + rawValue.getClass().getName() + " to Map");
    }

    /**
     * Converts a raw value to an array.
     */
    private static Object convertToArray(Object rawValue, Class<?> arrayType) {
        Class<?> componentType = arrayType.getComponentType();
        
        if (rawValue instanceof List) {
            List<?> list = (List<?>) rawValue;
            Object array = java.lang.reflect.Array.newInstance(componentType, list.size());
            for (int i = 0; i < list.size(); i++) {
                Object item = deserialize(list.get(i), componentType);
                java.lang.reflect.Array.set(array, i, item);
            }
            return array;
        }
        
        // Single value - create array with one element
        Object array = java.lang.reflect.Array.newInstance(componentType, 1);
        Object item = deserialize(rawValue, componentType);
        java.lang.reflect.Array.set(array, 0, item);
        return array;
    }

    /**
     * Checks if a type is a primitive or its wrapper.
     */
    private static boolean isPrimitiveOrWrapper(Class<?> type) {
        return type.isPrimitive() ||
               type == Integer.class ||
               type == Long.class ||
               type == Double.class ||
               type == Float.class ||
               type == Boolean.class ||
               type == Byte.class ||
               type == Short.class ||
               type == Character.class;
    }
}