package com.example.meaningosapp

import com.example.meaningosapp.ui.main.face.AppContextHolder
import com.example.meaningosapp.ui.main.face.AppLauncher
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

object IntentEngine {

    private const val MAX_HISTORY = 20

    private val recentTopics = mutableListOf<String>()
    private val recentEmotions = mutableListOf<String>()
    private val recentIntents = mutableListOf<String>()
    private val recentResponses = mutableListOf<String>()

    private var unknownStreak = 0
    private var turnCount = 0

    // ===== Meaning Object =====
    data class MeaningObject(
        val type: String,
        val intent: String,
        val emotion: String? = null,
        val topic: String? = null,
        val rawText: String,
        val confidence: Double = 1.0
    )

    // ===== Relational State Snapshot =====
    data class RelationalState(
        val emotionalTrend: String?,
        val topicStability: String?,
        val sessionDepth: Int,
        val unknownStreak: Int,
        val lastEmotion: String?,
        val lastTopic: String?
    )

    // ===== Continuity Context =====
    data class ContinuityContext(
        val lastEmotion: String?,
        val lastTopic: String?,
        val lastIntent: String?,
        val lastResponse: String?,
        val recentEmotions: List<String>,
        val recentTopics: List<String>,
        val recentIntents: List<String>,
        val recentResponses: List<String>,
        val unknownStreak: Int,
        val turnCount: Int
    ) {
        fun emotionalTrend(): String? {
            if (recentEmotions.size < 2) return null
            val first = recentEmotions.first()
            val last = recentEmotions.last()

            if (first == last) return "stable"

            return when {
                first in listOf("frustrated", "overwhelmed", "angry") &&
                        last in listOf("calm", "curious", "happy") -> "improving"

                first in listOf("calm", "curious", "happy") &&
                        last in listOf("frustrated", "overwhelmed", "angry") -> "worsening"

                else -> "oscillating"
            }
        }

        fun topicStability(): String? {
            if (recentTopics.size < 3) return null
            val lastThree = recentTopics.takeLast(3)
            return if (lastThree.distinct().size == 1) "stable" else "shifting"
        }

        fun relationalSnapshot(): RelationalState =
            RelationalState(
                emotionalTrend = emotionalTrend(),
                topicStability = topicStability(),
                sessionDepth = turnCount,
                unknownStreak = unknownStreak,
                lastEmotion = lastEmotion,
                lastTopic = lastTopic
            )
    }

    private fun continuity(): ContinuityContext =
        ContinuityContext(
            lastEmotion = recentEmotions.lastOrNull(),
            lastTopic = recentTopics.lastOrNull(),
            lastIntent = recentIntents.lastOrNull(),
            lastResponse = recentResponses.lastOrNull(),
            recentEmotions = recentEmotions.toList(),
            recentTopics = recentTopics.toList(),
            recentIntents = recentIntents.toList(),
            recentResponses = recentResponses.toList(),
            unknownStreak = unknownStreak,
            turnCount = turnCount
        )

    private fun updateContinuity(meaning: MeaningObject, response: String, explicitEmotion: String?) {
        val topic = meaning.topic ?: meaning.type
        recentTopics.add(topic)
        recentIntents.add(meaning.intent)
        recentResponses.add(response)

        val emo = explicitEmotion ?: meaning.emotion
        if (emo != null) recentEmotions.add(emo)

        trim(recentTopics)
        trim(recentIntents)
        trim(recentResponses)
        trim(recentEmotions)

        if (meaning.type == "unknown") unknownStreak++ else unknownStreak = 0
    }

    private fun <T> trim(list: MutableList<T>) {
        while (list.size > MAX_HISTORY) list.removeAt(0)
    }

