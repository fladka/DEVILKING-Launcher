package com.devilking.os.ai

import android.content.Context
import java.io.File
import java.io.InputStream
import java.io.FileOutputStream

class LocalAICore(private val context: Context) {

    init {
        System.loadLibrary("devilking_engine")
    }

    private external fun stringFromJNI(): String
    private external fun loadModelFromJNI(path: String): String
    private external fun generateResponseFromJNI(prompt: String): String 

    private var isModelLoaded = false
    private val regexRouter = RegexRouter(context) // Initialize the Gatekeeper

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
            
            loadModelFromJNI(privateFile.absolutePath)
            isModelLoaded = true
            return "> [DEVILKING AI]: Neural Core stabilized. Active Cooling engaged."
        } catch (e: Exception) {
            return "> [!] KOTLIN STREAM ERROR: ${e.message}"
        }
    }

    fun generateResponse(prompt: String): String {
        
        // --- 1. THE REFLEX ARC (Zero Latency) ---
        val reflexAnswer = regexRouter.route(prompt)
        if (reflexAnswer != null) {
            return reflexAnswer // Bypasses the AI completely!
        }

        // --- 2. THE NEURAL BRAIN (Heavy Compute) ---
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
            val systemPrompt = "You are DEVILKING OS, a cold, secure, and highly efficient hacker terminal. Keep all answers brutally short, direct, and under 2 sentences unless specifically asked to explain."
            val formattedPrompt = "<|im_start|>system\n$systemPrompt<|im_end|>\n<|im_start|>user\n$prompt<|im_end|>\n<|im_start|>assistant\n"
            
            val rawAnswer = generateResponseFromJNI(formattedPrompt)
            val cleanAnswer = rawAnswer.substringAfter("assistant\n").substringBefore("<|im_end|>").trim()
            
            if (cleanAnswer.isBlank()) "> [DEVILKING AI]: (Signal Lost)" else "> [DEVILKING AI]: $cleanAnswer"
        } catch (e: Exception) {
            "> [!] ENGINE CRASH: ${e.message}"
        }
    }
}
