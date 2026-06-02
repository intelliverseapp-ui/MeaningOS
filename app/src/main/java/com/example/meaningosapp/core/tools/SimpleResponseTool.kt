package com.example.meaningosapp.core.tools

class SimpleResponseTool {

    fun handleSimpleRequest(): String {
        val responses = listOf(
            "Here’s something simple: you’re doing great.",
            "A small thought: things grow one step at a time.",
            "Something simple: I’m here with you.",
            "A tiny idea: progress is happening.",
            "Here’s a simple one: you’re not alone in this."
        )

        return responses.random()
    }
}
