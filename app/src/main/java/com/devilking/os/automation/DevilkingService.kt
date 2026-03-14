package com.devilking.os.automation

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

class DevilkingService : AccessibilityService() {

    companion object {
        // The Master Instance allows our Terminal to talk directly to this background service
        var instance: DevilkingService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("DEVILKING_SYS", "Accessibility Core Online. Ghost Fingers ready.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Passive listening disabled for absolute stability
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    // --- THE MACRO EXECUTION ENGINE ---
    fun executeAction(command: String, target: String): String {
        return try {
            when (command.uppercase()) {
                "LAUNCH" -> {
                    val launchIntent = packageManager.getLaunchIntentForPackage(target)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(launchIntent)
                        "> [GHOST]: Launched target ($target)"
                    } else {
                        "> [!] ERROR: Target package not found."
                    }
                }
                "CLICK" -> {
                    val rootNode = rootInActiveWindow
                    if (rootNode != null) {
                        // Scan the invisible UI tree for any node matching the target text
                        val nodes = rootNode.findAccessibilityNodeInfosByText(target)
                        if (nodes.isNotEmpty()) {
                            val targetNode = nodes[0]
                            targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            targetNode.recycle()
                            "> [GHOST]: Clicked node ($target)"
                        } else {
                            "> [!] ERROR: Node ($target) not found on screen."
                        }
                    } else {
                        "> [!] ERROR: Cannot read screen. UI tree is null."
                    }
                }
                else -> "> [!] ERROR: Unknown Macro Command."
            }
        } catch (e: Exception) {
            "> [!] GHOST FATAL: ${e.message}"
        }
    }
}
