package com.devilking.os.terminal

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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
import com.devilking.os.ai.AcousticShield
import kotlinx.coroutines.*
import org.json.JSONObject
import org.vosk.Recognizer
import org.vosk.android.SpeechService
import org.vosk.android.RecognitionListener

class MainActivity : AppCompatActivity() {
    private lateinit var terminalRecyclerView: RecyclerView
    private lateinit var commandInput: EditText
    private lateinit var micButton: Button
    private lateinit var adapter: TerminalAdapter
    private val commandHistory = mutableListOf<String>()

    // THE NEURAL & ACOUSTIC CORES
    private lateinit var aiCore: LocalAICore
    private lateinit var acousticShield: AcousticShield
    private val uiScope = CoroutineScope(Dispatchers.Main + Job())
    
    // VOSK OFFLINE ENGINE
    private var speechService: SpeechService? = null

    // THE HARDWARE HIJACK RECEIVER (Walkie-Talkie Mode)
    private val hardwareHijackReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            when (intent?.action) {
                "com.devilking.os.MIC_START" -> checkAudioPermissionAndStart()
                "com.devilking.os.MIC_STOP" -> stopVoskListening()
            }
        }
    }

    // CORE INJECTOR (brain.gguf)
    private val corePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
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
                    withContext(Dispatchers.Main) { printToTerminal("> [!] CORE INJECTION ERROR: ${e.message}") }
                }
            }
        } else {
            printToTerminal("> [!] INJECTION ABORTED: No core selected.")
        }
    }

    // SHIELD INJECTOR (model.zip)
    private val voskPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            printToTerminal("> [SYSTEM]: Acoustic Model selected. Extracting...")
            uiScope.launch(Dispatchers.IO) {
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val result = acousticShield.injectModelZip(inputStream)
                        withContext(Dispatchers.Main) { 
                            printToTerminal(result)
                            initVoskService() 
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { printToTerminal("> [!] SHIELD INJECTION ERROR: ${e.message}") }
                }
            }
        } else {
            printToTerminal("> [!] INJECTION ABORTED: No model selected.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        aiCore = LocalAICore(this)
        acousticShield = AcousticShield(this)
        
        setupUI()
        setupKeyboardTraps()

        // REGISTER THE HARDWARE HIJACK LISTENER (Updated for Dual Intents)
        val filter = android.content.IntentFilter().apply {
            addAction("com.devilking.os.MIC_START")
            addAction("com.devilking.os.MIC_STOP")
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(hardwareHijackReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(hardwareHijackReceiver, filter)
        }

        commandHistory.add("DEVILKING OS [Version 1.0.0]")
        commandHistory.add("> Hardware Hijack: Walkie-Talkie Mode Armed.")
        commandHistory.add(aiCore.checkCoreStatus())
        
        // Auto-load Vosk if previously injected
        if (acousticShield.autoLoadExistingModel()) {
            initVoskService()
        }
        commandHistory.add(acousticShield.checkShieldStatus())
        adapter.notifyDataSetChanged()

        micButton.setOnClickListener {
            checkAudioPermissionAndListen() // Preserved for manual UI toggling
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
        commandInput.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                val input = commandInput.text.toString().trim()
                if (input.isNotEmpty()) processInput(input)
                return@setOnKeyListener true
            }
            false
        }

        commandInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                val input = commandInput.text.toString().trim()
                if (input.isNotEmpty()) processInput(input)
                true
            } else false
        }
    }

    private fun initVoskService() {
        if (acousticShield.isArmed && acousticShield.voskModel != null && speechService == null) {
            try {
                // Vosk strictly requires 16000.0f sample rate
                val recognizer = Recognizer(acousticShield.voskModel, 16000.0f)
                speechService = SpeechService(recognizer, 16000.0f)
            } catch (e: Exception) {
                printToTerminal("> [!] VOSK ERROR: Failed to init audio engine.")
            }
        }
    }

    // --- ON-SCREEN UI MIC CONTROLS ---
    private fun checkAudioPermissionAndListen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        } else {
            toggleListening()
        }
    }

    private fun toggleListening() {
        if (!acousticShield.isArmed) {
            printToTerminal("> [!] SHIELD OFFLINE: Run 'inject vosk' to load the model.")
            return
        }

        if (speechService == null) initVoskService()

        if (micButton.text == "[ LISTENING ]") {
            speechService?.stop()
            resetMicButton()
        } else {
            micButton.text = "[ LISTENING ]"
            micButton.setBackgroundColor(android.graphics.Color.RED)
            speechService?.startListening(voskListener)
        }
    }

    // --- HARDWARE WALKIE-TALKIE CONTROLS ---
    private fun checkAudioPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        } else {
            startVoskListening()
        }
    }

    private fun startVoskListening() {
        if (!acousticShield.isArmed) {
            printToTerminal("> [!] SHIELD OFFLINE: Run 'inject vosk' to load the model.")
            return
        }
        if (speechService == null) initVoskService()

        micButton.text = "[ LISTENING ]"
        micButton.setBackgroundColor(android.graphics.Color.RED)
        speechService?.startListening(voskListener)
    }

    private fun stopVoskListening() {
        // Calling stop() forces Vosk to instantly process the final audio frame
        speechService?.stop()
        resetMicButton()
    }

    // --- THE VOSK CALLBACK LISTENER ---
    private val voskListener = object : RecognitionListener {
        override fun onPartialResult(hypothesis: String?) {}
        
        override fun onResult(hypothesis: String?) {
            // Vosk detects a pause in speech and triggers this. We instantly kill the mic.
            speechService?.stop()
            resetMicButton()
            handleVoskJSON(hypothesis)
        }
        
        override fun onFinalResult(hypothesis: String?) {
            resetMicButton()
            handleVoskJSON(hypothesis)
        }
        
        override fun onError(exception: Exception?) {
            resetMicButton()
            printToTerminal("> [!] VOSK ERROR: ${exception?.message}")
        }
        
        override fun onTimeout() {
            resetMicButton()
        }
    }

    private fun handleVoskJSON(jsonStr: String?) {
        if (jsonStr == null) return
        try {
            val jsonObject = JSONObject(jsonStr)
            val text = jsonObject.optString("text", "")
            if (text.isNotBlank()) {
                processInput(text)
            }
        } catch (e: Exception) {}
    }

    private fun resetMicButton() {
        micButton.text = "[ MIC ]"
        micButton.setBackgroundColor(android.graphics.Color.parseColor("#00FF41"))
    }

    private fun processInput(input: String) {
        val cleanInput = input.trim()
        printToTerminal("root@devilking:~$ $cleanInput")
        commandInput.text.clear()

        // INTERCEPT FILE INJECTIONS
        if (cleanInput.lowercase() == "inject core") {
            corePickerLauncher.launch("*/*")
            return
        }
        if (cleanInput.lowercase() == "inject vosk") {
            voskPickerLauncher.launch("application/zip")
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
        speechService?.stop()
        speechService?.shutdown()
        unregisterReceiver(hardwareHijackReceiver)
        uiScope.cancel()
    }
}
