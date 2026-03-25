package com.devilking.os.ai

import android.content.Context
import org.vosk.Model
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

class AcousticShield(private val context: Context) {
    
    var isArmed = false
        private set
    
    var voskModel: Model? = null
        private set

    private val modelDir = File(context.filesDir, "vosk_model")

    fun checkShieldStatus(): String {
        return if (isArmed || (modelDir.exists() && modelDir.list()?.isNotEmpty() == true)) {
            "> ACOUSTIC SHIELD: ARMED.\n> Status: Vosk Offline Engine Ready."
        } else {
            "> [!] ACOUSTIC SHIELD: OFFLINE. Run 'inject vosk' to load model."
        }
    }

    // Unzips the user's file and injects it into the OS vault
    fun injectModelZip(inputStream: InputStream): String {
        return try {
            if (modelDir.exists()) {
                modelDir.deleteRecursively()
            }
            modelDir.mkdirs()

            // 1. Unzip the archive
            val zis = ZipInputStream(inputStream)
            var entry = zis.nextEntry
            while (entry != null) {
                val newFile = File(modelDir, entry.name)
                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    val fos = FileOutputStream(newFile)
                    val buffer = ByteArray(8192)
                    var len: Int
                    while (zis.read(buffer).also { len = it } > 0) {
                        fos.write(buffer, 0, len)
                    }
                    fos.close()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
            zis.close()
            inputStream.close()

            // 2. Find the root (Sometimes users zip the folder, sometimes the files)
            val actualModelDir = findModelRoot(modelDir) ?: modelDir

            // 3. Load into RAM
            voskModel = Model(actualModelDir.absolutePath)
            isArmed = true
            "> [SYSTEM]: Acoustic Shield stabilized. Vosk Model loaded successfully."
        } catch (e: Exception) {
            "> [!] SHIELD INJECTION ERROR: ${e.message}"
        }
    }

    // Recursively searches the unzipped folder to find the actual neural files
    private fun findModelRoot(dir: File): File? {
        if (File(dir, "am").exists() && File(dir, "conf").exists()) return dir
        val subdirs = dir.listFiles { file -> file.isDirectory }
        if (subdirs != null) {
            for (sub in subdirs) {
                val root = findModelRoot(sub)
                if (root != null) return root
            }
        }
        return null
    }
    
    // Auto-loads the model on app startup if it was already injected previously
    fun autoLoadExistingModel(): Boolean {
        return try {
            if (modelDir.exists() && !isArmed) {
                val actualModelDir = findModelRoot(modelDir) ?: modelDir
                if (File(actualModelDir, "am").exists()) {
                    voskModel = Model(actualModelDir.absolutePath)
                    isArmed = true
                    return true
                }
            }
            false
        } catch (e: Exception) { false }
    }
}
