package com.devilking.os.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*

class DevilkingService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var volDownPressTime = 0L
    private var volUpPressTime = 0L

    companion object {
        var instance: DevilkingService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        
        // THE FIX: 'serviceInfo' is the correct Kotlin property
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        serviceInfo = info
        
        Log.d("DEVILKING_SYS", "God Mode Online. Volume Hijack Active.")
    }

    // --- HARDWARE VOLUME HIJACK ---
    override fun onKeyEvent(event: KeyEvent): Boolean {
        val action = event.action
        val keyCode = event.keyCode

        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (action == KeyEvent.ACTION_DOWN) {
                if (volDownPressTime == 0L) volDownPressTime = System.currentTimeMillis()
                return true
            } else if (action == KeyEvent.ACTION_UP) {
                val duration = System.currentTimeMillis() - volDownPressTime
                volDownPressTime = 0L
                if (duration > 500) sendBroadcast(Intent("com.devilking.os.WAKE_WORD_TRIGGERED"))
                return true
            }
        }

        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (action == KeyEvent.ACTION_DOWN) {
                if (volUpPressTime == 0L) volUpPressTime = System.currentTimeMillis()
                return true
            } else if (action == KeyEvent.ACTION_UP) {
                val duration = System.currentTimeMillis() - volUpPressTime
                volUpPressTime = 0L
                if (duration > 500) executePhantomTap(540f, 1200f)
                return true
            }
        }
        return super.onKeyEvent(event)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        serviceScope.cancel() 
    }

    // --- TIER 4: PHANTOM GESTURES ---
    fun executePhantomTap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 50)).build()
        dispatchGesture(gesture, null, null)
    }

    fun performSwipeUp() {
        val path = Path().apply { moveTo(500f, 1500f); lineTo(500f, 500f) }
        val gesture = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 500)).build()
        dispatchGesture(gesture, null, null)
    }

    fun performSwipeDown() {
        val path = Path().apply { moveTo(500f, 500f); lineTo(500f, 1500f) }
        val gesture = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 500)).build()
        dispatchGesture(gesture, null, null)
    }

    fun performDoubleSwipeUp() {
        performSwipeUp()
        Handler(Looper.getMainLooper()).postDelayed({ performSwipeUp() }, 600)
    }

    // THE FIX: Restored the missing Double Swipe Down function
    fun performDoubleSwipeDown() {
        performSwipeDown()
        Handler(Looper.getMainLooper()).postDelayed({ performSwipeDown() }, 600)
    }

    // --- THE LETHAL RECURSIVE SNIPER ---
    fun executeSniperStrike(targetText: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val targetLower = targetText.lowercase()

        fun scanNodes(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            val text = node.text?.toString()?.lowercase() ?: ""
            val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""

            // "Contains" check ignores brackets and exact matching
            if (text.contains(targetLower) || contentDesc.contains(targetLower)) return node

            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                val result = scanNodes(child)
                if (result != null) return result
            }
            return null
        }

        val foundNode = scanNodes(rootNode)

        if (foundNode != null) {
            var clickableNode: AccessibilityNodeInfo? = foundNode
            while (clickableNode != null && !clickableNode.isClickable) {
                clickableNode = clickableNode.parent
            }
            if (clickableNode != null) {
                clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
        }
        return false
    }

    fun executeGhostType(targetField: String, textToInject: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val targetNodes = rootNode.findAccessibilityNodeInfosByText(targetField)
        
        for (node in targetNodes) {
            if (node.isEditable || node.className?.toString()?.contains("EditText") == true) {
                val arguments = Bundle()
                arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToInject)
                node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                return true
            }
        }
        return false
    }

    // --- THE MACRO ENGINE ---
    fun executeWhatsAppMacro(contactName: String, messageText: String) {
        serviceScope.launch {
            val launchIntent = packageManager.getLaunchIntentForPackage("com.whatsapp")
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
            } else return@launch 

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
