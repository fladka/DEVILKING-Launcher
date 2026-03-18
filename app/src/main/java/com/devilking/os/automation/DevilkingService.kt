package com.devilking.os.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
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
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        serviceInfo = info
        Log.d("DEVILKING_SYS", "God Mode Online. Physical Tap Engine Active.")
    }

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

    fun performDoubleSwipeDown() {
        performSwipeDown()
        Handler(Looper.getMainLooper()).postDelayed({ performSwipeDown() }, 600)
    }

    // --- UPGRADED LETHAL SNIPER ---
    fun executeSniperStrike(targetText: String): Boolean {
        val targetLower = targetText.lowercase()
        var bestNode: AccessibilityNodeInfo? = null

        fun scanNodes(node: AccessibilityNodeInfo) {
            val text = node.text?.toString()?.lowercase() ?: ""
            val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
            val hint = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                node.hintText?.toString()?.lowercase() ?: ""
            } else ""

            // Ignore our own terminal logs so it doesn't click history
            if (text.startsWith("root@devilking:~") || text.startsWith("> [")) {
                // Skip
            } else if (text.contains(targetLower) || contentDesc.contains(targetLower) || hint.contains(targetLower)) {
                // Prefer elements that are actual buttons or clickable
                if (bestNode == null || node.isClickable || node.className?.toString()?.contains("Button") == true) {
                    bestNode = node
                }
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                scanNodes(child)
            }
        }

        for (window in windows) {
            window.root?.let { scanNodes(it) }
        }

        if (bestNode != null) {
            // FORCE PHYSICAL X/Y TAP: Calculate coordinates and tap the glass
            val rect = Rect()
            bestNode!!.getBoundsInScreen(rect)
            executePhantomTap(rect.centerX().toFloat(), rect.centerY().toFloat())
            return true
        }
        return false
    }

    // --- UPGRADED AUTO-FOCUS INJECTOR ---
    fun executeGhostType(textToInject: String): Boolean {
        var targetNode: AccessibilityNodeInfo? = null

        fun findEditable(node: AccessibilityNodeInfo) {
            if (targetNode != null) return
            // Finds the FIRST available text box on the screen
            if (node.isEditable || node.className?.toString()?.contains("EditText") == true) {
                targetNode = node
                return
            }
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                findEditable(child)
            }
        }

        for (window in windows) {
            window.root?.let { findEditable(it) }
            if (targetNode != null) break
        }
        
        if (targetNode != null) {
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToInject)
            targetNode!!.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            return true
        }
        return false
    }

    // --- UPGRADED WHATSAPP ENGINE ---
    fun executeWhatsAppMacro(contactName: String, messageText: String) {
        serviceScope.launch {
            val launchIntent = packageManager.getLaunchIntentForPackage("com.whatsapp")
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(launchIntent)
            } else return@launch 

            delay(3000) // Wait for WhatsApp to fully open
            executeSniperStrike("Search") // Clicks the magnifying glass
            delay(1500)
            executeGhostType(contactName) // Auto-injects name into search bar
            delay(2000) 
            executeSniperStrike(contactName) // Clicks the resulting contact
            delay(2000)
            executeGhostType(messageText) // Auto-injects message into chat box
            delay(1000)
            executeSniperStrike("Send") // Clicks the send arrow
        }
    }
}
