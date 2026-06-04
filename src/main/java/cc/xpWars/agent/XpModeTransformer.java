package cc.xpWars.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class XpModeTransformer implements ClassFileTransformer {

    private static final Logger LOG = Logger.getLogger("XPWars-Agent");

    private static final Map<String, Function<ClassVisitor, ClassVisitor>> hooks =
            new ConcurrentHashMap<>();

    public static void register(String internalName,
                                Function<ClassVisitor, ClassVisitor> factory) {
        hooks.put(internalName, factory);
        LOG.info("Hook registered for: " + internalName);
    }

    public static void unregister(String internalName) {
        hooks.remove(internalName);
        LOG.info("Hook unregistered for: " + internalName);
    }

    public static void clearAll() {
        hooks.clear();
        LOG.info("All hooks cleared");
    }

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) {

        Function<ClassVisitor, ClassVisitor> factory = hooks.get(className);
        if (factory == null) {
            return null;
        }

        try {
            ClassReader cr = new ClassReader(classfileBuffer);
            ClassWriter cw = new ClassWriter(
                    cr,
                    ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES
            );
            ClassVisitor cv = factory.apply(cw);
            cr.accept(cv, 0);

            LOG.info("Transformed: " + className);
            return cw.toByteArray();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Transform failed for " + className, e);
            return null;
        }
    }
}
