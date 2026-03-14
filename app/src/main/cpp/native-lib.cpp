#include <jni.h>
#include <string>
#include <vector>
#include <fstream>
#include <thread>
#include <chrono>
#include "llama.h"

struct llama_model * model = nullptr;
llama_context * ctx = nullptr; // Keep context alive in memory
std::vector<llama_token> system_tokens; // Store the system prompt memory
int system_token_count = 0;

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

    // Initialize the continuous context
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 1024; 
    ctx_params.n_threads = 4; 
    ctx_params.n_threads_batch = 4;
    ctx = llama_new_context_with_model(model, ctx_params);

    // INJECT THE IDENTITY DIRECTLY INTO C++ MEMORY (KV CACHE)
    const char * system_prompt = "<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\nYou are DEVILKING OS, a high-performance terminal AI. Your goal is hardware optimization, system defense, and process management. You use a cold, technical hacker persona.<|eot_id|>";
    
    const llama_vocab * vocab = llama_model_get_vocab(model);
    system_tokens.resize(1024);
    system_token_count = llama_tokenize(vocab, system_prompt, strlen(system_prompt), system_tokens.data(), system_tokens.size(), true, true);
    if (system_token_count < 0) {
        system_token_count = -system_token_count;
        system_tokens.resize(system_token_count);
        llama_tokenize(vocab, system_prompt, strlen(system_prompt), system_tokens.data(), system_tokens.size(), true, true);
    } else system_tokens.resize(system_token_count);

    // Pre-calculate the math for the persona and save it in RAM
    llama_batch batch = llama_batch_get_one(system_tokens.data(), system_tokens.size());
    llama_decode(ctx, batch);

    return env->NewStringUTF("> [DEVILKING AI]: Neural Core stabilized. KV Cache Pre-loaded.");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_devilking_os_ai_LocalAICore_generateResponseFromJNI(JNIEnv* env, jobject, jstring prompt) {
    if (model == nullptr || ctx == nullptr) return env->NewStringUTF("ERROR: Core not in RAM.");

    const char * prompt_c = env->GetStringUTFChars(prompt, nullptr);
    std::string result_text = "";

    // Format the user question to attach to the pre-loaded memory
    std::string formatted_prompt = "<|start_header_id|>user<|end_header_id|>\n\n" + std::string(prompt_c) + "<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n";

    const llama_vocab * vocab = llama_model_get_vocab(model);
    std::vector<llama_token> tokens(512);
    int n_tokens = llama_tokenize(vocab, formatted_prompt.c_str(), formatted_prompt.length(), tokens.data(), tokens.size(), false, true);
    if (n_tokens < 0) {
        n_tokens = -n_tokens;
        tokens.resize(n_tokens);
        llama_tokenize(vocab, formatted_prompt.c_str(), formatted_prompt.length(), tokens.data(), tokens.size(), false, true);
    } else tokens.resize(n_tokens);

    // We start calculating FROM the end of the system prompt memory
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

        std::this_thread::sleep_for(std::chrono::milliseconds(15));
    }

    // Wipe ONLY the user prompt memory so the next question is fresh, but keep the persona memory
    llama_kv_cache_seq_rm(ctx, 0, system_token_count, -1); 

    env->ReleaseStringUTFChars(prompt, prompt_c);
    return env->NewStringUTF(result_text.c_str());
}
