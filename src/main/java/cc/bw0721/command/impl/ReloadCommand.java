package cc.bw0721.command.impl;

import cc.bw0721.BedWars0721;
import cc.bw0721.command.SubCommand;
import cc.bw0721.config.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class ReloadCommand extends SubCommand {

    public ReloadCommand() {
        super("reload", "bw0721.command.reload");
    }

    @Override
    public boolean execute(String[] args, CommandSender sender) {
        ConfigManager configManager = BedWars0721.getInstance().getConfigManager();
        configManager.init();
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getMainConfig().getPrefix() + "Config reloaded."));
        return true;
    }
}
