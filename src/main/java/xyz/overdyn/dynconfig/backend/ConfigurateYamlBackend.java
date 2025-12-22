package xyz.overdyn.dynconfig.backend;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * YAML backend implementation using SpongePowered Configurate.
 * Properly handles nested structures, comments, and YAML formatting.
 */
public final class ConfigurateYamlBackend implements ConfigBackend {

    @Override
    public @NotNull Map<String, Object> load(@NotNull Path path) throws IOException {
        if (!Files.exists(path)) {
            return new LinkedHashMap<>();
        }

        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(path)
                .nodeStyle(NodeStyle.BLOCK)
                .indent(2)
                .build();

        try {
            ConfigurationNode root = loader.load();
            return flattenNode(root, "");
        } catch (Exception e) {
            // If parsing fails (e.g., due to cyrillic characters or malformed YAML),
            // backup the original file and return empty config to regenerate
            System.out.println("[DynConfig] Warning: Failed to parse YAML file " + path.getFileName() + ": " + e.getMessage());
            System.out.println("[DynConfig] Creating backup and regenerating config...");
            
            // Create backup only if it doesn't exist yet
            Path backupPath = path.resolveSibling(path.getFileName() + ".backup");
            if (!Files.exists(backupPath)) {
                try {
                    Files.copy(path, backupPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("[DynConfig] Backup created at: " + backupPath.getFileName());
                } catch (IOException backupError) {
                    System.err.println("[DynConfig] Failed to create backup: " + backupError.getMessage());
                }
            }
            
            // Return empty config to trigger regeneration
            return new LinkedHashMap<>();
        }
    }

    @Override
    public void save(@NotNull Path path,
                     @NotNull Map<String, Object> data,
                     @NotNull Map<String, List<String>> comments) throws IOException {
        
        // Ensure parent directories exist
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(path)
                .nodeStyle(NodeStyle.BLOCK)
                .indent(2)
                .build();

        CommentedConfigurationNode root = loader.createNode();

        // Build node tree from flat data with proper comment handling
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Split dotted key into path components
            Object[] pathComponents = key.split("\\.");
            CommentedConfigurationNode node = root.node(pathComponents);

            // Set value first
            try {
                node.set(value);
            } catch (Exception e) {
                throw new IOException("Failed to set value for key '" + key + "': " + e.getMessage(), e);
            }

            // Apply comment if present - try multiple approaches
            List<String> commentLines = comments.get(key);
            if (commentLines != null && !commentLines.isEmpty()) {
                try {
                    // Join comment lines with newlines
                    String fullComment = String.join("\n", commentLines);
                    
                    // Try setting comment directly on the node (for potential future Configurate fixes)
                    node.comment(fullComment);
                } catch (Exception e) {
                    // Log warning but don't fail the save operation
                    System.err.println("Warning: Failed to set comment for key '" + key + "': " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        // Save the configuration using our custom writer that properly handles comments
        try {
            // Use our custom YAML writer that properly handles comments and cyrillic
            CustomYamlWriter.writeYaml(path, data, comments);
        } catch (Exception e) {
            throw new IOException("Failed to save configuration to " + path + ": " + e.getMessage(), e);
        }
    }

    /**
     * Flattens a ConfigurationNode tree into a Map with dotted keys.
     *
     * @param node   the node to flatten
     * @param prefix current key prefix
     * @return flattened map
     */
    private Map<String, Object> flattenNode(ConfigurationNode node, String prefix) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (node.isMap()) {
            // Node has children - recurse into them
            for (Map.Entry<Object, ? extends ConfigurationNode> entry : node.childrenMap().entrySet()) {
                String key = entry.getKey().toString();
                String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
                result.putAll(flattenNode(entry.getValue(), fullKey));
            }
        } else if (node.isList()) {
            // Node is a list - store as-is
            result.put(prefix, node.raw());
        } else {
            // Leaf node - store value
            Object value = node.raw();
            if (value != null) {
                result.put(prefix, value);
            }
        }

        return result;
    }

    @Override
    public Object get(@NotNull Map<String, Object> data, @NotNull String key) {
        return data.get(key);
    }

    @Override
    public void set(@NotNull Map<String, Object> data, @NotNull String key, Object value) {
        data.put(key, value);
    }
}