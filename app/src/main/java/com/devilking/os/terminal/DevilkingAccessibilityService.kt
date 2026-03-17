package com.devilking.os.terminal

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class DevilkingAccessibilityService : AccessibilityService() {

    // THE NEURAL RECEIVER: Listens for commands from the SystemExecutor
    private val commandReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.getStringExtra("ACTION")
            val target = intent?.getStringExtra("TARGET")
            
            Log.d("DEVILKING_GODMODE", "Neural Link Active: Action=\$action, Target=\$target")
            
            when (action) {
                "SNIPE" -> target?.let { snipeAndClickText(it) }
                "SCROLL" -> performSwipeUp()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val filter = IntentFilter("com.devilking.os.COMMAND")
        
        // Bulletproof registration for all Android versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(commandReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(commandReceiver, filter)
        }
        Log.d("DEVILKING_GODMODE", "God Mode Online. Receiver bound.")
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(commandReceiver)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Passive monitoring can be added here later
    }

    override fun onInterrupt() {}

    private fun performSwipeUp() {
        val path = Path()
        path.moveTo(500f, 1500f)
        path.lineTo(500f, 500f)
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 500))
            .build()
        dispatchGesture(gesture, null, null)
    }

    private fun snipeAndClickText(targetText: String, exactMatch: Boolean = false): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val foundNodes = rootNode.findAccessibilityNodeInfosByText(targetText)
        
        if (foundNodes.isNullOrEmpty()) {
            rootNode.recycle()
            return false
        }

        for (node in foundNodes) {
            val nodeText = node.text?.toString() ?: node.contentDescription?.toString() ?: ""
            if (exactMatch && !nodeText.equals(targetText, ignoreCase = true)) {
                node.recycle()
                continue
            }

            if (node.isClickable) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                node.recycle()
                rootNode.recycle()
                return true
            } else {
                var parent = node.parent
                while (parent != null) {
                    if (parent.isClickable) {
                        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        parent.recycle()
                        node.recycle()
                        rootNode.recycle()
                        return true
                    }
                    val nextParent = parent.parent
                    parent.recycle()
                    parent = nextParent
                }
            }
            node.recycle()
        }
        rootNode.recycle()
        return false
    }
}
