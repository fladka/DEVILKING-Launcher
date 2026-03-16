package com.devilking.os.system

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

class DevilkingAccessibilityService : AccessibilityService() {

    companion object {
        var instance: DevilkingAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("DEVILKING_OS", "God Mode Accessibility Service Connected.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Monitoring phase reserved for future passive triggers
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    // THE PHANTOM FINGER
    fun performSwipeUp() {
        val displayMetrics = resources.displayMetrics
        val middleX = displayMetrics.widthPixels / 2f
        val startY = displayMetrics.heightPixels * 0.8f 
        val endY = displayMetrics.heightPixels * 0.2f   

        val path = Path()
        path.moveTo(middleX, startY)
        path.lineTo(middleX, endY)

        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 500)) 
        
        dispatchGesture(gestureBuilder.build(), null, null)
    }

    // THE SMART UI SNIPER
    fun executeSniperStrike(targetText: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val targetNode = findNodeByText(rootNode, targetText)

        if (targetNode != null) {
            // Tier 1: Attempt native Accessibility Click
            if (targetNode.isClickable) {
                targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
            
            // Tier 2: Search for a clickable parent
            var parent = targetNode.parent
            while (parent != null) {
                if (parent.isClickable) {
                    parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return true
                }
                parent = parent.parent
            }
            
            // Tier 3: The Ghost Strike (Coordinate XY Tap Fallback)
            val rect = Rect()
            targetNode.getBoundsInScreen(rect)
            val x = rect.centerX().toFloat()
            val y = rect.centerY().toFloat()
            performCoordinateClick(x, y)
            return true
        }
        return false
    }

    private fun findNodeByText(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val nodeText = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""
        
        if (nodeText.contains(text, ignoreCase = true) || contentDesc.contains(text, ignoreCase = true)) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val result = findNodeByText(child, text)
                if (result != null) return result
            }
        }
        return null
    }

    private fun performCoordinateClick(x: Float, y: Float) {
        val path = Path()
        path.moveTo(x, y)
        val gestureBuilder = GestureDescription.Builder()
        // 50ms stroke simulates a rapid screen tap
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 50)) 
        dispatchGesture(gestureBuilder.build(), null, null)
    }
}
