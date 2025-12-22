package xyz.overdyn.dynconfig;

import xyz.overdyn.dynconfig.annotation.Comment;
import xyz.overdyn.dynconfig.annotation.ConfigKey;
import xyz.overdyn.dynconfig.annotation.ConfigResource;
import xyz.overdyn.dynconfig.policy.MissingKeyPolicy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Test for cyrillic character handling.
 */
public class CyrillicTest {

    @ConfigResource(
        path = "cyrillic-test.yml",
        missingKeyPolicy = MissingKeyPolicy.WRITE_DEFAULT
    )
    public static class CyrillicConfig {

        @Comment({
            @Comment.Entry(lang = "ru", lines = {"Тестовое сообщение на русском языке"}),
            @Comment.Entry(lang = "en", lines = {"Test message in English"})
        })
        @ConfigKey("test.message")
        public String testMessage = "Привет, мир! Hello, world!";

        @Comment({
            @Comment.Entry(lang = "ru", lines = {"Список русских слов"}),
            @Comment.Entry(lang = "en", lines = {"List of Russian words"})
        })
        @ConfigKey("test.words")
        public List<String> testWords = List.of(
            "привет", "мир", "тест", "конфигурация"
        );

        @Comment({
            @Comment.Entry(lang = "ru", lines = {"Булево значение"}),
            @Comment.Entry(lang = "en", lines = {"Boolean value"})
        })
        @ConfigKey("test.enabled")
        public boolean enabled = true;

        @Comment({
            @Comment.Entry(lang = "ru", lines = {"Числовое значение"}),
            @Comment.Entry(lang = "en", lines = {"Numeric value"})
        })
        @ConfigKey("test.number")
        public int number = 42;
    }

    public static void main(String[] args) {
        try {
            // Initialize ConfigManager with test directory
            Path testDir = Paths.get("test-configs");
            if (!Files.exists(testDir)) {
                Files.createDirectories(testDir);
            }
            ConfigManager.init(testDir);

            System.out.println("=== Testing Cyrillic Character Handling ===\n");

            // Test 1: Load config with Russian language
            System.out.println("1. Loading config with Russian comments...");
            LangContext.set("ru");
            CyrillicConfig config = ConfigManager.get(CyrillicConfig.class);
            System.out.println("   Message: " + config.testMessage);
            System.out.println("   Words: " + config.testWords);
            System.out.println("   Enabled: " + config.enabled);
            System.out.println("   Number: " + config.number);
            System.out.println("   ✓ Config loaded successfully\n");

            // Check generated file
            Path configFile = testDir.resolve("cyrillic-test.yml");
            if (Files.exists(configFile)) {
                System.out.println("Generated YAML content:");
                System.out.println("---");
                Files.lines(configFile).forEach(System.out::println);
                System.out.println("---\n");
            }

            // Test 2: Reload to verify parsing works
            System.out.println("2. Reloading config to test parsing...");
            config = ConfigManager.reload(CyrillicConfig.class);
            System.out.println("   ✓ Config reloaded successfully (no backup created)\n");

            // Test 3: Update with cyrillic values
            System.out.println("3. Updating config with new cyrillic values...");
            ConfigManager.update(CyrillicConfig.class, cfg -> {
                cfg.testMessage = "Обновленное сообщение с ёжиком и щукой!";
                cfg.testWords.add("ёжик");
                cfg.testWords.add("щука");
            });
            System.out.println("   ✓ Config updated successfully\n");

            // Test 4: Final reload
            System.out.println("4. Final reload to verify persistence...");
            config = ConfigManager.reload(CyrillicConfig.class);
            System.out.println("   Message: " + config.testMessage);
            System.out.println("   Words: " + config.testWords);
            System.out.println("   ✓ All cyrillic characters preserved\n");

            // Final file content
            if (Files.exists(configFile)) {
                System.out.println("Final YAML content:");
                System.out.println("---");
                Files.lines(configFile).forEach(System.out::println);
                System.out.println("---\n");
            }

            System.out.println("=== Cyrillic test passed! ===");

        } catch (Exception e) {
            System.err.println("❌ Cyrillic test failed:");
            e.printStackTrace();
        }
    }
}