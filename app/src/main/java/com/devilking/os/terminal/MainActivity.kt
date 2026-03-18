package com.devilking.os.terminal

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.devilking.os.ai.LocalAICore
import kotlinx.coroutines.*
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var terminalRecyclerView: RecyclerView
    private lateinit var commandInput: EditText
    private lateinit var micButton: Button
    private lateinit var adapter: TerminalAdapter
    private val commandHistory = mutableListOf<String>()

    // THE AI CORE LINK
    private lateinit var aiCore: LocalAICore
    private val uiScope = CoroutineScope(Dispatchers.Main + Job())
    
    // VOICE RECOGNIZER
    private lateinit var speechRecognizer: SpeechRecognizer

    // THE HARDWARE HIJACK RECEIVER (Volume Button Integration)
    private val hardwareHijackReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            checkAudioPermissionAndListen()
        }
    }

    // THE FILE PICKER FOR CORE INJECTION
    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            printToTerminal("> [SYSTEM]: Neural Core selected. Injecting...")
            uiScope.launch(Dispatchers.IO) {
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val result = aiCore.injectFromStream(inputStream)
                        withContext(Dispatchers.Main) { printToTerminal(result) }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { printToTerminal("> [!] INJECTION ERROR: ${e.message}") }
                }
            }
        } else {
            printToTerminal("> [!] INJECTION ABORTED: No file selected.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        aiCore = LocalAICore(this)
        
        setupUI()
        setupVoiceAgent()
        setupKeyboardTraps()

        // REGISTER THE HARDWARE HIJACK LISTENER
        val filter = android.content.IntentFilter("com.devilking.os.WAKE_WORD_TRIGGERED")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(hardwareHijackReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(hardwareHijackReceiver, filter)
        }

        commandHistory.add("DEVILKING OS [Version 1.0.0]")
        commandHistory.add("> Audio Matrix: Offline. Tap [MIC] or HOLD VOL DOWN to engage.")
        commandHistory.add(aiCore.checkCoreStatus())
        adapter.notifyDataSetChanged()

        micButton.setOnClickListener {
            checkAudioPermissionAndListen()
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
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            setBackgroundColor(android.graphics.Color.parseColor("#0a0e27")) 
        }

        adapter = TerminalAdapter(commandHistory)
        terminalRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        terminalRecyclerView.adapter = adapter

        val inputContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(android.graphics.Color.parseColor("#0f1419"))
            setPadding(16, 12, 16, 12)
        }

        commandInput = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            hint = "Enter command..."
            setHintTextColor(android.graphics.Color.parseColor("#475569"))
            setTextColor(android.graphics.Color.parseColor("#00FF41"))
            textSize = 14f
            typeface = android.graphics.Typeface.MONOSPACE
            setBackgroundColor(android.graphics.Color.parseColor("#1F2937"))
            setPadding(24, 24, 24, 24)
            
            // LOCK TO SINGLE LINE
            maxLines = 1
            isSingleLine = true
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_ACTION_SEND
        }

        micButton = Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply { setMargins(16, 0, 0, 0) }
            text = "[ MIC ]"
            setTextColor(android.graphics.Color.parseColor("#0a0e27"))
            setBackgroundColor(android.graphics.Color.parseColor("#00FF41"))
            typeface = android.graphics.Typeface.MONOSPACE
        }

        inputContainer.addView(commandInput)
        inputContainer.addView(micButton)
        mainContainer.addView(terminalRecyclerView)
        mainContainer.addView(inputContainer)

        setContentView(mainContainer)
    }

    private fun setupKeyboardTraps() {
        // THE BULLETPROOF ENTER KEY TRAP
        commandInput.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                val input = commandInput.text.toString().trim()
                if (input.isNotEmpty()) {
                    processInput(input)
                }
                return@setOnKeyListener true // Consume the Enter key instantly
            }
            false
        }

        // CATCHES VIRTUAL KEYBOARD "SEND" BUTTON
        commandInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                val input = commandInput.text.toString().trim()
                if (input.isNotEmpty()) {
                    processInput(input)
                }
                true
            } else {
                false
            }
        }
    }

    private fun setupVoiceAgent() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                micButton.text = "[ LISTENING ]"
                micButton.setBackgroundColor(android.graphics.Color.RED)
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                resetMicButton()
            }
            override fun onError(error: Int) {
                resetMicButton()
                printToTerminal("> [!] AUDIO ERROR: Signal lost or no speech detected.")
            }
            override fun onResults(results: Bundle?) {
                resetMicButton()
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0]
                    processInput(spokenText)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun checkAudioPermissionAndListen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        } else {
            startListening()
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        speechRecognizer.startListening(intent)
    }

    private fun resetMicButton() {
        micButton.text = "[ MIC ]"
        micButton.setBackgroundColor(android.graphics.Color.parseColor("#00FF41"))
    }

    private fun processInput(input: String) {
        val cleanInput = input.trim()
        printToTerminal("root@devilking:~$ $cleanInput")
        commandInput.text.clear()

        // INTERCEPT INJECT CORE COMMAND
        if (cleanInput.lowercase() == "inject core") {
            filePickerLauncher.launch("*/*")
            return
        }

        uiScope.launch(Dispatchers.IO) {
            val response = aiCore.generateResponse(cleanInput)
            withContext(Dispatchers.Main) {
                printToTerminal(response)
            }
        }
    }

    private fun printToTerminal(text: String) {
        commandHistory.add(text)
        adapter.notifyDataSetChanged()
        terminalRecyclerView.scrollToPosition(commandHistory.size - 1)
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
        unregisterReceiver(hardwareHijackReceiver)
        uiScope.cancel()
    }
}
