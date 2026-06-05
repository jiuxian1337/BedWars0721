package cc.bw0721.utils;

import cc.bw0721.BedWars0721;
import cc.bw0721.asm.ASMTransformer;
import cc.bw0721.asm.TransformerManager;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;

public class NativeUtils {

    public static HashMap<Class, byte[]> cachedClassBytes = new HashMap<>();
    private static boolean loaded;

    static {
        try {
            loadNativeLibrary();
            loaded = true;
        } catch (Exception e) {
            BedWars0721.getInstance().getLogger().warning("Failed to load native library: " + e.getMessage());
        }
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static byte[] b(Class<?> clazz) {
        try {
            return cachedClassBytes.get(clazz);
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new RuntimeException(exception);
        }
    }

    private static void loadNativeLibrary() throws Exception {
        String target = detectTarget();
        String libName;
        if (target.contains("windows")) {
            libName = "NativeUtils.dll";
        } else {
            libName = "libNativeUtils.so";
        }

        String resourcePath = "natives/" + target + "/" + libName;
        InputStream in = NativeUtils.class.getClassLoader().getResourceAsStream(resourcePath);
        if (in == null) {
            throw new RuntimeException("Native library not found in JAR: " + resourcePath);
        }

        File tempFile = File.createTempFile("NativeUtils-", "." + libName);
        tempFile.deleteOnExit();
        Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        in.close();

        System.load(tempFile.getAbsolutePath());
    }

    private static String detectTarget() {
        String arch = detectArch();
        String osSuffix = detectOsSuffix();
        return arch + "-" + osSuffix;
    }

    private static String detectOsSuffix() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "windows";
        }
        if (os.contains("linux")) {
            return "linux-gnu";
        }
        throw new RuntimeException("Unsupported OS: " + os);
    }

    private static String detectArch() {
        String arch = System.getProperty("os.arch").toLowerCase();
        if (arch.equals("amd64") || arch.equals("x86_64")) {
            return "x86_64";
        }
        if (arch.equals("x86") || arch.equals("i386") || arch.equals("i686")) {
            return "x86";
        }
        if (arch.equals("aarch64")) {
            return "aarch64";
        }
        throw new RuntimeException("Unsupported architecture: " + arch);
    }

    public static byte[] a(Class capturedClass, ClassLoader itsClassLoader, String className, byte[] classBytesIn) {
        try {
            for (ASMTransformer asmTransformer : TransformerManager.transform.transformers) {
                if (asmTransformer.getTarget() == capturedClass) {
                    cachedClassBytes.putIfAbsent(capturedClass, classBytesIn);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new RuntimeException(exception);
        }
        return classBytesIn;
    }

    public static native void a(Class cls);

    public static native void b(Class<?> targetClass, byte[] newClassBytes);

}
