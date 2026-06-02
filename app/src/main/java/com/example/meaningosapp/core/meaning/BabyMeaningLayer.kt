package com.example.meaningosapp.core.meaning

import com.example.meaningosapp.core.ptp.PTPGraph
import com.example.meaningosapp.core.ptp.PTPGraphNode
import kotlin.math.max

/**
 * BabyMeaningLayer — Full semantic engine (Baby Step: C)
 *
 * This version:
 * - Uses the full PTPGraph
 * - Computes a semantic score for every primitive
 * - Considers:
 *   - name similarity
 *   - token overlap with utterance
 *   - definition overlap
 *   - dependency overlap
 *   - layer weighting (lower layers = more fundamental)
 * - Returns DIRECT / PARTIAL / NONE based on thresholds
 */
class BabyMeaningLayer(
    private val graph: PTPGraph
) {

    fun interpret(text: String): MeaningResult {
        val cleaned = text.trim().lowercase()
        if (cleaned.isBlank()) {
            return MeaningResult(
                primitive = null,
                matchType = MatchType.NONE,
                notes = "Empty input"
            )
        }

        // Tokenize utterance
        val utterTokens = tokenize(cleaned)

        // 1. Exact primitive name match → DIRECT
        graph.getNode(cleaned)?.let { direct ->
            return MeaningResult(
                primitive = direct,
                matchType = MatchType.DIRECT,
                notes = "Direct primitive name match"
            )
        }

        // 2. Score all primitives semantically
        var bestNode: PTPGraphNode? = null
        var bestScore = 0.0
        var bestNotes = "No semantic grounding found"

        for (node in graph.nodesByName.values) {
            val scoreResult = scorePrimitive(node, utterTokens, cleaned)
            if (scoreResult.score > bestScore) {
                bestScore = scoreResult.score
                bestNode = node
                bestNotes = scoreResult.notes
            }
        }

        // 3. Decide match type based on score thresholds
        return if (bestNode == null || bestScore < NONE_THRESHOLD) {
            MeaningResult(
                primitive = null,
                matchType = MatchType.NONE,
                notes = "No semantic grounding found (score=$bestScore)"
            )
        } else if (bestScore >= DIRECT_THRESHOLD) {
            MeaningResult(
                primitive = bestNode,
                matchType = MatchType.DIRECT,
                notes = "Strong semantic grounding: $bestNotes (score=$bestScore)"
            )
        } else {
            MeaningResult(
                primitive = bestNode,
                matchType = MatchType.PARTIAL,
                notes = "Partial semantic grounding: $bestNotes (score=$bestScore)"
            )
        }
    }

    // --- Scoring configuration ---
    companion object {
        private const val DIRECT_THRESHOLD = 0.75
        private const val NONE_THRESHOLD = 0.20

        private const val WEIGHT_NAME_EXACT = 0.5
        private const val WEIGHT_NAME_PARTIAL = 0.3
        private const val WEIGHT_TOKEN_OVERLAP = 0.4
        private const val WEIGHT_DEFINITION_OVERLAP = 0.3
        private const val WEIGHT_DEPENDENCY_OVERLAP = 0.2
        private const val WEIGHT_LAYER = 0.1
    }

    private data class ScoreResult(
        val score: Double,
        val notes: String
    )

    /**
     * Compute a semantic score for a single primitive.
     */
    private fun scorePrimitive(
        node: PTPGraphNode,
        utterTokens: Set<String>,
        cleanedUtterance: String
    ): ScoreResult {

        val notes = mutableListOf<String>()
        var score = 0.0

        val name = node.name.lowercase()
        val nameTokens = tokenize(name)
        val defTokens = tokenize(node.definition.lowercase())
        val depTokens = node.dependencies.map { it.lowercase() }.toSet()

        // 1. Name exact / partial
        if (cleanedUtterance == name) {
            score += WEIGHT_NAME_EXACT
            notes += "Exact name match"
        } else if (cleanedUtterance.contains(name)) {
            score += WEIGHT_NAME_PARTIAL
            notes += "Name substring match"
        } else {
            val nameOverlap = jaccard(utterTokens, nameTokens)
            if (nameOverlap > 0.0) {
                score += WEIGHT_NAME_PARTIAL * nameOverlap
                notes += "Name token overlap=$nameOverlap"
            }
        }

        // 2. Utterance ↔ definition overlap
        val defOverlap = jaccard(utterTokens, defTokens)
        if (defOverlap > 0.0) {
            score += WEIGHT_DEFINITION_OVERLAP * defOverlap
            notes += "Definition overlap=$defOverlap"
        }

        // 3. Utterance ↔ dependency overlap
        val depOverlap = jaccard(utterTokens, depTokens)
        if (depOverlap > 0.0) {
            score += WEIGHT_DEPENDENCY_OVERLAP * depOverlap
            notes += "Dependency overlap=$depOverlap"
        }

        // 4. Token overlap (utterance vs name+definition+deps)
        val allPrimitiveTokens = nameTokens + defTokens + depTokens
        val tokenOverlap = jaccard(utterTokens, allPrimitiveTokens)
        if (tokenOverlap > 0.0) {
            score += WEIGHT_TOKEN_OVERLAP * tokenOverlap
            notes += "Global token overlap=$tokenOverlap"
        }

        // 5. Layer weighting (lower layers slightly favored)
        // layerIndex: 0 = most fundamental
        val layerWeight = 1.0 / (1.0 + node.layerIndex.toDouble())
        score += WEIGHT_LAYER * layerWeight
        notes += "Layer weight=${"%.2f".format(layerWeight)}"

        // Clamp score to [0, 1]
        val finalScore = max(0.0, score.coerceAtMost(1.0))

        return ScoreResult(
            score = finalScore,
            notes = notes.joinToString("; ")
        )
    }

    // --- Helpers ---

    private fun tokenize(text: String): Set<String> {
        return text
            .lowercase()
            .split(Regex("[^a-z0-9_]+"))
            .filter { it.isNotBlank() }
            .toSet()
    }

    private fun jaccard(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val intersection = a.intersect(b).size.toDouble()
        val union = a.union(b).size.toDouble()
        return if (union == 0.0) 0.0 else intersection / union
    }
}

data class MeaningResult(
    val primitive: PTPGraphNode?,
    val matchType: MatchType,
    val notes: String
)

enum class MatchType {
    DIRECT,
    PARTIAL,
    NONE
}
