package cc.xpWars;

import cc.xpWars.asm.TransformerManager;
import cc.xpWars.command.CommandManager;
import cc.xpWars.config.ConfigManager;
import cc.xpWars.listener.PickupItemListener;
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

            Library asmTree = Library.builder()
                    .groupId("org.ow2.asm")
                    .artifactId("asm-tree")
                    .version("9.10.1")
                    .resolveTransitiveDependencies(true)
                    .build();

            libraryManager.loadLibraries(configurate, asm, asmTree);

            configManager = new ConfigManager(getLogger());
            configManager.init();

            CommandManager commandManager = new CommandManager();
            getCommand("xpwars").setExecutor(commandManager);
            getCommand("xpwars").setTabCompleter(commandManager);
            TransformerManager.init();
            getServer().getPluginManager().registerEvents(new PickupItemListener(), this);
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
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        console.sendMessage("-------------------------------------------------");
        console.sendMessage(ChatColor.translateAlternateColorCodes('&', "&r "));
        console.sendMessage(this.pluginName + " v" + this.pluginVersion);
        console.sendMessage(ChatColor.translateAlternateColorCodes('&', "&r "));
        console.sendMessage("Successfully Loaded");
        console.sendMessage(ChatColor.translateAlternateColorCodes('&', "&r "));
        console.sendMessage(ChatColor.translateAlternateColorCodes('&', hookMessage));
        console.sendMessage(ChatColor.translateAlternateColorCodes('&', "&r "));
        console.sendMessage("Author - jiuxian_baka");
        console.sendMessage(ChatColor.translateAlternateColorCodes('&', "&r "));
        console.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Running Java &f" + System.getProperty("java.version")));
        console.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Running &f" + Bukkit.getServer().getName() + " &7fork &fv" + Bukkit.getServer().getBukkitVersion()));
        console.sendMessage("-------------------------------------------------");
        console.sendMessage(ChatColor.translateAlternateColorCodes('&', "&r "));
    }

}
