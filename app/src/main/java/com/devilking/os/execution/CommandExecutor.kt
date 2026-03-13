package com.devilking.os.execution

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

class CommandExecutor(private val context: Context) {

    fun launchApp(appName: String): String {
        val pm = context.packageManager
        
        // Create an intent that looks for apps with a launcher icon
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        
        // Safely query only launchable apps (Bypasses Funtouch OS security block)
        val apps = pm.queryIntentActivities(mainIntent, 0)
        
        for (resolveInfo in apps) {
            val name = resolveInfo.loadLabel(pm).toString().lowercase()
            
            if (name == appName.lowercase()) {
                val packageName = resolveInfo.activityInfo.packageName
                val launchIntent = pm.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                    return "> EXECUTION: Launching '$appName'..."
                }
            }
        }
        return "> [!] ERROR: Target '$appName' not found in system registry."
    }
}
