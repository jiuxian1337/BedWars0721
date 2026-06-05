package cc.bw0721.command;

import com.andrei1058.bedwars.api.BedWars;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.command.CommandSender;

import java.util.List;

@Getter
@AllArgsConstructor
public class SubCommand {
    private String name;
    private String permission;


    public boolean execute(String[] args, CommandSender s) {
        return false;
    }

    public boolean canSee(CommandSender sender) {
        return hasPermission(sender);
    }


    public List<String> getTabComplete(String[] args) {
        return null;
    }

    public boolean hasPermission(CommandSender p) {
        return permission.isEmpty() || p.hasPermission("bw.*") || p.hasPermission("bw0721.*") || p.hasPermission(permission);
    }
}
