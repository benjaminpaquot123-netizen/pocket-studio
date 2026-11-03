package com.pocketstudio.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * Pad Live (Benjamin's n° 25, lot B1) in a project: [home] marks the « Live » project the home
 * screen's « Pad Live » card opens (created the first time, then resumed; it is listed in « Mes
 * projets » like any other), [page] the page of pads shown (A to D), [pack] the last pack put on
 * the pads. Everything is left out of the file at its defaults.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PadLiveSet(
    @EncodeDefault(EncodeDefault.Mode.NEVER) val home: Boolean = false,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val page: Int = 0,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val pack: String = "",
    /** Lot B3: the best stars (0..3) of each challenge played ([PadChallenge.LEVELS], in order). */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val stars: List<Int> = emptyList(),
    /**
     * Lot B3: the pads the last Live take kept (its « Pads live » audio track), as they played: what
     * « Pistes séparées » renders again, one file per pad. Null: no take kept its pads.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val take: PadTakeRecord? = null,
    /**
     * Fix 2, n° 6: the pack loaded on each page (index: page, "" none), so that page B never says
     * page A's. [pack], from older files, stays page A's.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val packs: List<String> = emptyList(),
) {
    /** The pack loaded on [page] ("" none). */
    fun packOf(page: Int): String =
        packs.getOrNull(page)?.takeIf { it.isNotEmpty() } ?: if (page == 0) pack else ""

    /** Pack [id] loaded on [page]. */
    fun withPack(page: Int, id: String): PadLiveSet {
        if (page !in 0 until PadLive.PAGES) return this
        val list = (packs.take(PadLive.PAGES) + List((page + 1 - packs.size).coerceAtLeast(0)) { "" }).toMutableList()
        list[page] = id
        return copy(packs = list)
    }

    /** [page] read safely (a hand-edited file). */
    val shownPage: Int get() = page.coerceIn(0, PadLive.PAGES - 1)

    /** The challenge to play next: the first without two stars (1-based; past the last: the last). */
    val nextChallenge: Int get() {
        val first = PadChallenge.LEVELS.indices.firstOrNull { (stars.getOrNull(it) ?: 0) < 2 } ?: PadChallenge.LEVELS.lastIndex
        return first + 1
    }

    /** [stars] with level [level] (1-based) at [got] if better. */
    fun withStars(level: Int, got: Int): PadLiveSet {
        val i = level - 1
        if (i !in PadChallenge.LEVELS.indices) return this
        val list = stars.take(PadChallenge.LEVELS.size).toMutableList()
        while (list.size <= i) list += 0
        if (got.coerceIn(0, 3) <= list[i]) return this
        list[i] = got.coerceIn(0, 3)
        return copy(stars = list)
    }
}

/**
 * Lot B3: one pad of a Live take as it sounded ([LivePadTake.Span]), kept with the project for
 * « Pistes séparées ». [until] < 0: a hit, its whole sound once.
 */
@Serializable
data class PadSpanRecord(val pad: Int, val from: Double, val until: Double, val velocity: Float = 1f) {
    fun span(): LivePadTake.Span = LivePadTake.Span(pad, from, if (until < 0) Double.POSITIVE_INFINITY else until, oneShot = until < 0, velocity = velocity)

    companion object {
        fun of(s: LivePadTake.Span) = PadSpanRecord(s.pad, s.from, if (s.oneShot || !s.until.isFinite()) -1.0 else s.until, s.velocity)
    }
}

/**
 * Lot B3: the pads a Live take kept on audio track [track] (its « Pads live »): transport steps
 * [start] to [end] at [bpm], each pad's [spans], and the pads' sounds as they were then ([clips],
 * by pad), so that a pad changed since still gives the take's sound.
 */
@Serializable
data class PadTakeRecord(
    val track: Int,
    val start: Int,
    val end: Int,
    val bpm: Double,
    val spans: List<PadSpanRecord> = emptyList(),
    val clips: Map<Int, LiveClip> = emptyMap(),
) {
    /** The pads it played, in order. */
    val pads: List<Int> get() = spans.map { it.pad }.distinct().sorted()

    /** The clips as a list by pad index (what [com.pocketstudio.model.LivePadTake] and the bounce read). */
    fun clipList(): List<LiveClip?> = List(LivePads.ALL) { clips[it] }

    companion object {
        /** At most this many spans are kept (a long set: the rest plays on the track, not in the separate files). */
        const val MAX_SPANS = 4096
    }
}

