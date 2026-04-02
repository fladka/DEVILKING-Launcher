package com.devilking.os.ai

import android.content.Context
import java.io.File
import java.io.InputStream
import java.io.FileOutputStream

class LocalAICore(private val context: Context) {

    init { System.loadLibrary("devilking_engine") }

    private external fun stringFromJNI(): String
    private external fun loadModelFromJNI(path: String): String
    private external fun generateResponseFromJNI(prompt: String): String 

    private var isModelLoaded = false
    private val regexRouter = RegexRouter(context)
    private val vaultManager = VaultManager()

    fun checkCoreStatus(): String {
        val privateFile = File(context.filesDir, "brain.gguf")
        return if (privateFile.exists() && privateFile.length() > 50 * 1024 * 1024) "> NEURAL CORE LOCATED.\n> Status: Ready for Inference."
        else "> [!] NEURAL CORE OFFLINE."
    }

    fun injectFromStream(inputStream: InputStream): String {
        val privateFile = File(context.filesDir, "brain.gguf")
        try {
            if (privateFile.exists()) privateFile.delete()
            val outputStream = FileOutputStream(privateFile)
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) outputStream.write(buffer, 0, bytesRead)
            outputStream.flush()
            outputStream.close()
            inputStream.close()
            
            loadModelFromJNI(privateFile.absolutePath)
            isModelLoaded = true
            return "> [DEVILKING AI]: Neural Core stabilized. " + vaultManager.loadVault()
        } catch (e: Exception) { return "> [!] KOTLIN STREAM ERROR: ${e.message}" }
    }

    fun generateResponse(prompt: String): String {
        val lowerPrompt = prompt.lowercase()
        
        if (lowerPrompt == "vault.reload") return vaultManager.loadVault()

        val reflexAnswer = regexRouter.route(prompt)
        if (reflexAnswer != null) return reflexAnswer

        if (!isModelLoaded) {
            val privateFile = File(context.filesDir, "brain.gguf")
            if (privateFile.exists() && privateFile.length() > 50 * 1024 * 1024) {
                loadModelFromJNI(privateFile.absolutePath)
                isModelLoaded = true
            } else return "> [!] CORE NOT INJECTED. Run 'inject core' first."
        }

        return try {
            // Pass the raw prompt directly. Do not add any extra ChatML tags here 
            // because MainActivity is now handling the strict Forced Prefill.
            val rawAnswer = generateResponseFromJNI(prompt)
            
            // Aggressive cleanup to catch runaway text
            val cleanAnswer = rawAnswer.replace("<|im_end|>", "")
                .replace(Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL), "")
                .trim()
            
            if (cleanAnswer.isBlank()) "> [DEVILKING AI]: (Signal Lost)" else cleanAnswer
        } catch (e: Exception) { 
            "> [!] ENGINE CRASH: ${e.message}" 
        }
    }
}
