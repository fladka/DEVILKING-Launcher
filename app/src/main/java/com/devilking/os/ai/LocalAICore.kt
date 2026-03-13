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

    private var isModelLoaded = false

    fun checkCoreStatus(): String {
        val privateFile = File(context.filesDir, "brain.gguf")
        return if (privateFile.exists() && privateFile.length() > 100 * 1024 * 1024) {
            "> NEURAL CORE LOCATED in Private Vault.\n> Status: Ready for Inference."
        } else {
            "> [!] NEURAL CORE OFFLINE: Type 'inject core' to load the AI."
        }
    }

    // The new VIP Stream Injector
    fun injectFromStream(inputStream: InputStream): String {
        val privateFile = File(context.filesDir, "brain.gguf")
        
        try {
            // Ghost Eraser
            if (privateFile.exists() && privateFile.length() < 100 * 1024 * 1024) {
                privateFile.delete()
            }
            
            if (!privateFile.exists()) {
                val outputStream = FileOutputStream(privateFile)
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                outputStream.flush()
                outputStream.close()
                inputStream.close()
            }
            
            val sizeMB = privateFile.length() / (1024 * 1024)
            val result = loadModelFromJNI(privateFile.absolutePath)
            
            if (result.contains("successfully")) {
                isModelLoaded = true
            }
            return "$result \n> (Vault File Size Verified: ${sizeMB}MB)"
            
        } catch (e: Exception) {
            return "> [!] KOTLIN STREAM ERROR: Failed to copy the file. ${e.message}"
        }
    }

    fun generateResponse(prompt: String): String {
        if (prompt.lowercase() == "ping cpp") return stringFromJNI()
        
        if (!isModelLoaded) {
            // Check if it's already in the vault from a previous successful run
            val privateFile = File(context.filesDir, "brain.gguf")
            if (privateFile.exists() && privateFile.length() > 100 * 1024 * 1024) {
                val result = loadModelFromJNI(privateFile.absolutePath)
                if (result.contains("successfully")) isModelLoaded = true
                return result
            }
            return "> [!] CORE NOT INJECTED: Type 'inject core' to load the AI."
        }

        return "> [DEVILKING AI]: I am alive in your RAM! (Awaiting Inference Loop Update...)"
    }
}
