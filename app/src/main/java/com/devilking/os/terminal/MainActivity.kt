package com.devilking.os.terminal

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
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
import com.devilking.os.automation.IntentVault
import kotlinx.coroutines.*
import org.json.JSONObject
import org.vosk.Recognizer
import org.vosk.android.SpeechService
import org.vosk.android.RecognitionListener
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var terminalRecyclerView: RecyclerView
    private lateinit var commandInput: EditText
    private lateinit var micButton: Button
    private lateinit var adapter: TerminalAdapter
    private val commandHistory = mutableListOf<String>()

    // THE NEURAL & ACOUSTIC CORES & VAULTS
    private lateinit var aiCore: LocalAICore
    private lateinit var acousticShield: AcousticShield
    private lateinit var intentVault: IntentVault
    private val uiScope = CoroutineScope(Dispatchers.Main + Job())
    
    // VOSK OFFLINE ENGINE
    private var speechService: SpeechService? = null

    // THE HARDWARE HIJACK RECEIVER (Walkie-Talkie Mode & Wiretap)
    private val hardwareHijackReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            when (intent?.action) {
                "com.devilking.os.MIC_START" -> checkAudioPermissionAndStart()
                "com.devilking.os.MIC_STOP" -> stopVoskListening()
                "com.devilking.os.WIRETAP_LOG" -> {
                    val data = intent.getStringExtra("activity_data") ?: "UNKNOWN"
                    printToTerminal("> [WIRETAP EXPOSED]: $data")
                }
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
        intentVault = IntentVault(this) // Initialize the Teleporter Vault
        
        setupUI()
        setupKeyboardTraps()

        val filter = android.content.IntentFilter().apply {
            addAction("com.devilking.os.MIC_START")
            addAction("com.devilking.os.MIC_STOP")
            addAction("com.devilking.os.WIRETAP_LOG")
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(hardwareHijackReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(hardwareHijackReceiver, filter)
        }

        commandHistory.add("DEVILKING OS [Version 1.0.0]")
        commandHistory.add("> Hardware Hijack: Walkie-Talkie Mode Armed.")
        commandHistory.add(aiCore.checkCoreStatus())
        
        if (acousticShield.autoLoadExistingModel()) {
            initVoskService()
        }
        commandHistory.add(acousticShield.checkShieldStatus())
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
                val recognizer = Recognizer(acousticShield.voskModel, 16000.0f)
                speechService = SpeechService(recognizer, 16000.0f)
            } catch (e: Exception) {
                printToTerminal("> [!] VOSK ERROR: Failed to init audio engine.")
            }
        }
    }

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
        speechService?.stop()
        resetMicButton()
    }

    private val voskListener = object : RecognitionListener {
        override fun onPartialResult(hypothesis: String?) {}
        
        override fun onResult(hypothesis: String?) {
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

    // --- WEAPON B: THE SKILL LIBRARY (MACRO ENGINE) ---
    private fun runSkillScript(scriptName: String) {
        val fileName = if (scriptName.endsWith(".txt")) scriptName else "$scriptName.txt"
        
        // Target the public Documents/DEVILKING_VAULT folder on your Vivo
        val vaultDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "DEVILKING_VAULT")
        val scriptFile = File(vaultDir, fileName)

        if (!scriptFile.exists()) {
            printToTerminal("> [!] SKILL NOT FOUND: Create '$fileName' inside Documents/DEVILKING_VAULT/")
            return
        }

        printToTerminal("> [SYSTEM]: Initiating Skill Sequence: $scriptName...")
        
        // Launch on a background thread so the UI doesn't freeze
        uiScope.launch(Dispatchers.IO) {
            try {
                val lines = scriptFile.readLines()
                for (line in lines) {
                    val cmd = line.trim()
                    
                    // Ignore empty lines or comments
                    if (cmd.isEmpty() || cmd.startsWith("//") || cmd.startsWith("#")) continue
                    
                    // Handle specific delay commands to let the UI catch up
                    if (cmd.lowercase().startsWith("delay ")) {
                        val time = cmd.substring(6).toLongOrNull() ?: 1000L
                        withContext(Dispatchers.Main) { printToTerminal("> [WAITING]: ${time}ms") }
                        delay(time)
                    } else {
                        // Pass the string right back into your existing terminal logic
                        withContext(Dispatchers.Main) { 
                            // Using a direct call to the service for 'snipe' to avoid the AI loop
                            if (cmd.lowercase().startsWith("snipe ")) {
                                printToTerminal("root@devilking:~$ $cmd")
                                val target = cmd.substring(6).trim()
                                com.devilking.os.automation.DevilkingService.instance?.executeSniperStrike(target)
                            } 
                            // Using a direct call for 'open'
                            else if (cmd.lowercase().startsWith("open ")) {
                                printToTerminal("root@devilking:~$ $cmd")
                                val appName = cmd.substring(5).trim()
                                val intent = packageManager.getLaunchIntentForPackage("com.$appName") ?: packageManager.getLaunchIntentForPackage(appName)
                                if (intent != null) {
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    startActivity(intent)
                                } else {
                                    printToTerminal("> [!] FAILED TO OPEN: '$appName'")
                                }
                            }
                            // Add other direct commands here as needed
                            else {
                                processInput(cmd) 
                            }
                        }
                        // Default buffer between physical actions so the screen can render
                        delay(800) 
                    }
                }
                withContext(Dispatchers.Main) { printToTerminal("> [SYSTEM]: Skill Sequence Complete.") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { printToTerminal("> [!] SKILL ERROR: Failed to read file. Check Storage Permissions.") }
            }
        }
    }

    private fun processInput(input: String) {
        val cleanInput = input.trim()
        printToTerminal("root@devilking:~$ $cleanInput")
        commandInput.text.clear()

        // --- HARDWARE INTERCEPTS ---
        if (cleanInput.lowercase() == "inject core") {
            corePickerLauncher.launch("*/*")
            return
        }
        if (cleanInput.lowercase() == "inject vosk") {
            voskPickerLauncher.launch("application/zip")
            return
        }
        
        // --- STORAGE PERMISSION BYPASS ---
        if (cleanInput.lowercase() == "grant storage") {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                try {
                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = android.net.Uri.parse("package:$packageName")
                    startActivity(intent)
                    printToTerminal("> [SYSTEM]: Opening Storage Settings. Please grant 'All files access'.")
                } catch (e: Exception) {
                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            } else {
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 2)
            }
            return
        }

        // --- THE SKILL LIBRARY TRIGGER ---
        if (cleanInput.lowercase().startsWith("run ")) {
            val scriptName = cleanInput.substring(4).trim()
            runSkillScript(scriptName)
            return
        }

        // --- THE WIRETAP CONTROLS ---
        if (cleanInput.lowercase() == "wiretap on") {
            com.devilking.os.automation.DevilkingService.isWiretapEnabled = true
            printToTerminal("> [SYSTEM]: WIRETAP ARMED. Open any app to reveal its hidden Activity name.")
            return
        }
        if (cleanInput.lowercase() == "wiretap off") {
            com.devilking.os.automation.DevilkingService.isWiretapEnabled = false
            printToTerminal("> [SYSTEM]: WIRETAP DISARMED.")
            return
        }

        // --- THE TELEPORTER (INTENT VAULT) CONTROLS ---
        if (cleanInput.lowercase() == "dump vault") {
            printToTerminal(intentVault.dumpVault())
            return
        }

        if (cleanInput.lowercase().startsWith("teleport ")) {
            val target = cleanInput.substring(9).trim()
            if (target.isNotEmpty()) {
                printToTerminal(intentVault.teleport(target))
            } else {
                printToTerminal("> [!] SYNTAX ERROR: Use 'teleport [target]'")
            }
            return
        }

        if (cleanInput.lowercase().startsWith("lock route ")) {
            val payload = cleanInput.substring(11).trim()
            val lastSpace = payload.lastIndexOf(' ')
            if (lastSpace != -1) {
                val commandName = payload.substring(0, lastSpace)
                val packageClass = payload.substring(lastSpace + 1)
                printToTerminal(intentVault.lockRoute(commandName, packageClass))
            } else {
                printToTerminal("> [!] SYNTAX ERROR: Use 'lock route [name] [package|class]'")
            }
            return
        }

        // --- FALLBACK TO NEURAL CORE ---
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
