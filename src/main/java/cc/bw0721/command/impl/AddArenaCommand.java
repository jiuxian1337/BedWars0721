package cc.bw0721.command.impl;

import cc.bw0721.BedWars0721;
import cc.bw0721.command.SubCommand;
import cc.bw0721.config.ConfigManager;
import com.andrei1058.bedwars.BedWars;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AddArenaCommand extends SubCommand {

    public AddArenaCommand() {
        super("addxparena", "bw0721.command.addxparena");
    }

    @Override
    public boolean execute(String[] args, CommandSender sender) {
        ConfigManager configManager = BedWars0721.getInstance().getConfigManager();
        if (args.length < 1) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getMainConfig().getPrefix() + ChatColor.RED + "Usage: /bw0721 addxparena <arena>"));
            return true;
        }
        if (configManager.getMainConfig().getXpArenas().contains(args[0])) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getMainConfig().getPrefix() + ChatColor.RED + "Arena already exists: " + args[0]));
            return true;
        }
        configManager.getMainConfig().getXpArenas().add(args[0]);
        configManager.save();
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getMainConfig().getPrefix() + "Arena added: " + args[0]));
        return true;
    }

    @Override
    public List<String> getTabComplete(String[] args) {
        List<String> tab = new ArrayList<>();
        File dir = new File(BedWars.plugin.getDataFolder(), "/Arenas");
        if (dir.exists()) {
            File[] fls = dir.listFiles();
            for (File fl : Objects.requireNonNull(fls)) {
                if (fl.isFile()) {
                    if (fl.getName().contains(".yml")) {
                        String replace = fl.getName().replace(".yml", "");
                        if (replace.startsWith(args[0])) {
                            tab.add(replace);
                        }
                    }
                }
            }
        }
        return tab;
    }
}
