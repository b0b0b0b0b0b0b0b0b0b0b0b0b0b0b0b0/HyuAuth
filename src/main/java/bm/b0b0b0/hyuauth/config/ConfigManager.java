package bm.b0b0b0.hyuauth.config;

import org.yaml.snakeyaml.Yaml;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ConfigManager {
    private Map<String, Object> config;
    private final Path configPath;

    public ConfigManager(Path dataDirectory) {
        this.configPath = dataDirectory.resolve("config.yml");
        loadConfig();
    }

    @SuppressWarnings("unchecked")
    private void loadConfig() {
        try {
            if (!Files.exists(configPath)) {
                copyDefaultConfig();
            }
            
            Yaml yaml = new Yaml();
            try (InputStream inputStream = Files.newInputStream(configPath)) {
                config = yaml.load(inputStream);
            }
            
            if (config == null) {
                config = Map.of();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config", e);
        }
    }

    private void copyDefaultConfig() throws IOException {
        try (InputStream defaultConfig = getClass().getClassLoader().getResourceAsStream("config.yml")) {
            if (defaultConfig != null) {
                Files.createDirectories(configPath.getParent());
                Files.copy(defaultConfig, configPath);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public int getLoginTimeoutSeconds() {
        Map<String, Object> settings = (Map<String, Object>) config.get("settings");
        if (settings != null) {
            Object timeoutObj = settings.get("login_timeout_seconds");
            if (timeoutObj instanceof Integer) {
                return (Integer) timeoutObj;
            }
        }
        return 60;
    }

    @SuppressWarnings("unchecked")
    public int getSessionTimeoutMinutes() {
        Map<String, Object> settings = (Map<String, Object>) config.get("settings");
        if (settings != null) {
            Object timeoutObj = settings.get("session_timeout_minutes");
            if (timeoutObj instanceof Integer) {
                return (Integer) timeoutObj;
            }
        }
        return 5;
    }

    @SuppressWarnings("unchecked")
    public String getLanguage() {
        Map<String, Object> settings = (Map<String, Object>) config.get("settings");
        if (settings != null) {
            Object lang = settings.get("language");
            if (lang instanceof String) {
                return (String) lang;
            }
        }
        return "ru";
    }

    @SuppressWarnings("unchecked")
    public String getDatabaseFileName() {
        Map<String, Object> settings = (Map<String, Object>) config.get("settings");
        if (settings != null) {
            Map<String, Object> db = (Map<String, Object>) settings.get("database");
            if (db != null) {
                Object file = db.get("file");
                if (file instanceof String) {
                    return (String) file;
                }
            }
        }
        return "auth.db";
    }

    public Path getConfigPath() {
        return configPath;
    }

    public void setLoginTimeoutSeconds(int seconds) {
        @SuppressWarnings("unchecked")
        Map<String, Object> settings = (Map<String, Object>) config.get("settings");
        if (settings == null) {
            settings = new java.util.HashMap<>();
            config.put("settings", settings);
        }
        settings.put("login_timeout_seconds", seconds);
        saveConfig();
    }

    private void saveConfig() {
        try {
            Yaml yaml = new Yaml();
            yaml.dump(config, Files.newBufferedWriter(configPath));
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config", e);
        }
    }
}
