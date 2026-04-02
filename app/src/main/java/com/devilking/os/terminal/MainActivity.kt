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
import com.devilking.os.automation.CommandRegistry
import com.devilking.os.automation.IntentVault
import com.devilking.os.automation.ToolManifest
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

    // THE CORE SYSTEMS
    private lateinit var aiCore: LocalAICore
    private lateinit var acousticShield: AcousticShield
    private lateinit var intentVault: IntentVault
    private lateinit var commandRegistry: CommandRegistry
    private val uiScope = CoroutineScope(Dispatchers.Main + Job())
    
    // VOSK OFFLINE ENGINE
    private var speechService: SpeechService? = null

    // THE HARDWARE HIJACK RECEIVER
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
        }
    }

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
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        aiCore = LocalAICore(this)
        acousticShield = AcousticShield(this)
        intentVault = IntentVault(this)
        
        setupCommandRegistry() // Initialize the Dynamic Router
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

        commandHistory.add("DEVILKING OS [Version 1.2.0 - ReAct Agent]")
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

    // --- WEAPON D: THE TOOL MANIFEST (Dynamic Routing) ---
    private fun setupCommandRegistry() {
        commandRegistry = CommandRegistry()

        commandRegistry.register(ToolManifest("Manifest", "Lists all active DEVILKING tools", "help") {
            printToTerminal(commandRegistry.getManifestMenu())
        })

        commandRegistry.register(ToolManifest("Clear", "Wipes the terminal display", "clear") {
            commandHistory.clear()
            adapter.notifyDataSetChanged()
        })

        commandRegistry.register(ToolManifest("Inject Core", "Loads GGUF Neural model", "inject core") {
            corePickerLauncher.launch("*/*")
        })

        commandRegistry.register(ToolManifest("Inject Vosk", "Loads Acoustic model", "inject vosk") {
            voskPickerLauncher.launch("application/zip")
        })

        commandRegistry.register(ToolManifest("Storage Bypass", "Grants master storage keys", "grant storage") {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                try {
                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = android.net.Uri.parse("package:$packageName")
                    startActivity(intent)
                    printToTerminal("> [SYSTEM]: Opening Storage Settings. Please grant 'All files access'.")
                } catch (e: Exception) {
                    startActivity(Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }
            } else {
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 2)
            }
        })

        commandRegistry.register(ToolManifest("Skill Engine", "Executes local .txt macro scripts", "run ") { input ->
            runSkillScript(input.substring(4).trim())
        })

        commandRegistry.register(ToolManifest("Wiretap Arm", "Intercepts UI telemetry", "wiretap on") {
            com.devilking.os.automation.DevilkingService.isWiretapEnabled = true
            printToTerminal("> [SYSTEM]: WIRETAP ARMED. Open any app to reveal its hidden Activity name.")
        })

        commandRegistry.register(ToolManifest("Wiretap Disarm", "Disables telemetry", "wiretap off") {
            com.devilking.os.automation.DevilkingService.isWiretapEnabled = false
            printToTerminal("> [SYSTEM]: WIRETAP DISARMED.")
        })

        commandRegistry.register(ToolManifest("Vault Dump", "Prints all locked Intent routes", "dump vault") {
            printToTerminal(intentVault.dumpVault())
        })

        commandRegistry.register(ToolManifest("Teleporter", "Bypasses UI to open locked routes", "teleport ") { input ->
            val target = input.substring(9).trim()
            if (target.isNotEmpty()) printToTerminal(intentVault.teleport(target))
            else printToTerminal("> [!] SYNTAX ERROR: Use 'teleport [target]'")
        })

        commandRegistry.register(ToolManifest("Route Locker", "Saves a new Intent target", "lock route ") { input ->
            val payload = input.substring(11).trim()
            val lastSpace = payload.lastIndexOf(' ')
            if (lastSpace != -1) {
                val commandName = payload.substring(0, lastSpace)
                val packageClass = payload.substring(lastSpace + 1)
                printToTerminal(intentVault.lockRoute(commandName, packageClass))
            } else {
                printToTerminal("> [!] SYNTAX ERROR: Use 'lock route [name] [package|class]'")
            }
        })
    }

    private fun runSkillScript(scriptName: String) {
        val fileName = if (scriptName.endsWith(".txt")) scriptName else "$scriptName.txt"
        val vaultDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "DEVILKING_VAULT")
        val scriptFile = File(vaultDir, fileName)

        if (!scriptFile.exists()) {
            printToTerminal("> [!] SKILL NOT FOUND: Create '$fileName' inside Documents/DEVILKING_VAULT/")
            return
        }

        printToTerminal("> [SYSTEM]: Initiating Skill Sequence: $scriptName...")
        
        uiScope.launch(Dispatchers.IO) {
            try {
                val lines = scriptFile.readLines()
                for (line in lines) {
                    val cmd = line.trim()
                    if (cmd.isEmpty() || cmd.startsWith("//") || cmd.startsWith("#")) continue
                    
                    if (cmd.lowercase().startsWith("delay ")) {
                        val time = cmd.substring(6).toLongOrNull() ?: 1000L
                        withContext(Dispatchers.Main) { printToTerminal("> [WAITING]: ${time}ms") }
                        delay(time)
                    } else {
                        withContext(Dispatchers.Main) { 
                            if (cmd.lowercase().startsWith("snipe ")) {
                                printToTerminal("root@devilking:~$ $cmd")
                                val target = cmd.substring(6).trim()
                                com.devilking.os.automation.DevilkingService.instance?.executeSniperStrike(target)
                            } 
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
                            } else {
                                processInput(cmd) 
                            }
                        }
                        delay(800) 
                    }
                }
                withContext(Dispatchers.Main) { printToTerminal("> [SYSTEM]: Skill Sequence Complete.") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { printToTerminal("> [!] SKILL ERROR: Failed to read file. Check Storage Permissions.") }
            }
        }
    }

    // --- PATH 1: THE AUTONOMOUS AGENT (ReAct Engine) ---
    private fun askAutonomousAgent(userInput: String) {
        val manifest = commandRegistry.getManifestMenu()
        
        val systemPrompt = """
            You are DEVILKING OS, an autonomous mobile AI. 
            You control an Android phone using these exact tools:
            $manifest
            
            Analyze the user's request. If a tool can fulfill it, you MUST respond in this EXACT format:
            THOUGHT: [Your reasoning]
            COMMAND: [The exact tool command to run]
            
            If no tool is needed, just converse normally.
            
            User Request: $userInput
        """.trimIndent()

        printToTerminal("> [DEVILKING AI]: Analyzing request...")
        
        uiScope.launch(Dispatchers.IO) {
            try {
                // We wrap this in a try-catch to intercept memory buffer overflows
                val response = aiCore.generateResponse(systemPrompt)
                withContext(Dispatchers.Main) {
                    handleAgentResponse(response)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    printToTerminal("> [!] AI CORE FAILURE: ${e.message ?: "Unknown Error. Check C++ Engine Memory."}")
                }
            }
        }
    }

    private fun handleAgentResponse(response: String) {
        printToTerminal(response)
        
        val commandRegex = Regex("(?i)COMMAND:\\s*(.+)")
        val match = commandRegex.find(response)
        
        if (match != null) {
            val extractedCommand = match.groupValues[1].replace("*", "").replace("[", "").replace("]", "").trim()
            printToTerminal("> [SYSTEM]: Autonomous Execution Triggered -> '$extractedCommand'")
            
            uiScope.launch {
                delay(1000) 
                processInput(extractedCommand)
            }
        }
    }

    private fun processInput(input: String) {
        val cleanInput = input.trim()
        if (cleanInput != "clear") {
             printToTerminal("root@devilking:~$ $cleanInput")
        }
        commandInput.text.clear()

        // 1. Check if the input is a registered system tool
        if (commandRegistry.process(cleanInput)) {
            return
        }

        // 2. If it is NOT a system tool, trigger the Autonomous ReAct Agent
        askAutonomousAgent(cleanInput)
    }

    // --- UI & AUDIO BOILERPLATE BELOW ---
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
