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
                "answer": "[EXECUTE: LAUNCH_APP_CHROME]"
              }
            ]"""
            File(context.filesDir, "memory.json").writeText(defaultJson)
            return "> [SYSTEM]: memory.json constructed in local storage. Neural pathways locked."
        }

        if (input == "matrix.export") {
            return try {
                val internalFile = File(context.filesDir, "memory.json")
                val vaultDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "DEVILKING_VAULT")
                if (!vaultDir.exists()) vaultDir.mkdirs()
                val exportFile = File(vaultDir, "memory.json")
                internalFile.copyTo(exportFile, overwrite = true)
                "> [SYSTEM]: Matrix exported to Documents/DEVILKING_VAULT/memory.json"
            } catch (e: Exception) {
                "> [!] EXPORT ERROR: ${e.message}"
            }
        }

        if (input == "matrix.import") {
            return try {
                val vaultDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "DEVILKING_VAULT")
                val importFile = File(vaultDir, "memory.json")
                if (!importFile.exists()) return "> [!] IMPORT ERROR: memory.json not found in Vault."
                val internalFile = File(context.filesDir, "memory.json")
                importFile.copyTo(internalFile, overwrite = true)
                "> [SYSTEM]: Matrix imported from Vault. Brain updated."
            } catch (e: Exception) {
                "> [!] IMPORT ERROR: ${e.message}"
            }
        }

        val memoryResult = checkMemoryMatrix(input)
        if (memoryResult != null) {
            return if (memoryResult.contains("[EXECUTE:")) {
                executor.executeCommand(memoryResult)
            } else {
                "> [DEVILKING AI]: $memoryResult"
            }
        }

        if (input == "flashlight" || input == "lumos") return executor.executeCommand("[EXECUTE: FLASHLIGHT_TOGGLE]")
        if (input.startsWith("open ")) {
            val appName = input.removePrefix("open ").trim()
            return executor.executeCommand("[EXECUTE: LAUNCH_APP_$appName]")
        }

        // 6. THE PHANTOM FINGER TRIGGER
        if (input == "scroll") {
            val service = com.devilking.os.system.DevilkingAccessibilityService.instance
            if (service != null) {
                service.performSwipeUp()
                return "> [SYSTEM]: Phantom Finger executed. Swiping screen."
            } else {
                return "> [!] GOD MODE OFFLINE: You must enable DEVILKING OS in Android Accessibility Settings."
            }
        }

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
