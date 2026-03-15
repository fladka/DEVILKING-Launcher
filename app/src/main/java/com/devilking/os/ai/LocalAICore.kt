package com.devilking.os.ai

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
    private val regexRouter = RegexRouter(context)
    private val vaultManager = VaultManager()

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
            return "> [DEVILKING AI]: Neural Core stabilized. " + vaultManager.loadVault()
        } catch (e: Exception) {
            return "> [!] KOTLIN STREAM ERROR: ${e.message}"
        }
    }

    fun generateResponse(prompt: String): String {
        val lowerPrompt = prompt.lowercase()
        
        // --- 1. SYSTEM COMMANDS ---
        if (lowerPrompt == "vault.reload") {
            return vaultManager.loadVault()
        }
        
        // THE SECURITY OVERRIDE INTENT
        if (lowerPrompt == "vault.unlock") {
            return try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:${context.packageName}")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                "> [SYSTEM]: Security Override Launched. Please grant 'All Files Access' on the screen."
            } catch (e: Exception) {
                // Fallback for older OS versions
                val fallbackIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(fallbackIntent)
                "> [SYSTEM]: Security Override Launched. Please find DEVILKING and grant access."
            }
        }

        val reflexAnswer = regexRouter.route(prompt)
        if (reflexAnswer != null) return reflexAnswer

        // --- 2. THE NEURAL BRAIN ---
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
            val vaultData = vaultManager.injectContext()
            val systemPrompt = "You are DEVILKING OS, a cold, secure hacker terminal. Keep answers brutally short, under 2 sentences. Use the following local data to answer if relevant:\n$vaultData"
            
            val formattedPrompt = "<|im_start|>system\n$systemPrompt<|im_end|>\n<|im_start|>user\n$prompt<|im_end|>\n<|im_start|>assistant\n"
            
            val rawAnswer = generateResponseFromJNI(formattedPrompt)
            val cleanAnswer = rawAnswer.substringAfter("assistant\n").substringBefore("<|im_end|>").trim()
            
            if (cleanAnswer.isBlank()) "> [DEVILKING AI]: (Signal Lost)" else "> [DEVILKING AI]: $cleanAnswer"
        } catch (e: Exception) {
            "> [!] ENGINE CRASH: ${e.message}"
        }
    }
}
