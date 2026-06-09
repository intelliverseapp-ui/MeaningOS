package com.example.meaningosapp.ui.main.face.audio

/**
 * BabyNodeMode — canonical UI state machine for Baby Node 1.0+
 *
 * These states drive:
 *  - BabyNodeFace animations (eyes, mouth, breathing, blinking)
 *  - UI transitions
 *  - ViewModel logic
 *
 * Backwards‑compatible aliases are preserved so older files compile.
 *
 * New states added for the fully animated Baby Node face:
 *  - SPEAKING (animated mouth)
 *  - THINKING (squint + pulse)
 */
enum class BabyNodeMode {
    IDLE,        // Neutral breathing + blinking
    LISTENING,   // Eyes widen, listening ring active
    PROCESSING,  // Legacy name for THINKING
    THINKING,    // Eyes squint, thinking pulse active
    SPEAKING,    // Animated mouth movement
    TYPING,      // Legacy state (kept for compatibility)
    ERROR;       // Error fallback

    companion object {
        // ⭐ Legacy PascalCase aliases (do NOT remove)
        val Idle = IDLE
        val Listening = LISTENING
        val Meaning = PROCESSING
        val Typing = TYPING
        val ErrorMode = ERROR
    }
}

/** ⭐ Backwards‑compatible top‑level aliases used by older files */
val Idle = BabyNodeMode.IDLE
val Listening = BabyNodeMode.LISTENING
val Meaning = BabyNodeMode.PROCESSING
val Typing = BabyNodeMode.TYPING
val ErrorMode = BabyNodeMode.ERROR
