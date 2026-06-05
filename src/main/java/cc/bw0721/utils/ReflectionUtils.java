package cc.bw0721.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @Author：jiuxian_baka
 * @Date：2025/11/30 02:39
 * @Filename：ReflectionUtils
 */
public class ReflectionUtils {
    public static Class<?> getClass(String classFullName) {
        try {
            return Class.forName(classFullName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Method getMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            Method method = clazz.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Field getField(Class<?> clazz, String name) {
        try {
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object getFieldValue(Class<?> clazz, String name, Object obj) {
        try {
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            Object value = field.get(obj);
            return value;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Boolean setFieldValue(Class<?> clazz, String name, Object obj, Object value) {
        try {
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            field.set(obj, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
