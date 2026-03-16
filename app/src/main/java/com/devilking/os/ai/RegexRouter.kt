package com.devilking.os.ai

import android.content.Context
import com.devilking.os.system.SystemExecutor
import org.json.JSONArray
import java.io.File

class RegexRouter(private val context: Context) {
    
    // THE FLASHLIGHT FIX: Permanent memory for the hardware state.
    private val executor = SystemExecutor(context)

    fun route(prompt: String): String? {
        val input = prompt.lowercase().trim()

        // 1. Matrix Initialization (Creates a sample JSON file for you)
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

        // 2. The JSON Memory Matrix (The Custom Brain)
        val memoryResult = checkMemoryMatrix(input)
        if (memoryResult != null) {
            // If the JSON tells it to execute hardware, it routes to the hands. Otherwise, it speaks.
            return if (memoryResult.contains("[EXECUTE:")) {
                executor.executeCommand(memoryResult)
            } else {
                "> [DEVILKING AI]: $memoryResult"
            }
        }

        // 3. Hardcoded Fallbacks (0-Second Execution)
        if (input == "flashlight" || input == "lumos") {
            return executor.executeCommand("[EXECUTE: FLASHLIGHT_TOGGLE]")
        }
        if (input.startsWith("open ")) {
            val appName = input.removePrefix("open ").trim()
            return executor.executeCommand("[EXECUTE: LAUNCH_APP_$appName]")
        }

        // 4. Wake the heavy Qwen AI if it's a completely unknown question
        return null 
    }

    private fun checkMemoryMatrix(input: String): String? {
        try {
            val memoryFile = File(context.filesDir, "memory.json")
            if (!memoryFile.exists()) return null

            val jsonString = memoryFile.readText()
            val jsonArray = JSONArray(jsonString)

            // Pass 1: The Sniper (Exact Match)
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val exacts = item.getJSONArray("exact_variations")
                for (j in 0 until exacts.length()) {
                    if (input == exacts.getString(j).lowercase()) {
                        return item.getString("answer")
                    }
                }
            }

            // Pass 2: The Shotgun (Keyword Intersection)
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                if (item.has("keywords")) {
                    val keywords = item.getJSONArray("keywords")
                    var matchCount = 0
                    for (j in 0 until keywords.length()) {
                        if (input.contains(keywords.getString(j).lowercase())) {
                            matchCount++
                        }
                    }
                    if (keywords.length() > 0 && matchCount == keywords.length()) {
                        return item.getString("answer")
                    }
                }
            }
        } catch (e: Exception) {
            return "> [!] JSON PARSE ERROR: memory.json is corrupted."
        }
        return null
    }
}
