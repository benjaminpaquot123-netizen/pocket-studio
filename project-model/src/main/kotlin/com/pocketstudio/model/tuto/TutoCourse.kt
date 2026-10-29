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
    /**
     * The course's own lines, by role ([LINE_ROLES]: "bass", "pad", "lead", "acid"), where the
     * assistant's would be too long to write by hand (testers, lot « tuto 5 »: the same 14 notes four
     * times). A phrase of 1, 2, 4 or 8 bars ([lineBars], else the fewest that hold its notes), its
     * notes as "step:Note:length" (16ths from its start, « F2 », « Gb2 »: 41, 42; a chord:
     * "0:F3+Ab3+C4:16"; a 4th field: its strength "0.6", or for the 303 "a" accent, "s" slide), played
     * over the whole clip; its repeated bars are written by « Copier sur les mesures suivantes ».
     * In the 808 styles ([com.pocketstudio.model.StudioDrums.BASS_808]) "bass" is the 808's line.
     */
    val lines: Map<String, String> = emptyMap(),
    /** The bars of a line's phrase, by role (1, 2, 4 or 8), when they are not the fewest that hold its notes. */
    val lineBars: Map<String, Int> = emptyMap(),
    /**
     * The roles whose phrase follows the chords where it repeats (a bass or a riff of one bar over
     * a progression of four): each bar moved along the key's scale from the chord it was written on
     * to its own bar's ([TutoTarget.chordDegrees]); « Copier » writes it so.
     */
    val follow: Set<String> = emptySet(),
    /**
     * The song's feeling, which picks the style's chords for it ([com.pocketstudio.model.StarterWizard.Mood]
     * id: "dark", "happy", "future", "dreamy"); empty: the assistant's own for the style.
     */
    val mood: String = "",
    /** The song's key, when it is not the style's: its root and its scale, "A minor", "F# dorian", "Eb phrygian". */
    val key: String = "",
    /** The chords, one a bar, as degrees of the key in Roman numerals: "i iv", "i VI III VII" (empty: the mood's). */
    val chords: String = "",
    /**
     * The course's own drums, by part ("*": every part, in place of the recipe's lines; "Drop": that
     * part, and its variants « Drop 2 »…, exactly these pads), then by drum role ([DRUM_NAMES]):
     * a cell a step, 16, 32 or 64 of them, "X" strong, "x" medium, "o" soft, "2" "3" "4" a roll,
     * "." nothing. "crash": a hit played once at the part's start, never looped (« crash:once »).
     */
    val drums: Map<String, Map<String, String>> = emptyMap(),
) {
    /** The chapters of [level], each with only its steps of that level. */
    fun chaptersFor(level: TutoLevel): List<TutoChapter> =
        chapters.filter { level.code in it.levels }
            .map { c -> c.copy(title = c.titleFor(level), steps = c.steps.filter { level.code in it.levels }) }
            .filter { it.steps.isNotEmpty() }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        private val cache = ConcurrentHashMap<String, TutoCourse>()

        /** The roles a course's [lines] can write: the assistant's parts, the 303, a second pad and a counter-melody. */
        val LINE_ROLES = setOf("bass", "pad", "lead", "acid", "pad2", "counter")

        /** The part of [drums] every part plays (the recipe's lines replaced). */
        const val ALL_PARTS = "*"

        /** The crash, played once at a part's start. */
        const val CRASH = "crash"

        /**
         * The drum roles a course's [drums] names, as the assistant's ([com.pocketstudio.model.StarterWizard.Role]
         * ids), with the kit's other names for their pads: "openhat" the open hat, "shaker" the second
         * percussion's (the shaker pad), "rim" the percussion's (the rim pad); and [CRASH].
         */
        val DRUM_NAMES: Map<String, String> = com.pocketstudio.model.StarterWizard.Role.entries.associate { it.id to it.id } +
            mapOf("openhat" to "open_hat", "shaker" to "perc2", "rim" to "perc", CRASH to CRASH)

        /** A pattern cell: strong, medium, soft, a roll, nothing. */
        val DRUM_CELLS = setOf('X', 'x', 'o', '2', '3', '4', '.')

        /**
         * The styles with a course written: every style of the assistant that has its file
         * `tutos/<style>.json` (a new course is a new file, nothing else); the others show « Bientôt
         * pour les autres styles ». In the assistant's order.
         */
        val STYLES: List<String> by lazy {
            com.pocketstudio.model.StarterWizard.STYLES.map { it.id }.filter { resource(it) != null }
        }

        private fun resource(style: String): java.net.URL? = TutoCourse::class.java.classLoader?.getResource("tutos/$style.json")

        /** Tests: a course in place of a style's file (null: its file again). */
        internal val testCourses = ConcurrentHashMap<String, TutoCourse>()

        internal fun forTest(style: String, course: TutoCourse?) {
            if (course == null) testCourses.remove(style) else testCourses[style] = course
            TutoTarget.forget(style)
        }

        fun of(style: String): TutoCourse? {
            testCourses[style]?.let { return it }
            if (style !in STYLES) return null
            return cache.getOrPut(style) {
                val stream = TutoCourse::class.java.classLoader?.getResourceAsStream("tutos/$style.json") ?: return null
                parse(stream.bufferedReader(Charsets.UTF_8).use { it.readText() })
            }
        }

        private val ROMAN = listOf("I", "II", "III", "IV", "V", "VI", "VII")
        private val ROOTS = mapOf("C" to 0, "D" to 2, "E" to 4, "F" to 5, "G" to 7, "A" to 9, "B" to 11)

        /** "i iv VI" as degrees (0, 3, 5); null when one is not a numeral. */
        fun degreesOf(chords: String): List<Int>? = chords.split(Regex("[\\s,–-]+")).filter { it.isNotBlank() }
            .map { ROMAN.indexOf(it.trim().uppercase()).takeIf { d -> d >= 0 } ?: return null }

        /** "A minor" as its root (0..11) and scale; null when it is not one. */
        fun keyOf(key: String): Pair<Int, com.pocketstudio.model.Scale>? {
            val (note, scale) = key.trim().split(Regex("\\s+"), limit = 2).takeIf { it.size == 2 } ?: return null
            val letter = ROOTS[note.take(1).uppercase()] ?: return null
            val acc = when (note.drop(1)) { "" -> 0; "#" -> 1; "b" -> -1; else -> return null }
            val sc = com.pocketstudio.model.Scale.entries.firstOrNull { it != com.pocketstudio.model.Scale.Off && it.name.equals(scale.replace(" ", ""), ignoreCase = true) } ?: return null
            return Math.floorMod(letter + acc, 12) to sc
        }

        /** A course from its file's text (the validation reads each file so). */
        fun parse(text: String): TutoCourse = json.decodeFromString(serializer(), text)
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
 * [skip]: what the song misses when « Suivant » leaves this step undone (said before it lets it go).
 * [build]: the pad whose hits accelerate bar after bar in the part the step makes (the Montée's snare).
 * [part]: the part a transition step works in (the fill, the gap and the curve: "Montée" by default;
 * the crash: "Drop,Drop 2"), so a song with other parts (a Couplet before its Refrain) has them too.
 * [length]: how many cells a gap silences (and a fill stops before). A gap's [target]: the drum roles
 * it silences ("kick": the kick alone drops out), empty: every pad.
 * Every field, every detector: docs/tuto/AUTHORING.md.
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
    val skip: String = "",
    val build: String = "",
    val part: String = "",
    /**
     * The cells a "gap" leaves silent at the end of its part (default: a beat, half a beat under 100
     * BPM; 16: a whole bar), and those a "fill" ends before (default: its part's gap step's, else 0).
     */
    val length: Int = 0,
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
    /**
     * The style's drum kit ([TutoKit]): "" not decided yet (the offer to download it may show),
     * [KIT_PACK] its pack's sounds on the pads, [KIT_BUILT_IN] « Plus tard », the built-in kit.
     */
    val kit: String = "",
) {
    fun encode(): String = Json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        const val KIT_PACK = "pack"
        const val KIT_BUILT_IN = "builtin"

        /** Null when [text] is missing or unreadable (an older or damaged entry: no tutorial). */
        fun decode(text: String?): TutoProgress? = text?.let { runCatching { json.decodeFromString(serializer(), it) }.getOrNull() }
    }
}
