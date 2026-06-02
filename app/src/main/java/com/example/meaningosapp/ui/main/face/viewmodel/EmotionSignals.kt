package com.example.meaningosapp.ui.main.face.emotion

/**
 * EmotionSignals
 *
 * Canonical emotional input bundle for Baby Node 1.0+
 *
 * This is the structured data packet passed into the
 * SemanticEmojiEngine. It represents the *raw signals*
 * from the conversation and system state before they
 * collapse into a BabyEmotion.
 *
 * Pipeline:
 *   EmotionSignals →
 *   SemanticEmojiEngine.pickEmoji() →
 *   BabyEmotion.fromEmoji() →
 *   BabyNodeFace (UI expression)
 *
 * Keep this file extremely stable — it is the foundation
 * of the entire emotional inference system.
 */
data class EmotionSignals(

    // The user's last message (after STT)
    val userText: String? = null,

    // Baby Node's reply text
    val nodeReply: String? = null,

    // Whether Baby Node is currently thinking
    val isThinking: Boolean = false,

    // Whether Baby Node is currently listening
    val isListening: Boolean = false,

    // Sentiment score from SentimentAnalyzer (-1.0 to +1.0)
    val sentimentScore: Float? = null,

    // Emotional intensity (0.0 to 1.0)
    val intensity: Float? = null
)
