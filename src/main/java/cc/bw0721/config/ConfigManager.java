package cc.bw0721.config;

import cc.bw0721.BedWars0721;
import lombok.Getter;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

@Getter
public final class ConfigManager {

    private final Logger logger;
    private MainConfig mainConfig;
    private YamlConfigurationLoader loader;

    public ConfigManager(Logger logger) {
        this.logger = logger;
    }

    public void init() {
        Path configFile = BedWars0721.getInstance().getDataFolder().toPath().resolve("config.yml");

        this.loader = YamlConfigurationLoader.builder()
                .path(configFile)
                .nodeStyle(NodeStyle.BLOCK)
                .build();

        Class<? extends MainConfig> configClass = resolveConfigClass();

        if (Files.exists(configFile)) {
            try {
                ConfigurationNode root = loader.load();
                mainConfig = root.get(configClass);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to load config.yml, using defaults", e);
                mainConfig = newDefault(configClass);
            }
        } else {
            mainConfig = newDefault(configClass);
            save();
            logger.info("Created default config.yml (" + configClass.getSimpleName() + ")");
        }

    }

    public void save() {
        if (loader == null || mainConfig == null) {
            return;
        }
        try {
            ConfigurationNode root = loader.createNode();
            root.set(mainConfig.getClass(), mainConfig);
            loader.save(root);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save config.yml", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends MainConfig> resolveConfigClass() {
        String lang = Locale.getDefault().getLanguage().toLowerCase(Locale.ROOT);
        if (lang.equals("zh")) {
            return MainConfigChinese.class;
        }
        return MainConfigEnglish.class;
    }

    private static MainConfig newDefault(Class<? extends MainConfig> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to instantiate config class: " + clazz.getName(), e);
        }
    }
}