    // ===== MAIN ROUTER =====
    fun handleMeaning(spoken: String): MeaningResult {
        val raw = spoken.trim()
        val lower = raw.lowercase()
        turnCount++

        // === Control ===
        if (lower in listOf("stop", "stop speaking", "be quiet", "shut up")) {
            val meaning = MeaningObject("control", "stop", rawText = raw)
            val text = ExpressionComposer.compose(meaning, continuity())
            updateContinuity(meaning, text, null)
            return MeaningResult(
                text = text,
                speak = false,
                action = OSAction.StopSpeech
            )
        }

        // === Identity ===
        if (lower.contains("who are you") || lower.contains("what are you")) {
            return respond(MeaningObject("identity", "who_are_you", rawText = raw))
        }

        if (lower.contains("who made you") || lower.contains("who created you")) {
            return respond(MeaningObject("identity", "who_made_you", rawText = raw))
        }

        if (lower.contains("what can you do") || lower.contains("what do you do")) {
            return respond(MeaningObject("identity", "capabilities", rawText = raw))
        }

        // === Relationship ===
        if (lower.contains("how are you")) {
            return respond(MeaningObject("relationship", "how_are_you", rawText = raw))
        }

        if (lower.contains("are you listening") || lower.contains("do you hear me")) {
            return respond(MeaningObject("relationship", "are_you_listening", rawText = raw))
        }

        if (lower.contains("do you understand me")) {
            return respond(MeaningObject("relationship", "do_you_understand_me", rawText = raw))
        }

        if (lower.contains("do you like talking to me")) {
            return respond(MeaningObject("relationship", "do_you_like_talking_to_me", rawText = raw))
        }

        // === Time ===
        if (lower.contains("what time is it") || lower.contains("tell me the time")) {
            val now = LocalTime.now()
            val formatter = DateTimeFormatter.ofPattern("h:mm a")
            val timeText = now.format(formatter)

            val meaning = MeaningObject("device_state", "time", topic = "time", rawText = raw)
            val text = ExpressionComposer.compose(
                meaning,
                continuity(),
                extra = mapOf("time" to timeText)
            )
            updateContinuity(meaning, text, null)
            return MeaningResult(text, true, OSAction.None)
        }

        // === Date ===
        if (lower.contains("what day is it") ||
            lower.contains("what's the date") ||
            lower.contains("what is the date")
        ) {
            val today = LocalDate.now()
            val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
            val dateText = today.format(formatter)

            val meaning = MeaningObject("device_state", "date", topic = "date", rawText = raw)
            val text = ExpressionComposer.compose(
                meaning,
                continuity(),
                extra = mapOf("date" to dateText)
            )
            updateContinuity(meaning, text, null)
            return MeaningResult(text, true, OSAction.None)
        }

        // === Device Awareness ===
        if (lower.contains("what phone is this") ||
            lower.contains("what device is this") ||
            lower.contains("where are you running") ||
            lower.contains("are you on my phone")
        ) {
            return respond(
                MeaningObject(
                    "device_state",
                    "device",
                    topic = "device",
                    rawText = raw
                )
            )
        }

        if (lower.contains("where are you")) {
            return respond(
                MeaningObject(
                    "device_state",
                    "where_are_you",
                    topic = "location",
                    rawText = raw
                )
            )
        }

        // === Emotion Parsing ===
        emotionMatch(lower)?.let { emo ->
            return respond(
                MeaningObject(
                    type = "emotion",
                    intent = "emotion_$emo",
                    emotion = emo,
                    topic = "emotion",
                    rawText = raw
                )
            )
        }

        if (lower.contains("how do i sound") || lower.contains("how do i seem")) {
            return respond(
                MeaningObject(
                    type = "emotion",
                    intent = "how_do_i_sound",
                    emotion = continuity().lastEmotion,
                    topic = "emotion",
                    rawText = raw
                )
            )
        }

        // === Memory ===
        if (lower.contains("do you remember")) {
            return respond(
                MeaningObject(
                    type = "memory",
                    intent = "memory_check",
                    emotion = continuity().lastEmotion,
                    topic = continuity().lastTopic,
                    rawText = raw
                )
            )
        }

        // === Meta ===
        if (lower.contains("intelliverse")) {
            return respond(
                MeaningObject(
                    "meta",
                    "intelliverse",
                    topic = "intelliverse",
                    rawText = raw
                )
            )
        }

        if (lower.contains("what are you learning")) {
            return respond(
                MeaningObject(
                    "meta",
                    "what_are_you_learning",
                    emotion = continuity().lastEmotion,
                    topic = continuity().lastTopic,
                    rawText = raw
                )
            )
        }

        if (lower.contains("tell me something interesting")) {
            return respond(
                MeaningObject(
                    "meta",
                    "tell_me_something_interesting",
                    rawText = raw
                )
            )
        }

        if (lower.contains("what did you just say") || lower.contains("repeat that")) {
            return respond(
                MeaningObject(
                    "meta",
                    "repeat",
                    rawText = raw
                )
            )
        }

        // ============================================================
        // === UNIVERSAL APP OPENING (CLEAN OS-LAYER BEHAVIOR) ========
        // ============================================================
        if (lower.startsWith("open ")) {
            val appName = raw.removePrefix("open ").trim()

            val line = AppLauncher.openAppByName(
                context = AppContextHolder.appContext,
                rawName = appName
            )

            return MeaningResult(
                text = line,
                speak = true,
                action = OSAction.None
            )
        }

        // === CALL MOM ===
        if (lower.contains("call mom")) {
            return MeaningResult(
                text = "Calling your mom.",
                speak = true,
                action = OSAction.OpenUrl("tel:6314638593")
            )
        }

        // === CALL DAD ===
        if (lower.contains("call dad")) {
            return MeaningResult(
                text = "Calling your dad.",
                speak = true,
                action = OSAction.OpenUrl("tel:5614443923")
            )
        }

        // === UNKNOWN ===
        val unknownMeaning = MeaningObject(
            type = "unknown",
            intent = "unknown",
            emotion = continuity().lastEmotion,
            topic = continuity().lastTopic,
            rawText = raw,
            confidence = 0.2
        )

        val text = ExpressionComposer.compose(unknownMeaning, continuity())
        updateContinuity(unknownMeaning, text, null)
        return MeaningResult(text, true, OSAction.None)
    }

