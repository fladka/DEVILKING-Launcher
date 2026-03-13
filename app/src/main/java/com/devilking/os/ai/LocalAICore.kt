package com.devilking.os.ai

import android.content.Context
import java.io.File

class LocalAICore(private val context: Context) {

    init {
        System.loadLibrary("devilking_engine")
    }

    private external fun stringFromJNI(): String
    private external fun loadModelFromJNI(path: String): String

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
                return "> NEURAL CORE LOCATED at: $path\n> Status: Ready for Internal Transfer."
            }
        }
        return "> [!] NEURAL CORE OFFLINE: Model not found."
    }

    fun generateResponse(prompt: String): String {
        if (prompt.lowercase() == "ping cpp") {
            return stringFromJNI()
        }
        
        if (prompt.lowercase() == "inject core") {
            if (activeModelPath == null) checkCoreStatus()
            
            if (activeModelPath != null) {
                // The Smuggle Protocol: Copying to the C++ Private Vault
                val privateVaultDir = context.filesDir
                val privateFile = File(privateVaultDir, "brain.gguf")
                
                if (!privateFile.exists()) {
                    try {
                        File(activeModelPath!!).copyTo(privateFile)
                    } catch (e: Exception) {
                        return "> [!] KOTLIN ERROR: Failed to copy the file to the Private Vault. ${e.message}"
                    }
                }
                
                // Injecting from the safe Private Vault
                val result = loadModelFromJNI(privateFile.absolutePath)
                if (result.contains("successfully")) {
                    isModelLoaded = true
                }
                return result
            }
            return "> [!] Cannot inject: Core file not found."
        }

        if (!isModelLoaded) {
            return "> [!] CORE NOT INJECTED: Type 'inject core' to load the AI into RAM."
        }

        return "> [DEVILKING AI]: I am alive in your RAM! (Awaiting Inference Loop Update...)"
    }
}
