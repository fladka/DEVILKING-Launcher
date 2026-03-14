package com.devilking.os.ai

import android.content.Context
import java.io.File
import java.io.InputStream
import java.io.FileOutputStream
import com.devilking.os.automation.DevilkingService

class LocalAICore(private val context: Context) {

    init {
        System.loadLibrary("devilking_engine")
    }

    private external fun stringFromJNI(): String
    private external fun loadModelFromJNI(path: String): String
    private external fun generateResponseFromJNI(prompt: String): String 

    private var isModelLoaded = false

    fun checkCoreStatus(): String {
        val privateFile = File(context.filesDir, "brain.gguf")
        return if (privateFile.exists() && privateFile.length() > 50 * 1024 * 1024) {
            "> NEURAL CORE LOCATED.\n> Status: Ready for Inference."
        } else {
            "> [!] NEURAL CORE OFFLINE."
        }
    }

    fun injectFromStream(inputStream: InputStream): String {
        val privateFile = File(context.filesDir, "brain.gguf")
        try {
            if (privateFile.exists()) privateFile.delete()
            val outputStream = FileOutputStream(privateFile)
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            outputStream.flush()
            outputStream.close()
            inputStream.close()
            
            val result = loadModelFromJNI(privateFile.absolutePath)
            isModelLoaded = true // Force unlock the AI
            return result
        } catch (e: Exception) {
            return "> [!] KOTLIN STREAM ERROR: ${e.message}"
        }
    }

    fun generateResponse(prompt: String): String {
        val lowerPrompt = prompt.lowercase()
        
        // --- THE TRAFFIC COP (NATIVE INTERCEPTORS) ---
        if (lowerPrompt == "ping cpp") return stringFromJNI()
        
        if (lowerPrompt.startsWith("macro.launch ")) {
            val target = prompt.substring(13).trim()
            return DevilkingService.instance?.executeAction("LAUNCH", target) 
                ?: "> [!] ERROR: Ghost Service Offline. Check Accessibility Settings."
        }

        if (lowerPrompt.startsWith("macro.click ")) {
            val target = prompt.substring(12).trim()
            return DevilkingService.instance?.executeAction("CLICK", target) 
                ?: "> [!] ERROR: Ghost Service Offline. Check Accessibility Settings."
        }

        // --- THE AI BRAIN (FALLBACK) ---
        // Auto-load if file exists but not in memory yet (e.g., app restart)
        if (!isModelLoaded) {
            val privateFile = File(context.filesDir, "brain.gguf")
            if (privateFile.exists() && privateFile.length() > 50 * 1024 * 1024) {
                loadModelFromJNI(privateFile.absolutePath)
                isModelLoaded = true
            } else {
                return "> [!] CORE NOT INJECTED. Run 'inject core' first."
            }
        }

        return try {
            // Send raw prompt directly. Qwen GGUF handles its own formatting inside llama.cpp.
            val rawAnswer = generateResponseFromJNI(prompt)
            if (rawAnswer.isNullOrBlank()) {
                "> [DEVILKING AI]: (Engine returned empty. Core may need re-injection.)"
            } else {
                "> [DEVILKING AI]: ${rawAnswer.trim()}"
            }
        } catch (e: Exception) {
            "> [!] ENGINE CRASH: ${e.message}"
        }
    }
}