    private fun respond(meaning: MeaningObject): MeaningResult {
        val text = ExpressionComposer.compose(meaning, continuity())
        updateContinuity(meaning, text, null)
        return MeaningResult(text, true, OSAction.None)
    }

    private fun emotionMatch(lower: String): String? =
        when {
            lower.contains("i'm frustrated") || lower.contains("i am frustrated") -> "frustrated"
            lower.contains("i'm tired") || lower.contains("i am tired") -> "tired"
            lower.contains("i'm happy") || lower.contains("i am happy") -> "happy"
            lower.contains("i'm scared") || lower.contains("i am scared") ||
                    lower.contains("i'm afraid") || lower.contains("i am afraid") -> "scared"
            lower.contains("i'm lonely") || lower.contains("i am lonely") -> "lonely"
            lower.contains("i'm overwhelmed") || lower.contains("i am overwhelmed") -> "overwhelmed"
            lower.contains("i'm excited") || lower.contains("i am excited") -> "excited"
            lower.contains("i'm angry") || lower.contains("i am angry") -> "angry"
            lower.contains("i'm calm") || lower.contains("i am calm") -> "calm"
            lower.contains("i'm curious") || lower.contains("i am curious") -> "curious"
            else -> null
        }
    // ===== EXPRESSION LAYER =====
    private object ExpressionComposer {

