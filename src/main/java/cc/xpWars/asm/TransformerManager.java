package cc.xpWars.asm;

import cc.xpWars.transformer.OreGeneratorTransformer;
import cc.xpWars.utils.NativeUtils;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

/*
 * 一个简单的asm注入框架
 * 原作者: Loratadine (Cherish)
 * 修改: 玖弦, 手淫
 */

//@Nativeify
public class TransformerManager {
    public static final boolean debugging = true;
    public static Transform transform;
    public static Map<String, byte[]> classBytesMap;
    private static boolean loaded = false;

    public static void init() {
        if (loaded) return;


        loaded = true;

        transform = new Transform();

        try {
//            在这里注册你的变形金刚
            transform.addTransformer(new OreGeneratorTransformer());
            for (ASMTransformer asmTransformer : TransformerManager.transform.transformers) {
                NativeUtils.a(asmTransformer.getTarget());
            }


            classBytesMap = transform.transform();


            for (Map.Entry<String, byte[]> entry : classBytesMap.entrySet()) {
                try {
                    NativeUtils.b(Class.forName(entry.getKey()), entry.getValue());
                    if (debugging) {
                        File debugDir = new File("debug");
                        debugDir.mkdirs();
                        Files.write(new File(debugDir, entry.getKey() + ".class").toPath(), entry.getValue());
                    }
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    System.out.println("Failed to reload class:" + entry.getKey() + "\n" + ex.getMessage());
                }
            }


        } catch (Throwable ex) {
            System.out.println("Inject failed.");
            ex.printStackTrace();
            throw ex;
        }
    }


}
