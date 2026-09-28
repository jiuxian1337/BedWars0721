package cc.bw0721.listener;

import cc.bw0721.utils.XPUtils;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.arena.Arena;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class DeathListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeathEarly(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        IArena arena = Arena.getArenaByPlayer(victim);
        if (arena == null || !XPUtils.isXPArena(arena.getArenaName())) return;
        event.setDroppedExp(0);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeathLate(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        IArena arena = Arena.getArenaByPlayer(victim);
        if (arena == null || !XPUtils.isXPArena(arena.getArenaName())) return;
        if (!arena.isPlayer(victim) || arena.isSpectator(victim)) return;

        int level = victim.getLevel();
        if (level <= 0) return;

        Player killer = victim.getKiller();
        if (killer != null && !killer.equals(victim) && arena.isPlayer(killer) && !arena.isReSpawning(killer)) {
            ITeam victimsTeam = arena.getTeam(victim);
            if (victimsTeam != null && !victimsTeam.isBedDestroyed()) {
                killer.setLevel(killer.getLevel() + level);
            }
        }

        victim.setLevel(0);
        victim.setExp(0f);
    }
}