/** How a pad plays: once per touch (« coup »), a loop a touch starts and the next stops (« boucle »), or a loop while held (« tenir »). */
enum class PadMode { SHOT, LOOP, HOLD }

/** What a touch on a pad does to the engine's slot: toggle it, or nothing. */
enum class PadTouch { TOGGLE, NONE }

/**
 * Pad Live's rules, pure (no Android, no engine): pages, modes, « un seul à la fois » groups,
 * packs by style. The app's [com.pocketstudio.app.padlive] package plays them.
 */
object PadLive {
    const val PAGES = 4
    const val PAGE_SIZE = LivePads.MAX
    val PAGE_NAMES = listOf("A", "B", "C", "D")

    /** A page's rows of pads (lot B2: a row is launched together, on the bar). */
    const val ROWS = 4
    const val ROW_SIZE = 4

    /** What launching a row does: [starting] start on the next bar line, [stopping] stop now. */
    data class RowLaunch(val starting: List<Int>, val stopping: List<Int>)

    /**
     * The row of pads from [first] ([ROW_SIZE] pads) launched, [playing] and [waiting] the pads
     * playing or waiting for their line, as the engine does it (ClipLauncher::launchRow): its loops
     * all on (playing or waiting), they stop; else every pad of it with a sound that is not a loop
     * playing starts on the next bar line (hits too; one waiting moves there).
     */
    fun rowLaunch(pads: LivePads, first: Int, playing: Set<Int>, waiting: Set<Int>): RowLaunch {
        val row = (first until (first + ROW_SIZE).coerceAtMost(LivePads.ALL)).filter { pads.at(it) != null }
        val loops = row.filter { pads.at(it)?.oneShot == false }
        if (loops.isNotEmpty() && loops.all { it in playing || it in waiting }) return RowLaunch(emptyList(), loops)
        return RowLaunch(row.filter { pads.at(it)?.oneShot == true || it !in playing }, emptyList())
    }

    /** « Un seul à la fois » groups a pad can join (0: none). */
    const val GROUPS = 4

    /** The home's « Live » project's name (French, as every name the app gives; shown translated by [DefaultNames]). */
    const val PROJECT_NAME = "Live"

    /** The tempo a new « Live » project starts at (the mockup's). */
    const val TEMPO = 124.0

    fun pad(page: Int, slot: Int): Int = page.coerceIn(0, PAGES - 1) * PAGE_SIZE + slot.coerceIn(0, PAGE_SIZE - 1)
    fun pageOf(pad: Int): Int = (pad / PAGE_SIZE).coerceIn(0, PAGES - 1)

    /** The 16 pads of [page]. */
    fun pageClips(pads: LivePads, page: Int): List<LiveClip?> = pads.all.subList(pad(page, 0), pad(page, 0) + PAGE_SIZE)

    /** How many pads of [page] hold a sound (the page's letter shows it). */
    fun filled(pads: LivePads, page: Int): Int = pageClips(pads, page).count { it != null }

    fun modeOf(clip: LiveClip): PadMode = when {
        clip.oneShot -> PadMode.SHOT
        clip.hold -> PadMode.HOLD
        else -> PadMode.LOOP
    }

    /** [clip] played as [mode] (a « tenir » pad is a loop the finger keeps on). */
    fun withMode(clip: LiveClip, mode: PadMode): LiveClip = when (mode) {
        PadMode.SHOT -> clip.copy(oneShot = true, hold = false, beats = 1)
        PadMode.LOOP -> clip.copy(oneShot = false, hold = false, beats = if (clip.oneShot) LOOP_BEATS else clip.beats)
        PadMode.HOLD -> clip.copy(oneShot = false, hold = true, beats = if (clip.oneShot) 1 else clip.beats)
    }.sanitized()

    /** The beats a one-shot turned into a loop repeats over (a bar). */
    const val LOOP_BEATS = 4

    fun withGroup(clip: LiveClip, group: Int): LiveClip = clip.copy(group = group).sanitized()

