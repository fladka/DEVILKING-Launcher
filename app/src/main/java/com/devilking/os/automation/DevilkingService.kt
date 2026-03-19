package com.devilking.os.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class DevilkingService : AccessibilityService() {

    private var volDownPressTime = 0L

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
        Log.d("DEVILKING_SYS", "God Mode Online. Hybrid Eye Ready.")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val prefs = getSharedPreferences("DEVILKING_SETTINGS", Context.MODE_PRIVATE)
        val isHijackEnabled = prefs.getBoolean("vol_hijack_enabled", true)

        if (!isHijackEnabled) {
            return super.onKeyEvent(event) 
        }

        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                if (volDownPressTime == 0L) {
                    volDownPressTime = System.currentTimeMillis()
                }
                return true 
            } else if (event.action == KeyEvent.ACTION_UP) {
                val duration = System.currentTimeMillis() - volDownPressTime
                volDownPressTime = 0L
                
                if (duration > 500) {
                    sendBroadcast(Intent("com.devilking.os.WAKE_WORD_TRIGGERED"))
                } else {
                    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                }
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
    }

    fun executePhantomTap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0L, 50L)).build()
        dispatchGesture(gesture, null, null)
    }

    fun performSwipeUp() {
        val path = Path().apply { moveTo(500f, 1500f); lineTo(500f, 500f) }
        val gesture = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0L, 500L)).build()
        dispatchGesture(gesture, null, null)
    }

    fun performSwipeDown() {
        val path = Path().apply { moveTo(500f, 500f); lineTo(500f, 1500f) }
        val gesture = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0L, 500L)).build()
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

    fun dumpScreenMatrix(): String {
        val sb = StringBuilder()
        sb.append("\n--- ACTIVE SCREEN MATRIX ---\n")
        var counter = 1

        fun scanNode(node: AccessibilityNodeInfo) {
            if (node.isVisibleToUser) {
                val text = node.text?.toString() ?: ""
                val desc = node.contentDescription?.toString() ?: ""
                val hint = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    node.hintText?.toString() ?: ""
                } else ""
                
                val uiType = node.className?.toString()?.split(".")?.last() ?: "UI Element"

                if (text.isNotEmpty() || desc.isNotEmpty() || hint.isNotEmpty() || node.isClickable || node.isEditable) {
                    val rect = Rect()
                    node.getBoundsInScreen(rect)
                    
                    var label = text
                    if (label.isEmpty()) label = hint
                    if (label.isEmpty()) label = desc
                    if (label.isEmpty() && node.isClickable) label = "Unnamed Button"

                    if (label.isNotEmpty() && rect.width() > 0 && rect.height() > 0) {
                        sb.append("[$counter] $uiType: '$label' (Center X: ${rect.centerX()}, Y: ${rect.centerY()})\n")
                        counter++
                    }
                }
            }
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                scanNode(child)
            }
        }

        for (window in windows) {
            window.root?.let { scanNode(it) }
        }

        if (counter == 1) {
            return getFallbackOCR()
        }
        return sb.toString()
    }

    // THE ULTIMATE FIX: Concurrency without Kotlin Coroutines. 
    // This physically cannot throw a "resume" compile error.
    private fun getFallbackOCR(): String {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
            return "> [!] VISION OFFLINE: Requires Android 11+."
        }
        
        var finalResult = ""
        val latch = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        
        takeScreenshot(android.view.Display.DEFAULT_DISPLAY, executor, object : AccessibilityService.TakeScreenshotCallback {
            override fun onSuccess(screenshotResult: AccessibilityService.ScreenshotResult) {
                val bitmap = android.graphics.Bitmap.wrapHardwareBuffer(screenshotResult.hardwareBuffer, screenshotResult.colorSpace)
                if (bitmap != null) {
                    val image = com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0)
                    val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS)
                    
                    recognizer.process(image)
                        .addOnSuccessListener { text ->
                            val sb = java.lang.StringBuilder("\n--- ACTIVE SCREEN MATRIX (VISION OCR FALLBACK) ---\n")
                            var visionCounter = 1
                            
                            for (block in text.textBlocks) {
                                val rect = block.boundingBox
                                if (rect != null) {
                                    val label = block.text.replace("\n", " ")
                                    sb.append("[$visionCounter] VisionText: '$label' (Center X: ${rect.centerX()}, Y: ${rect.centerY()})\n")
                                    visionCounter++
                                }
                            }
                            screenshotResult.hardwareBuffer.close()
                            finalResult = if (visionCounter == 1) "> [!] MATRIX EMPTY: Screen is completely blank." else sb.toString()
                            latch.countDown()
                        }
                        .addOnFailureListener {
                            screenshotResult.hardwareBuffer.close()
                            finalResult = "> [!] VISION ERROR: ML Kit failed to read pixels."
                            latch.countDown()
                        }
                } else {
                    screenshotResult.hardwareBuffer.close()
                    finalResult = "> [!] VISION ERROR: Hardware buffer conversion failed."
                    latch.countDown()
                }
            }
            override fun onFailure(errorCode: Int) {
                finalResult = "> [!] VISION ERROR: System blocked the screenshot (Code: $errorCode)."
                latch.countDown()
            }
        })
        
        try {
            latch.await(3, TimeUnit.SECONDS)
        } catch (e: Exception) {
            finalResult = "> [!] VISION ERROR: Thread Timeout."
        }
        
        return if (finalResult.isEmpty()) "> [!] VISION ERROR: Unknown timeout." else finalResult
    }

    fun executeSniperStrike(targetText: String): Boolean {
        val targetLower = targetText.lowercase()
        var bestNode: AccessibilityNodeInfo? = null

        fun scanNodesForSniper(node: AccessibilityNodeInfo) {
            if (node.isVisibleToUser) {
                val text = node.text?.toString()?.lowercase() ?: ""
                val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
                val hint = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    node.hintText?.toString()?.lowercase() ?: ""
                } else ""

                if (text.startsWith("root@devilking:~") || text.startsWith("> [") || text.contains("--- active screen matrix ---")) {
                } else if (text.contains(targetLower) || contentDesc.contains(targetLower) || hint.contains(targetLower)) {
                    if (bestNode == null || node.isClickable || node.className?.toString()?.contains("Button") == true) {
                        bestNode = node
                    }
                }
            }
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                scanNodesForSniper(child)
            }
        }

        for (window in windows) {
            window.root?.let { scanNodesForSniper(it) }
        }

        if (bestNode != null) {
            val rect = Rect()
            bestNode!!.getBoundsInScreen(rect)
            executePhantomTap(rect.centerX().toFloat(), rect.centerY().toFloat())
            return true
        }
        return false
    }

    fun executeGhostType(textToInject: String): Boolean {
        var targetNode: AccessibilityNodeInfo? = null

        fun findEditable(node: AccessibilityNodeInfo) {
            if (targetNode != null) return
            if (node.isVisibleToUser && (node.isEditable || node.className?.toString()?.contains("EditText") == true)) {
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

    fun executeWhatsAppMacro(contactName: String, messageText: String) {
        // Rewritten without coroutines to guarantee zero compile errors
        Handler(Looper.getMainLooper()).postDelayed({
            val launchIntent = packageManager.getLaunchIntentForPackage("com.whatsapp")
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(launchIntent)
            }
        }, 0)
        
        Handler(Looper.getMainLooper()).postDelayed({ executeSniperStrike("Search") }, 3000)
        Handler(Looper.getMainLooper()).postDelayed({ executeGhostType(contactName) }, 4500)
        Handler(Looper.getMainLooper()).postDelayed({ executePhantomTap(540f, 500f) }, 6500)
        Handler(Looper.getMainLooper()).postDelayed({ executeGhostType(messageText) }, 8500)
        Handler(Looper.getMainLooper()).postDelayed({ executeSniperStrike("Send") }, 9500)
    }
}
