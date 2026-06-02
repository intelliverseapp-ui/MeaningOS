package com.example.meaningosapp.ui.main.face.audio.timbre.base

fun applyBrightness(text: String, brightness: Double): String {
    if (text.isBlank()) return text
    val clamped = brightness.coerceIn(0.0, 1.0)
    val semitones = (-2 + clamped * 5).coerceIn(-2.0, 3.0)
    val pitch = if (semitones >= 0) "+${semitones}st" else "${semitones}st"
    return """<prosody pitch="$pitch">$text</prosody>""".trimIndent()
}

fun applyWarmth(text: String, warmth: Double): String {
    if (text.isBlank()) return text
    val clamped = warmth.coerceIn(0.0, 1.0)
    val volumeBoost = (clamped * 12).toInt()
    val pitchShift = (-2 * clamped).coerceIn(-2.0, 0.0)
    val pitch = "${pitchShift}st"
    return """<prosody volume="+${volumeBoost}%" pitch="$pitch">$text</prosody>""".trimIndent()
}

fun applySharpness(text: String, sharpness: Double): String {
    if (text.isBlank()) return text
    val clamped = sharpness.coerceIn(0.0, 1.0)
    val pitchShift = (clamped * 3).coerceIn(0.0, 3.0)
    val rateBoost = (100 + clamped * 10).toInt()
    val volumeBoost = (clamped * 8).toInt()
    val pitch = "+${pitchShift}st"
    return """<prosody pitch="$pitch" rate="${rateBoost}%" volume="+${volumeBoost}%">$text</prosody>""".trimIndent()
}

fun applyResonance(text: String, resonance: Double): String {
    if (text.isBlank()) return text
    val clamped = resonance.coerceIn(0.0, 1.0)
    val volumeBoost = (clamped * 10).toInt()
    val rateBoost = (100 + clamped * 8).toInt()
    return """<prosody volume="+${volumeBoost}%" rate="${rateBoost}%">$text</prosody>""".trimIndent()
}
