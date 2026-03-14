package com.devilking.os.automation

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

class DevilkingService : AccessibilityService() {

    companion object {
        // This allows our Terminal to talk directly to this background service
        var instance: DevilkingService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("DEVILKING_SYS", "Accessibility Core Online. Ghost Fingers ready.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // This is where our "Record Mode" will eventually live.
        // For now, it silently watches without doing anything so it doesn't drain battery.
    }

    override fun onInterrupt() {
        Log.e("DEVILKING_SYS", "Accessibility Core Interrupted!")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
