package com.devilking.os.system

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.net.Uri
import com.devilking.os.automation.DevilkingService

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
            
            commandText.startsWith("scroll") || commandText.startsWith("2x scroll") -> {
                val service = DevilkingService.instance
                if (service != null) {
                    when (commandText) {
                        "scroll" -> service.performSwipeUp()
                        "scroll down" -> service.performSwipeDown()
                        "2x scroll" -> service.performDoubleSwipeUp()
                        "2x scroll down" -> service.performDoubleSwipeDown()
                    }
                    "> [SYSTEM]: Phantom Gestures executed."
                } else {
                    "> [!] GOD MODE OFFLINE: Accessibility Service not bound."
                }
            }
            
            commandText.startsWith("snipe ") -> {
                val target = commandText.removePrefix("snipe ").trim()
                val service = DevilkingService.instance
                if (service != null) {
                    val success = service.executeSniperStrike(target)
                    if (success) "> [SYSTEM]: Target '$target' acquired and eliminated."
                    else "> [!] SNIPER ERROR: Target '$target' not found."
                } else {
                    "> [!] GOD MODE OFFLINE: Accessibility Service not bound."
                }
            }

            commandText.startsWith("type ") -> {
                if (commandText.contains(">")) {
                    val parts = commandText.removePrefix("type ").split(">", limit = 2)
                    val targetField = parts[0].trim()
                    val textToInject = parts[1].trim()
                    
                    val service = DevilkingService.instance
                    if (service != null) {
                        val success = service.executeGhostType(targetField, textToInject)
                        if (success) "> [SYSTEM]: Ghost Typed '$textToInject' into '$targetField'."
                        else "> [!] GHOST ERROR: Input field '$targetField' not found on screen."
                    } else {
                        "> [!] GOD MODE OFFLINE: Accessibility Service not bound."
                    }
                } else {
                    "> [!] SYNTAX ERROR: Incorrect format. Use -> type [target field] > [text]"
                }
            }

            commandText.startsWith("macro whatsapp ") -> {
                if (commandText.contains(">")) {
                    val parts = commandText.removePrefix("macro whatsapp ").split(">", limit = 2)
                    if (parts.size == 2) {
                        val contactName = parts[0].trim()
                        val messageText = parts[1].trim()
                        
                        val service = DevilkingService.instance
                        if (service != null) {
                            service.executeWhatsAppMacro(contactName, messageText)
                            "> [SYSTEM]: Macro engaged. Hijacking UI to message $contactName..."
                        } else {
                            "> [!] GOD MODE OFFLINE."
                        }
                    } else {
                        "> [!] SYNTAX ERROR: macro whatsapp > [name] > [message]"
                    }
                } else {
                    "> [!] SYNTAX ERROR: macro whatsapp > [name] > [message]"
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
