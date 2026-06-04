package cc.xpWars.config;

import java.util.List;
import java.util.Map;

public interface MainConfig {

    String getPrefix();

    String getLevel();

    Map<String, Integer> getCurrency();

    List<String> getXpArenas();
}
