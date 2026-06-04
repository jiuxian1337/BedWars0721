package cc.xpWars.command.impl;

import cc.xpWars.XPWars;
import cc.xpWars.command.SubCommand;
import cc.xpWars.config.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class ReloadCommand extends SubCommand {

    public ReloadCommand() {
        super("reload", "xpwars.command.reload");
    }

    @Override
    public boolean execute(String[] args, CommandSender sender) {
        ConfigManager configManager = XPWars.getInstance().getConfigManager();
        configManager.init();
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getMainConfig().getPrefix() + "Config reloaded."));
        return true;
    }
}
