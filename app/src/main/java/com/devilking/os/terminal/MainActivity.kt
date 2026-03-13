package com.devilking.os.terminal

import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.devilking.os.execution.CommandExecutor
import com.devilking.os.ai.LocalAICore
import com.devilking.os.ai.AIContext
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var tvTerminalOutput: TextView
    private lateinit var etCommandInput: EditText
    private lateinit var scrollView: ScrollView
    
    private lateinit var commandExecutor: CommandExecutor
    private lateinit var aiCore: LocalAICore
    private lateinit var aiContext: AIContext

    // The VIP File Picker
    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            printToTerminal("> [SYSTEM]: File selected. Commencing secure vault transfer...")
            thread {
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val response = aiCore.injectFromStream(inputStream)
                        runOnUiThread {
                            printToTerminal(response)
                            etCommandInput.isEnabled = true
                            etCommandInput.requestFocus()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        printToTerminal("> [!] ERROR: ${e.message}")
                        etCommandInput.isEnabled = true
                    }
                }
            }
        } else {
            printToTerminal("> [!] ACTION ABORTED: No file selected.")
            etCommandInput.isEnabled = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        tvTerminalOutput = findViewById(R.id.tv_terminal_output)
        etCommandInput = findViewById(R.id.et_command_input)
        scrollView = findViewById(R.id.scroll_view)
        
        commandExecutor = CommandExecutor(this)
        aiCore = LocalAICore(this) 
        aiContext = AIContext()

        setupInputListener()
    }

    private fun setupInputListener() {
        etCommandInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || 
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                
                val rawInput = etCommandInput.text.toString().trim()
                if (rawInput.isNotEmpty()) {
                    etCommandInput.isEnabled = false 
                    processCommand(rawInput)
                }
                true
            } else false
        }
    }

    private fun processCommand(input: String) {
        printToTerminal("root@devilking:~# $input")
        etCommandInput.text.clear()

        val parts = input.split(Regex("\\s+"), 2)
        val command = parts[0].lowercase()
        val target = if (parts.size > 1) parts[1] else ""

        when (command) {
            "open" -> {
                if (target.isNotEmpty()) printToTerminal(commandExecutor.launchApp(target))
                else printToTerminal("> [!] SYNTAX ERROR: 'open' requires a target.")
                etCommandInput.isEnabled = true
            }
            "help" -> {
                printToTerminal("> SYSTEM DIRECTORY:\n  - open [app]\n  - core (Check AI Status)\n  - inject core (Load AI)\n  - clear (Wipe screen)")
                etCommandInput.isEnabled = true
            }
            "clear" -> {
                tvTerminalOutput.text = ""
                etCommandInput.isEnabled = true
            }
            "core" -> {
                printToTerminal(aiCore.checkCoreStatus())
                etCommandInput.isEnabled = true
            }
            "inject" -> {
                if (target == "core") {
                    printToTerminal("> [DEVILKING AI]: Requesting secure file selection...")
                    filePickerLauncher.launch(arrayOf("*/*")) // Opens Android File Manager
                } else {
                    printToTerminal("> [!] SYNTAX ERROR: Did you mean 'inject core'?")
                    etCommandInput.isEnabled = true
                }
            }
            else -> processAICommand(input)
        }
    }

    private fun processAICommand(input: String) {
        printToTerminal("> [DEVILKING AI]: Processing intent...")
        thread {
            val response = aiCore.generateResponse(input) 
            runOnUiThread {
                printToTerminal(response)
                etCommandInput.isEnabled = true 
                etCommandInput.requestFocus()
            }
        }
    }

    private fun printToTerminal(text: String) {
        tvTerminalOutput.append(text + "\n")
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }
}
