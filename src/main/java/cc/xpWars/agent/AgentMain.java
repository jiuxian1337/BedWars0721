package cc.xpWars.agent;

import java.lang.instrument.Instrumentation;
import java.util.logging.Logger;

public class AgentMain {

    private static final Logger LOG = Logger.getLogger("XPWars-Agent");

    public static void premain(String args, Instrumentation inst) {
        agentmain(args, inst);
    }

    public static void agentmain(String args, Instrumentation inst) {
        LOG.info("Agent loaded — registering transformers");

        inst.addTransformer(new XpModeTransformer(), true);

        retransformLoadedClasses(inst);

        LOG.info("Agent initialization complete");
    }

    private static void retransformLoadedClasses(Instrumentation inst) {
        Class<?>[] loaded = inst.getAllLoadedClasses();
        int count = 0;

        for (Class<?> clazz : loaded) {
            if (!isBedWarsClass(clazz)) {
                continue;
            }
            if (!inst.isModifiableClass(clazz)) {
                continue;
            }
            try {
                inst.retransformClasses(clazz);
                count++;
            } catch (Exception e) {
                LOG.warning("Failed to retransform " + clazz.getName() + ": " + e.getMessage());
            }
        }

        LOG.info("Retransformed " + count + " BedWars1058 class(es)");
    }

    private static boolean isBedWarsClass(Class<?> clazz) {
        String name = clazz.getName();
        return name.startsWith("com.andrei1058.bedwars.")
                || name.startsWith("com.andrei1058.bedwars.api.");
    }
}
