#include <jni.h>
#include <string>
#include <vector>
#include <fstream>
#include <thread>
#include <chrono>
#include "llama.h"

struct llama_model * model = nullptr;

extern "C" JNIEXPORT jstring JNICALL
Java_com_devilking_os_ai_LocalAICore_stringFromJNI(JNIEnv* env, jobject) {
    return env->NewStringUTF("> [C++ NDK ENGINE ONLINE]: Hardware bridge established.");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_devilking_os_ai_LocalAICore_loadModelFromJNI(JNIEnv* env, jobject, jstring path) {
    const char * model_path = env->GetStringUTFChars(path, nullptr);
    std::ifstream file(model_path);
    if (!file.good()) {
        env->ReleaseStringUTFChars(path, model_path);
        return env->NewStringUTF("> [!] C++ ERROR: Vault access denied.");
    }
    file.close();

    llama_backend_init();
    llama_model_params model_params = llama_model_default_params();
    model_params.use_mmap = true; 
    
    model = llama_load_model_from_file(model_path, model_params);
    env->ReleaseStringUTFChars(path, model_path);

    if (model == nullptr) return env->NewStringUTF("> [!] FATAL ERROR: Core rejected.");
    return env->NewStringUTF("> [DEVILKING AI]: Neural Core stabilized. Active Cooling engaged.");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_devilking_os_ai_LocalAICore_generateResponseFromJNI(JNIEnv* env, jobject, jstring prompt) {
    if (model == nullptr) return env->NewStringUTF("ERROR: Core not in RAM.");

    const char * prompt_c = env->GetStringUTFChars(prompt, nullptr);
    std::string result_text = "";

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 512; 
    ctx_params.n_threads = 4; 
    ctx_params.n_threads_batch = 4;

    llama_context * ctx = llama_new_context_with_model(model, ctx_params);
    if (ctx == nullptr) {
        env->ReleaseStringUTFChars(prompt, prompt_c);
        return env->NewStringUTF("ERROR: Context failure.");
    }

    const llama_vocab * vocab = llama_model_get_vocab(model);
    std::vector<llama_token> tokens(512);
    int n_tokens = llama_tokenize(vocab, prompt_c, strlen(prompt_c), tokens.data(), tokens.size(), true, true);
    if (n_tokens < 0) {
        n_tokens = -n_tokens;
        tokens.resize(n_tokens);
        llama_tokenize(vocab, prompt_c, strlen(prompt_c), tokens.data(), tokens.size(), true, true);
    } else tokens.resize(n_tokens);

    llama_batch batch = llama_batch_get_one(tokens.data(), tokens.size());
    llama_decode(ctx, batch);

    for (int i = 0; i < 100; i++) {
        float * logits = llama_get_logits_ith(ctx, batch.n_tokens - 1);
        int n_vocab = llama_vocab_n_tokens(vocab);
        
        llama_token new_token_id = 0;
        float max_logit = -1e9f;
        for (int j = 0; j < n_vocab; j++) {
            if (logits[j] > max_logit) {
                max_logit = logits[j];
                new_token_id = j;
            }
        }
        
        if (new_token_id == llama_vocab_eos(vocab)) break;

        char buf[128];
        int n_chars = llama_token_to_piece(vocab, new_token_id, buf, sizeof(buf), 0, true);
        if (n_chars > 0) result_text += std::string(buf, n_chars);

        batch = llama_batch_get_one(&new_token_id, 1);
        if (llama_decode(ctx, batch) != 0) break;

        // --- THE CPU BREATHER ---
        // Forces the processor to sleep for 15 milliseconds between every single word
        std::this_thread::sleep_for(std::chrono::milliseconds(15));
    }

    llama_free(ctx);
    env->ReleaseStringUTFChars(prompt, prompt_c);
    return env->NewStringUTF(result_text.c_str());
}
