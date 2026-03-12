package com.high.command.ui

import android.graphics.Color
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TerminalAdapter(private val history: List<String>) : RecyclerView.Adapter<TerminalAdapter.ViewHolder>() {

    class ViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val textView = TextView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setTextColor(Color.parseColor("#00FF41")) 
            textSize = 13f
            typeface = Typeface.MONOSPACE
            setPadding(16, 6, 16, 6)
            setBackgroundColor(Color.parseColor("#0a0e27")) 
            
            // The Fix: Use the official setter functions instead of trying to reassign vals
            setTextIsSelectable(true)
            setLineSpacing(0f, 1.3f)
        }
        return ViewHolder(textView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = history[position]
    }

    override fun getItemCount() = history.size
}