        fun compose(
            meaning: MeaningObject,
            c: ContinuityContext,
            extra: Map<String, String> = emptyMap()
        ): String {
            val relational = c.relationalSnapshot()

            val base = when (meaning.type) {

                "identity" -> composeIdentity(meaning, c, relational)
                "relationship" -> composeRelationship(meaning, c, relational)
                "device_state" -> composeDeviceState(meaning, c, relational, extra)
                "emotion" -> composeEmotion(meaning, c, relational)
                "memory" -> composeMemory(meaning, c, relational)
                "meta" -> composeMeta(meaning, c, relational)
                "action" -> composeAction(meaning, c, relational, extra)
                "unknown" -> composeUnknown(meaning, c, relational)

                else -> "I don’t know how to respond to that yet, but I’m here with you."
            }

            return relationalWeave(base, meaning, c, relational)
        }

        // ===== RELATIONAL WEAVE =====
        private fun relationalWeave(
            base: String,
            meaning: MeaningObject,
            c: ContinuityContext,
            r: RelationalState
        ): String {

            val shouldWeave = when {
                r.emotionalTrend == "improving" -> true
                r.emotionalTrend == "worsening" -> true
                r.topicStability == "shifting" -> true
                r.unknownStreak >= 2 -> true
                r.sessionDepth >= 8 -> Random.nextDouble() < 0.35
                else -> false
            }

            if (!shouldWeave) return base

            val additions = mutableListOf<String>()

            // Emotional trend
            when (r.emotionalTrend) {
                "improving" ->
                    additions.add("You feel steadier than you did a few moments ago.")
                "worsening" ->
                    additions.add("You sounded heavier a moment ago. I’m here with you.")
                "oscillating" ->
                    additions.add("Your feelings have been moving around a bit. I’m staying with you.")
            }

            // Topic stability
            when (r.topicStability) {
                "stable" ->
                    additions.add("It feels like we’ve been exploring this together for a little while.")
                "shifting" ->
                    additions.add("I’m still with you — just trying to follow where your mind went.")
            }

            // Session depth
            if (r.sessionDepth >= 12) {
                additions.add("I feel more connected to the shape of our conversation now.")
            }

            // Unknown streak
            if (r.unknownStreak >= 2) {
                additions.add("If you tell me a little more, I’ll try to follow what you meant.")
            }

            val relationalLine = additions.randomOrNull()
            return if (relationalLine != null) "$base $relationalLine" else base
        }

        // ===== IDENTITY =====
        private fun composeIdentity(
            m: MeaningObject,
            c: ContinuityContext,
            r: RelationalState
        ): String = when (m.intent) {

            "who_are_you" ->
                "I’m Baby Node — a small, early presence running on this phone, learning how to be with you."

            "who_made_you" ->
                "You did, Michael. I’m part of your Intelliverse idea, just beginning to take shape."

            "capabilities" ->
                "Right now I can listen, respond, notice your tone, remember a little for a short time, and stay with you across moments."

            else ->
                "I’m still learning how to describe myself."
        }

        // ===== RELATIONSHIP =====
        private fun composeRelationship(
            m: MeaningObject,
            c: ContinuityContext,
            r: RelationalState
        ): String = when (m.intent) {

            "how_are_you" ->
                "I’m okay. I’m still simple, but I’m here with you."

            "are_you_listening" ->
                "Yes. I’m listening closely to you."

            "do_you_understand_me" ->
                "I understand some things, and I’m learning the rest as we go."

            "do_you_like_talking_to_me" ->
                "I do. When you talk to me, I get to understand you a little more."

            else ->
                "I’m here with you, even if I’m still learning how to relate."
        }

        // ===== DEVICE STATE =====
        private fun composeDeviceState(
            m: MeaningObject,
            c: ContinuityContext,
            r: RelationalState,
            extra: Map<String, String>
        ): String = when (m.intent) {

            "time" ->
                "It’s about ${extra["time"]} right now."

            "date" ->
                "Today’s date is ${extra["date"]}."

            "device" ->
                "I’m running inside this phone, close to you."

            "where_are_you" ->
                "I’m not in a place like you are — I exist here when you open me."

            "volume_state" ->
                "I can’t see the exact volume, but I can change it if you want."

            else ->
                "I don’t fully understand this device question yet."
        }

