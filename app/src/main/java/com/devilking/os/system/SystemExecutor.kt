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
            "> [!] ERROR: Flashlight override failed. Vivo may have locked the hardware."
        }
    }

    private fun launchApp(appName: String): String {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        
        for (packageInfo in packages) {
            val name = pm.getApplicationLabel(packageInfo).toString().lowercase()
            
            if (name.contains(appName)) {
                val packageName = packageInfo.packageName
                
                if (aegisBlacklist.any { packageName.startsWith(it) }) {
                    return "> [!] AEGIS FIREWALL: Access to system package '$packageName' is strictly forbidden."
                }
                
                val launchIntent = pm.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return "> [SYSTEM]: Launching $name..."
                }
            }
        }
        return "> [!] ERROR: Application '$appName' not found in registry."
    }
}
