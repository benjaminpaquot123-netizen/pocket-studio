package com.pocketstudio.model.guide

import com.pocketstudio.model.DefaultNames
import com.pocketstudio.model.Project
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.instrumentTracks

/**
 * G5 (docs/PLAN-GUIDE.md §5, « Fusion des anciennes surfaces »): how a song starts in the Guide.
 * « Ton morceau est prêt » becomes the first card of a song the assistant just made ([SONG]); the
 * Missions « Mon premier morceau » become the first cards of a first song ([FIRST]); the old
 * « Prochaine étape » of a song made by hand becomes its first cards (detectors [beat], [bassline],
 * [tune]). The cards are in `guides/common.json` ([GuideLibrary.COMMON]).
 */
object GuideStart {
    /** A song the assistant (or a style in one touch) just made. */
    const val SONG = "song"

    /** « Mon premier morceau »: the old Missions, as cards. */
    const val FIRST = "first"

    /** What [signature] reads: the sounds, the notes, the parts and curves. */
    const val SOUND = "sound"
    const val NOTES = "notes"
    const val ARRANGE = "arrange"

    /**
     * [p] starting in the Guide as [mode] ([SONG] or [FIRST]), with its song guide [pages] when the
     * assistant wrote one: what its sounds, notes and parts are now, so that « Change un son » is done
     * once they change. A song already on [FIRST] stays on it (and keeps its marks).
     */
    fun begin(p: Project, mode: String, pages: List<GuidePageText> = p.guide.songGuide): Project {
        val g = p.guide
        if (g.start == FIRST && mode == FIRST) return if (pages == g.songGuide) p else p.copy(guide = g.copy(songGuide = pages).sanitized())
        val marks = mapOf(SOUND to signature(p, SOUND), NOTES to signature(p, NOTES), ARRANGE to signature(p, ARRANGE))
        return p.copy(guide = g.copy(start = mode, startMarks = marks, songGuide = pages).sanitized())
    }

    /** What [p] looks like for [what] ([SOUND], [NOTES], [ARRANGE]): the Missions' own signatures. */
    fun signature(p: Project, what: String): Int = when (what) {
        SOUND -> listOf(
            p.tracks.map { listOf(it.synth, it.plaits, it.engine, it.instrument, it.multi, it.sampled, it.layers) },
            p.acid.sound, p.kitVersion, p.drums.lanes.map { it.sample },
        ).hashCode()
        NOTES -> p.tracks.sumOf { t -> t.notes.notes.size } + p.allScenes.sumOf { s -> s.synthNotes.notes.size + s.trackNotes.values.sumOf { it.notes.size } }
        ARRANGE -> listOf(p.song, p.automation.size).hashCode()
        else -> 0
    }

    /** [what] changed since the song started ([GuideState.startMarks]); false when it was never marked. */
    fun changed(p: Project, what: String): Boolean = p.guide.startMarks[what]?.let { it != signature(p, what) } ?: false

    /**
     * The cards a user mid-mission had reached (the app's old « missions » settings: [step] the
     * Mission's index, [skipped] those passed over), on [p] that becomes a first song: the steps
     * before [step] decided (or skipped), the others to do. Mission order: Style, Listen, Sound,
     * Melody, Arrange, Share.
     */
    fun fromMissions(p: Project, step: Int, skipped: Set<Int>, at: Long): Project {
        var q = begin(p, FIRST)
        var g = q.guide
        MISSION_CARDS.forEachIndexed { i, card ->
            if (i < step) g = g.withStatus(card, if (i in skipped) GuideStatus.SKIPPED else GuideStatus.DONE, at)
        }
        q = q.copy(guide = g)
        return q
    }

    /**
     * The song's start waits in the Guide: « Ton morceau est prêt » not decided yet (or, a first song
     * with no style, « Choisis un style »): the app opens the Guide's panel on it by itself.
     */
    fun welcomes(p: Project): Boolean {
        val g = p.guide
        if (g.start.isEmpty()) return false
        val card = if (p.style != null) READY else if (g.start == FIRST) "start.style" else return false
        return g.statusOf(card) == GuideStatus.TODO && !g.refused(card)
    }

    /** « Ton morceau est prêt »: the first card of a song just made. */
    const val READY = "start.ready"

    /** The card each Mission became, in their order. */
    val MISSION_CARDS = listOf("start.style", "start.ready", "start.sound", "start.melody", "start.arrange", "share.ready")

    // --- the first steps of a song made by hand (the old « Prochaine étape ») -----------------------

    /** Notes below this pitch (C3) make a track a bass line. */
    private const val BASS_BELOW = 48

    /** Drum hits in some scene. */
    fun beat(p: Project): Boolean = p.allScenes.any { s -> s.drums.lanes.any { lane -> lane.hits.keys.any { it < s.drums.steps } } }

    private fun noted(p: Project): List<Pair<String, List<Int>>> {
        val scenes = p.allScenes
        return p.instrumentTracks.map { t ->
            val notes = scenes.flatMap { s -> (if (t.id == 0) s.synthNotes else s.trackNotes[t.id])?.notes.orEmpty() }
            t.name to notes.map { it.pitch }
        }.filter { it.second.isNotEmpty() }
    }

    private fun isBass(name: String, pitches: List<Int>): Boolean {
        // A default name in any language reads as its French ("Bas" → "Basse").
        val lower = (DefaultNames.french(name) ?: name).lowercase()
        if ("bass" in lower || "808" in lower) return true
        val sorted = pitches.sorted()
        return sorted[sorted.size / 2] < BASS_BELOW
    }

    /** A bass line: the acid line, or a low (or bass-named) instrument track with notes. */
    fun bassline(p: Project): Boolean =
        p.allScenes.any { s -> s.acid.steps.take(s.acid.length).any { it.gate } } || noted(p).any { (n, pitches) -> isBass(n, pitches) }

    /** A tune: a higher instrument track with notes, or a recording. */
    fun tune(p: Project): Boolean = noted(p).any { (n, pitches) -> !isBass(n, pitches) } || p.audioTracks.any { it.clip != null }

    /** Two parts with something in them. */
    fun parts(p: Project): Int = p.allScenes.count { !it.isEmpty }
}
