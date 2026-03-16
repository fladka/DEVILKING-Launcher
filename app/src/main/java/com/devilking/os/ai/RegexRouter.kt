package com.devilking.os.ai

import android.content.Context
import com.devilking.os.system.SystemExecutor

class RegexRouter(private val context: Context) {
    fun route(prompt: String): String? {
        val input = prompt.lowercase()
        val executor = SystemExecutor(context)
        
        // --- THE TIER 1 FAST PATH ---
        
        // 1. Hardware Control (0-Second Execution)
        if (input == "flashlight" || input == "lumos") {
            return executor.executeCommand("[EXECUTE: FLASHLIGHT_TOGGLE]")
        }
        
        // 2. App Launching (0-Second Execution)
        if (input.startsWith("open ")) {
            val appName = input.removePrefix("open ").trim()
            return executor.executeCommand("[EXECUTE: LAUNCH_APP_$appName]")
        }

        // If no keywords match, return null to wake up the Qwen AI
        return null 
    }
}
