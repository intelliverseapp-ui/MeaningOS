package com.example.meaningosapp.ui.model

data class ChatMessage(
    val sender: Sender,
    val text: String
)

enum class Sender {
    USER,
    AGENT
}


