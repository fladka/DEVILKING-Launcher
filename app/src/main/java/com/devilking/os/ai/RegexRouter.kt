package com.devilking.os.ai

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.devilking.os.automation.DevilkingService

class RegexRouter(private val context: Context) {

    // Pre-compile Regex patterns in RAM for O(n) execution speed
    private val identityRegex = Regex("(?i)^(hello|hi|hey|wake up|who are you|status)$")
    private val timeRegex = Regex("(?i).*\\b(time|date|day|clock)\\b.*")
    private val batteryRegex = Regex("(?i).*\\b(battery|power|charge|hardware)\\b.*")
    
    // Slot Filling: Captures the word AFTER the command
    private val launchRegex = Regex("(?i)^(?:macro\\.launch|open|launch)\\s+([a-zA-Z0-9_.]+)")
    private val clickRegex = Regex("(?i)^(?:macro\\.click|click|tap)\\s+(.+)")

    fun route(prompt: String): String? {
        val cleanPrompt = prompt.trim()

        // 1. The Identity Reflex
        if (identityRegex.matches(cleanPrompt)) {
            return "> [DEVILKING AI]: System online. Awaiting command."
        }

        // 2. The Time & Space Reflex
        if (timeRegex.matches(cleanPrompt)) {
            val sdf = SimpleDateFormat("EEEE, MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
            val currentDate = sdf.format(Date())
            return "> [SYSTEM]: $currentDate."
        }

        // 3. The Telemetry Reflex (Reads Android Hardware Data)
        if (batteryRegex.matches(cleanPrompt)) {
            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                context.registerReceiver(null, ifilter)
            }
            val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryPct = if (scale > 0) (level * 100 / scale.toFloat()).toInt() else -1
            return "> [HARDWARE]: Vivo Core Battery at $batteryPct%."
        }

        // 4. The Action Reflex (Slot Filling for Ghost Fingers)
        launchRegex.find(cleanPrompt)?.let { matchResult ->
            val target = matchResult.groupValues[1].trim()
            val result = DevilkingService.instance?.executeAction("LAUNCH", target)
            return result ?: "> [!] ERROR: Ghost Service Offline. Check Accessibility."
        }

        clickRegex.find(cleanPrompt)?.let { matchResult ->
            val target = matchResult.groupValues[1].trim()
            val result = DevilkingService.instance?.executeAction("CLICK", target)
            return result ?: "> [!] ERROR: Ghost Service Offline. Check Accessibility."
        }

        // If no patterns match, return null. This tells the system to wake up Qwen 0.8B.
        return null
    }
}
