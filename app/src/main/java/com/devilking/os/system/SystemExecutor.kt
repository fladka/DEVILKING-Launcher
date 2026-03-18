package com.devilking.os.system

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.provider.ContactsContract

class SystemExecutor(private val context: Context) {

    private var isFlashlightOn = false

    fun executeCommand(commandString: String): String {
        val cmd = commandString.removePrefix("[CMD: ").removeSuffix("]").trim()

        // 1. AEGIS FIREWALL (Whitelist upgraded to allow 2x)
        val isSafe = cmd.startsWith("flashlight") ||
                     cmd.startsWith("open ") ||
                     cmd.startsWith("call ") ||
                     cmd.startsWith("scroll") ||
                     cmd.startsWith("2x ") || 
                     cmd.startsWith("snipe ") ||
                     cmd.startsWith("type ") ||
                     cmd.startsWith("macro ")

        if (!isSafe) {
            return "> [!] AEGIS FIREWALL: Unauthorized core command blocked ($cmd)."
        }

        // TIER 1: HARDWARE CONTROLS
        if (cmd == "flashlight") return toggleFlashlight()
        if (cmd.startsWith("call ")) return initiateCall(cmd.removePrefix("call ").trim())
        if (cmd.startsWith("open ")) return launchApp(cmd.removePrefix("open ").trim())

        // TIER 4: GOD MODE RELAYS (Accessibility)
        val godMode = com.devilking.os.automation.DevilkingService.instance
            ?: return "> [!] GOD MODE OFFLINE: Accessibility Service not bound."

        if (cmd == "scroll" || cmd == "scroll up") {
            godMode.performSwipeUp(); return "> [SYSTEM]: Phantom Finger - Swiping Up."
        }
        if (cmd == "scroll down") {
            godMode.performSwipeDown(); return "> [SYSTEM]: Phantom Finger - Swiping Down."
        }
        if (cmd == "2x scroll" || cmd == "2x scroll up") {
            godMode.performDoubleSwipeUp(); return "> [SYSTEM]: Phantom Finger - Double Swiping Up."
        }
        if (cmd == "2x scroll down" || cmd == "2xscroll down") {
            godMode.performDoubleSwipeDown(); return "> [SYSTEM]: Phantom Finger - Double Swiping Down."
        }

        // MACRO ENGINE
        if (cmd.startsWith("macro whatsapp")) {
            val payload = cmd.removePrefix("macro whatsapp").trim().removePrefix(">").trim()
            val parts = payload.split(">").map { it.trim() }
            if (parts.size >= 2) {
                godMode.executeWhatsAppMacro(parts[0], parts[1])
                return "> [SYSTEM]: Executing WhatsApp Macro for ${parts[0]}..."
            } else {
                return "> [!] MACRO SYNTAX ERROR: Use 'macro whatsapp > Name > Message'"
            }
        }

        // LETHAL SNIPER
        if (cmd.startsWith("snipe ")) {
            val target = cmd.removePrefix("snipe ").trim()
            val cleanTarget = target.replace("[", "").replace("]", "").trim()
            val success = godMode.executeSniperStrike(cleanTarget)
            return if (success) "> [SYSTEM]: Sniper Strike confirmed on '$cleanTarget'."
                   else "> [!] SNIPER ERROR: Target '$cleanTarget' not found on screen."
        }

        // GHOST TYPING
        if (cmd.startsWith("type ")) {
            val payload = cmd.removePrefix("type ").trim()
            val parts = payload.split(">").map { it.trim() }
            if (parts.size >= 2) {
                val success = godMode.executeGhostType(parts[0], parts[1])
                return if (success) "> [SYSTEM]: Ghost Typed into '${parts[0]}'." else "> [!] TYPE ERROR: Target field not found."
            }
            return "> [!] TYPE SYNTAX ERROR: Use 'type [UI Target] > [Text]'"
        }

        return "> [!] EXECUTOR ERROR: Command bypassed Aegis but lacks execution logic."
    }

    private fun toggleFlashlight(): String {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            isFlashlightOn = !isFlashlightOn
            cameraManager.setTorchMode(cameraId, isFlashlightOn)
            if (isFlashlightOn) "> [SYSTEM]: Flashlight Engaged." else "> [SYSTEM]: Flashlight Disabled."
        } catch (e: Exception) {
            "> [!] HARDWARE ERROR: Flashlight module unavailable."
        }
    }

    private fun launchApp(appName: String): String {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
        for (app in packages) {
            val name = pm.getApplicationLabel(app).toString().lowercase()
            if (name.contains(appName.lowercase())) {
                if (app.packageName.contains("com.vivo.") && !app.packageName.contains("calculator")) continue
                val intent = pm.getLaunchIntentForPackage(app.packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return "> [SYSTEM]: Launching $name..."
                }
            }
        }
        return "> [!] ERROR: Application '$appName' not found or is restricted by Aegis."
    }

    private fun initiateCall(contactName: String): String {
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI, Uri.encode(contactName))
            val cursor = context.contentResolver.query(uri, arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts.HAS_PHONE_NUMBER), null, null, null)
            
            var phoneNum: String? = null
            if (cursor != null && cursor.moveToFirst()) {
                val hasPhone = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER))
                if (hasPhone > 0) {
                    val id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                    val pCur = context.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", arrayOf(id), null)
                    if (pCur != null && pCur.moveToFirst()) {
                        phoneNum = pCur.getString(pCur.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                        pCur.close()
                    }
                }
                cursor.close()
            }

            if (phoneNum != null) {
                val callIntent = Intent(Intent.ACTION_CALL)
                callIntent.data = Uri.parse("tel:$phoneNum")
                callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(callIntent)
                "> [SYSTEM]: Initiating cellular override. Calling $contactName..."
            } else {
                "> [!] TELEPHONY ERROR: Contact '$contactName' not found or has no number."
            }
        } catch (e: Exception) {
            "> [!] PERMISSION ERROR: Call failed. Grant Contacts and Phone permissions."
        }
    }
}
