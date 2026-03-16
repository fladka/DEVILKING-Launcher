package com.devilking.os.terminal

import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
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
                            etCommandInput.requestFocus()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread { printToTerminal("> [!] ERROR: ${e.message}") }
                }
            }
        } else {
            printToTerminal("> [!] ACTION ABORTED: No file selected.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                    processCommand(rawInput)
                }
                true
            } else false
        }
    }

    private fun evaluateMath(input: String): String? {
        try {
            val clean = input.replace("\\s".toRegex(), "").replace("=", "").replace("?", "")
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
                val resultStr = if (result % 1.0 == 0.0) result.toLong().toString() else result.toString()
                return "> [SYSTEM CALC]: $clean = $resultStr"
            }
        } catch (e: Exception) { return null }
        return null
    }

    private fun processCommand(input: String) {
        // THE FIX: Removed the backslash so it prints your actual command
        printToTerminal("root@devilking:~# $input")
        etCommandInput.text.clear()

        val mathResult = evaluateMath(input)
        if (mathResult != null) {
            printToTerminal(mathResult)
            return
        }

        val parts = input.split(Regex("\\s+"), 2)
        val command = parts[0].lowercase()
        val target = if (parts.size > 1) parts[1] else ""

        when (command) {
            "open" -> {
                if (target.isNotEmpty()) printToTerminal(commandExecutor.launchApp(target))
                else printToTerminal("> [!] SYNTAX ERROR: 'open' requires a target.")
            }
            "help" -> printToTerminal("> SYSTEM DIRECTORY:\n  - open [app]\n  - core (Check AI Status)\n  - inject core (Load AI)\n  - clear (Wipe screen)")
            "clear" -> tvTerminalOutput.text = ""
            "core" -> printToTerminal(aiCore.checkCoreStatus())
            "inject" -> {
                if (target == "core") {
                    printToTerminal("> [DEVILKING AI]: Requesting secure file selection...")
                    filePickerLauncher.launch(arrayOf("*/*")) 
                } else printToTerminal("> [!] SYNTAX ERROR: Did you mean 'inject core'?")
            }
            else -> processAICommand(input)
        }
    }

    private fun processAICommand(input: String) {
        printToTerminal("> [DEVILKING AI]: Processing intent...")
        
        val startTime = System.currentTimeMillis()
        
        thread {
            val response = aiCore.generateResponse(input) 
            val timeTaken = System.currentTimeMillis() - startTime
            
            runOnUiThread {
                printToTerminal(response)
                
                if (timeTaken > 1500) {
                    Toast.makeText(this@MainActivity, "DEVILKING AI: Thought Complete", Toast.LENGTH_SHORT).show()
                }
                
                etCommandInput.requestFocus()
            }
        }
    }

    private fun printToTerminal(text: String) {
        tvTerminalOutput.append(text + "\n")
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }
}
