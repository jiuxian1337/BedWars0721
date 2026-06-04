package cc.xpWars.agent;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AgentInjector {

    private static final Logger LOG = Logger.getLogger("XPWars-AgentInjector");

    private AgentInjector() {
    }

    public static boolean inject() {
        try {
            String pid = getPid();
            String jarPath = getOwnJarPath();
            if (jarPath == null) {
                LOG.severe("Cannot resolve own JAR path — injection aborted");
                return false;
            }

            LOG.info("Self-attaching agent (PID=" + pid + ", JAR=" + jarPath + ")");

            if (isJava9OrLater()) {
                injectJava9Plus(pid, jarPath);
            } else {
                injectJava8(pid, jarPath);
            }

            LOG.info("Agent injected successfully");
            return true;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Agent injection failed", e);
            return false;
        }
    }

    private static String getPid() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        return name.split("@")[0];
    }

    private static String getOwnJarPath() {
        CodeSource cs = AgentInjector.class.getProtectionDomain().getCodeSource();
        if (cs == null) {
            return null;
        }
        try {
            return new File(cs.getLocation().toURI()).getAbsolutePath();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to resolve JAR path from CodeSource", e);
            return null;
        }
    }

    private static boolean isJava9OrLater() {
        try {
            Class.forName("java.lang.Module");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static void injectJava8(String pid, String jarPath) throws Exception {
        File toolsJar = findToolsJar();
        LOG.info("Java 8 detected — loading tools.jar from " + toolsJar.getAbsolutePath());

        URLClassLoader cl = new URLClassLoader(
                new URL[]{toolsJar.toURI().toURL()},
                ClassLoader.getSystemClassLoader()
        );

        try {
            Class<?> vmClass = cl.loadClass("com.sun.tools.attach.VirtualMachine");
            Object vm = vmClass.getMethod("attach", String.class).invoke(null, pid);
            try {
                vmClass.getMethod("loadAgent", String.class).invoke(vm, jarPath);
            } finally {
                vmClass.getMethod("detach").invoke(vm);
            }
        } finally {
            cl.close();
        }
    }

    private static File findToolsJar() {
        String javaHome = System.getProperty("java.home");

        File tools = new File(javaHome, "../lib/tools.jar");
        if (tools.exists()) {
            return tools;
        }

        tools = new File(javaHome, "lib/tools.jar");
        if (tools.exists()) {
            return tools;
        }

        throw new IllegalStateException(
                "tools.jar not found under " + javaHome + " — is this a JDK (not a JRE)?"
        );
    }

    private static void injectJava9Plus(String pid, String jarPath) throws Exception {
        Instrumentation inst = getInstrumentationViaSharedSecrets();
        if (inst != null) {
            LOG.info("Java 9+ — using SharedSecrets to get Instrumentation directly");
            AgentMain.agentmain(null, inst);
            return;
        }

        inst = getInstrumentationViaAgentLoader(jarPath);
        if (inst != null) {
            LOG.info("Java 9+ — using HotSpotDiagnosticMXBean agent path injection");
            AgentMain.agentmain(null, inst);
            return;
        }

        throw new IllegalStateException(
                "Cannot obtain Instrumentation on Java 9+. "
                        + "Add -Djdk.attach.allowAttachSelf=true to JVM arguments."
        );
    }

    private static Instrumentation getInstrumentationViaSharedSecrets() {
        try {
            Class<?> c = Class.forName("jdk.internal.access.SharedSecrets");
            Object access = c.getMethod("getJavaLangInstrumentAccess").invoke(null);
            Method getInst = access.getClass().getMethod("getInstrumentation");
            return (Instrumentation) getInst.invoke(access);
        } catch (Exception e) {
            LOG.warning("SharedSecrets unavailable: " + e.getMessage());
            return null;
        }
    }

    private static Instrumentation getInstrumentationViaAgentLoader(String jarPath) {
        try {
            Class<?> c = Class.forName("com.sun.tools.attach.VirtualMachine");
            Object vm = c.getMethod("attach", String.class).invoke(null, getPid());
            try {
                c.getMethod("loadAgent", String.class).invoke(vm, jarPath);
            } finally {
                c.getMethod("detach").invoke(vm);
            }
        } catch (Exception e) {
            LOG.warning("VirtualMachine.attach self-attach blocked: " + e.getMessage());
            return null;
        }
        return getInstrumentationViaSharedSecrets();
    }
}
