package com.devilking.os.terminal

import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.devilking.os.execution.CommandExecutor

class MainActivity : AppCompatActivity() {

    private lateinit var tvTerminalOutput: TextView
    private lateinit var etCommandInput: EditText
    private lateinit var scrollView: ScrollView
    private lateinit var commandExecutor: CommandExecutor

    private var systemName = "devilking"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.hide()

        tvTerminalOutput = findViewById(R.id.tv_terminal_output)
        etCommandInput = findViewById(R.id.et_command_input)
        scrollView = findViewById(R.id.scroll_view)
        
        // Initialize the Muscle
        commandExecutor = CommandExecutor(this)

        setupInputListener()
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
            } else {
                false
            }
        }
    }

    private fun processCommand(input: String) {
        printToTerminal("root@$systemName:~# $input")
        etCommandInput.text.clear()

        // Split the input into Command and Target (e.g., "open" and "youtube")
        val parts = input.split(Regex("\\s+"), 2)
        val command = parts[0].lowercase()
        val target = if (parts.size > 1) parts[1] else ""

        when (command) {
            "open" -> {
                if (target.isNotEmpty()) {
                    printToTerminal(commandExecutor.launchApp(target))
                } else {
                    printToTerminal("> [!] SYNTAX ERROR: 'open' requires a target (e.g., 'open camera')")
                }
            }
            "help" -> printToTerminal("> SYSTEM DIRECTORY:\n  - open [app]\n  - stealth (RAM Purge)\n  - clear (Wipe screen)")
            "clear" -> tvTerminalOutput.text = ""
            "stealth" -> printToTerminal("> Initiating Absolute Zero... RAM Purged.")
            else -> printToTerminal("> [!] UNKNOWN COMMAND: '$command'.")
        }
    }

    private fun printToTerminal(text: String) {
        tvTerminalOutput.append(text + "\n")
        scrollView.post {
            scrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }
}
