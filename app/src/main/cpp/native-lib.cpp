#include <jni.h>
#include <string>
#include <fstream>
#include "llama.h"

struct llama_model * model = nullptr;

extern "C" JNIEXPORT jstring JNICALL
Java_com_devilking_os_ai_LocalAICore_stringFromJNI(JNIEnv* env, jobject) {
    std::string hello = "> [C++ NDK ENGINE ONLINE]: Hardware bridge established.";
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_devilking_os_ai_LocalAICore_loadModelFromJNI(JNIEnv* env, jobject, jstring path) {
    const char * model_path = env->GetStringUTFChars(path, nullptr);

    // Hardware File Check: Make sure C++ can actually see the Kotlin file
    std::ifstream file(model_path);
    if (!file.good()) {
        env->ReleaseStringUTFChars(path, model_path);
        return env->NewStringUTF("> [!] C++ ERROR: The engine cannot physically open the file in the Vault. Access Denied.");
    }
    file.close();

    llama_backend_init();

    llama_model_params model_params = llama_model_default_params();
    // THE BYPASS: Turn off Memory Mapping to avoid Android 14 storage security crashes
    model_params.use_mmap = false; 
    
    model = llama_load_model_from_file(model_path, model_params);

    env->ReleaseStringUTFChars(path, model_path);

    if (model == nullptr) {
        return env->NewStringUTF("> [!] FATAL ERROR: llama.cpp engine rejected the file. (Is the file corrupted?)");
    }

    return env->NewStringUTF("> [DEVILKING AI]: Neural Core successfully injected into physical RAM! System is stabilized.");
}
