package com.example.meaningosapp.ui.main.face.audio.timbre.prosody

object BoundaryDetector {
    fun tokenizeWithPunctuation(text: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        for (ch in text) {
            when (ch) {
                ',', ';', ':', '—', '-', '.', '!', '?' -> {
                    if (sb.isNotEmpty()) {
                        tokens.add(sb.toString())
                        sb.clear()
                    }
                    tokens.add(ch.toString())
                }
                else -> sb.append(ch)
            }
        }
        if (sb.isNotEmpty()) tokens.add(sb.toString())
        return tokens
    }

    fun detectBoundaryForToken(token: String, nextIsPhraseStart: Boolean): ProsodicBoundary {
        val t = token.trim()
        if (t.isEmpty()) return ProsodicBoundary.NONE
        return when (t) {
            "," -> ProsodicBoundary.COMMA
            ";" -> ProsodicBoundary.PAUSE
            ":" -> ProsodicBoundary.PAUSE
            "—", "-" -> ProsodicBoundary.PAUSE
            "." -> ProsodicBoundary.PHRASE_END
            "!" -> ProsodicBoundary.PHRASE_END
            "?" -> ProsodicBoundary.PHRASE_END
            else -> if (nextIsPhraseStart) ProsodicBoundary.PHRASE_START else ProsodicBoundary.NONE
        }
    }
}
