package cc.xpWars;

import cc.xpWars.command.CommandManager;
import cc.xpWars.config.ConfigManager;
import com.alessiodp.libby.Library;
import com.alessiodp.libby.BukkitLibraryManager;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class XPWars extends JavaPlugin {

    @Getter
    private static XPWars instance;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        instance = this;

        BukkitLibraryManager libraryManager = new BukkitLibraryManager(this);
        libraryManager.addRepository("https://maven.aliyun.com/nexus/content/groups/public/");
        libraryManager.addMavenCentral();

        Library configurateYaml = Library.builder()
                .groupId("org.spongepowered")
                .artifactId("configurate-yaml")
                .version("4.2.0")
                .resolveTransitiveDependencies(true)
                .build();

        libraryManager.loadLibraries(configurateYaml);

        configManager = new ConfigManager(getLogger());
        configManager.init();

        CommandManager commandManager = new CommandManager();
        getCommand("xpwars").setExecutor(commandManager);
        getCommand("xpwars").setTabCompleter(commandManager);
    }

    @Override
    public void onDisable() {
    }

}
