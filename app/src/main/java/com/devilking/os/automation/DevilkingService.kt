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
        Log.d("DEVILKING_SYS", "God Mode Online. Multi-Window Deep Scan Active.")
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

    // --- UPGRADED LETHAL DEEP SCANNER ---
    private fun scanNodes(node: AccessibilityNodeInfo, targetLower: String): AccessibilityNodeInfo? {
        val text = node.text?.toString()?.lowercase() ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        val hint = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            node.hintText?.toString()?.lowercase() ?: ""
        } else ""

        if (text.contains(targetLower) || contentDesc.contains(targetLower) || hint.contains(targetLower)) return node

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = scanNodes(child, targetLower)
            if (result != null) return result
        }
        return null
    }

    fun executeSniperStrike(targetText: String): Boolean {
        val targetLower = targetText.lowercase()
        var foundNode: AccessibilityNodeInfo? = null

        // Deep Scan: Check all active interactive windows (keyboards, apps, overlays)
        val windowList = windows
        for (window in windowList) {
            val root = window.root ?: continue
            foundNode = scanNodes(root, targetLower)
            if (foundNode != null) break
        }

        if (foundNode != null) {
            var clickableNode: AccessibilityNodeInfo? = foundNode
            while (clickableNode != null && !clickableNode.isClickable) {
                clickableNode = clickableNode.parent
            }
            if (clickableNode != null) {
                clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            } else {
                // FALLBACK: If Android denies digital click, calculate coordinates and Phantom Tap it physically
                val rect = Rect()
                foundNode.getBoundsInScreen(rect)
                executePhantomTap(rect.centerX().toFloat(), rect.centerY().toFloat())
                return true
            }
        }
        return false
    }

    fun executeGhostType(targetField: String, textToInject: String): Boolean {
        val targetLower = targetField.lowercase()
        var targetNode: AccessibilityNodeInfo? = null

        val windowList = windows
        for (window in windowList) {
            val root = window.root ?: continue
            targetNode = scanNodes(root, targetLower)
            if (targetNode != null) break
        }
        
        if (targetNode != null) {
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToInject)
            targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            return true
        }
        return false
    }

    fun executeWhatsAppMacro(contactName: String, messageText: String) {
        serviceScope.launch {
            val launchIntent = packageManager.getLaunchIntentForPackage("com.whatsapp")
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(launchIntent)
            } else return@launch 

            // WhatsApp specific timings and targeting
            delay(3000) 
            executeSniperStrike("Search") 
            delay(1000)
            executeGhostType("Search", contactName) 
            delay(1500) 
            executeSniperStrike(contactName)
            delay(1500)
            executeGhostType("Message", messageText)
            delay(1000)
            executeSniperStrike("Send")
        }
    }
}
