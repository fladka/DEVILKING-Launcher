package com.devilking.os.automation

data class ToolManifest(
    val name: String,
    val description: String,
    val prefix: String,
    val action: (String) -> Unit
)

class CommandRegistry {
    private val tools = mutableListOf<ToolManifest>()

    fun register(tool: ToolManifest) {
        tools.add(tool)
    }

    // Evaluates the input and fires the correct tool action.
    // Returns true if a system tool handled it, false if it should fall back to the AI core.
    fun process(input: String): Boolean {
        val cleanInput = input.trim().lowercase()
        
        // Find the matching tool with the longest prefix (e.g., matching "wiretap on" before "wiretap")
        val matchedTool = tools.sortedByDescending { it.prefix.length }
            .firstOrNull { cleanInput.startsWith(it.prefix) }

        if (matchedTool != null) {
            matchedTool.action(cleanInput)
            return true
        }
        return false
    }

    // Generates a dynamic help menu based on currently registered tools
    fun getManifestMenu(): String {
        val sb = java.lang.StringBuilder("> --- DEVILKING OS MANIFEST ---\n")
        tools.forEach { tool ->
            sb.append("> ${tool.prefix.padEnd(15)} : ${tool.description}\n")
        }
        return sb.toString()
    }
}
