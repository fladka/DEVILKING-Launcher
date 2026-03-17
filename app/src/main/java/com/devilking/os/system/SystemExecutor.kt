package com.devilking.os.system

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.net.Uri

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
        val commandText = rawCommand.substringAfter("[CMD:").substringBefore("]").trim()
        
        return when {
            commandText == "flashlight" -> toggleFlashlight()
            
            commandText.startsWith("open ") -> {
                val appName = commandText.removePrefix("open ").lowercase()
                launchApp(appName)
            }

            commandText.startsWith("call ") -> {
                val targetName = commandText.removePrefix("call ").trim()
                makeCall(targetName)
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

    private fun makeCall(contactName: String): String {
        return try {
            val resolver = context.contentResolver
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
            // Using LIKE allows for partial matches (e.g., "Mike" will find "Mike Smith")
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$contactName%")

            val cursor = resolver.query(uri, projection, selection, selectionArgs, null)
            
            if (cursor != null && cursor.moveToFirst()) {
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val phoneNumber = cursor.getString(numberIndex)
                cursor.close()

                val callIntent = Intent(Intent.ACTION_CALL)
                callIntent.data = Uri.parse("tel:$phoneNumber")
                callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(callIntent)
                
                "> [SYSTEM]: Initiating cellular override. Calling $contactName ($phoneNumber)..."
            } else {
                cursor?.close()
                "> [!] ERROR: Target '$contactName' not found in system registry."
            }
        } catch (e: SecurityException) {
            "> [!] ERROR: Security override failed. Ensure CALL_PHONE permission is granted."
        } catch (e: Exception) {
            "> [!] ERROR: Telecom matrix failure: ${e.message}"
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
                
                val isBlacklisted = aegisBlacklist.any { packageName.startsWith(it) }
                val isWhitelisted = aegisWhitelist.contains(packageName)
                
                if (isBlacklisted && !isWhitelisted) continue 
                
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return "> [SYSTEM]: Launching $name..."
            }
        }
        return "> [!] ERROR: Application '$appName' not found or is restricted by Aegis."
    }
}
