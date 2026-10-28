package com.pocketstudio.model.tuto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/**
 * « Tuto de A à Z »: a whole track built by the user, chapter after chapter, each made of small
 * steps (one gesture or one listening). Read from `tutos/<style>.json` (resources of this module:
 * data, not code; a new style is a new file). Every text is French, the key of its translation
 * (`i18n/<lang>/tuto.json`). [TutoChecks] knows the detectors ([TutoStep.check]), what « Montre-moi »
 * does and what « Écouter l'exemple » plays ([TutoStep.listen]).
 */
@Serializable
data class TutoCourse(
    val format: Int = 1,
    val style: String = "",
    val chapters: List<TutoChapter> = emptyList(),
) {
    /** The chapters of [level], each with only its steps of that level. */
    fun chaptersFor(level: TutoLevel): List<TutoChapter> =
        chapters.filter { level.code in it.levels }
            .map { c -> c.copy(title = c.titleFor(level), steps = c.steps.filter { level.code in it.levels }) }
            .filter { it.steps.isNotEmpty() }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        private val cache = ConcurrentHashMap<String, TutoCourse>()

        /** The styles with a course written (the others show « Bientôt pour les autres styles »). */
        val STYLES = listOf("jump_up")

        fun of(style: String): TutoCourse? {
            if (style !in STYLES) return null
            return cache.getOrPut(style) {
                val stream = TutoCourse::class.java.classLoader?.getResourceAsStream("tutos/$style.json") ?: return null
                stream.bufferedReader(Charsets.UTF_8).use { json.decodeFromString(serializer(), it.readText()) }
            }
        }
    }
}

/** « Facile » (the Mode simple's words and screens) or « Pro » (the producer's, every tool). */
enum class TutoLevel(val code: String) { Facile("f"), Pro("p") }

/**
 * A chapter: its [title] ([titlePro]: in Pro, when the words differ), its « Pourquoi ? » ([why]);
 * [map]: the song's map (parts, bars, energy) is shown under it. [levels]: "f", "p" or "fp".
 */
@Serializable
data class TutoChapter(
    val id: String,
    val title: String,
    val titlePro: String? = null,
    val why: String = "",
    val levels: String = "fp",
    val map: Boolean = false,
    val steps: List<TutoStep> = emptyList(),
) {
    fun titleFor(level: TutoLevel): String = if (level == TutoLevel.Pro) titlePro ?: title else title
}

/**
 * One step. [text]: « À toi : … » ([textPro]: in Pro, when the words differ); [why]: its own
 * « Pourquoi ? ». [check]: its detector ([TutoChecks.done]): "listen" (ticked once the example is
 * heard), "manual" (« C'est fait »), "tempo", "lane:kick"… [spot], [target]: where « Je le fais »
 * goes (the Guide's [com.pocketstudio.model.guide.GuideAction] screens). [listen]: what
 * « Écouter l'exemple » plays ("drums:kick,snare", "parts:bass", "part:Drop", "vs:Drop:Intro",
 * "song"). [keep], [lanesFrom], [transpose]: how « Montre-moi » makes a part; [song]: the
 * arrangement it lays out ("Intro:32,Montée:8,…"). [demo]: a « Montre-moi » of its own for a step
 * ticked by hand ("kick": a long kick, then the kit's short one, each heard). [hearMine]: a listening
 * step that plays the user's own music, then the example (« Écoute ta boucle, puis l'exemple »):
 * ticked once both were heard (testers, lot « tuto 4 »: it was ticked on the user's loop alone).
 */
@Serializable
data class TutoStep(
    val id: String,
    val text: String,
    val textPro: String? = null,
    val why: String? = null,
    val levels: String = "fp",
    val check: String = "manual",
    val spot: String = "",
    val target: String = "",
    val listen: String = "",
    val keep: String = "",
    val lanesFrom: String = "",
    val transpose: Int = 0,
    val song: String = "",
    val demo: String = "",
    val hearMine: Boolean = false,
) {
    fun textFor(level: TutoLevel): String = if (level == TutoLevel.Pro) textPro ?: text else text

    /** A step ticked by hand (« C'est fait » / « J'ai compris »). */
    val manual: Boolean get() = check == "manual"

    /** A step ticked once its example was heard. */
    val listening: Boolean get() = check == "listen"
}

/**
 * Where a user is in a course, kept per project (the app's [TutoStore]): its style and level,
 * the steps done, the step shown ([current], a step id; empty: the first not done). [active]:
 * false once quit (« Reprendre le tuto » brings it back).
 */
@Serializable
data class TutoProgress(
    val style: String,
    val level: TutoLevel = TutoLevel.Facile,
    val done: Set<String> = emptySet(),
    val current: String = "",
    val active: Boolean = true,
) {
    fun encode(): String = Json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /** Null when [text] is missing or unreadable (an older or damaged entry: no tutorial). */
        fun decode(text: String?): TutoProgress? = text?.let { runCatching { json.decodeFromString(serializer(), it) }.getOrNull() }
    }
}
