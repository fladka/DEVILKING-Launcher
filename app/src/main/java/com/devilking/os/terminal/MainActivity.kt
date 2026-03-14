package com.devilking.os.terminal

import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
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
        
        // Initial screenshot unblocker
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)

        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        tvTerminalOutput = findViewById(R.id.tv_terminal_output)
        etCommandInput = findViewById(R.id.et_command_input)
        scrollView = findViewById(R.id.scroll_view)
        
        commandExecutor = CommandExecutor(this)
        aiCore = LocalAICore(this) 
        aiContext = AIContext()

        if (savedInstanceState != null) {
            tvTerminalOutput.text = savedInstanceState.getString("terminal_text")
        }

        setupInputListener()
    }

    // FIX 1: AGGRESSIVE SCREENSHOT OVERRIDE
    // Funtouch OS tries to re-lock the screen when you switch apps. This breaks the lock every time you return.
    override fun onResume() {
        super.onResume()
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("terminal_text", tvTerminalOutput.text.toString())
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

    // FIX 2: THE MATH INTERCEPTOR
    private fun evaluateMath(input: String): String? {
        try {
            // Clean up spaces, equals signs, and question marks
            val clean = input.replace("\\s".toRegex(), "").replace("=", "").replace("?", "")
            // Regex to catch basic math: Number Operator Number
            val regex = Regex("^(-?\\d+\\.?\\d*)([+\\-*/])(-?\\d+\\.?\\d*)$")
            val match = regex.find(clean)
            
            if (match != null) {
                val (num1Str, op, num2Str) = match.destructured
                val num1 = num1Str.toDouble()
                val num2 = num2Str.toDouble()
                val result = when (op) {
                    "+" -> num1 + num2
                    "-" -> num1 - num2
                    "*" -> num1 * num2
                    "/" -> if (num2 != 0.0) num1 / num2 else return "> [!] MATH ERROR: Division by zero."
                    else -> return null
                }
                // Format nicely (remove .0 if it's a whole number)
                val resultStr = if (result % 1.0 == 0.0) result.toLong().toString() else result.toString()
                return "> [SYSTEM CALC]: $clean = $resultStr"
            }
        } catch (e: Exception) { return null }
        return null
    }

    private fun processCommand(input: String) {
        printToTerminal("root@devilking:~# $input")
        etCommandInput.text.clear()

        // Check if it's just a math problem before waking up the AI
        val mathResult = evaluateMath(input)
        if (mathResult != null) {
            printToTerminal(mathResult)
            etCommandInput.isEnabled = true
            return
        }

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
                    filePickerLauncher.launch(arrayOf("*/*")) 
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
