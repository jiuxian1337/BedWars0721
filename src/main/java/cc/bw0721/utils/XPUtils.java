package cc.bw0721.utils;

import cc.bw0721.BedWars0721;
import cc.bw0721.config.MainConfig;
import com.andrei1058.bedwars.BedWars;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Sound;

import java.util.Map;

public class XPUtils {

    @Getter
    private final static Sound sound = Sound.valueOf(BedWars.getForCurrentVersion("ORB_PICKUP", "ENTITY_EXPERIENCE_ORB_PICKUP", "ENTITY_EXPERIENCE_ORB_PICKUP"));

    public static int getExp(Material material) {
        MainConfig mainConfig = BedWars0721.getInstance().getConfigManager().getMainConfig();
        Map<String, Integer> currency = mainConfig.getCurrency();
        return currency.getOrDefault(material.name(), 0);
    }

    public static boolean isXPArena(String arena) {
        MainConfig mainConfig = BedWars0721.getInstance().getConfigManager().getMainConfig();
        return mainConfig.getXpArenas().contains(arena);
    }

}
