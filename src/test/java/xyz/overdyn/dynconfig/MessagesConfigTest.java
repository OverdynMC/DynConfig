package xyz.overdyn.dynconfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Test for MessagesConfig showing real-world usage.
 */
public class MessagesConfigTest {

    public static void main(String[] args) {
        try {
            // Initialize ConfigManager
            Path configDir = Paths.get("plugin-configs");
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            ConfigManager.init(configDir);

            System.out.println("=== MessagesConfig Test ===\n");

            // Test 1: Load English config
            System.out.println("1. Loading MessagesConfig in English...");
            LangContext.set("en");
            MessagesConfig config = MessagesConfig.get();
            
            System.out.println("   Welcome: " + config.getWelcomeMessage());
            System.out.println("   Goodbye: " + config.getGoodbyeMessage());
            System.out.println("   Filter enabled: " + config.isFilterEnabled());
            System.out.println("   Banned words: " + config.getBannedWords());
            
            // Show generated English file
            Path englishFile = configDir.resolve("messages_en.yml");
            if (Files.exists(englishFile)) {
                System.out.println("\nGenerated English config:");
                System.out.println("---");
                Files.lines(englishFile).forEach(System.out::println);
                System.out.println("---\n");
            }

            // Test 2: Load Russian config
            System.out.println("2. Loading MessagesConfig in Russian...");
            LangContext.set("ru");
            config = ConfigManager.reload(MessagesConfig.class);
            
            // Show generated Russian file
            Path russianFile = configDir.resolve("messages_ru.yml");
            if (Files.exists(russianFile)) {
                System.out.println("Generated Russian config:");
                System.out.println("---");
                Files.lines(russianFile).forEach(System.out::println);
                System.out.println("---\n");
            }

            // Test 3: Update config
            System.out.println("3. Updating config...");
            ConfigManager.update(MessagesConfig.class, cfg -> {
                cfg.setWelcomeMessage("Добро пожаловать на сервер!");
                cfg.getBannedWords().add("новое_слово");
            });

            System.out.println("   Updated welcome message and added banned word");
            
            // Show final Russian file
            if (Files.exists(russianFile)) {
                System.out.println("\nFinal Russian config:");
                System.out.println("---");
                Files.lines(russianFile).forEach(System.out::println);
                System.out.println("---\n");
            }

            System.out.println("=== Test completed successfully! ===");
            System.out.println("English config: " + englishFile.toAbsolutePath());
            System.out.println("Russian config: " + russianFile.toAbsolutePath());

        } catch (Exception e) {
            System.err.println("❌ Test failed:");
            e.printStackTrace();
        }
    }
}