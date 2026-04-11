package xyz.overdyn.dynconfig;

import xyz.overdyn.dynconfig.annotation.Comment;
import xyz.overdyn.dynconfig.annotation.ConfigKey;
import xyz.overdyn.dynconfig.annotation.ConfigResource;
import xyz.overdyn.dynconfig.policy.MissingKeyPolicy;

import java.util.ArrayList;
import java.util.List;

@ConfigResource(
    path = "test-config.yml",
    missingKeyPolicy = MissingKeyPolicy.WRITE_DEFAULT //@!
)
public class TestConfig {

    @Comment({
        @Comment.Entry(lang = "en", lines = {"Server port number", "Default: 8080"}),
        @Comment.Entry(lang = "ru", lines = {"Номер порта сервера", "По умолчанию: 8080"})
    })
    private int port = 8080;

    @ConfigKey("database.host")
    @Comment({
        @Comment.Entry(lang = "en", lines = {"Database host address"}),
        @Comment.Entry(lang = "ru", lines = {"Адрес хоста базы данных"})
    })
    private String dbHost = "localhost";

    @ConfigKey("database.port")
    @Comment({
        @Comment.Entry(lang = "en", lines = {"Database port number"}),
        @Comment.Entry(lang = "ru", lines = {"Номер порта базы данных"})
    })
    private int dbPort = 3306;

    @Comment({
        @Comment.Entry(lang = "en", lines = {"List of enabled features"}),
        @Comment.Entry(lang = "ru", lines = {"Список включенных функций"})
    })
    private List<String> features = new ArrayList<>(List.of("feature1", "feature2", "feature3"));

    @Comment({
        @Comment.Entry(lang = "en", lines = {"Enable debug mode for detailed logging"}),
        @Comment.Entry(lang = "ru", lines = {"Включить режим отладки для подробного логирования"})
    })
    private boolean debugMode = false;

    // Getters and setters
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDbHost() {
        return dbHost;
    }

    public void setDbHost(String dbHost) {
        this.dbHost = dbHost;
    }

    public int getDbPort() {
        return dbPort;
    }

    public void setDbPort(int dbPort) {
        this.dbPort = dbPort;
    }

    public List<String> getFeatures() {
        return features;
    }

    public void setFeatures(List<String> features) {
        this.features = features;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }
}
