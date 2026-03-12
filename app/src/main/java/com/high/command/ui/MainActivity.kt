package com.high.command.ui

import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    private lateinit var terminalRecyclerView: RecyclerView
    private lateinit var commandInput: EditText
    private lateinit var adapter: TerminalAdapter
    private val commandHistory = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create UI programmatically (avoids R class dependency issues)
        setupUI()

        // Welcome Message
        commandHistory.add("DEVILKING OS [Version 1.0.0]")
        commandHistory.add("Type 'help' for a list of commands.")
        adapter.notifyDataSetChanged()

        commandInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND || 
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                val input = commandInput.text.toString().trim()
                if (input.isNotEmpty()) {
                    executeCommand(input)
                }
                true
            } else {
                false
            }
        }
    }

    private fun setupUI() {
        val mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        terminalRecyclerView = RecyclerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f 
            )
            setBackgroundColor(android.graphics.Color.parseColor("#0a0e27")) 
        }

        adapter = TerminalAdapter(commandHistory)
        terminalRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        terminalRecyclerView.adapter = adapter

        val inputContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(android.graphics.Color.parseColor("#0f1419"))
        }

        commandInput = EditText(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 12, 16, 12)
            }
            hint = "Enter command..."
            setHintTextColor(android.graphics.Color.parseColor("#475569"))
            setTextColor(android.graphics.Color.parseColor("#00FF41"))
            textSize = 14f
            typeface = android.graphics.Typeface.MONOSPACE
            setBackgroundColor(android.graphics.Color.parseColor("#1F2937"))
            setPadding(12, 8, 12, 8)
        }

        inputContainer.addView(commandInput)
        mainContainer.addView(terminalRecyclerView)
        mainContainer.addView(inputContainer)

        setContentView(mainContainer)
    }

    private fun executeCommand(input: String) {
        commandHistory.add("root@devilking:~$ $input")
        
        val output = when (input.lowercase()) {
            "clear" -> {
                commandHistory.clear()
                ""
            }
            "help" -> "Available commands: clear, help, whoami, echo [text]"
            "whoami" -> "devil_admin"
            else -> {
                if (input.startsWith("echo ")) {
                    input.substring(5)
                } else {
                    "bash: $input: command not found"
                }
            }
        }
        
        if (output.isNotEmpty()) {
            commandHistory.add(output)
        }
        
        adapter.notifyDataSetChanged()
        terminalRecyclerView.scrollToPosition(commandHistory.size - 1)
        commandInput.text.clear()
    }
}
