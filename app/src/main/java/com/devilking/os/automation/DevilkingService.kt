package com.devilking.os.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*

class DevilkingService : AccessibilityService() {

    // The coroutine scope that keeps the macro engine running asynchronously
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    companion object {
        var instance: DevilkingService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("DEVILKING_SYS", "Accessibility Core Online. God Mode ready.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        // Cancel all running macros if the service shuts down
        serviceScope.cancel() 
    }

    // --- TIER 4: PHANTOM GESTURES ---
    fun performSwipeUp() {
        val path = Path()
        path.moveTo(500f, 1500f)
        path.lineTo(500f, 500f)
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 500))
            .build()
        dispatchGesture(gesture, null, null)
    }

    fun performSwipeDown() {
        val path = Path()
        path.moveTo(500f, 500f)
        path.lineTo(500f, 1500f)
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 500))
            .build()
        dispatchGesture(gesture, null, null)
    }

    fun performDoubleSwipeUp() {
        performSwipeUp()
        Handler(Looper.getMainLooper()).postDelayed({
            performSwipeUp()
        }, 600)
    }

    fun performDoubleSwipeDown() {
        performSwipeDown()
        Handler(Looper.getMainLooper()).postDelayed({
            performSwipeDown()
        }, 600)
    }

    // --- TIER 2: TELEKINESIS (SNIPER) ---
    fun executeSniperStrike(targetText: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val nodes = rootNode.findAccessibilityNodeInfosByText(targetText)
        
        if (nodes.isNotEmpty()) {
            val targetNode = nodes[0]
            var clickableNode: AccessibilityNodeInfo? = targetNode
            
            while (clickableNode != null && !clickableNode.isClickable) {
                clickableNode = clickableNode.parent
            }
            
            if (clickableNode != null) {
                clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                clickableNode.recycle()
                targetNode.recycle()
                rootNode.recycle()
                return true
            }
            targetNode.recycle()
        }
        rootNode.recycle()
        return false
    }

    // --- TIER 2: TELEKINESIS (GHOST TYPING) ---
    fun executeGhostType(targetField: String, textToInject: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val targetNodes = rootNode.findAccessibilityNodeInfosByText(targetField)
        
        for (node in targetNodes) {
            if (node.isEditable || node.className?.toString()?.contains("EditText") == true) {
                val arguments = Bundle()
                arguments.putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, 
                    textToInject
                )
                node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                node.recycle()
                rootNode.recycle()
                return true
            }
            node.recycle()
        }
        rootNode.recycle()
        return false
    }

    // --- TIER 5: THE MACRO ENGINE ---
    fun executeWhatsAppMacro(contactName: String, messageText: String) {
        serviceScope.launch {
            val launchIntent = packageManager.getLaunchIntentForPackage("com.whatsapp")
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
            } else {
                return@launch 
            }

            delay(2000)
            executeSniperStrike("Search")
            delay(500)
            executeGhostType("Search…", contactName) 
            delay(1000)
            executeSniperStrike(contactName)
            delay(1000)
            executeGhostType("Message", messageText)
            delay(500)
            executeSniperStrike("Send")
        }
    }
}
