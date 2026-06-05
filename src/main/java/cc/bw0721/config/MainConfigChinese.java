package cc.bw0721.config;

import com.andrei1058.bedwars.BedWars;
import lombok.Getter;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.*;

@Getter
@ConfigSerializable
public class MainConfigChinese implements MainConfig {
    @Setting("messages.prefix")
    @Comment("插件消息前缀")
    private String prefix = "&7[&bBedWars0721&7] &r";

    @Setting("messages.level")
    @Comment("经验模式商店中货币的文本")
    private String level = "&f经验";

    @Setting("currency")
    @Comment("可转换成经验的货币")
    private Map<String, Integer> currency = new HashMap<>() {{
        put("IRON_INGOT", 1);
        put("GOLD_INGOT", 10);
        put(BedWars.getForCurrentVersion("EXP_BOTTLE", "EXP_BOTTLE", "EXPERIENCE_BOTTLE"), 10);
        put("EMERALD", 100);
    }};

    @Setting("xp-arenas")
    @Comment("启用 XP 模式的 arena 列表")
    private List<String> xpArenas = new ArrayList<>();
}
