package cc.xpWars.utils;

import cc.xpWars.XPWars;
import cc.xpWars.config.MainConfig;
import org.bukkit.Material;

import java.util.Map;

public class XPUtils {
    public static int getLevel(Material material) {
        MainConfig mainConfig = XPWars.getInstance().getConfigManager().getMainConfig();
        Map<String, Integer> currency = mainConfig.getCurrency();
        return currency.getOrDefault(material.name(), 0);
    }

    public static boolean isXPArena(String arena) {
        MainConfig mainConfig = XPWars.getInstance().getConfigManager().getMainConfig();
        return mainConfig.getXpArenas().contains(arena);
    }
}
