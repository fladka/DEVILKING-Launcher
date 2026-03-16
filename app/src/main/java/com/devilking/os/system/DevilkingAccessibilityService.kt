package com.devilking.os.system

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
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
        // Phase 2: This is where we will write the code to "read" YouTube/WhatsApp later
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
        val startY = displayMetrics.heightPixels * 0.8f // Start near the bottom
        val endY = displayMetrics.heightPixels * 0.2f   // Drag to the top

        val path = Path()
        path.moveTo(middleX, startY)
        path.lineTo(middleX, endY)

        val gestureBuilder = GestureDescription.Builder()
        // 500 milliseconds = 0.5 second swipe speed
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 500)) 
        
        dispatchGesture(gestureBuilder.build(), null, null)
    }
}
