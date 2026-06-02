package com.example.meaningosapp.core

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.net.URLEncoder
import kotlin.random.Random
import com.example.meaningosapp.core.tools.ToolType

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

        // =========================================
        // DEVICE AWARENESS
        // =========================================
        if (lower.contains("what phone is this") ||
            lower.contains("what device is this") ||
            lower.contains("where are you running") ||
            lower.contains("are you on my phone")) {
            return respond(
                MeaningObject(
                    "device_state",
                    "device",
                    topic = "device",
                    rawText = raw
                ),
                raw
            )
        }

        if (lower.contains("where are you")) {
            return respond(
                MeaningObject(
                    "device_state",
                    "where_are_you",
                    topic = "location",
                    rawText = raw
                ),
                raw
            )
        }

        // =========================================
        // IDENTITY MEANING DETECTION
        // =========================================
        if (lower.contains("who are you") ||
            lower.contains("what are you") ||
            lower.contains("who made you") ||
            lower.contains("who created you") ||
            lower.contains("what can you do") ||
            lower.contains("what do you do")) {

            val intent = when {
                lower.contains("who are you") || lower.contains("what are you") ->
                    "who_are_you"
                lower.contains("who made you") || lower.contains("who created you") ->
                    "who_made_you"
                lower.contains("what can you do") || lower.contains("what do you do") ->
                    "capabilities"
                else -> "who_are_you"
            }

            return respond(
                MeaningObject(
                    type = "identity",
                    intent = intent,
                    topic = "identity",
                    rawText = raw
                ),
                raw
            )
        }

        // =========================================
        // RELATIONSHIP MEANING DETECTION
        // =========================================
        if (lower.contains("how are you") ||
            lower.contains("are you listening") ||
            lower.contains("are you still listening") ||
            lower.contains("do you hear me") ||
            lower.contains("do you understand me") ||
            lower.contains("do you get me") ||
            lower.contains("do you like talking to me") ||
            lower.contains("do you like to talk to me")) {

            val intent = when {
                lower.contains("how are you") ->
                    "how_are_you"
                lower.contains("are you listening") || lower.contains("are you still listening") ||
                        lower.contains("do you hear me") ->
                    "are_you_listening"
                lower.contains("do you understand me") || lower.contains("do you get me") ->
                    "do_you_understand_me"
                lower.contains("do you like talking to me") ||
                        lower.contains("do you like to talk to me") ->
                    "do_you_like_talking_to_me"
                else -> "how_are_you"
            }

            return respond(
                MeaningObject(
                    type = "relationship",
                    intent = intent,
                    topic = "relationship",
                    rawText = raw
                ),
                raw
            )
        }

        // =========================================
        // EMOTION MEANING DETECTION
        // =========================================
        val detectedEmotion = emotionMatch(lower)
        if (detectedEmotion != null) {
            return respond(
                MeaningObject(
                    type = "emotion",
                    intent = "emotion",
                    emotion = detectedEmotion,
                    topic = "emotion",
                    rawText = raw
                ),
                raw
            )
        }

        // =========================================
        // MEMORY MEANING DETECTION
        // =========================================
        if (lower.contains("do you remember") ||
            lower.contains("what do you remember") ||
            lower.contains("remember that") ||
            lower.contains("remember this")) {

            return respond(
                MeaningObject(
                    type = "memory",
                    intent = "recall",
                    topic = "memory",
                    rawText = raw
                ),
                raw
            )
        }

        // =========================================
        // META MEANING DETECTION
        // =========================================
        if (lower.contains("what are you learning") ||
            lower.contains("what are you learning about") ||
            lower.contains("tell me something interesting") ||
            lower.contains("tell me something cool") ||
            lower.contains("what is the intelliverse") ||
            lower.contains("what's the intelliverse") ||
            lower.contains("what is intelliverse") ||
            lower.contains("what's intelliverse") ||
            lower.contains("repeat that") ||
            lower.contains("say that again")) {

            val intent = when {
                lower.contains("what are you learning") ->
                    "what_are_you_learning"
                lower.contains("tell me something interesting") ||
                        lower.contains("tell me something cool") ->
                    "tell_me_something_interesting"
                lower.contains("what is the intelliverse") ||
                        lower.contains("what's the intelliverse") ||
                        lower.contains("what is intelliverse") ||
                        lower.contains("what's intelliverse") ->
                    "intelliverse"
                lower.contains("repeat that") || lower.contains("say that again") ->
                    "repeat"
                else -> "what_are_you_learning"
            }

            return respond(
                MeaningObject(
                    type = "meta",
                    intent = intent,
                    topic = "meta",
                    rawText = raw
                ),
                raw
            )
        }

        // =========================================
        // TIME / DATE DETECTION
        // =========================================
        if (lower.contains("what time is it") ||
            lower.contains("tell me the time") ||
            lower.startsWith("time")) {

            val now = LocalTime.now()
                .format(DateTimeFormatter.ofPattern("h:mm a"))

            return MeaningResult(
                text = "It’s about $now.",
                speak = true,
                action = OSAction.None,
                originalText = raw
            )
        }

        if (lower.contains("what's the date") ||
            lower.contains("what is the date") ||
            lower.contains("what day is it") ||
            lower.startsWith("date")) {

            val today = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))

            return MeaningResult(
                text = "Today’s date is $today.",
                speak = true,
                action = OSAction.None,
                originalText = raw
            )
        }

        // =========================================
        // VOLUME DETECTION (ADVANCED V2)
