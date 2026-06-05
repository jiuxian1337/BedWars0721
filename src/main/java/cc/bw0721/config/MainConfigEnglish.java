package cc.bw0721.config;

import cc.bw0721.utils.XPUtils;
import com.andrei1058.bedwars.BedWars;
import lombok.Getter;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.*;

@Getter
@ConfigSerializable
public class MainConfigEnglish implements MainConfig {
    @Setting("messages.prefix")
    @Comment("Plugin message prefix")
    private String prefix = "&7[&bBedWars0721&7] &r";

    @Setting("messages.experience")
    @Comment("Currency display text in the XP mode shop")
    private String expMsg = "Experience";

    @Setting("messages.experience-color")
    @Comment("Currency display color in the XP mode shop")
    private String expColor = "f";

    @Setting("currency")
    @Comment("Currencies convertible to XP. Key = material name, Value = XP amount")
    private Map<String, Integer> currency = new HashMap<>() {{
        put("IRON_INGOT", 1);
        put("GOLD_INGOT", 10);
        put(XPUtils.getExpBottleMaterial().name(), 10);
        put("EMERALD", 100);
    }};

    @Setting("xp-arenas")
    @Comment("Arenas where XP mode is enabled")
    private List<String> xpArenas = new ArrayList<>();
}
