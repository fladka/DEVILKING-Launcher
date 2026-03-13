package com.devilking.os.ai

class AIContext {
    private val memory = mutableListOf<String>()
    
    // THIS IS THE SOUL OF YOUR OS: You can change this text later to change how the AI acts.
    val systemPrompt = "You are DEVILKING, a highly advanced, tactical AI assistant embedded in a Vivo T1 Pro operating system. You answer directly, concisely, and intelligently."

    fun addInteraction(userText: String, aiResponse: String) {
        memory.add("USER: $userText")
        memory.add("DEVILKING: $aiResponse")
        
        // Prevents the AI from using up all your 6GB RAM by forgetting old messages
        if (memory.size > 10) {
            memory.removeAt(0)
            memory.removeAt(0)
        }
    }

    fun buildPrompt(newCommand: String): String {
        val contextBuilder = StringBuilder()
        contextBuilder.append(systemPrompt).append("\n\n")
        
        for (msg in memory) {
            contextBuilder.append(msg).append("\n")
        }
        
        contextBuilder.append("USER: $newCommand\nDEVILKING: ")
        return contextBuilder.toString()
    }
}