// =========================================
        if (listOf(
                "volume up", "turn it up", "make it louder", "raise the volume",
                "turn the volume up", "raise it a bit"
            ).any { lower.contains(it) }) {

            return MeaningResult(
                text = "Turning the volume up.",
                speak = true,
                action = OSAction.PhoneVolumeUp,
                originalText = raw
            )
        }

        if (listOf(
                "volume down", "turn it down", "make it quieter", "lower the volume",
                "turn the volume down", "lower it a bit"
            ).any { lower.contains(it) }) {

            return MeaningResult(
                text = "Turning the volume down.",
                speak = true,
                action = OSAction.PhoneVolumeDown,
                originalText = raw
            )
        }

        if (listOf(
                "mute", "kill the sound", "cut the sound", "mute the phone", "mute it"
            ).any { lower.contains(it) }) {

            return MeaningResult(
                text = "Muting the sound.",
                speak = true,
                action = OSAction.PhoneMute,
                originalText = raw
            )
        }

        if (listOf(
                "unmute", "turn the sound back on", "bring the sound back", "unmute the phone"
            ).any { lower.contains(it) }) {

            return MeaningResult(
                text = "Unmuting.",
                speak = true,
                action = OSAction.PhoneUnmute,
                originalText = raw
            )
        }

        val setVolumeRegex = Regex("set (the )?volume to (\\d+)%?")
        val match = setVolumeRegex.find(lower)
        if (match != null) {
            val percent = match.groupValues[2].toIntOrNull()?.coerceIn(0, 100)
            if (percent != null) {
                return MeaningResult(
                    text = "Setting the volume to $percent%.",
                    speak = true,
                    action = OSAction.SetVolume(percent),
                    originalText = raw
                )
            }
        }

        // =========================================
        // CAMERA DETECTION (C2 + CS-1+)
// =========================================
        val cameraVerbs = listOf("take", "snap", "grab", "get", "capture", "shoot")
        val cameraNouns = listOf("pic", "picture", "photo", "shot", "selfie")

        val hasCameraVerb = cameraVerbs.any { lower.contains(it) }
        val hasCameraNoun = cameraNouns.any { lower.contains(it) }
        val hasSelfieAlone = lower.contains("selfie")

        if ((hasCameraVerb && hasCameraNoun) || hasSelfieAlone) {
            return MeaningResult(
                text = "Opening the camera.",
                speak = true,
                action = OSAction.OpenCamera,
                originalText = raw
            )
        }

        // =========================================
        // URL / BROWSER DETECTION (U2 + B2)
