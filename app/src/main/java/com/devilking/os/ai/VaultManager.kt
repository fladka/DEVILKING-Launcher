package com.devilking.os.ai

import android.os.Environment
import java.io.File

class VaultManager {
    // Target directory: Internal Storage -> Documents -> DEVILKING_VAULT
    private val vaultDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "DEVILKING_VAULT")
    private var vaultMemory: String = ""

    init {
        if (!vaultDir.exists()) {
            vaultDir.mkdirs()
        }
        loadVault()
    }

    fun loadVault(): String {
        if (!vaultDir.exists()) return "> [!] VAULT ERROR: Directory missing."
        
        val files = vaultDir.listFiles { _, name -> name.endsWith(".txt") }
        if (files == null || files.isEmpty()) {
            vaultMemory = ""
            return "> [VAULT]: Empty. Drop .txt files into Documents/DEVILKING_VAULT"
        }

        val memoryBuilder = java.lang.StringBuilder()
        for (file in files) {
            memoryBuilder.append(file.readText()).append("\n")
        }
        vaultMemory = memoryBuilder.toString()
        return "> [VAULT]: Loaded ${files.size} data files into active memory."
    }

    // This injects the file data into the AI's prompt invisibly
    fun injectContext(): String {
        if (vaultMemory.isBlank()) return ""
        return "LOCAL KNOWLEDGE VAULT DATA:\n$vaultMemory\n"
    }
}
