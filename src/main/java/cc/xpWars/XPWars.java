package cc.xpWars;

import cc.xpWars.command.CommandManager;
import cc.xpWars.config.ConfigManager;
import com.alessiodp.libby.Library;
import com.alessiodp.libby.BukkitLibraryManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class XPWars extends JavaPlugin {

    private final String pluginVersion = getDescription().getVersion();
    private final String pluginName = getDescription().getName();

    @Getter
    private static XPWars instance;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        if (isBedWars1058Present()) {
            instance = this;

            BukkitLibraryManager libraryManager = new BukkitLibraryManager(this);
            libraryManager.addRepository("https://maven.aliyun.com/nexus/content/groups/public/");
            libraryManager.addMavenCentral();

            Library configurate = Library.builder()
                    .groupId("org.spongepowered")
                    .artifactId("configurate-yaml")
                    .version("4.2.0")
                    .resolveTransitiveDependencies(true)
                    .build();

            Library asm = Library.builder()
                    .groupId("org.ow2.asm")
                    .artifactId("asm")
                    .version("9.10.1")
                    .resolveTransitiveDependencies(true)
                    .build();

            libraryManager.loadLibraries(configurate, asm);

            configManager = new ConfigManager(getLogger());
            configManager.init();

            CommandManager commandManager = new CommandManager();
            getCommand("xpwars").setExecutor(commandManager);
            getCommand("xpwars").setTabCompleter(commandManager);
            printStartupMessage("&fBedWars1058 &7found and hooked successfully.");
        } else {
            getLogger().warning("There is no BedWars plugin installed!");
            getLogger().warning("Disabling...");
            Bukkit.getPluginManager().disablePlugin(this);
            Bukkit.getScheduler().cancelTasks(this);
        }
    }

    @Override
    public void onDisable() {
    }

    public static boolean isBedWars1058Present() {
        return Bukkit.getPluginManager().getPlugin("BedWars1058") != null;
    }

    private void printStartupMessage(String hookMessage) {
        getLogger().info("-------------------------------------------------");
        getLogger().info(ChatColor.translateAlternateColorCodes('&', "&r "));
        getLogger().info(this.pluginName + " v" + this.pluginVersion);
        getLogger().info(ChatColor.translateAlternateColorCodes('&', "&r "));
        getLogger().info("Successfully Loaded");
        getLogger().info(ChatColor.translateAlternateColorCodes('&', "&r "));
        getLogger().info(ChatColor.translateAlternateColorCodes('&', hookMessage));
        getLogger().info(ChatColor.translateAlternateColorCodes('&', "&r "));
        getLogger().info("Author - jiuxian_baka");
        getLogger().info(ChatColor.translateAlternateColorCodes('&', "&r "));
        getLogger().info(ChatColor.translateAlternateColorCodes('&', "&7Running Java &f" + System.getProperty("java.version")));
        getLogger().info(ChatColor.translateAlternateColorCodes('&', "&7Running &f" + Bukkit.getServer().getName() + " &7fork &fv" + Bukkit.getServer().getBukkitVersion()));
        getLogger().info("-------------------------------------------------");
        getLogger().info(ChatColor.translateAlternateColorCodes('&', "&r "));
    }

}
