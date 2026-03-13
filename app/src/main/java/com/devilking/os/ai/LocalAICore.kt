package com.devilking.os.ai

import java.io.File

class LocalAICore {

    // Looking for the file in the main storage directory
    private val modelPaths = listOf(
        "/storage/emulated/0/DEVILKING_AI/llama-3.2-1b-instruct-q4_k_m.gguf",
        "/storage/emulated/0/Download/DEVILKING_AI/llama-3.2-1b-instruct-q4_k_m.gguf",
        "/storage/emulated/0/Documents/DEVILKING_AI/llama-3.2-1b-instruct-q4_k_m.gguf"
    )

    fun checkCoreStatus(): String {
        for (path in modelPaths) {
            if (File(path).exists()) {
                return "> NEURAL CORE LOCATED at: $path\n> Status: Ready for Memory Injection."
            }
        }
        return "> [!] NEURAL CORE OFFLINE: Model not found.\n> Please create a 'DEVILKING_AI' folder in your Downloads and place the .gguf file inside."
    }

    fun generateResponse(prompt: String): String {
        var isMounted = false
        for (path in modelPaths) {
            if (File(path).exists()) isMounted = true
        }

        if (!isMounted) {
            return checkCoreStatus()
        }

        // The file is found! This catches natural language like "Hello" or "Who are you?"
        return "> [DEVILKING AI]: Processing intent for '$prompt'...\n> (C++ Inference Engine preparing for next update)"
    }
}
