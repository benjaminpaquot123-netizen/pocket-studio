package com.pocketstudio.model.guide

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Where the Guide is in a project (docs/PLAN-GUIDE.md §4.1), saved with the project so that it
 * survives a save, an export to .zip and its import, and a version restored: the parcours it
 * follows, the card it is at, each card's status with its date and the option kept, the refusals
 * never offered again, what changed on each track (with the value before, to bring it back), and
 * Benjamin's free notes. Every field has a default and unknown keys are ignored: a newer app's
 * state reads in an older one. An empty state ([isEmpty]) is left out of the project file.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class GuideState(
    /** This state's own format ([VERSION]): a later one that changes a field's meaning migrates in [sanitized]. */
    val version: Int = VERSION,
    /** The parcours followed ([GuideParcours.style]: a style id, or [GuideParcours.GENERIC]) and its version. */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val parcours: String = "",
    @EncodeDefault(EncodeDefault.Mode.NEVER) val parcoursVersion: Int = 0,
    /** The card the Guide was at when it was last used (« Tu en étais à… »); empty: none yet. */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val step: String = "",
    /** Each card's status, by card id (a card never touched is not here: [GuideStatus.TODO]). */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val cards: Map<String, GuideCardState> = emptyMap(),
    /** What Benjamin said no to: never offered again (a whole card, or one of its options). */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val refusals: List<GuideRefusal> = emptyList(),
    /** What changed, oldest first, each on one track ([GuideChange.track]): the per-track history. */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val history: List<GuideChange> = emptyList(),
    /** Benjamin's own notes (« pas de réverbe sur la basse, c'est voulu »). */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val notes: List<GuideNote> = emptyList(),
    /**
     * The signature of each track's chain when the Guide first looked at it (track key → hash of its
     * inserts and sends): a chain changed since was decided elsewhere, in the mixer.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val seen: Map<String, Int> = emptyMap(),
    /** The song's last measures (the engine's loudness meter, an offline render), when the app took some. */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val measures: GuideMeasures? = null,
    /** When the Guide was last used (ms since 1970, 0: never): « des jours après » is told from it. */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val lastUsed: Long = 0L,
    /** The number the next group of changes takes ([GuideChange.id] "c<n>"): ids are never reused. */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val nextChange: Int = 0,
) {
    /** Nothing to keep: the project file leaves the field out. */
    val isEmpty: Boolean get() = this == EMPTY

    fun statusOf(card: String): GuideStatus = cards[card]?.status ?: GuideStatus.TODO

    /** Card [card] refused for good (never offered again). */
    fun refused(card: String): Boolean = refusals.any { it.card == card && it.option.isEmpty() }

    /** Option [option] of card [card] refused (the card offers its other options). */
    fun refused(card: String, option: String): Boolean = refusals.any { it.card == card && (it.option.isEmpty() || it.option == option) }

    /** The changes of group [id] (one option applied), in the order they were made. */
    fun group(id: String): List<GuideChange> = history.filter { it.id == id }

    /** The changes made on track [track] (a [GuideChange.track] key), newest first. */
    fun historyOf(track: String): List<GuideChange> = history.filter { it.track == track }.asReversed()

    /** Card [card] at [status] (its option kept), dated [at]. */
    fun withStatus(card: String, status: GuideStatus, at: Long, option: String = ""): GuideState =
        copy(cards = if (status == GuideStatus.TODO) cards - card else cards + (card to GuideCardState(status, at, option)), lastUsed = maxOf(lastUsed, at))

    fun withRefusal(card: String, option: String, at: Long): GuideState =
        if (refused(card, option) && (option.isNotEmpty() || refused(card))) this
        else copy(refusals = (refusals + GuideRefusal(card, option, at)).takeLast(MAX_REFUSALS))

    /** [changes] added to the history (the oldest go first past [MAX_HISTORY] entries or [MAX_HISTORY_CHARS]). */
    fun withChanges(changes: List<GuideChange>): GuideState =
        if (changes.isEmpty()) this else copy(history = capped(history + changes))

    fun withNote(text: String, at: Long, track: String = ""): GuideState {
        val t = text.trim().take(MAX_NOTE)
        if (t.isEmpty()) return this
        return copy(notes = (notes + GuideNote(t, at, track)).takeLast(MAX_NOTES), lastUsed = maxOf(lastUsed, at))
    }

    /** Every list and text inside its limits; the state itself when that changes nothing. */
    fun sanitized(): GuideState {
        if (isClean()) return this
        return copy(
            version = VERSION,
            parcours = parcours.take(MAX_ID),
            parcoursVersion = parcoursVersion.coerceAtLeast(0),
            step = step.take(MAX_ID),
            cards = cards.filterKeys { it.isNotBlank() && it.length <= MAX_ID }.entries.take(MAX_CARDS).associate { (k, v) -> k to v.sanitized() },
            refusals = refusals.filter { it.card.isNotBlank() }.map { it.sanitized() }.distinct().takeLast(MAX_REFUSALS),
            history = capped(history.map { it.sanitized() }),
            notes = notes.map { it.sanitized() }.filter { it.text.isNotEmpty() }.takeLast(MAX_NOTES),
            seen = seen.filterKeys { it.isNotBlank() && it.length <= MAX_ID }.entries.take(MAX_CARDS).associate { it.key to it.value },
            measures = measures?.sanitized(),
            lastUsed = lastUsed.coerceAtLeast(0L),
            nextChange = nextChange.coerceAtLeast(0),
        )
    }

    private fun isClean(): Boolean = version == VERSION && parcours.length <= MAX_ID && parcoursVersion >= 0 && step.length <= MAX_ID &&
        cards.size <= MAX_CARDS && cards.all { (k, v) -> k.isNotBlank() && k.length <= MAX_ID && v.sanitized() == v } &&
        refusals.size <= MAX_REFUSALS && refusals.all { it.card.isNotBlank() && it.sanitized() == it } && refusals.distinct().size == refusals.size &&
        history.size <= MAX_HISTORY && history.all { it.isClean() } && history.sumOf { it.chars } <= MAX_HISTORY_CHARS &&
        notes.size <= MAX_NOTES && notes.all { it.text.isNotEmpty() && it.sanitized() == it } &&
        seen.size <= MAX_CARDS && seen.keys.all { it.isNotBlank() && it.length <= MAX_ID } &&
        (measures == null || measures.sanitized() == measures) && lastUsed >= 0L && nextChange >= 0

    companion object {
        const val VERSION = 1
        val EMPTY = GuideState()
        const val MAX_ID = 64
        const val MAX_CARDS = 256
        const val MAX_REFUSALS = 256
        const val MAX_HISTORY = 400
        /** The history's size on disk at most, about (its values' JSON): a few tens of kB in a project file. */
        const val MAX_HISTORY_CHARS = 48_000

        /** [list]'s newest entries within [MAX_HISTORY] and [MAX_HISTORY_CHARS]. */
        private fun capped(list: List<GuideChange>): List<GuideChange> {
            var total = 0
            var from = list.size
            while (from > 0 && list.size - from < MAX_HISTORY) {
                val next = total + list[from - 1].chars
                if (next > MAX_HISTORY_CHARS) break
                total = next
                from--
            }
            if (from == 0) return list
            // A group goes whole: half of it could not be brought back.
            while (from < list.size && list[from].id.isNotEmpty() && list[from].id == list[from - 1].id) from++
            return list.subList(from, list.size).toList()
        }
        const val MAX_NOTES = 100
        const val MAX_NOTE = 500
    }
}

