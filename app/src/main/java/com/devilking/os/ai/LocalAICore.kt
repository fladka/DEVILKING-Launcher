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

    fun checkCoreStatus(): String {
        val privateFile = File(context.filesDir, "brain.gguf")
        return if (privateFile.exists() && privateFile.length() > 100 * 1024 * 1024) {
            "> NEURAL CORE LOCATED.\n> Status: Ready for Inference."
        } else {
            "> [!] NEURAL CORE OFFLINE."
        }
    }

    fun injectFromStream(inputStream: InputStream): String {
        val privateFile = File(context.filesDir, "brain.gguf")
        try {
            if (privateFile.exists() && privateFile.length() < 100 * 1024 * 1024) privateFile.delete()
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
            val result = loadModelFromJNI(privateFile.absolutePath)
            if (result.contains("stabilized")) isModelLoaded = true
            return result
        } catch (e: Exception) {
            return "> [!] KOTLIN STREAM ERROR: ${e.message}"
        }
    }

    fun generateResponse(prompt: String): String {
        if (prompt.lowercase() == "ping cpp") return stringFromJNI()
        
        if (!isModelLoaded) {
            val privateFile = File(context.filesDir, "brain.gguf")
            if (privateFile.exists() && privateFile.length() > 100 * 1024 * 1024) {
                val result = loadModelFromJNI(privateFile.absolutePath)
                if (result.contains("stabilized")) isModelLoaded = true
                else return result
            } else return "> [!] CORE NOT INJECTED."
        }

        // COMPRESSED IDENTITY: 9 words instead of 40 to slash math requirements
        val systemPrompt = "You are DEVILKING OS, a cold system terminal."
        
        val formattedPrompt = "<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n$systemPrompt<|eot_id|><|start_header_id|>user<|end_header_id|>\n\n$prompt<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n"
        
        val rawAnswer = generateResponseFromJNI(formattedPrompt)
        return "> [DEVILKING AI]: $rawAnswer"
    }
}