    /**
     * A finger [down] (true) or up (false) on [clip], [sounding] (playing or waiting for its beat):
     * a « coup » or a « boucle » toggles on the touch; a « tenir » starts on the touch and stops when
     * the finger leaves.
     */
    fun touch(clip: LiveClip?, down: Boolean, sounding: Boolean): PadTouch = when {
        clip == null -> PadTouch.NONE
        modeOf(clip) == PadMode.HOLD -> if (down != sounding) PadTouch.TOGGLE else PadTouch.NONE
        down -> PadTouch.TOGGLE
        else -> PadTouch.NONE
    }

    /**
     * The pads to stop when [pad] starts: the others of its group that sound ([sounding]), on any
     * page. A pad stopping (it sounds now) stops nothing; a « coup » stops the others too (an open
     * and a closed hi-hat).
     */
    fun choked(pads: LivePads, pad: Int, sounding: Set<Int>): List<Int> {
        val group = pads.at(pad)?.group ?: return emptyList()
        if (group == 0 || pad in sounding) return emptyList()
        return sounding.filter { it != pad && pads.at(it)?.group == group }.sorted()
    }

    /** A new « Live » project: page A of 16 pads, at the [TEMPO], marked as the home's. */
    fun newProject(): Project = Project(name = PROJECT_NAME, tempoBpm = TEMPO).let {
        it.copy(live = it.live.copy(size = PAGE_SIZE), padLive = PadLiveSet(home = true))
    }

    /** [pads] with [page] holding [clips] (one per slot, null: empty) and every other page as it was. */
    fun placed(pads: LivePads, page: Int, clips: List<LiveClip?>): LivePads {
        var out = pads
        for (slot in 0 until PAGE_SIZE) out = out.withAt(pad(page, slot), clips.getOrNull(slot)?.sanitized())
        return if (page == 0) out.copy(size = PAGE_SIZE, starterOffered = true) else out
    }

    /**
     * One sound of a pack that is not a loop of the style: a party sound of the app ([PartySounds]),
     * played as [mode]; [name] is one of the app's own names (shown translated by [DefaultNames]).
     */
    data class Hit(val name: String, val party: String, val mode: PadMode = PadMode.SHOT, val gain: Float = 0.8f)

    /**
     * A pack: [name] its style's name (« Fête » for the ready-to-play set); the loops of a style ([style], rendered at the project's tempo from the style's
     * recipe, one per track, as [LivePadPlan] does for a song) on the first pads, then [hits].
     * [style] null: the ready-to-play set of [LiveStarter] (« Fête »).
     */
    data class Pack(val id: String, val name: String, val style: String?, val hits: List<Hit>)

    /** Where a pack's hits start on its page: the third row (the loops above, under the thumb lower down). */
    const val HITS_FROM = 8

    /** At most this many loops of the style (two rows). */
    const val MAX_LOOPS = HITS_FROM

    val PACKS: List<Pack> = listOf(
        Pack("fete", "Fête", null, emptyList()),
        Pack(
            "techno", "Techno", "techno",
            listOf(Hit("Laser", "laser"), Hit("Montée", "riser"), Hit("Impact", "impact"), Hit("Sirène", "siren_dub", PadMode.HOLD, 0.7f)),
        ),
        Pack(
            "trap", "Trap", "trap",
            listOf(Hit("Klaxon", "air_horn"), Hit("« Go! »", "voice_go", gain = 1f), Hit("Chute", "drop"), Hit("Sirène", "siren_yelp", PadMode.HOLD, 0.7f)),
        ),
        Pack(
            "house", "House", "house",
            listOf(Hit("Sifflet", "whistle"), Hit("« Ready »", "voice_ready", gain = 1f), Hit("Montée", "riser_short"), Hit("Impact", "impact")),
        ),
        Pack(
            "dnb", "Drum & bass", "dnb",
            listOf(Hit("Zap électrique", "zap"), Hit("Montée", "riser"), Hit("« Fire in the hole! »", "voice_fire", gain = 1f), Hit("Sirène", "siren_wail", PadMode.HOLD, 0.7f)),
        ),
    )

    /** The pack a new « Live » project starts with. */
    val DEFAULT_PACK: Pack get() = PACKS.first()

    fun pack(id: String): Pack? = PACKS.firstOrNull { it.id == id }

    /** The clip of hit [hit] on its pad, its sound in [file]. */
    fun hitClip(hit: Hit, file: String): LiveClip = withMode(LiveClip(file, hit.name, 1, oneShot = true, gain = hit.gain), hit.mode)
}
