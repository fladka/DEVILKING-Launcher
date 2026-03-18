package com.devilking.os.ai

import android.content.Context
import com.devilking.os.system.SystemExecutor
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import android.os.Environment

class RegexRouter(private val context: Context) {
    
    private val executor = SystemExecutor(context)

    fun route(prompt: String): String? {
        val input = prompt.lowercase().trim()

        // 0. CUSTOM MACRO DATABASE INTERCEPTOR
        val prefs = context.getSharedPreferences("DEVILKING_MACROS", Context.MODE_PRIVATE)
        val customAction = prefs.getString(input, null)
        if (customAction != null) {
            return executor.executeCommand("[CMD: $customAction]")
        }

        // 1. THE NEW DYNAMIC LEARN COMMAND
        if (input.startsWith("learn ")) {
            val payload = input.removePrefix("learn ").trim()
            val parts = payload.split(">").map { it.trim() }
            if (parts.size >= 2) {
                return addMemory(parts[0], parts[1])
            }
            return "> [!] LEARN SYNTAX ERROR: Use 'learn [phrase] > [action/response]'"
        }

        // 2. THE HELP MENU
        if (input == "help") {
            return """
                *** DEVILKING OS COMMAND REGISTRY ***
                > settings      : Launch Macro Interface
                > learn [p]>[r] : Teach the OS a new reflex
                > open [app]    : Launches application
                > call [name]   : Initiates cellular override
                
                [ GOD MODE: SCREEN AUTOMATION ]
                > scan screen   : Dumps UI Matrix coordinates
                > scroll        : Phantom Finger swiping
                > snipe [text]  : Physically clicks UI
                > type [t]      : Ghost Typing text
                > macro whatsapp > [Name] > [Msg]
            """.trimIndent()
        }

        // Initialize default memory internally
        if (input == "matrix.init") {
            val defaultJson = """[{"exact_variations": ["who am i"], "answer": "You are the Architect."}]"""
            File(context.filesDir, "memory.json").writeText(defaultJson)
            return "> [SYSTEM]: Internal matrix initialized."
        }

        val memoryResult = checkMemoryMatrix(input)
        if (memoryResult != null) return if (memoryResult.contains("[CMD:")) executor.executeCommand(memoryResult) else "> [DEVILKING AI]: $memoryResult"

        // FAST PATHS
        if (input == "settings") return executor.executeCommand("[CMD: settings]")
        if (input == "scan screen") return executor.executeCommand("[CMD: scan screen]") 
        if (input == "flashlight" || input == "lumos") return executor.executeCommand("[CMD: flashlight]")
        if (input.startsWith("open ")) return executor.executeCommand("[CMD: open ${input.removePrefix("open ").trim()}]")
        if (input.startsWith("call ")) return executor.executeCommand("[CMD: call ${input.removePrefix("call ").trim()}]")
        if (input.startsWith("scroll") || input.startsWith("2x ")) return executor.executeCommand("[CMD: $input]")
        if (input.startsWith("snipe ")) return executor.executeCommand("[CMD: $input]")
        if (input.startsWith("type ")) return executor.executeCommand("[CMD: $input]")
        if (input.startsWith("macro ")) return executor.executeCommand("[CMD: $input]")

        return null 
    }

    private fun addMemory(trigger: String, response: String): String {
        return try {
            val memoryFile = File(context.filesDir, "memory.json")
            val jsonArray = if (memoryFile.exists()) JSONArray(memoryFile.readText()) else JSONArray()
            
            val newItem = JSONObject()
            val exacts = JSONArray()
            exacts.put(trigger.lowercase())
            newItem.put("exact_variations", exacts)
            newItem.put("answer", response)
            
            jsonArray.put(newItem)
            memoryFile.writeText(jsonArray.toString())
            "> [SYSTEM]: Memory updated. I have learned to respond to '$trigger'."
        } catch (e: Exception) {
            "> [!] MEMORY ERROR: ${e.message}"
        }
    }

    private fun checkMemoryMatrix(input: String): String? {
        try {
            val memoryFile = File(context.filesDir, "memory.json")
            if (!memoryFile.exists()) return null
            val jsonArray = JSONArray(memoryFile.readText())
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val exacts = item.getJSONArray("exact_variations")
                for (j in 0 until exacts.length()) {
                    if (input == exacts.getString(j).lowercase()) return item.getString("answer")
                }
            }
        } catch (e: Exception) {}
        return null
    }
}