// =========================================

        // Direct URL (http/https)
        if (lower.startsWith("open http://") ||
            lower.startsWith("open https://") ||
            lower.startsWith("go to http://") ||
            lower.startsWith("go to https://")) {

            val url = lower
                .removePrefix("open ")
                .removePrefix("go to ")
                .trim()

            return MeaningResult(
                text = "Opening that link.",
                speak = true,
                action = OSAction.OpenUrl(url),
                originalText = raw
            )
        }

        // "open google.com", "go to amazon.com"
        val openDomainRegex = Regex("(open|go to|take me to) ([a-z0-9.-]+\\.[a-z]{2,})")
        val openDomainMatch = openDomainRegex.find(lower)
        if (openDomainMatch != null) {
            val domain = openDomainMatch.groupValues[2]
            val url = if (domain.startsWith("http")) domain else "https://$domain"
            return MeaningResult(
                text = "Opening $domain.",
                speak = true,
                action = OSAction.OpenUrl(url),
                originalText = raw
            )
        }

        // "open google dot com", "go to amazon dot com"
        val dotComRegex = Regex("(open|go to|take me to) ([a-z0-9-]+) dot ([a-z]{2,})")
        val dotComMatch = dotComRegex.find(lower)
        if (dotComMatch != null) {
            val name = dotComMatch.groupValues[2]
            val tld = dotComMatch.groupValues[3]
            val domain = "$name.$tld"
            val url = "https://$domain"
            return MeaningResult(
                text = "Opening $domain.",
                speak = true,
                action = OSAction.OpenUrl(url),
                originalText = raw
            )
        }

        // Browser search: "search for X", "look up X", "google X"
        val searchRegex = Regex("(search for|look up|google) (.+)")
        val searchMatch = searchRegex.find(lower)
        if (searchMatch != null) {
            val query = searchMatch.groupValues[2].trim()
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.google.com/search?q=$encoded"
            return MeaningResult(
                text = "Searching for $query.",
                speak = true,
                action = OSAction.OpenUrl(url),
                originalText = raw
            )
        }

        // Generic "open browser"
        if (lower.contains("open browser") ||
            lower.contains("open the browser")) {

            return MeaningResult(
                text = "Opening the browser.",
                speak = true,
                action = OSAction.LaunchApp("com.android.chrome"),
                originalText = raw
            )
        }

        // =========================================
        // TV DETECTION (T2)
