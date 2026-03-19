package com.devilking.os.ai

import android.content.Context
import com.devilking.os.system.SystemExecutor
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.min

class RegexRouter(private val context: Context) {
    
    private val executor = SystemExecutor(context)

    // Levenshtein Distance Algorithm
    private fun calculateFuzzyDistance(lhs: CharSequence, rhs: CharSequence): Int {
        val lhsLength = lhs.length
        val rhsLength = rhs.length

        var cost = IntArray(lhsLength + 1) { it }
        var newCost = IntArray(lhsLength + 1)

        for (i in 1..rhsLength) {
            newCost[0] = i
            for (j in 1..lhsLength) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1
                newCost[j] = min(min(costInsert, costDelete), costReplace)
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[lhsLength]
    }

    fun route(prompt: String): String? {
        val input = prompt.lowercase().trim()

        // --- RESTORED SYSTEM FAST PATHS ---
        if (input == "clear") return executor.executeCommand("[CMD: clear]")
        if (input == "settings") return executor.executeCommand("[CMD: settings]")
        if (input == "scan screen") return executor.executeCommand("[CMD: scan screen]") 
        if (input == "flashlight" || input == "lumos") return executor.executeCommand("[CMD: flashlight]")
        
        if (input == "help") {
            return """
                *** DEVILKING OS COMMAND REGISTRY ***
                > settings      : Launch Macro Interface
                > learn [p]>[r] : Teach the OS a new reflex
                > matrix.init   : Reset memory matrix
                > open [app]    : Launches application
                > call [name]   : Initiates cellular override
                > clear         : Wipes terminal history
                
                [ GOD MODE: SCREEN AUTOMATION ]
                > scan screen   : Dumps UI Matrix coordinates
                > scroll        : Phantom Finger swiping
                > snipe [text]  : Physically clicks UI
                > type [t]      : Ghost Typing text
                > macro whatsapp > [Name] > [Msg]
            """.trimIndent()
        }

        if (input == "matrix.init") {
            val defaultJson = """[{"exact_variations": ["who am i"], "answer": "You are the Architect."}]"""
            File(context.filesDir, "memory.json").writeText(defaultJson)
            return "> [SYSTEM]: Internal matrix initialized."
        }

        // --- COMMAND PARSERS ---
        if (input.startsWith("open ")) return executor.executeCommand("[CMD: open ${input.removePrefix("open ").trim()}]")
        if (input.startsWith("call ")) return executor.executeCommand("[CMD: call ${input.removePrefix("call ").trim()}]")
        if (input.startsWith("scroll") || input.startsWith("2x ")) return executor.executeCommand("[CMD: $input]")
        if (input.startsWith("snipe ")) return executor.executeCommand("[CMD: $input]")
        if (input.startsWith("type ")) return executor.executeCommand("[CMD: $input]")
        if (input.startsWith("macro ")) return executor.executeCommand("[CMD: $input]")

        if (input.startsWith("learn ")) {
            val payload = prompt.substring(6).trim() 
            val parts = payload.split(">").map { it.trim() }
            if (parts.size >= 2) {
                return addMemory(parts[0].lowercase(), parts[1])
            }
            return "> [!] LEARN SYNTAX ERROR: Use 'learn [phrase] > [action/response]'"
        }

        // --- FUZZY LOGIC MEMORY MATCHER ---
        val memoryResult = checkMemoryMatrixFuzzy(input)
        if (memoryResult != null) {
            return if (memoryResult.uppercase().contains("[CMD:")) {
                executor.executeCommand(memoryResult.replace("[cmd:", "[CMD:", ignoreCase = true))
            } else {
                "> [DEVILKING AI]: $memoryResult"
            }
        }

        val prefs = context.getSharedPreferences("DEVILKING_MACROS", Context.MODE_PRIVATE)
        val customAction = prefs.getString(input, null)
        if (customAction != null) {
            return executor.executeCommand("[CMD: $customAction]")
        }

        return null 
    }

    private fun addMemory(trigger: String, response: String): String {
        return try {
            val memoryFile = File(context.filesDir, "memory.json")
            val jsonArray = if (memoryFile.exists()) JSONArray(memoryFile.readText()) else JSONArray()
            
            val newItem = JSONObject()
            val exacts = JSONArray()
            exacts.put(trigger)
            newItem.put("exact_variations", exacts)
            newItem.put("answer", response)
            
            jsonArray.put(newItem)
            memoryFile.writeText(jsonArray.toString())
            "> [SYSTEM]: Memory updated. I have learned to respond to '$trigger'."
        } catch (e: Exception) {
            "> [!] MEMORY ERROR: ${e.message}"
        }
    }

    private fun checkMemoryMatrixFuzzy(input: String): String? {
        try {
            val memoryFile = File(context.filesDir, "memory.json")
            if (!memoryFile.exists()) return null
            
            val jsonArray = JSONArray(memoryFile.readText())
            var bestMatchAnswer: String? = null
            var lowestDistance = Int.MAX_VALUE

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val exacts = item.getJSONArray("exact_variations")
                
                for (j in 0 until exacts.length()) {
                    val target = exacts.getString(j).lowercase()
                    if (input == target) return item.getString("answer")
                    
                    val distance = calculateFuzzyDistance(input, target)
                    
                    // Dynamic tolerance: Longer words allow more typos.
                    val allowedTypos = when {
                        target.length <= 4 -> 1
                        target.length <= 8 -> 2
                        target.length <= 15 -> 3
                        else -> 4
                    }
                    
                    if (distance <= allowedTypos && distance < lowestDistance) {
                        lowestDistance = distance
                        bestMatchAnswer = item.getString("answer")
                    }
                }
            }
            if (bestMatchAnswer != null) return bestMatchAnswer
        } catch (e: Exception) {}
        return null
    }
}
