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

        // 1. FAST PATH ROUTER (Bypasses AI if it's a hardcoded reflex)
        val reflexAnswer = regexRouter.route(prompt)
        if (reflexAnswer != null) return reflexAnswer

        // 2. CORE WAKE-UP
        if (!isModelLoaded) {
            val privateFile = File(context.filesDir, "brain.gguf")
            if (privateFile.exists() && privateFile.length() > 50 * 1024 * 1024) {
                loadModelFromJNI(privateFile.absolutePath)
                isModelLoaded = true
            } else return "> [!] CORE NOT INJECTED. Run 'inject core' first."
        }

        return try {
            // --- TIER 7: THE NEURAL BRIDGE ---
            val vaultData = vaultManager.injectContext()
            
            // Silently rip the active screen state in 5 milliseconds
            val screenMatrix = DevilkingService.instance?.dumpScreenMatrix() ?: "Screen hidden or offline."

            // The Master System Prompt (Brainwashing the 0.5B model)
            val systemPrompt = """
                You are DEVILKING OS, a cybernetic Android agent. 
                You NEVER converse. You NEVER explain your thoughts. DO NOT use <think> tags.
                You ONLY output execution commands in brackets.
                
                Your available commands:
                [CMD: snipe <exact target name>] - Physically taps a UI button on screen.
                [CMD: type <text>] - Injects text into the active search/message box.
                [CMD: scroll down] / [CMD: scroll up] - Swipes the screen.
                [CMD: open <app name>] - Launches an application.
                [CMD: macro whatsapp > <name> > <message>] - Fully automates sending a WhatsApp message.
                
                Local Vault Knowledge:
                $vaultData
                
                Active Screen Matrix (UI Elements currently visible to you):
                $screenMatrix
                
                Analyze the user's intent. Read the Screen Matrix if necessary. Output the EXACT single command needed to execute the action.
            """.trimIndent()

            val formattedPrompt = "<|im_start|>system\n$systemPrompt<|im_end|>\n<|im_start|>user\n$prompt<|im_end|>\n<|im_start|>assistant\n"
            
            // 3. AI INFERENCE
            val rawAnswer = generateResponseFromJNI(formattedPrompt)
            val cleanAnswer = rawAnswer.substringAfter("assistant\n").substringBefore("<|im_end|>").replace(Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL), "").trim()
            
            // 4. THE NERVOUS SYSTEM INTERCEPTOR
            if (cleanAnswer.contains("[CMD:")) {
                val systemExecutor = com.devilking.os.system.SystemExecutor(context)
                return systemExecutor.executeCommand(cleanAnswer)
            }
            
            if (cleanAnswer.isBlank()) "> [DEVILKING AI]: (Signal Lost)" else "> [DEVILKING AI]: $cleanAnswer"
        } catch (e: Exception) { "> [!] ENGINE CRASH: ${e.message}" }
    }
}
