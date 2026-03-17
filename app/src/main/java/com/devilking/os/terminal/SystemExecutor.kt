package com.devilking.os.terminal

import android.util.Log

class SystemExecutor {
    // This regex catches the exact format we brainwashed the AI to output
    private val cmdRegex = Regex("""\[CMD:\s*(.*?)\]""")

    fun parseAndExecute(aiResponse: String): String {
        val match = cmdRegex.find(aiResponse)
        
        return if (match != null) {
            // The AI gave a direct order. Strip the brackets and execute.
            val command = match.groupValues[1].trim()
            Log.d("DEVILKING_ROUTER", "Target Acquired: $command")
            executeRawCommand(command)
        } else {
            // The AI gave a text response. Pass it back to UI.
            Log.d("DEVILKING_UI", "AI Speech: $aiResponse")
            aiResponse
        }
    }

    private fun executeRawCommand(command: String): String {
        return when {
            command == "flashlight" -> { 
                Log.d("DEVILKING_EXEC", "Toggling Flashlight")
                // TODO: CameraManager toggle
                "System: Flashlight toggled."
            }
            command == "scroll" -> { 
                Log.d("DEVILKING_EXEC", "Triggering Phantom Finger")
                // TODO: Broadcast to DevilkingAccessibilityService
                "System: Phantom Finger executed."
            }
            command.startsWith("snipe ") -> {
                val target = command.removePrefix("snipe ").trim()
                Log.d("DEVILKING_EXEC", "Sniping UI node: $target")
                // TODO: Broadcast target to Accessibility Service Smart Sniper
                "System: Sniping UI target [$target]."
            }
            command.startsWith("open ") -> {
                val appName = command.removePrefix("open ").trim()
                Log.d("DEVILKING_EXEC", "Bypassing firewall to open: $appName")
                // TODO: Launch App Intent
                "System: Opening $appName."
            }
            command == "none" -> {
                "System: Standing by." // Silent execution, AI just wanted to speak.
            }
            else -> { 
                Log.e("DEVILKING_ERROR", "Unrecognized execution code: $command")
                "Error: Unknown command [$command]"
            }
        }
    }
}
