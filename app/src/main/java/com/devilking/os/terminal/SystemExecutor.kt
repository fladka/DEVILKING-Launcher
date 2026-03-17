package com.devilking.os.terminal

import android.content.Context
import android.content.Intent
import android.util.Log

// Notice: We injected Context into the class so it has the authority to send Broadcasts
class SystemExecutor(private val context: Context) {
    
    private val cmdRegex = Regex("""\[CMD:\s*(.*?)\]""")

    fun parseAndExecute(aiResponse: String): String {
        val match = cmdRegex.find(aiResponse)
        
        return if (match != null) {
            val command = match.groupValues[1].trim()
            Log.d("DEVILKING_ROUTER", "Target Acquired: \$command")
            executeRawCommand(command)
        } else {
            Log.d("DEVILKING_UI", "AI Speech: \$aiResponse")
            aiResponse
        }
    }

    private fun executeRawCommand(command: String): String {
        return when {
            command == "flashlight" -> { 
                Log.d("DEVILKING_EXEC", "Toggling Flashlight")
                "System: Flashlight toggled."
            }
            command == "scroll" -> { 
                Log.d("DEVILKING_EXEC", "Triggering Phantom Finger")
                // Transmitting signal to God Mode
                val intent = Intent("com.devilking.os.COMMAND")
                intent.putExtra("ACTION", "SCROLL")
                context.sendBroadcast(intent)
                "System: Phantom Finger executed."
            }
            command.startsWith("snipe ") -> {
                val target = command.removePrefix("snipe ").trim()
                Log.d("DEVILKING_EXEC", "Sniping UI node: \$target")
                // Transmitting coordinates to God Mode
                val intent = Intent("com.devilking.os.COMMAND")
                intent.putExtra("ACTION", "SNIPE")
                intent.putExtra("TARGET", target)
                context.sendBroadcast(intent)
                "System: Sniping UI target [\$target]."
            }
            command.startsWith("open ") -> {
                val appName = command.removePrefix("open ").trim()
                Log.d("DEVILKING_EXEC", "Bypassing firewall to open: \$appName")
                "System: Opening \$appName."
            }
            command == "none" -> {
                "System: Standing by."
            }
            else -> { 
                Log.e("DEVILKING_ERROR", "Unrecognized execution code: \$command")
                "Error: Unknown command [\$command]"
            }
        }
    }
}
