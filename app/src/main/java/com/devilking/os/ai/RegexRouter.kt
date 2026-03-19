package com.devilking.os.ai

import android.content.Context
import com.devilking.os.system.SystemExecutor
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.min

class RegexRouter(private val context: Context) {
    
    private val executor = SystemExecutor(context)

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

        // --- FAST PATHS ---
        if (input == "clear") return executor.executeCommand("[CMD: clear]")
        if (input == "settings") return executor.executeCommand("[CMD: settings]")
        if (input == "scan screen") return executor.executeCommand("[CMD: scan screen]") 
        if (input == "flashlight" || input == "lumos") return executor.executeCommand("[CMD: flashlight]")
        
        // THE FIX: Route the Hijack Commands to the Executor
        if (input == "hijack on") return executor.executeCommand("[CMD: hijack on]")
        if (input == "hijack off") return executor.executeCommand("[CMD: hijack off]")
        
        if (input == "help") {
            return """
                *** DEVILKING OS COMMAND REGISTRY ***
                > settings      : Launch Macro Interface
                > learn [p]>[r] : Teach the OS a new reflex
                > matrix.init   : Reset memory matrix
                > open [app]    : Launches application
                > hijack off    : Disable Vol Button mic
                > hijack on     : Enable Vol Button mic
                > clear         : Wipes terminal history
                
                [ GOD MODE: SCREEN AUTOMATION ]
                > scan screen   : Dumps UI Matrix coordinates
                > scroll        : Phantom Finger swiping
                > snipe [text]  : Physically clicks UI
                > type [t]      : Ghost Typing text
            """.trimIndent()
        }

        if (input == "matrix.init") {
            val masterJson = """
                [
                  {
                    "exact_variations": ["who am i", "who is my creator"],
                    "answer": "You are the Architect."
                  },
                  {
                    "exact_variations": ["read my screen", "scan screen", "what is on the screen", "check screen", "look at screen", "screen dekho"],
                    "answer": "[CMD: scan screen]"
                  },
                  {
                    "exact_variations": ["scroll down", "go down", "swipe down", "page down", "niche karo", "aur niche"],
                    "answer": "[CMD: scroll down]"
                  },
                  {
                    "exact_variations": ["scroll up", "go up", "swipe up", "page up", "upar karo", "wapas upar"],
                    "answer": "[CMD: scroll up]"
                  },
                  {
                    "exact_variations": ["scroll fast", "2x scroll", "double swipe down", "tez niche karo"],
                    "answer": "[CMD: 2x scroll down]"
                  },
                  {
                    "exact_variations": ["turn on flashlight", "lumos", "torch on", "light on", "batti jalao", "flash on"],
                    "answer": "[CMD: flashlight]"
                  },
                  {
                    "exact_variations": ["clear terminal", "wipe screen", "clean screen", "clear", "saaf karo"],
                    "answer": "[CMD: clear]"
                  },
                  {
                    "exact_variations": ["open settings", "system settings", "macro interface", "settings kholo"],
                    "answer": "[CMD: settings]"
                  },
                  {
                    "exact_variations": ["help me", "show commands", "what can you do", "commands dikhao", "kya kar sakte ho"],
                    "answer": "[CMD: help]"
                  },
                  {
                    "exact_variations": ["go home", "home screen", "main screen", "home pe jao", "back to home"],
                    "answer": "[CMD: home]"
                  },
                  {
                    "exact_variations": ["recent apps", "show recents", "background apps", "recent kholo"],
                    "answer": "[CMD: recents]"
                  },
                  {
                    "exact_variations": ["lock phone", "screen off", "display off", "phone band karo", "lock it"],
                    "answer": "[CMD: lock]"
                  },
                  {
                    "exact_variations": ["volume up", "increase volume", "awaaz badao", "louder", "make it loud"],
                    "answer": "[CMD: volume up]"
                  },
                  {
                    "exact_variations": ["volume down", "decrease volume", "awaaz kam karo", "quieter", "make it quiet"],
                    "answer": "[CMD: volume down]"
                  },
                  {
                    "exact_variations": ["open whatsapp", "whatsapp kholo", "launch whatsapp", "start whatsapp"],
                    "answer": "[CMD: open whatsapp]"
                  },
                  {
                    "exact_variations": ["open youtube", "youtube kholo", "launch youtube", "play youtube"],
                    "answer": "[CMD: open youtube]"
                  },
                  {
                    "exact_variations": ["open camera", "camera kholo", "take a picture", "photo khicho"],
                    "answer": "[CMD: open camera]"
                  },
                  {
                    "exact_variations": ["play music", "gana bajao", "open spotify", "spotify kholo", "music chalu karo"],
                    "answer": "[CMD: open spotify]"
                  },
                  {
                    "exact_variations": ["click search", "tap search", "snipe search", "search pe click karo", "khojo"],
                    "answer": "[CMD: snipe search]"
                  },
                  {
                    "exact_variations": ["click send", "tap send", "snipe send", "bhej do", "send pe click karo"],
                    "answer": "[CMD: snipe send]"
                  },
                  {
                    "exact_variations": ["click back", "go back", "snipe back", "peeche jao", "wapas jao"],
                    "answer": "[CMD: snipe back]"
                  },
                  {
                    "exact_variations": ["click play", "play video", "snipe play", "chalu karo", "play karo"],
                    "answer": "[CMD: snipe play]"
                  },
                  {
                    "exact_variations": ["click pause", "stop video", "snipe pause", "roko", "pause karo"],
                    "answer": "[CMD: snipe pause]"
                  },
                  {
                    "exact_variations": ["click cancel", "tap cancel", "snipe cancel", "hatao", "cancel karo"],
                    "answer": "[CMD: snipe cancel]"
                  },
                  {
                    "exact_variations": ["click ok", "tap ok", "snipe ok", "theek hai", "confirm karo"],
                    "answer": "[CMD: snipe ok]"
                  },
                  {
                    "exact_variations": ["click profile", "open profile", "snipe profile", "meri profile", "account kholo"],
                    "answer": "[CMD: snipe profile]"
                  }
                ]
            """.trimIndent()
            
            File(context.filesDir, "memory.json").writeText(masterJson)
            return "> [SYSTEM]: Maximized Master matrix initialized. 26 core reflexes armed."
        }

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
