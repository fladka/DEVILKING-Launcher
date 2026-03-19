package com.devilking.os.system

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.provider.ContactsContract
import com.devilking.os.automation.DevilkingService

class SystemExecutor(private val context: Context) {

    private var isFlashlightOn = false

    fun executeCommand(commandString: String): String {
        val cmd = commandString.removePrefix("[CMD: ").removeSuffix("]").trim()

        // 1. AEGIS FIREWALL WHITELIST (UPGRADED)
        val isSafe = cmd.startsWith("flashlight") ||
                     cmd.startsWith("open ") ||
                     cmd.startsWith("call ") ||
                     cmd.startsWith("scroll") ||
                     cmd.startsWith("2x ") || 
                     cmd.startsWith("snipe ") ||
                     cmd.startsWith("type ") ||
                     cmd.startsWith("macro ") ||
                     cmd == "settings" ||
                     cmd == "scan screen" ||
                     cmd == "clear" ||
                     cmd == "home" ||
                     cmd == "recents" ||
                     cmd == "lock" ||
                     cmd == "volume up" ||
                     cmd == "volume down"

        if (!isSafe) {
            return "> [!] AEGIS FIREWALL: Unauthorized core command blocked ($cmd)."
        }

        // TIER 0: SETTINGS MATRIX LAUNCHER
        if (cmd == "settings") {
            val intent = Intent(context, com.devilking.os.terminal.SettingsActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return "> [SYSTEM]: Initializing Neural Settings Matrix..."
        }

        // TIER 1: HARDWARE CONTROLS
        if (cmd == "flashlight") return toggleFlashlight()
        if (cmd.startsWith("call ")) return initiateCall(cmd.removePrefix("call ").trim())
        if (cmd.startsWith("open ")) return launchApp(cmd.removePrefix("open ").trim())

        // NEW: VOLUME CONTROLS
        if (cmd == "volume up" || cmd == "volume down") {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (cmd == "volume up") {
                audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                return "> [SYSTEM]: Volume Increased."
            } else {
                audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                return "> [SYSTEM]: Volume Decreased."
            }
        }

        // TERMINAL CONTROL
        if (cmd == "clear") {
            return "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n> [SYSTEM]: Terminal wiped clean."
        }

        // TIER 4/6/7: GOD MODE RELAYS (Accessibility)
        val godMode = DevilkingService.instance
            ?: return "> [!] GOD MODE OFFLINE: Accessibility Service not bound."

        // SYSTEM NAVIGATION
        if (cmd == "home") {
            godMode.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            return "> [SYSTEM]: Returning to Home Screen."
        }
        if (cmd == "recents") {
            godMode.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
            return "> [SYSTEM]: Opening Recent Apps."
        }
        if (cmd == "lock") {
            godMode.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
            return "> [SYSTEM]: Device Locked."
        }

        // UI MATRIX DUMP
        if (cmd == "scan screen") {
            return godMode.dumpScreenMatrix()
        }

        // PHANTOM GESTURES
        if (cmd == "scroll" || cmd == "scroll up") { godMode.performSwipeUp(); return "> [SYSTEM]: Swiping Up." }
        if (cmd == "scroll down") { godMode.performSwipeDown(); return "> [SYSTEM]: Swiping Down." }
        if (cmd == "2x scroll" || cmd == "2x scroll up") { godMode.performDoubleSwipeUp(); return "> [SYSTEM]: Double Swiping Up." }
        if (cmd == "2x scroll down" || cmd == "2xscroll down") { godMode.performDoubleSwipeDown(); return "> [SYSTEM]: Double Swiping Down." }

        // MACRO ENGINE
        if (cmd.startsWith("macro whatsapp")) {
            val payload = cmd.removePrefix("macro whatsapp").trim().removePrefix(">").trim()
            val parts = payload.split(">").map { it.trim() }
            if (parts.size >= 2) {
                godMode.executeWhatsAppMacro(parts[0], parts[1])
                return "> [SYSTEM]: Executing WhatsApp Macro for ${parts[0]}..."
            }
            return "> [!] MACRO SYNTAX ERROR."
        }

        // LETHAL SNIPER
        if (cmd.startsWith("snipe ")) {
            val target = cmd.removePrefix("snipe ").trim().replace("[", "").replace("]", "").trim()
            val success = godMode.executeSniperStrike(target)
            return if (success) "> [SYSTEM]: Sniper Strike confirmed on '$target'." else "> [!] SNIPER ERROR: Target '$target' not found."
        }

        // AUTO-FOCUS INJECTOR
        if (cmd.startsWith("type ")) {
            val payload = cmd.removePrefix("type ").trim()
            val success = godMode.executeGhostType(payload)
            return if (success) "> [SYSTEM]: Ghost Typed '$payload'." else "> [!] TYPE ERROR: No active input field found."
        }

        return "> [!] EXECUTOR ERROR."
    }

    private fun toggleFlashlight(): String {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            isFlashlightOn = !isFlashlightOn
            cameraManager.setTorchMode(cameraManager.cameraIdList[0], isFlashlightOn)
            if (isFlashlightOn) "> [SYSTEM]: Flashlight Engaged." else "> [SYSTEM]: Flashlight Disabled."
        } catch (e: Exception) { "> [!] HARDWARE ERROR: Flashlight module unavailable." }
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
        return "> [!] ERROR: Application '$appName' not found."
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
                "> [SYSTEM]: Calling $contactName..."
            } else { "> [!] TELEPHONY ERROR: Contact not found." }
        } catch (e: Exception) { "> [!] PERMISSION ERROR: Call failed." }
    }
}
