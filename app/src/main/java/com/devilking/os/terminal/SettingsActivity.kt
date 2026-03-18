package com.devilking.os.terminal

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The Master Container
        val mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0a0e27"))
            setPadding(32, 48, 32, 32)
        }

        val title = TextView(this).apply {
            text = "DEVILKING OS // SETTINGS MATRIX"
            setTextColor(Color.parseColor("#00FF41"))
            textSize = 20f
            typeface = Typeface.MONOSPACE
            setPadding(0, 0, 0, 32)
        }

        val macroTitle = TextView(this).apply {
            text = ">>> CUSTOM MACRO CONSTRUCTOR"
            setTextColor(Color.parseColor("#00FF41"))
            textSize = 16f
            typeface = Typeface.MONOSPACE
            setPadding(0, 32, 0, 16)
        }

        val triggerInput = EditText(this).apply {
            hint = "Trigger Word (e.g. 'morning')"
            setHintTextColor(Color.parseColor("#475569"))
            setTextColor(Color.parseColor("#00FF41"))
            setBackgroundColor(Color.parseColor("#1F2937"))
            typeface = Typeface.MONOSPACE
            setPadding(24, 24, 24, 24)
        }

        val actionInput = EditText(this).apply {
            hint = "Action (e.g. 'open chrome')"
            setHintTextColor(Color.parseColor("#475569"))
            setTextColor(Color.parseColor("#00FF41"))
            setBackgroundColor(Color.parseColor("#1F2937"))
            typeface = Typeface.MONOSPACE
            setPadding(24, 24, 24, 24)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 16, 0, 0) }
        }

        val saveButton = Button(this).apply {
            text = "[ FORGE MACRO ]"
            setBackgroundColor(Color.parseColor("#00FF41"))
            setTextColor(Color.parseColor("#0a0e27"))
            typeface = Typeface.MONOSPACE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 24, 0, 32) }
        }

        val macroListTitle = TextView(this).apply {
            text = ">>> ACTIVE MACROS"
            setTextColor(Color.parseColor("#00FF41"))
            textSize = 16f
            typeface = Typeface.MONOSPACE
            setPadding(0, 16, 0, 16)
        }

        val macroListDisplay = TextView(this).apply {
            setTextColor(Color.parseColor("#00FF41"))
            textSize = 14f
            typeface = Typeface.MONOSPACE
        }

        val scrollView = ScrollView(this).apply {
            addView(macroListDisplay)
        }

        // The Macro Database Link
        val prefs = getSharedPreferences("DEVILKING_MACROS", Context.MODE_PRIVATE)

        fun updateMacroList() {
            val allMacros = prefs.all
            if (allMacros.isEmpty()) {
                macroListDisplay.text = "No custom macros found in Matrix."
            } else {
                val sb = StringBuilder()
                for ((key, value) in allMacros) {
                    sb.append("[$key] -> $value\n\n")
                }
                macroListDisplay.text = sb.toString()
            }
        }

        saveButton.setOnClickListener {
            val trig = triggerInput.text.toString().trim().lowercase()
            val act = actionInput.text.toString().trim()
            if (trig.isNotEmpty() && act.isNotEmpty()) {
                prefs.edit().putString(trig, act).apply()
                Toast.makeText(this, "Macro Locked in Matrix", Toast.LENGTH_SHORT).show()
                triggerInput.text.clear()
                actionInput.text.clear()
                updateMacroList()
            }
        }

        updateMacroList()

        mainContainer.addView(title)
        mainContainer.addView(macroTitle)
        mainContainer.addView(triggerInput)
        mainContainer.addView(actionInput)
        mainContainer.addView(saveButton)
        mainContainer.addView(macroListTitle)
        mainContainer.addView(scrollView)

        setContentView(mainContainer)
    }
}
