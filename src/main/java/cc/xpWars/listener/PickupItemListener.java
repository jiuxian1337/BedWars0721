package cc.xpWars.listener;

import cc.xpWars.utils.XPUtils;
import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.arena.Arena;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;

public class PickupItemListener implements Listener {
    @EventHandler
    public void onPickupItem(PlayerPickupItemEvent event){
        Player player = event.getPlayer();
        IArena arena = Arena.getArenaByPlayer(player);
        if (arena == null || !XPUtils.isXPArena(arena.getArenaName()) || !arena.isPlayer(event.getPlayer()) || arena.isSpectator(event.getPlayer())) return;
        Item item = event.getItem();
        int xp = XPUtils.getLevel(item.getItemStack().getType()) * item.getItemStack().getAmount();
        if (xp != 0 && !event.isCancelled()){
            event.setCancelled(true);
            player.playSound(player.getLocation(), XPUtils.getSound(), 0.6f, 1.3f);
            player.setLevel(player.getLevel() + xp);
            item.remove();
        }
    }
}
