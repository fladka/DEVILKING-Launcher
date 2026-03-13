#include <jni.h>
#include <string>
#include "llama.h"

// Global model pointer (keeps the AI in RAM once loaded)
struct llama_model * model = nullptr;

extern "C" JNIEXPORT jstring JNICALL
Java_com_devilking_os_ai_LocalAICore_stringFromJNI(JNIEnv* env, jobject) {
    std::string hello = "> [C++ NDK ENGINE ONLINE]: Hardware bridge established.";
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_devilking_os_ai_LocalAICore_loadModelFromJNI(JNIEnv* env, jobject, jstring path) {
    const char * model_path = env->GetStringUTFChars(path, nullptr);

    // Initialize the Llama.cpp backend
    llama_backend_init();

    // Set default parameters for the model
    llama_model_params model_params = llama_model_default_params();
    
    // Attempt to load the massive file into your Vivo's RAM
    model = llama_load_model_from_file(model_path, model_params);

    env->ReleaseStringUTFChars(path, model_path);

    if (model == nullptr) {
        return env->NewStringUTF("> [!] FATAL ERROR: C++ Engine failed to load the model into RAM. Check file permissions or RAM limits.");
    }

    return env->NewStringUTF("> [DEVILKING AI]: Neural Core successfully injected into physical RAM! System is stabilized.");
}
