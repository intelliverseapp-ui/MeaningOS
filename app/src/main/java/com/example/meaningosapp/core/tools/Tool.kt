package com.example.meaningosapp.core.tools

interface Tool {
    val type: ToolType
    suspend fun execute(input: String): String
}
