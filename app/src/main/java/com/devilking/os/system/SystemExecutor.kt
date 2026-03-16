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
    // THE FIX: VIP Pass for safe system apps
    private val aegisWhitelist = listOf("com.android.chrome", "com.android.vending")

    init {
        try {
            cameraId = cameraManager.cameraIdList[0] 
        } catch (e: Exception) { }
    }

    fun executeCommand(rawCommand: String): String {
        val commandText = rawCommand.substringAfter("[EXECUTE:").substringBefore("]").trim()
        
        return when {
            commandText == "FLASHLIGHT_TOGGLE" -> toggleFlashlight()
            commandText.startsWith("LAUNCH_APP_") -> {
                val appName = commandText.removePrefix("LAUNCH_APP_").lowercase()
                launchApp(appName)
            }
            else -> "> [!] AEGIS FIREWALL: Unauthorized core command blocked ($commandText)."
        }
    }

    private fun toggleFlashlight(): String {
        return try {
            if (cameraId != null) {
                isFlashlightOn = !isFlashlightOn
                cameraManager.setTorchMode(cameraId!!, isFlashlightOn)
                if (isFlashlightOn) "> [SYSTEM]: Flashlight Engaged." else "> [SYSTEM]: Flashlight Disabled."
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
                
                // If the app doesn't have a launcher UI, it's a hidden background process. Skip it.
                if (launchIntent == null) continue
                
                // AEGIS FIREWALL CHECK
                val isBlacklisted = aegisBlacklist.any { packageName.startsWith(it) }
                val isWhitelisted = aegisWhitelist.contains(packageName)
                
                if (isBlacklisted && !isWhitelisted) {
                    // THE FIX: Skip this protected app and keep searching instead of crashing!
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