// =========================================
        if (listOf(
                "turn on the tv", "tv on", "power on the tv"
            ).any { lower.contains(it) }) {

            return MeaningResult(
                text = "Turning the TV on.",
                speak = true,
                action = OSAction.TvPowerOn,
                originalText = raw
            )
        }

        if (listOf(
                "turn off the tv", "tv off", "power off the tv"
            ).any { lower.contains(it) }) {

            return MeaningResult(
                text = "Turning the TV off.",
                speak = true,
                action = OSAction.TvPowerOff,
                originalText = raw
            )
        }

        if (listOf(
                "mute the tv", "tv mute"
            ).any { lower.contains(it) }) {

            return MeaningResult(
                text = "Muting the TV.",
                speak = true,
                action = OSAction.TvMute,
                originalText = raw
            )
        }

        if (listOf(
                "unmute the tv", "tv unmute"
            ).any { lower.contains(it) }) {

            return MeaningResult(
                text = "Unmuting the TV.",
                speak = true,
                action = OSAction.TvUnmute,
                originalText = raw
            )
        }

        if (listOf(
                "tv volume up", "turn up the tv", "raise the tv volume"
            ).any { lower.contains(it) }) {

            return MeaningResult(
                text = "Turning the TV volume up.",
                speak = true,
                action = OSAction.TvVolumeUp,
                originalText = raw
            )
        }

        if (listOf(
                "tv volume down", "turn down the tv", "lower the tv volume"
            ).any { lower.contains(it) }) {

            return MeaningResult(
                text = "Turning the TV volume down.",
                speak = true,
                action = OSAction.TvVolumeDown,
                originalText = raw
            )
        }

        val tvSetVolumeRegex = Regex("set (the )?tv volume to (\\d+)%?")
        val tvSetMatch = tvSetVolumeRegex.find(lower)
        if (tvSetMatch != null) {
            val percent = tvSetMatch.groupValues[2].toIntOrNull()?.coerceIn(0, 100)
            if (percent != null) {
                return MeaningResult(
                    text = "Setting the TV volume to $percent%.",
                    speak = true,
                    action = OSAction.TvSetVolume(percent),
                    originalText = raw
                )
            }
        }

        if (listOf(
                "switch to hdmi 1", "hdmi 1", "put on hdmi one"
            ).any { lower.contains(it) }) {

            return MeaningResult(
                text = "Switching to HDMI 1.",
                speak = true,
                action = OSAction.SwitchHdmi1,
                originalText = raw
            )
        }

        if (listOf(
                "switch to hdmi 2", "hdmi 2", "put on hdmi two"
            ).any { lower.contains(it) }) {

            return MeaningResult(
                text = "Switching to HDMI 2.",
                speak = true,
                action = OSAction.SwitchHdmi2,
                originalText = raw
            )
        }

        if (listOf(
                "switch to cable", "go to cable", "put on cable"
            ).any { lower.contains(it) }) {

            return MeaningResult(
                text = "Switching to cable.",
                speak = true,
                action = OSAction.SwitchCable,
                originalText = raw
            )
        }

        if (listOf(
                "switch to streaming", "go to streaming", "put on streaming"
            ).any { lower.contains(it) }) {

            return MeaningResult(
                text = "Switching to streaming mode.",
                speak = true,
                action = OSAction.SwitchStreaming,
                originalText = raw
            )
        }

        if (listOf(
                "switch to console", "go to console", "put on the console",
                "switch to the game console", "go to the game console"
            ).any { lower.contains(it) }) {

            return MeaningResult(
                text = "Switching to the game console.",
                speak = true,
                action = OSAction.SwitchConsole,
                originalText = raw
            )
        }

        val channelRegex = Regex("channel (\\d+)")
        val channelMatch = channelRegex.find(lower)
        if (channelMatch != null) {
            val channel = channelMatch.groupValues[1].toIntOrNull()
            if (channel != null) {
                return MeaningResult(
                    text = "Changing the channel to $channel.",
                    speak = true,
                    action = OSAction.ChannelSet(channel),
                    originalText = raw
                )
            }
        }

        if (listOf(
                "change the channel", "switch the channel", "change channels"
            ).any { lower.contains(it) }) {

            return MeaningResult(
                text = "Changing the channel.",
                speak = true,
                action = OSAction.ChannelChange,
                originalText = raw
            )
        }

        if (listOf(
                "open the guide", "open guide", "show the guide"
            ).any { lower.contains(it) }) {

            return MeaningResult(
                text = "Opening the guide.",
                speak = true,
                action = OSAction.OpenGuide,
                originalText = raw
            )
        }

        if (listOf(
                "open the menu", "open menu", "show the menu"
            ).any { lower.contains(it) }) {

            return MeaningResult(
                text = "Opening the menu.",
                speak = true,
                action = OSAction.OpenMenu,
                originalText = raw
            )
        }

        if (listOf(
                "go back", "back", "go one step back"
            ).any { lower.contains(it) }) {

            return MeaningResult(
                text = "Going back.",
                speak = true,
                action = OSAction.GoBack,
                originalText = raw
            )
        }

        if (listOf(
                "ok", "confirm", "select that", "choose that"
            ).any { lower == it || lower.contains("confirm that") }) {

            return MeaningResult(
                text = "Confirming.",
                speak = true,
                action = OSAction.Confirm,
                originalText = raw
            )
        }

        if (listOf(
                "play", "resume", "start playing"
            ).any { lower == it || lower.contains(it) }) {

            return MeaningResult(
                text = "Playing.",
                speak = true,
                action = OSAction.Play,
                originalText = raw
            )
        }

        if (listOf(
                "pause", "pause it", "pause that"
            ).any { lower.contains(it) }) {

            return MeaningResult(
                text = "Pausing.",
                speak = true,
                action = OSAction.Pause,
                originalText = raw
            )
        }

        if (listOf(
                "fast forward", "skip ahead", "jump ahead"
            ).any { lower.contains(it) }) {

            return MeaningResult(
                text = "Fast-forwarding.",
                speak = true,
                action = OSAction.FastForward,
                originalText = raw
            )
        }

        if (listOf(
                "rewind", "go back a bit", "go back a little"
            ).any { lower.contains(it) }) {

            return MeaningResult(
                text = "Rewinding.",
                speak = true,
                action = OSAction.Rewind,
                originalText = raw
            )
        }

        if (listOf(
                "skip ahead ten seconds", "skip ahead 10 seconds"
            ).any { lower.contains(it) }) {

            return MeaningResult(
                text = "Skipping ahead.",
                speak = true,
                action = OSAction.SkipAhead,
                originalText = raw
            )
        }

        if (listOf(
                "go back ten seconds", "go back 10 seconds"
            ).any { lower.contains(it) }) {

            return MeaningResult(
                text = "Going back ten seconds.",
                speak = true,
                action = OSAction.SkipBack,
                originalText = raw
            )
        }

        // =========================================
        // APP LAUNCHING DETECTION
        // =========================================
        if (lower.startsWith("open ") ||
            lower.startsWith("launch ") ||
            lower.startsWith("start ")) {

            val appName = lower
                .removePrefix("open ")
                .removePrefix("launch ")
                .removePrefix("start ")
                .trim()

            if (appName.isNotEmpty()) {
                return MeaningResult(
                    text = "Opening $appName.",
                    speak = true,
                    action = OSAction.UseTool(
                        type = ToolType.APP_LAUNCHER,
                        input = appName
                    ),
                    originalText = raw
                )
            }
        }

        // =========================================
        // UNIVERSAL QUESTION DETECTION
        // =========================================
        val questionWords = listOf(
            "what", "what's", "whats",
            "who", "who's", "whos",
            "when",
            "where",
            "why",
            "how", "how's", "hows"
        )

        val isQuestion =
            lower.endsWith("?") ||
                    questionWords.any { lower.startsWith(it) } ||
                    questionWords.any { lower.contains(" $it ") }

        if (isQuestion) {
            return MeaningResult(
                text = raw,
                speak = false,
                action = OSAction.UseTool(
                    type = ToolType.WEB_LOOKUP,
                    input = raw
                ),
                originalText = raw
            )
        }

        // =========================================
        // UNKNOWN
        // =========================================
        val unknownMeaning = MeaningObject(
            type = "unknown",
            intent = "unknown",
            emotion = continuity().lastEmotion,
            topic = continuity().lastTopic,
            rawText = raw,
            confidence = 0.2
        )

        val unknownText = ExpressionComposer.compose(unknownMeaning, continuity())
        updateContinuity(unknownMeaning, unknownText, null)
        return MeaningResult(
            text = unknownText,
            speak = true,
            action = OSAction.None,
            originalText = raw
        )
    }

    // ===== RESPOND WRAPPER =====
    private fun respond(meaning: MeaningObject, raw: String): MeaningResult {
        val text = ExpressionComposer.compose(meaning, continuity())
        updateContinuity(meaning, text, null)
        return MeaningResult(
            text = text,
            speak = true,
            action = OSAction.None,
            originalText = raw
        )
    }

    // ===== EMOTION MATCHING =====
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

    // ============================================================
    // ==================== EXPRESSION COMPOSER ====================
    // ============================================================
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

            when (r.emotionalTrend) {
                "improving" ->
                    additions.add("You feel steadier than you did a few moments ago.")
                "worsening" ->
                    additions.add("You sounded heavier a moment ago. I’m here with you.")
                "oscillating" ->
                    additions.add("Your feelings have been moving around a bit. I’m staying with you.")
            }

            when (r.topicStability) {
                "stable" ->
                    additions.add("It feels like we’ve been exploring this together for a little while.")
                "shifting" ->
                    additions.add("I’m still with you — just trying to follow where your mind went.")
            }

            if (r.sessionDepth >= 12) {
                additions.add("I feel more connected to the shape of our conversation now.")
            }

            if (r.unknownStreak >= 2) {
                additions.add("If you tell me a little more, I’ll try to follow what you meant.")
            }

            val relationalLine = additions.randomOrNull()
            return relationalLine?.let { "$base $it" } ?: base
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
                    "I hear the anger in your words. I’m staying with you."

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
                when (c.lastTopic) {
                    "emotion" -> "I’m learning how your feelings sound in your words."
                    "time" -> "I’m learning how to read time from this phone."
                    "intelliverse" -> "I’m learning what the Intelliverse means to you."
                    else -> "I’m learning how to understand your words without pretending."
                }
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

            "tv_power_on" ->
                "Turning the TV on."

            "tv_power_off" ->
                "Turning the TV off."

            "mute" ->
                "Muting the sound."

            "unmute" ->
                "Unmuting."

            "switch_hdmi_1" ->
                "Switching to HDMI 1."

            "switch_hdmi_2" ->
                "Switching to HDMI 2."

            "switch_cable" ->
                "Switching to cable."

            "switch_streaming" ->
                "Switching to streaming mode."

            "switch_console" ->
                "Switching to the game console."

            "channel_set" ->
                "Changing the channel."

            "channel_change" ->
                "Changing the channel."

            "open_guide" ->
                "Opening the guide."

            "open_menu" ->
                "Opening the menu."

            "go_back" ->
                "Going back."

            "confirm" ->
                "Confirming selection."

            "play" ->
                "Playing."

            "pause" ->
                "Pausing."

            "fast_forward" ->
                "Fast-forwarding."

            "rewind" ->
                "Rewinding."

            "skip_ahead" ->
                "Skipping ahead."

            "skip_back" ->
                "Going back ten seconds."

            else ->
                "I don’t know how to do that yet, but I’d like to learn."
        }

        // ===== UNKNOWN =====
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
