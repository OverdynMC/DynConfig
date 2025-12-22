package xyz.overdyn.dynconfig.backend;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Custom YAML writer that properly handles comments.
 * This is a workaround for Configurate 4.x comment issues.
 */
public final class CustomYamlWriter {

    private CustomYamlWriter() {}

    /**
     * Writes YAML data with comments to a file.
     */
    public static void writeYaml(Path path, 
                                Map<String, Object> data, 
                                Map<String, List<String>> comments) throws IOException {
        
        // Ensure parent directories exist
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(path, java.nio.charset.StandardCharsets.UTF_8)) {
            writeYamlContent(writer, data, comments, 0);
        }
    }

    /**
     * Writes YAML content from flat data structure.
     */
    private static void writeYamlContent(BufferedWriter writer,
                                        Map<String, Object> data,
                                        Map<String, List<String>> comments,
                                        int indent) throws IOException {
        
        // Group keys by their root level
        Map<String, Object> rootKeys = new java.util.LinkedHashMap<>();
        Map<String, Map<String, Object>> nestedKeys = new java.util.LinkedHashMap<>();
        
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (key.contains(".")) {
                // Nested key
                String rootKey = key.substring(0, key.indexOf("."));
                String nestedKey = key.substring(key.indexOf(".") + 1);
                
                nestedKeys.computeIfAbsent(rootKey, k -> new java.util.LinkedHashMap<>())
                         .put(nestedKey, value);
            } else {
                // Root level key
                rootKeys.put(key, value);
            }
        }
        
        String indentStr = "  ".repeat(indent);
        
        // Write root level keys first
        for (Map.Entry<String, Object> entry : rootKeys.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            writeKeyWithComment(writer, key, value, comments, indentStr);
        }
        
        // Write nested keys
        for (Map.Entry<String, Map<String, Object>> entry : nestedKeys.entrySet()) {
            String rootKey = entry.getKey();
            Map<String, Object> nestedData = entry.getValue();
            
            // Write comment for the root key if it exists
            List<String> rootComment = comments.get(rootKey);
            if (rootComment != null && !rootComment.isEmpty()) {
                for (String commentLine : rootComment) {
                    writer.write(indentStr + "# " + commentLine);
                    writer.newLine();
                }
            }
            
            writer.write(indentStr + rootKey + ":");
            writer.newLine();
            
            // Create nested comments map
            Map<String, List<String>> nestedComments = new java.util.HashMap<>();
            String keyPrefix = rootKey + ".";
            for (Map.Entry<String, List<String>> commentEntry : comments.entrySet()) {
                String commentKey = commentEntry.getKey();
                if (commentKey.startsWith(keyPrefix)) {
                    String nestedKey = commentKey.substring(keyPrefix.length());
                    nestedComments.put(nestedKey, commentEntry.getValue());
                }
            }
            
            writeYamlContent(writer, nestedData, nestedComments, indent + 1);
        }
    }
    
    /**
     * Writes a key-value pair with its comment.
     */
    private static void writeKeyWithComment(BufferedWriter writer,
                                           String key,
                                           Object value,
                                           Map<String, List<String>> comments,
                                           String indentStr) throws IOException {
        
        // Write comments for this key
        List<String> commentLines = comments.get(key);
        if (commentLines != null && !commentLines.isEmpty()) {
            for (String commentLine : commentLines) {
                writer.write(indentStr + "# " + commentLine);
                writer.newLine();
            }
        }
        
        // Write the key-value pair
        if (value instanceof List) {
            // List
            writer.write(indentStr + key + ":");
            writer.newLine();
            
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) value;
            
            if (list.isEmpty()) {
                writer.write(indentStr + "  []");
                writer.newLine();
            } else {
                for (Object item : list) {
                    writer.write(indentStr + "- " + formatValue(item));
                    writer.newLine();
                }
            }
        } else {
            // Simple value
            writer.write(indentStr + key + ": " + formatValue(value));
            writer.newLine();
        }
    }

    /**
     * Formats a value for YAML output.
     */
    private static String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        
        if (value instanceof String) {
            String str = (String) value;
            // Always quote strings to avoid parsing issues with cyrillic and special characters
            return "'" + str.replace("'", "''") + "'";
        }
        
        if (value instanceof Boolean || value instanceof Number) {
            return value.toString();
        }
        
        // For other types, convert to string and quote
        return "'" + value.toString().replace("'", "''") + "'";
    }

    /**
     * Checks if a string represents a number.
     */
    private static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}