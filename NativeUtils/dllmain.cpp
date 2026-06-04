#define _CRT_SECURE_NO_WARNINGS

#include <stdio.h>
#include <iostream>
#include <cstring>

#include "jni.h"
#include "jvmti.h"
#include "jni_md.h"

jvmtiEnv *jvmti_env_global;
jobject saved_classloader = NULL;

jclass LoadClass(JNIEnv *jni_env, const char *ClassName, jobject TargetCL) {
    jmethodID loadClass = jni_env->GetMethodID(jni_env->GetObjectClass(TargetCL), "loadClass",
                                               "(Ljava/lang/String;)Ljava/lang/Class;");
    return (jclass) jni_env->CallObjectMethod(TargetCL, loadClass, jni_env->NewStringUTF(ClassName));
}

void ProcessHookedClassFile(jvmtiEnv *jvmti_env, JNIEnv *jni_env,
                            jclass captured_class, jobject its_own_classloader, const char *its_name,
                            jobject protection_domain, jint its_data_len, const unsigned char *its_data,
                            jint *replace_with_len, unsigned char **replace_with) {
    if (captured_class == NULL)return;
    if (its_own_classloader == NULL)return;
    if (its_name == NULL)return;
    if (saved_classloader == NULL)return;

    jbyteArray old = jni_env->NewByteArray(its_data_len);
    jni_env->SetByteArrayRegion(old, 0, its_data_len, (jbyte *) its_data);

    jclass ModifierClass = LoadClass(jni_env, "a.a", saved_classloader);
    jmethodID ModifierMethod = jni_env->GetStaticMethodID(ModifierClass, "a",
                                                          "(Ljava/lang/Class;Ljava/lang/ClassLoader;Ljava/lang/String;[B)[B");
    jbyteArray result = (jbyteArray) jni_env->CallStaticObjectMethod(ModifierClass, ModifierMethod, captured_class,
                                                                     its_own_classloader,
                                                                     jni_env->NewStringUTF(its_name), old);

    jsize new_size = jni_env->GetArrayLength(result);
    unsigned char *new_ = nullptr;
    jvmti_env->Allocate(new_size, &new_);
    jni_env->GetByteArrayRegion(result, 0, new_size, (jbyte *) new_);
    *replace_with_len = new_size;
    *replace_with = new_;
}

void SetJVMTIClassFileLoadHook(jvmtiEnv *jvmti_env) {
    jvmtiCapabilities capas{};
    capas.can_generate_all_class_hook_events = JVMTI_ENABLE;
    capas.can_retransform_any_class = JVMTI_ENABLE;
    capas.can_retransform_classes = JVMTI_ENABLE;
    capas.can_redefine_any_class = JVMTI_ENABLE;
    capas.can_redefine_classes = JVMTI_ENABLE;
    jvmti_env->AddCapabilities(&capas);
    jvmtiEventCallbacks callbacks{};
    callbacks.ClassFileLoadHook = ProcessHookedClassFile;
    jvmti_env->SetEventCallbacks(&callbacks, sizeof(jvmtiEventCallbacks));
    jvmti_env->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, 0);
}

JNIEXPORT jint JNI_OnLoad(JavaVM *java_vm, void *reserved) {
    java_vm->GetEnv((void **) &jvmti_env_global, JVMTI_VERSION);
    SetJVMTIClassFileLoadHook(jvmti_env_global);
    return JNI_VERSION_10;
}

void saveClassLoader(JNIEnv *jni_env, jclass caller) {
    if (saved_classloader != NULL) {
        return;
    }
    jclass classClass = jni_env->FindClass("java/lang/Class");
    jmethodID getClassLoader = jni_env->GetMethodID(classClass, "getClassLoader", "()Ljava/lang/ClassLoader;");
    jobject cl = jni_env->CallObjectMethod(caller, getClassLoader);
    saved_classloader = jni_env->NewGlobalRef(cl);
}

extern "C" {
JNIEXPORT void JNICALL Java_a_a_a(JNIEnv *jni_env, jclass caller, jclass arg1) {
    saveClassLoader(jni_env, caller);
    jvmti_env_global->RetransformClasses(1, &arg1);
}

JNIEXPORT void JNICALL Java_a_a_b(JNIEnv *jni_env, jclass caller, jclass target, jbyteArray data) {
    saveClassLoader(jni_env, caller);
    jvmtiClassDefinition classDef;
    classDef.klass = target;
    classDef.class_byte_count = jni_env->GetArrayLength(data);
    classDef.class_bytes = (unsigned char *) jni_env->GetByteArrayElements(data, NULL);
    jvmti_env_global->RedefineClasses(1, &classDef);
}
}
