package com.devilking.os.ai

import java.io.File

class LocalAICore {

    // THIS IS THE BRIDGE: Loading the C++ Engine into Android's RAM
    init {
        System.loadLibrary("devilking_engine")
    }

    // Declaring the native C++ function so Kotlin knows it exists
    private external fun stringFromJNI(): String

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
        return "> [!] NEURAL CORE OFFLINE: Model not found."
    }

    fun generateResponse(prompt: String): String {
        // THE C++ TEST COMMAND
        if (prompt.lowercase() == "ping cpp") {
            return stringFromJNI()
        }

        var isMounted = false
        for (path in modelPaths) {
            if (File(path).exists()) isMounted = true
        }

        if (!isMounted) {
            return checkCoreStatus()
        }

        return "> [DEVILKING AI]: Processing intent for '$prompt'...\n> (Awaiting full llama.cpp integration)"
    }
}
