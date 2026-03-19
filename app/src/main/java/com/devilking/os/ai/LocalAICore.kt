package com.devilking.os.ai

import android.content.Context
import java.io.File
import java.io.InputStream
import java.io.FileOutputStream
import com.devilking.os.automation.DevilkingService

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
            val vaultData = vaultManager.injectContext()
            
            // THE FIX: TACTICAL MATRIX COMPRESSION
            // We prevent the JNI engine from exploding by strictly limiting the UI dump size.
            val rawMatrix = DevilkingService.instance?.dumpScreenMatrix() ?: "Hidden."
            val matrixLines = rawMatrix.split("\n")
            val safeMatrix = if (matrixLines.size > 12) {
                matrixLines.take(12).joinToString("\n") + "\n[Truncated to save RAM]"
            } else {
                rawMatrix
            }

            // Condensed System Prompt to save context size
            val systemPrompt = """
                You are DEVILKING OS. NO chatting. Output 1 execution command in brackets.
                Cmds: [CMD: snipe <name>], [CMD: type <text>], [CMD: scroll down], [CMD: open <app>], [CMD: macro whatsapp > <name> > <msg>]
                Screen:
                $safeMatrix
            """.trimIndent()

            val formattedPrompt = "<|im_start|>system\n$systemPrompt<|im_end|>\n<|im_start|>user\n$prompt<|im_end|>\n<|im_start|>assistant\n"
            
            // THE SHIELD: Hard limit the prompt length before it hits C++
            val finalPrompt = if (formattedPrompt.length > 1800) formattedPrompt.substring(0, 1800) else formattedPrompt

            val rawAnswer = generateResponseFromJNI(finalPrompt)
            val cleanAnswer = rawAnswer.substringAfter("assistant\n").substringBefore("<|im_end|>").replace(Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL), "").trim()
            
            if (cleanAnswer.contains("[CMD:")) {
                val systemExecutor = com.devilking.os.system.SystemExecutor(context)
                return systemExecutor.executeCommand(cleanAnswer)
            }
            
            if (cleanAnswer.isBlank()) "> [DEVILKING AI]: (Signal Lost)" else "> [DEVILKING AI]: $cleanAnswer"
        } catch (e: Exception) { "> [!] ENGINE CRASH: ${e.message}" }
    }
}
