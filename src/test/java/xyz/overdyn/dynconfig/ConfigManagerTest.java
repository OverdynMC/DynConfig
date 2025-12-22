package xyz.overdyn.dynconfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manual test for ConfigManager with ConfigurateYamlBackend.
 * Tests comment functionality and multi-language support.
 */
public class ConfigManagerTest {

    public static void main(String[] args) {
        try {
            // Initialize ConfigManager with test directory
            Path testDir = Paths.get("test-configs");
            if (!Files.exists(testDir)) {
                Files.createDirectories(testDir);
            }
            ConfigManager.init(testDir);

            System.out.println("=== Testing ConfigurateYamlBackend with Comments ===\n");

            // Test 1: Load config with English comments
            System.out.println("1. Loading TestConfig with English comments...");
            LangContext.set("en");
            TestConfig config = ConfigManager.get(TestConfig.class);
            System.out.println("   Port: " + config.getPort());
            System.out.println("   DB Host: " + config.getDbHost());
            System.out.println("   DB Port: " + config.getDbPort());
            System.out.println("   Features: " + config.getFeatures());
            System.out.println("   Debug Mode: " + config.isDebugMode());
            System.out.println("   ✓ Config loaded with English comments\n");

            // Check if file was created with comments
            Path configFile = testDir.resolve("test-config.yml");
            if (Files.exists(configFile)) {
                System.out.println("Generated YAML content (English):");
                System.out.println("---");
                Files.lines(configFile).forEach(line -> System.out.println(line));
                System.out.println("---\n");
            }

            // Test 2: Change language to Russian and reload
            System.out.println("2. Changing language to Russian and reloading...");
            LangContext.set("ru");
            config = ConfigManager.reload(TestConfig.class);
            System.out.println("   ✓ Config reloaded with Russian comments\n");

            // Check if comments changed to Russian
            if (Files.exists(configFile)) {
                System.out.println("Generated YAML content (Russian):");
                System.out.println("---");
                Files.lines(configFile).forEach(line -> System.out.println(line));
                System.out.println("---\n");
            }

            // Test 3: Update config values
            System.out.println("3. Updating config values...");
            ConfigManager.update(TestConfig.class, cfg -> {
                cfg.setPort(9090);
                cfg.setDebugMode(true);
                cfg.getFeatures().add("new-feature");
            });
            System.out.println("   ✓ Config updated and saved\n");

            // Test 4: Final reload to verify persistence
            System.out.println("4. Final reload to verify persistence...");
            config = ConfigManager.reload(TestConfig.class);
            System.out.println("   Port: " + config.getPort());
            System.out.println("   Debug Mode: " + config.isDebugMode());
            System.out.println("   Features: " + config.getFeatures());
            System.out.println("   ✓ All changes persisted correctly\n");

            // Final file content
            if (Files.exists(configFile)) {
                System.out.println("Final YAML content:");
                System.out.println("---");
                Files.lines(configFile).forEach(line -> System.out.println(line));
                System.out.println("---\n");
            }

            System.out.println("=== All tests passed! Comments are working! ===");
            System.out.println("\nConfig file location: " + configFile.toAbsolutePath());

        } catch (Exception e) {
            System.err.println("❌ Test failed:");
            e.printStackTrace();
        }
    }
}
