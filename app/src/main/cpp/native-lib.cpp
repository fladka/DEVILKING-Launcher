#include <jni.h>
#include <string>
#include <vector>
#include <fstream>
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
        return env->NewStringUTF("> [!] C++ ERROR: The engine cannot physically open the file in the Vault.");
    }
    file.close();

    llama_backend_init();
    llama_model_params model_params = llama_model_default_params();
    model_params.use_mmap = false; 
    
    model = llama_load_model_from_file(model_path, model_params);
    env->ReleaseStringUTFChars(path, model_path);

    if (model == nullptr) {
        return env->NewStringUTF("> [!] FATAL ERROR: llama.cpp engine rejected the file.");
    }
    return env->NewStringUTF("> [DEVILKING AI]: Neural Core successfully injected into physical RAM! System is stabilized.");
}

// THE FINAL BOSS: The Generation Loop
extern "C" JNIEXPORT jstring JNICALL
Java_com_devilking_os_ai_LocalAICore_generateResponseFromJNI(JNIEnv* env, jobject, jstring prompt) {
    if (model == nullptr) return env->NewStringUTF("ERROR: Model not loaded in RAM. Type 'inject core' first.");

    const char * prompt_c = env->GetStringUTFChars(prompt, nullptr);
    std::string result_text = "";

    // 1. Open a "Thoughts" Memory Context
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 1024;
    llama_context * ctx = llama_new_context_with_model(model, ctx_params);
    
    if (ctx == nullptr) {
        env->ReleaseStringUTFChars(prompt, prompt_c);
        return env->NewStringUTF("ERROR: Failed to allocate CPU memory context.");
    }

    const llama_vocab * vocab = llama_model_get_vocab(model);
    
    // 2. Turn your English prompt into Math Tokens
    std::vector<llama_token> tokens(1024);
    int n_tokens = llama_tokenize(vocab, prompt_c, strlen(prompt_c), tokens.data(), tokens.size(), true, true);
    if (n_tokens < 0) {
        n_tokens = -n_tokens;
        tokens.resize(n_tokens);
        llama_tokenize(vocab, prompt_c, strlen(prompt_c), tokens.data(), tokens.size(), true, true);
    } else {
        tokens.resize(n_tokens);
    }

    // 3. Feed the math to the Llama Engine
    llama_batch batch = llama_batch_get_one(tokens.data(), tokens.size());
    llama_decode(ctx, batch);

    llama_sampler * smpl = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add_greedy(smpl);

    // 4. Generate 25 words (Limits CPU freezing)
    for (int i = 0; i < 25; i++) {
        llama_token new_token_id = llama_sampler_sample(smpl, ctx, -1);
        
        // Stop if the AI finishes its sentence
        if (new_token_id == llama_vocab_eos(vocab)) {
            break;
        }

        // Turn the new Math Token back into an English word
        char buf[128];
        int n_chars = llama_token_to_piece(vocab, new_token_id, buf, sizeof(buf), 0, true);
        if (n_chars > 0) {
            result_text += std::string(buf, n_chars);
        }

        // Feed the new word back into the engine to guess the next word
        batch = llama_batch_get_one(&new_token_id, 1);
        llama_decode(ctx, batch);
    }

    // 5. Cleanup
    llama_sampler_free(smpl);
    llama_free(ctx);
    env->ReleaseStringUTFChars(prompt, prompt_c);

    return env->NewStringUTF(result_text.c_str());
}
