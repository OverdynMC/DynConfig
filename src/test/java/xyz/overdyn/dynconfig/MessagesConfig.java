package xyz.overdyn.dynconfig;

import xyz.overdyn.dynconfig.annotation.Comment;
import xyz.overdyn.dynconfig.annotation.ConfigKey;
import xyz.overdyn.dynconfig.annotation.ConfigResource;
import xyz.overdyn.dynconfig.policy.MissingKeyPolicy;

import java.util.ArrayList;
import java.util.List;

/**
 * Example messages configuration with multi-language comments.
 */
@ConfigResource(
    path = "messages_{lang}.yml",
    missingKeyPolicy = MissingKeyPolicy.WRITE_DEFAULT
)
public class MessagesConfig {

    @Comment({
        @Comment.Entry(lang = "en", lines = {"Welcome message shown to new players"}),
        @Comment.Entry(lang = "ru", lines = {"Приветственное сообщение для новых игроков"})
    })
    @ConfigKey("messages.welcome")
    private String welcomeMessage = "Welcome to the server!";

    @Comment({
        @Comment.Entry(lang = "en", lines = {"Goodbye message shown when players leave"}),
        @Comment.Entry(lang = "ru", lines = {"Прощальное сообщение при выходе игроков"})
    })
    @ConfigKey("messages.goodbye")
    private String goodbyeMessage = "Thanks for playing!";

    @Comment({
        @Comment.Entry(lang = "en", lines = {"List of banned words that will be filtered"}),
        @Comment.Entry(lang = "ru", lines = {"Список запрещенных слов для фильтрации"})
    })
    @ConfigKey("filter.banned_words")
    private List<String> bannedWords = new ArrayList<>(List.of("spam", "hack", "cheat"));

    @Comment({
        @Comment.Entry(lang = "en", lines = {"Enable chat filtering", "Set to false to disable word filtering"}),
        @Comment.Entry(lang = "ru", lines = {"Включить фильтрацию чата", "Установите false для отключения фильтрации"})
    })
    @ConfigKey("filter.enabled")
    private boolean filterEnabled = true;

    // Static method for easy access
    public static MessagesConfig get() {
        return ConfigManager.get(MessagesConfig.class);
    }

    // Getters and setters
    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public void setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
    }

    public String getGoodbyeMessage() {
        return goodbyeMessage;
    }

    public void setGoodbyeMessage(String goodbyeMessage) {
        this.goodbyeMessage = goodbyeMessage;
    }

    public List<String> getBannedWords() {
        return bannedWords;
    }

    public void setBannedWords(List<String> bannedWords) {
        this.bannedWords = bannedWords;
    }

    public boolean isFilterEnabled() {
        return filterEnabled;
    }

    public void setFilterEnabled(boolean filterEnabled) {
        this.filterEnabled = filterEnabled;
    }
}