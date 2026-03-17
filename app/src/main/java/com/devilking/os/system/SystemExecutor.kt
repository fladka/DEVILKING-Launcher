package com.devilking.os.system

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.content.pm.PackageManager

class SystemExecutor(private val context: Context) {
    
    private var isFlashlightOn = false
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraId: String? = null

    private val aegisBlacklist = listOf("com.android", "com.vivo", "android")
    private val aegisWhitelist = listOf("com.android.chrome", "com.android.vending")

    init {
        try {
            cameraId = cameraManager.cameraIdList[0] 
        } catch (e: Exception) { }
    }

    fun executeCommand(rawCommand: String): String {
        // UPGRADED TO PARSE [CMD: ...]
        val commandText = rawCommand.substringAfter("[CMD:").substringBefore("]").trim()
        
        return when {
            commandText == "flashlight" -> toggleFlashlight()
            
            commandText.startsWith("open ") -> {
                val appName = commandText.removePrefix("open ").lowercase()
                launchApp(appName)
            }
            
            commandText == "scroll" -> {
                val service = com.devilking.os.system.DevilkingAccessibilityService.instance
                if (service != null) {
                    service.performSwipeUp()
                    "> [SYSTEM]: Phantom Finger executed. Swiping screen."
                } else {
                    "> [!] GOD MODE OFFLINE: Accessibility Service not bound."
                }
            }
            
            commandText.startsWith("snipe ") -> {
                val target = commandText.removePrefix("snipe ").trim()
                val service = com.devilking.os.system.DevilkingAccessibilityService.instance
                if (service != null) {
                    val success = service.executeSniperStrike(target)
                    if (success) "> [SYSTEM]: Target '$target' acquired and eliminated."
                    else "> [!] SNIPER ERROR: Target '$target' not found."
                } else {
                    "> [!] GOD MODE OFFLINE: Accessibility Service not bound."
                }
            }
            
            commandText == "none" -> "> [DEVILKING AI]: Standing by."
            
            else -> "> [!] AEGIS FIREWALL: Unauthorized core command blocked ($commandText)."
        }
    }

    private fun toggleFlashlight(): String {
        return try {
            if (cameraId != null) {
                isFlashlightOn = !isFlashlightOn
                cameraManager.setTorchMode(cameraId!!, isFlashlightOn)
                if (isFlashlightOn) "> [SYSTEM]: Flashlight Engaged."
                else "> [SYSTEM]: Flashlight Disabled."
            } else {
                "> [!] ERROR: Camera hardware not responding."
            }
        } catch (e: Exception) {
            "> [!] ERROR: Flashlight override failed."
        }
    }

    private fun launchApp(appName: String): String {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        
        for (packageInfo in packages) {
            val name = pm.getApplicationLabel(packageInfo).toString().lowercase()
            
            if (name.contains(appName)) {
                val packageName = packageInfo.packageName
                val launchIntent = pm.getLaunchIntentForPackage(packageName)
                
                if (launchIntent == null) continue
                
                // AEGIS FIREWALL CHECK
                val isBlacklisted = aegisBlacklist.any { packageName.startsWith(it) }
                val isWhitelisted = aegisWhitelist.contains(packageName)
                
                if (isBlacklisted && !isWhitelisted) {
                    continue 
                }
                
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return "> [SYSTEM]: Launching $name..."
            }
        }
        return "> [!] ERROR: Application '$appName' not found or is restricted by Aegis."
    }
}
