package cc.bw0721.config;

import java.util.List;
import java.util.Map;

public interface MainConfig {

    String getPrefix();

    String getExpMsg();

    Map<String, Integer> getCurrency();

    List<String> getXpArenas();
}
