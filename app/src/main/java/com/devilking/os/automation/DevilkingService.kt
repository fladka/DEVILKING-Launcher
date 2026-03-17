package com.devilking.os.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class DevilkingService : AccessibilityService() {

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

    // --- TIER 2: TELEKINESIS (SNIPER) ---
    fun executeSniperStrike(targetText: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val nodes = rootNode.findAccessibilityNodeInfosByText(targetText)
        
        if (nodes.isNotEmpty()) {
            val targetNode = nodes[0]
            var clickableNode: AccessibilityNodeInfo? = targetNode
            
            // Traverse up the UI tree to find the clickable parent
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
            // Verify node is an input field before injecting text
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
}