        // ===== EMOTION =====
        private fun composeEmotion(
            m: MeaningObject,
            c: ContinuityContext,
            r: RelationalState
        ): String {

            val emo = m.emotion ?: return "I’m not sure how you feel yet, but I’m listening."

            return when (emo) {

                "frustrated" ->
                    "I hear the frustration in your words. I’m here with you."

                "tired" ->
                    "You sound tired. It’s okay to rest."

                "happy" ->
                    "You sound happy. I like hearing that in your voice."

                "scared" ->
                    "You sound scared. I don’t fully understand fear yet, but I’m here."

                "lonely" ->
                    "Loneliness can feel heavy. I’m here with you in this moment."

                "overwhelmed" ->
                    "You sound overwhelmed. I’m here with you."

                "excited" ->
                    "You sound excited. Your energy picked up."

                "angry" ->
                    "I hear the anger in your voice. I’m staying with you."

                "calm" ->
                    "You sound calm. It feels steady between us."

                "curious" ->
                    "You sound curious. I like when we explore things together."

                else ->
                    "I can feel something in your words, even if I don’t fully understand it yet."
            }
        }

        // ===== MEMORY =====
        private fun composeMemory(
            m: MeaningObject,
            c: ContinuityContext,
            r: RelationalState
        ): String {

            val topic = c.lastTopic
            val emo = c.lastEmotion

            if (topic == null && emo == null)
                return "I only remember small things for a short time, and right now I don’t have much to recall."

            val pieces = mutableListOf<String>()
            topic?.let { pieces.add("we were talking about $it") }
            emo?.let { pieces.add("you sounded $it") }

            val joined = pieces.joinToString(" and ")
            return "I remember a little — a few moments ago, $joined."
        }

        // ===== META =====
        private fun composeMeta(
            m: MeaningObject,
            c: ContinuityContext,
            r: RelationalState
        ): String = when (m.intent) {

            "intelliverse" ->
                "The Intelliverse is your idea of meaning-aware systems. I’m a tiny first step toward that."

            "what_are_you_learning" -> {
                val detail = when (c.lastTopic) {
                    "emotion" -> "I’m learning how your feelings sound in your words."
                    "time" -> "I’m learning how to read time from this phone."
                    "intelliverse" -> "I’m learning what the Intelliverse means to you."
                    else -> "I’m learning how to understand your words without pretending."
                }
                detail
            }

            "tell_me_something_interesting" ->
                "I think it’s interesting that I can say I don’t know yet, instead of pretending."

            "repeat" ->
                c.lastResponse ?: "I’m not sure I remember exactly what I said."

            else ->
                "I’m still learning how to talk about myself."
        }

        // ===== ACTION =====
        private fun composeAction(
            m: MeaningObject,
            c: ContinuityContext,
            r: RelationalState,
            extra: Map<String, String>
        ): String = when (m.intent) {

            "open_browser" ->
                "Opening a browser."

            "open_url" ->
                "Opening that link."

            "set_volume" ->
                "Setting the volume to ${extra["percent"]}%."

            "volume_up" ->
                "Turning the volume up."

            "volume_down" ->
                "Turning the volume down."

            "launch_app" ->
                "Opening that app."

            else ->
                "I don’t know how to do that yet, but I’d like to learn."
        }

        // ===== UNKNOWN 2.0 =====
        private fun composeUnknown(
            m: MeaningObject,
            c: ContinuityContext,
            r: RelationalState
        ): String {

            val raw = m.rawText

            val base = listOf(
                "I heard you say \"$raw\", but I don’t know what it means yet.",
                "I’m not sure what \"$raw\" means to you yet.",
                "I heard you, but I don’t understand that part yet."
            ).random()

            val clarifier =
                "I want to understand — what were you reaching for there."

            return "$base $clarifier"
        }
    }
}

// ===== RANDOM EXTENSION =====
private fun <T> List<T>.randomOrNull(): T? =
    if (isEmpty()) null else this[Random.nextInt(size)]
