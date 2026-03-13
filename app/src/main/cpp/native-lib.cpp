#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_devilking_os_ai_LocalAICore_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "> [C++ NDK ENGINE ONLINE]: Hardware bridge established.";
    return env->NewStringUTF(hello.c_str());
}
