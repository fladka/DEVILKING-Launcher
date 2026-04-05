package com.devilking.os.automation

import android.view.accessibility.AccessibilityNodeInfo
import android.graphics.Rect

class SemanticEye {

    fun scan(rootNode: AccessibilityNodeInfo?): String {
        if (rootNode == null) return "> [!] SEMANTIC EYE BLIND: No active window found."
        
        val uiElements = mutableListOf<String>()
        var elementCounter = 1

        fun traverseNode(node: AccessibilityNodeInfo?) {
            if (node == null || !node.isVisibleToUser) return

            val text = node.text?.toString()?.trim() ?: ""
            val desc = node.contentDescription?.toString()?.trim() ?: ""
            val isClickable = node.isClickable
            val isEditable = node.isEditable

            // Determine the best human-readable label
            val label = if (text.isNotEmpty()) text else if (desc.isNotEmpty()) desc else ""

            // Only capture it if it has text OR if it's a clickable icon/input box
            if (label.isNotEmpty() || isClickable || isEditable) {
                val type = when {
                    isEditable -> "InputBox"
                    isClickable -> "Button"
                    else -> "Text"
                }
                
                val content = if (label.isNotEmpty()) "'$label'" else "[Unlabeled Icon]"
                
                // Get physical coordinates for the Sniper Strike
                val bounds = Rect()
                node.getBoundsInScreen(bounds)
                val coords = "(${bounds.centerX()}, ${bounds.centerY()})"

                // Format: [#1] Button: 'Send' @ (540, 1800)
                uiElements.add("[#$elementCounter] $type: $content @ $coords")
                elementCounter++
            }

            // Recursively dig into children
            for (i in 0 until node.childCount) {
                traverseNode(node.getChild(i))
            }
        }

        traverseNode(rootNode)
        
        // We do NOT recycle the rootNode here. We let the Accessibility Service handle its own memory.

        return if (uiElements.isEmpty()) {
            "> [SEMANTIC EYE]: Screen is blank or unreadable."
        } else {
            uiElements.joinToString("\n")
        }
    }
}
