package com.devilking.os.automation

import android.content.Context
import android.content.Intent
import android.content.ComponentName
import org.json.JSONObject
import java.io.File

class IntentVault(private val context: Context) {

    private val vaultFile = File(context.filesDir, "intent_vault.json")
    private val matrix = mutableMapOf<String, String>()

    init {
        loadVault()
    }

    // --- 1. LOAD THE MATRIX FROM RAM/STORAGE ---
    private fun loadVault() {
        if (!vaultFile.exists()) {
            // Seed the vault with a few basic Android settings if it's empty
            matrix["wifi"] = "com.android.settings/com.android.settings.Settings\$WifiSettingsActivity"
            matrix["bluetooth"] = "com.android.settings/com.android.settings.Settings\$BluetoothSettingsActivity"
            matrix["display"] = "com.android.settings/com.android.settings.Settings\$DisplaySettingsActivity"
            saveVault()
            return
        }
        try {
            val json = JSONObject(vaultFile.readText())
            json.keys().forEach { key ->
                matrix[key] = json.getString(key)
            }
        } catch (e: Exception) {
            // File corrupted, start fresh
            vaultFile.delete()
        }
    }

    // --- 2. SAVE NEW ROUTES TO STORAGE ---
    private fun saveVault() {
        val json = JSONObject()
        matrix.forEach { (key, value) -> json.put(key, value) }
        vaultFile.writeText(json.toString())
    }

    // --- 3. LEARN A NEW ROUTE (Used after Wiretapping) ---
    fun lockRoute(commandName: String, packageAndClass: String): String {
        val cleanName = commandName.lowercase().trim()
        matrix[cleanName] = packageAndClass.trim()
        saveVault()
        return "> [VAULT]: Route '$cleanName' locked successfully."
    }

    // --- 4. EXECUTE TELEPORTATION ---
    fun teleport(target: String): String {
        val cleanTarget = target.lowercase().trim()
        val route = matrix[cleanTarget] 
        
        if (route == null) {
            return "> [!] TELEPORT FAILED: Destination '$cleanTarget' not found in Vault. Use 'wiretap' to learn it."
        }

        return try {
            val parts = route.split("|").map { it.trim() }
            val pkg = parts[0]
            val cls = if (parts.size > 1) parts[1] else ""

            val intent = Intent().apply {
                if (cls.isNotEmpty()) {
                    component = ComponentName(pkg, cls)
                } else {
                    // Fallback to just launching the app if no specific class is provided
                    return context.packageManager.getLaunchIntentForPackage(pkg)?.let {
                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        context.startActivity(it)
                        "> [TELEPORT]: Launching $cleanTarget..."
                    } ?: "> [!] TELEPORT FAILED: Package $pkg not found."
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            
            context.startActivity(intent)
            "> [TELEPORT]: Jump to '$cleanTarget' successful."
        } catch (e: Exception) {
            "> [!] TELEPORT ERROR: Android blocked the jump. The Activity might be private or restricted. (${e.message})"
        }
    }

    // --- 5. LIST ALL ROUTES ---
    fun dumpVault(): String {
        if (matrix.isEmpty()) return "> [VAULT]: Empty."
        val sb = java.lang.StringBuilder("> --- INTENT VAULT MATRIX ---\n")
        matrix.forEach { (name, route) ->
            sb.append("> $name -> $route\n")
        }
        return sb.toString()
    }
}
