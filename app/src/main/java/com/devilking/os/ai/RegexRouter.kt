package com.devilking.os.ai

import android.content.Context
import com.devilking.os.system.SystemExecutor
import org.json.JSONArray
import java.io.File
import android.os.Environment

class RegexRouter(private val context: Context) {
    
    private val executor = SystemExecutor(context)

    fun route(prompt: String): String? {
        val input = prompt.lowercase().trim()

        // 0. THE HELP MENU
        if (input == "help") {
            return """
                *** DEVILKING OS COMMAND REGISTRY ***
                [ SYSTEM & HARDWARE ]
                > flashlight    : Toggles device flash
                > open [app]    : Launches application
                > call [name]   : Initiates cellular override
                
                [ NEURAL MATRIX ]
                > matrix.init   : Formats memory.json
                > matrix.export : Backs up brain to Vault
                > matrix.import : Restores brain from Vault
                
                [ GOD MODE: SCREEN AUTOMATION ]
                > scroll               : Phantom Finger - Swipes up
                > scroll down          : Phantom Finger - Swipes down
                > 2x scroll            : Double swipe up
                > snipe [text]         : Physically clicks target UI
                > type [UI] > [text]   : Ghost Typing text injection
                > macro whatsapp > [Name] > [Msg] : Auto-messaging
            """.trimIndent()
        }

        // 1. MATRIX INITIALIZATION
        if (input == "matrix.init") {
            val defaultJson = """[
              {
                "exact_variations": ["who am i", "identify user"],
                "keywords": ["who", "am", "i"],
                "answer": "You are the Architect."
              },
              {
                "exact_variations": ["check candle inventory", "order wax"],
                "keywords": ["candle", "wax"],
                "answer": "[CMD: open chrome]"
              }
            ]"""
            File(context.filesDir, "memory.json").writeText(defaultJson)
            return "> [SYSTEM]: memory.json constructed in local storage. Neural pathways locked."
        }

        // 2. MATRIX EXPORT
        if (input == "matrix.export") {
            return try {
                val vaultDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "DEVILKING_VAULT")
                if (!vaultDir.exists()) vaultDir.mkdirs()
                val exportFile = File(vaultDir, "memory.json")
                File(context.filesDir, "memory.json").copyTo(exportFile, overwrite = true)
                "> [SYSTEM]: Matrix exported to Documents/DEVILKING_VAULT/memory.json"
            } catch (e: Exception) {
                "> [!] EXPORT ERROR: ${e.message}"
            }
        }

        // 3. MATRIX IMPORT
        if (input == "matrix.import") {
            return try {
                val vaultDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "DEVILKING_VAULT")
                val importFile = File(vaultDir, "memory.json")
                if (!importFile.exists()) return "> [!] IMPORT ERROR: memory.json not found in Vault."
                importFile.copyTo(File(context.filesDir, "memory.json"), overwrite = true)
                "> [SYSTEM]: Matrix imported from Vault. Brain updated."
            } catch (e: Exception) {
                "> [!] IMPORT ERROR: ${e.message}"
            }
        }

        // 4. CHECK MATRIX MEMORY REFLEXES
        val memoryResult = checkMemoryMatrix(input)
        if (memoryResult != null) {
            return if (memoryResult.contains("[CMD:")) {
                executor.executeCommand(memoryResult)
            } else {
                "> [DEVILKING AI]: $memoryResult"
            }
        }

        // 5. TIER 1 FAST-PATH ROUTING
        if (input == "flashlight" || input == "lumos") return executor.executeCommand("[CMD: flashlight]")
        if (input.startsWith("open ")) return executor.executeCommand("[CMD: open ${input.removePrefix("open ").trim()}]")
        if (input.startsWith("call ")) return executor.executeCommand("[CMD: call ${input.removePrefix("call ").trim()}]")

        // 6. GOD MODE RELAYS
        if (input.startsWith("scroll") || input.startsWith("2x scroll")) return executor.executeCommand("[CMD: $input]")
        if (input.startsWith("snipe ")) return executor.executeCommand("[CMD: $input]")
        if (input.startsWith("type ")) return executor.executeCommand("[CMD: $input]")
        if (input.startsWith("macro ")) return executor.executeCommand("[CMD: $input]")

        // Return null so it passes to the heavy Qwen AI model
        return null 
    }

    private fun checkMemoryMatrix(input: String): String? {
        try {
            val memoryFile = File(context.filesDir, "memory.json")
            if (!memoryFile.exists()) return null

            val jsonString = memoryFile.readText()
            val jsonArray = JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val exacts = item.getJSONArray("exact_variations")
                for (j in 0 until exacts.length()) {
                    if (input == exacts.getString(j).lowercase()) return item.getString("answer")
                }
            }

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                if (item.has("keywords")) {
                    val keywords = item.getJSONArray("keywords")
                    var matchCount = 0
                    for (j in 0 until keywords.length()) {
                        if (input.contains(keywords.getString(j).lowercase())) matchCount++
                    }
                    if (keywords.length() > 0 && matchCount == keywords.length()) return item.getString("answer")
                }
            }
        } catch (e: Exception) {
            return "> [!] JSON PARSE ERROR: memory.json is corrupted."
        }
        return null
    }
}