/** A card's status in the plan (« faite ✓, sautée ⟶, à revoir ↺ »). Unknown values read as [TODO]. */
@Serializable
enum class GuideStatus {
    /** Still to decide. */
    TODO,
    /** Decided from the Guide (an option applied or kept). */
    DONE,
    /** Passed over on purpose: not offered again by itself, still in the plan. */
    SKIPPED,
    /** « Plus tard »: offered again once the others are decided. */
    LATER,
    /** Done without the Guide (in the editor): the analysis saw it and ticked it. */
    DONE_ELSEWHERE,
    ;

    /** Nothing more to do about it (it is not offered as the next card). */
    val settled: Boolean get() = this == DONE || this == SKIPPED || this == DONE_ELSEWHERE

    /** It counts as done in the progress. */
    val done: Boolean get() = this == DONE || this == DONE_ELSEWHERE
}

@Serializable
data class GuideCardState(
    val status: GuideStatus = GuideStatus.TODO,
    /** When it was set (ms since 1970). */
    val at: Long = 0L,
    /** The option kept ([GuideProposal.id]), empty for none. */
    val option: String = "",
) {
    fun sanitized(): GuideCardState = copy(at = at.coerceAtLeast(0L), option = option.take(GuideState.MAX_ID))
}

/** A refusal: card [card] (with [option] empty) or one of its options, never offered again. */
@Serializable
data class GuideRefusal(val card: String = "", val option: String = "", val at: Long = 0L) {
    fun sanitized(): GuideRefusal = copy(card = card.take(GuideState.MAX_ID), option = option.take(GuideState.MAX_ID), at = at.coerceAtLeast(0L))
}

