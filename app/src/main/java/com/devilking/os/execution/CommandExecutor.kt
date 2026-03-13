package com.devilking.os.execution

import android.content.Context
import android.content.pm.PackageManager

class CommandExecutor(private val context: Context) {

    fun launchApp(appName: String): String {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        
        for (appInfo in packages) {
            val name = pm.getApplicationLabel(appInfo).toString().lowercase()
            
            if (name == appName.lowercase()) {
                val launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName)
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                    return "> EXECUTION: Launching '$appName'..."
                }
            }
        }
        return "> [!] ERROR: Target '$appName' not found in system registry."
    }
}
