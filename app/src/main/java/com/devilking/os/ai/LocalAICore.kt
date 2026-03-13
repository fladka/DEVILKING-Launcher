package com.devilking.os.ai

import java.io.File

class LocalAICore {

    init {
        System.loadLibrary("devilking_engine")
    }

    private external fun stringFromJNI(): String
    private external fun loadModelFromJNI(path: String): String // The new C++ function

    private val modelPaths = listOf(
        "/storage/emulated/0/DEVILKING_AI/llama-3.2-1b-instruct-q4_k_m.gguf",
        "/storage/emulated/0/Download/DEVILKING_AI/llama-3.2-1b-instruct-q4_k_m.gguf",
        "/storage/emulated/0/Documents/DEVILKING_AI/llama-3.2-1b-instruct-q4_k_m.gguf"
    )

    private var activeModelPath: String? = null
    private var isModelLoaded = false

    fun checkCoreStatus(): String {
        for (path in modelPaths) {
            if (File(path).exists()) {
                activeModelPath = path
                return "> NEURAL CORE LOCATED at: $path\n> Status: Ready for Memory Injection."
            }
        }
        return "> [!] NEURAL CORE OFFLINE: Model not found."
    }

    fun generateResponse(prompt: String): String {
        if (prompt.lowercase() == "ping cpp") {
            return stringFromJNI()
        }
        
        // THE INJECTION COMMAND
        if (prompt.lowercase() == "inject core") {
            if (activeModelPath == null) checkCoreStatus()
            
            if (activeModelPath != null) {
                val result = loadModelFromJNI(activeModelPath!!)
                if (result.contains("successfully")) {
                    isModelLoaded = true
                }
                return result
            }
            return "> [!] Cannot inject: Core file not found."
        }

        // Safety lock: prevents you from talking to the AI before it's loaded
        if (!isModelLoaded) {
            return "> [!] CORE NOT INJECTED: Type 'inject core' to load the AI into RAM."
        }

        return "> [DEVILKING AI]: I am alive in your RAM! (Awaiting Inference Loop Update...)"
    }
}