/**
 * One change on one track, with what it was before and what it became, so that it can be brought
 * back in a gesture (« Ramener »). [track]: the track's key ([GuideTracks.key]: "inst:3", "pad:0",
 * "drums", "acid", "master", "bus:0"; a layer later "inst:3/layer:1"). [kind]: what changed
 * ([SOUND], [LAYER], [INSERT], [VALUE], [LANE], [PART]); [field] which of its settings ("inserts",
 * "slot:2", "send:0", "pan", "volume", "muted:<part>:<scene>", "laneTarget"…, one setting each: see
 * [GuideActions]); [before] and [after] its values as JSON (null: none).
 * [origin]: [GUIDE] (a card applied) or [USER] (seen in the editor). [card]: the card it came from.
 */
@Serializable
data class GuideChange(
    val track: String = "",
    val kind: String = VALUE,
    val field: String = "",
    val before: JsonElement? = null,
    val after: JsonElement? = null,
    val at: Long = 0L,
    val origin: String = GUIDE,
    val card: String = "",
    /** Undone since (« Ramener » or « Annuler mon choix »): kept, shown crossed out. */
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER) val reverted: Boolean = false,
    /** The group it belongs to (every setting one option changed: brought back together), "c<n>". */
    val id: String = "",
) {
    /** About what it takes in the project file. */
    val chars: Int by lazy { 80 + (before?.toString()?.length ?: 0) + (after?.toString()?.length ?: 0) }

    internal fun isClean(): Boolean = track.length <= GuideState.MAX_ID && kind.length <= 16 && field.length <= GuideState.MAX_ID && at >= 0L &&
        (origin == USER || origin == GUIDE) && card.length <= GuideState.MAX_ID && id.length <= 16

    fun sanitized(): GuideChange = copy(
        id = id.take(16),
        track = track.take(GuideState.MAX_ID),
        kind = kind.take(16),
        field = field.take(GuideState.MAX_ID),
        at = at.coerceAtLeast(0L),
        origin = if (origin == USER) USER else GUIDE,
        card = card.take(GuideState.MAX_ID),
    )

    companion object {
        const val SOUND = "sound"
        const val LAYER = "layer"
        const val INSERT = "insert"
        const val VALUE = "value"
        const val LANE = "lane"
        const val PART = "part"
        const val GUIDE = "guide"
        const val USER = "user"
    }
}

/** A free note, on the song ([track] empty) or on one track. */
@Serializable
data class GuideNote(val text: String = "", val at: Long = 0L, val track: String = "") {
    fun sanitized(): GuideNote = copy(text = text.trim().take(GuideState.MAX_NOTE), at = at.coerceAtLeast(0L), track = track.take(GuideState.MAX_ID))
}

/**
 * The song as last measured by the app (not by the analysis, which reads the project only): its
 * integrated loudness and true peak, and the short-term loudness of each section kind
 * ([GuideSections.Kind] name → LUFS). [at]: when, to tell a measure older than the last edit.
 * G4: each track's loudness heard alone in the first peak ([tracks], [GuideTrack.key] → LUFS), what
 * the mix loses folded to mono ([monoLossDb], LU, 0 = nothing), what a phone's speaker keeps of the
 * kick and the basses ([phoneDb]: their energy over 300 Hz against their own, dB, nearer 0 is
 * better), and the sound the measures were taken on ([signature], [GuideSignatures.sound]): another
 * one means the song changed since, and the measures are left aside.
 */
@Serializable
data class GuideMeasures(
    val at: Long = 0L,
    val integratedLufs: Float? = null,
    val truePeakDb: Float? = null,
    val sections: Map<String, Float> = emptyMap(),
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER) val tracks: Map<String, Float> = emptyMap(),
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER) val monoLossDb: Float? = null,
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER) val phoneDb: Float? = null,
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER) val signature: Int = 0,
    /**
     * G4 review: only the tracks' levels follow the song as it is (a balance moved the faders): its
     * loudness, sections and mono were cleared, the app measures them again.
     */
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER) val partial: Boolean = false,
) {
    fun sanitized(): GuideMeasures = copy(
        at = at.coerceAtLeast(0L),
        integratedLufs = integratedLufs?.takeIf { it.isFinite() }?.coerceIn(-90f, 10f),
        truePeakDb = truePeakDb?.takeIf { it.isFinite() }?.coerceIn(-90f, 24f),
        sections = sections.filterValues { it.isFinite() }.mapValues { it.value.coerceIn(-90f, 10f) }.entries.take(16).associate { it.key.take(16) to it.value },
        tracks = tracks.filterValues { it.isFinite() }.mapValues { it.value.coerceIn(-90f, 10f) }.entries.take(48).associate { it.key.take(GuideState.MAX_ID) to it.value },
        monoLossDb = monoLossDb?.takeIf { it.isFinite() }?.coerceIn(0f, 60f),
        phoneDb = phoneDb?.takeIf { it.isFinite() }?.coerceIn(-90f, 0f),
    )
}
