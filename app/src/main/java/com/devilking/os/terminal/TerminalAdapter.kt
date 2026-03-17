package com.devilking.os.terminal

import android.graphics.Color
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TerminalAdapter(private val history: List<String>) : RecyclerView.Adapter<TerminalAdapter.TerminalViewHolder>() {

    class TerminalViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TerminalViewHolder {
        val textView = TextView(parent.context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 8, 16, 8)
            }
            // DEVILKING Signature Green Terminal Text
            setTextColor(Color.parseColor("#00FF41"))
            textSize = 14f
            typeface = Typeface.MONOSPACE
        }
        return TerminalViewHolder(textView)
    }

    override fun onBindViewHolder(holder: TerminalViewHolder, position: Int) {
        holder.textView.text = history[position]
    }

    override fun getItemCount(): Int = history.size
}
