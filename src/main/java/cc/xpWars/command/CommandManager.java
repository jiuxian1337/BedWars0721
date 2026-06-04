package cc.xpWars.command;

import cc.xpWars.XPWars;
import cc.xpWars.command.impl.AddArenaCommand;
import cc.xpWars.command.impl.ReloadCommand;
import com.andrei1058.bedwars.BedWars;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandManager implements CommandExecutor, TabCompleter {

    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public CommandManager() {
        register(new ReloadCommand());
        register(new AddArenaCommand());
    }

    private void register(SubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return false;
        }

        SubCommand subCommand = subCommands.get(args[0].toLowerCase());
        if (subCommand == null) {
            return false;
        }

        if (!subCommand.hasPermission(sender)) {
            String prefix = XPWars.getInstance().getConfigManager().getMainConfig().getPrefix();
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + ChatColor.RED + "No permission."));
            return true;
        }

        String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, subArgs.length);
        return subCommand.execute(subArgs, sender);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            for (SubCommand subCommand : subCommands.values()) {
                if (subCommand.canSee(sender) && subCommand.getName().startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand.getName());
                }
            }
            return completions;
        }

        SubCommand subCommand = subCommands.get(args[0].toLowerCase());
        if (subCommand != null && subCommand.canSee(sender)) {
            String[] subArgs = new String[args.length - 1];
            System.arraycopy(args, 1, subArgs, 0, subArgs.length);
            return subCommand.getTabComplete(subArgs);
        }

        return null;
    }
}
