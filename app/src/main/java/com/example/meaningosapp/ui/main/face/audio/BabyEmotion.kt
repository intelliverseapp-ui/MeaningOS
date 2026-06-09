package com.example.meaningosapp.ui.main.face.audio

/**
 * BabyEmotion — canonical emotional states for Baby Node 1.0+
 *
 * These map directly to:
 *  - BabyNodeFace mouth shapes
 *  - Eye shapes (squint, widen)
 *  - Color accents (future)
 *  - SemanticEmojiEngine output
 *
 * IMPORTANT:
 *  - Keep this list small, expressive, and UI‑driven.
 *  - EmotionSignals → SemanticEmojiEngine → BabyEmotion
 *  - BabyEmotion drives the actual face rendering.
 */
enum class BabyEmotion {

    // ⭐ Default resting state
    NEUTRAL,

    // ⭐ Positive emotions
    HAPPY,
    LOVE,
    CONNECTED,

    // ⭐ Negative emotions
    SAD,
    ANGRY,
    SCARED,

    // ⭐ Cognitive / reactive states
    THINKING,
    CURIOUS,
    SURPRISED,

    // ⭐ Prosody-only states (for TTS expressiveness)
    CALM,
    EXCITED;

    companion object {
        fun fromEmoji(emoji: String?): BabyEmotion = when (emoji) {
            "😊" -> HAPPY
            "❤️" -> LOVE
            "🤝" -> CONNECTED
            "😢" -> SAD
            "😡" -> ANGRY
            "😱" -> SCARED
            "🤔" -> THINKING
            "🧐" -> CURIOUS
            "😮", "🤯", "🤩" -> SURPRISED
            else -> NEUTRAL
        }
    }
}
