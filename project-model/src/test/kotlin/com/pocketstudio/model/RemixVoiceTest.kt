package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Remix lot 1, R2: a song's voice put into the project. The song comes in where the user places
 * it (the drop), a cut never covers a pad the beat plays, cuts get names apart, the voice's
 * phrases are found between its silences, and « Voix » alone stays alone in the project.
 */
class RemixVoiceTest {
    private val song = Remix("abc", "Chanson", songBpm = 120.0, seconds = 40.0)

    /** Intro (scene 0, 8 bars), Montée (scene 1, 4 bars), Drop (scene 2, 8 bars), Drop again. */
    private fun withSong(): Project {
        val intro = Scene("A", DrumPattern(steps = 16), label = "Intro")
        val build = Scene("B", DrumPattern(steps = 16), label = "Montée")
        val drop = Scene("C", DrumPattern(steps = 16), label = "Drop")
        return Project(
            tempoBpm = 120.0, remix = song, scenes = listOf(intro, build, drop), scene = 0,
            song = listOf(SongPart(0, repeats = 8), SongPart(1, repeats = 4), SongPart(2, repeats = 8), SongPart(2, repeats = 8)),
        ).sanitized()
    }

    @Test
    fun theSongComesInWhereItIsPlaced() {
        val p = withSong()
        val rate = 48_000
        val sixteenth = 15.0 / 120.0 * rate  // 6000 frames
        val dropStep = p.songStepOf(2)
        assertEquals(12 * 16, dropStep)
        val placed = p.copy(remix = song.copy(atStep = dropStep))
        // From the song's start: 192 sixteenths of silence first (negative: the engine waits).
        assertEquals(Math.round(-dropStep * sixteenth), placed.remixStemStart(rate, 1.0, 0))
        // From the drop: the song's first frame right away; from a bar into the drop, a bar into the song.
        assertEquals(0L, placed.remixStemStart(rate, 1.0, dropStep))
        assertEquals(Math.round(16 * sixteenth), placed.remixStemStart(rate, 1.0, dropStep + 16))
        // Its own start bar and the copy fitted to the tempo still count (song frames x stretch).
        val later = placed.copy(remix = placed.remix.copy(startBar = 2))
        assertEquals(Math.round(2 * 96_000 * 1.5 - 16 * sixteenth), later.remixStemStart(rate, 1.5, dropStep - 16))
        // Nothing placed: as before, the song's start frame on the first step.
        assertEquals(0L, p.remixStemStart(rate, 1.0, 0))
        // A scene on its own is heard where it first plays in the song; a project without a song at once.
        assertEquals(dropStep, placed.remixFromStepAlone(2))
        assertEquals(0, placed.remixFromStepAlone(0))
        assertEquals(dropStep, placed.remixFromStepAlone(5))
        assertEquals(7, Project(remix = song.copy(atStep = 7)).remixFromStepAlone(0))
    }

    @Test
    fun aLaterSongEndsLater() {
        val p = Project(tempoBpm = 120.0, remix = song)
        // 40 s at 120 BPM: 20 bars, 320 sixteenths.
        assertEquals(320, p.remixSteps())
        assertEquals(20, p.remixEndBars())
        // Coming in on bar 13: it ends on bar 32.
        assertEquals(32, p.copy(remix = song.copy(atStep = 12 * 16)).remixEndBars())
        assertEquals(32, p.copy(remix = song.copy(atStep = 12 * 16)).exportEndBars())
        assertEquals(0, Project(remix = Remix()).remixSteps())
    }

    @Test
    fun thePlaceIsSavedAndOlderProjectsReadWithout() {
        val placed = Project(remix = song.copy(atStep = 192))
        val json = ProjectJson.encode(placed)
        assertEquals(192, ProjectJson.decode(json).remix.atStep)
        // At 0 it is left out: older files read the same, byte for byte.
        assertFalse(ProjectJson.encode(Project(remix = song)).contains("atStep"))
        assertEquals(0, ProjectJson.decode(ProjectJson.encode(Project(remix = song))).remix.atStep)
        assertEquals(0, Remix(atStep = -5).sanitized().atStep)
        assertEquals(Remix.MAX_AT_STEP, Remix(atStep = Int.MAX_VALUE).sanitized().atStep)
    }

    @Test
    fun thePlacesAreTheFirstPartOfEachName() {
        val p = withSong()
        val names = listOf("Intro", "Montée", "Drop")
        val places = RemixVoice.places(p) { names[it] }
        assertEquals(listOf("Intro", "Montée", "Drop"), places.map { it.name })
        assertEquals(listOf(0, 8 * 16, 12 * 16), places.map { it.step })
        assertEquals(listOf(0, 1, 2), places.map { it.index })
        assertEquals(2, RemixVoice.partAt(p, 12 * 16))
        assertEquals(3, RemixVoice.partAt(p, 20 * 16))
        assertEquals(-1, RemixVoice.partAt(p, 28 * 16))
    }

    @Test
    fun aCutNeverCoversAPadTheBeatPlays() {
        // The kick (pad 1) plays in the drop only, the snare has a sound of its own, pad 4 an 808.
        val drop = Scene("B", DrumPattern(steps = 16).toggle(0, 0))
        var p = Project(scenes = listOf(Scene("A", DrumPattern(steps = 16)), drop), scene = 0).sanitized()
        p = p.copy(drums = p.drums.withSample(1, PadSample("a.wav", "Caisse")), padVoices = mapOf(3 to PadVoice(PadVoice.BASS_808)))
        assertFalse("the kick plays in another scene", RemixVoice.padIsFree(p, 0))
        assertFalse(RemixVoice.padIsFree(p, 1))
        assertFalse(RemixVoice.padIsFree(p, 3))
        assertTrue(RemixVoice.padIsFree(p, 2))
        // The kick selected: the cut goes to the first free pad, not on it.
        assertEquals(2, RemixVoice.padFor(p, 0))
        // A free pad selected: that one.
        assertEquals(9, RemixVoice.padFor(p, 9))
        assertEquals((0 until 16).toList() - listOf(0, 1, 3), RemixVoice.freePads(p))
        // Every pad used: no pad (the screen asks first).
        var full = p
        for (lane in 0 until 16) full = full.copy(drums = full.drums.toggle(lane, 4))
        assertNull(RemixVoice.padFor(full, 0))
        assertTrue(RemixVoice.freePads(full).isEmpty())
    }

    @Test
    fun cutsAreNumberedAndKeepTheKitsName() {
        var p = Project()
        assertEquals("Voix 1", RemixVoice.cutName(p, "Voix"))
        p = p.copy(drums = p.drums.withSample(9, PadSample("a.wav", "Voix 1")).withSample(10, PadSample("b.wav", "Voix 3")))
        assertEquals("Voix 4", RemixVoice.cutName(p, "Voix"))
        assertEquals("Batterie 1", RemixVoice.cutName(p, "Batterie"))
        assertTrue(RemixVoice.isCut("Voix 12"))
        assertTrue(RemixVoice.isCut("Voix · Ma chanson préférée"))
        assertFalse(RemixVoice.isCut("Standard · Caisse"))
        assertFalse(RemixVoice.isCut("Voix robot"))
        // Shown translated like the app's other names (« Voice 2 » in English).
        assertEquals("Voix 2", DefaultNames.shownSample("Voix 2"))
    }

    @Test
    fun thePhrasesAreFoundBetweenTheSilences() {
        // 10 ms slices: silence, a 2 s phrase with a short breath inside, 1 s of silence, a click,
        // a 3 s phrase, a quiet hum (bleed) under a tenth of the loudest, a 1 s phrase.
        val peaks = FloatArray(1200)
        fun loud(from: Int, to: Int, v: Float = 0.8f) { for (i in from until to) peaks[i] = v }
        loud(100, 190); loud(200, 300)  // 0.1 s breath at 1.9 s: one phrase
        loud(400, 405)                  // a click: too short
        loud(500, 800, 0.5f)
        loud(800, 1000, 0.05f)          // bleed
        loud(1050, 1150)
        val found = RemixVoice.phrases(peaks, minGapSlices = 35, minSlices = 30, padBefore = 3, padAfter = 12)
        assertEquals(listOf(97 to 312, 497 to 812, 1047 to 1162), found)
        // At most what the free pads take, from the first.
        assertEquals(listOf(97 to 312, 497 to 812), RemixVoice.phrases(peaks, 35, 30, 3, 12, max = 2))
        // Two phrases closer than the gap asked are one.
        assertEquals(1, RemixVoice.phrases(peaks, minGapSlices = 300, minSlices = 30).size)
        // Silence: nothing.
        assertTrue(RemixVoice.phrases(FloatArray(500), 35, 30).isEmpty())
        // The widening never reaches into the next phrase.
        val close = FloatArray(200).also { a -> for (i in 10 until 50) a[i] = 1f; for (i in 90 until 150) a[i] = 1f }
        val (first, second) = RemixVoice.phrases(close, minGapSlices = 35, minSlices = 30, padBefore = 3, padAfter = 60)
        assertEquals(90, first.second)
        assertEquals(87, second.first)
    }

    @Test
    fun theVoiceAloneStaysAlone() {
        // « Voix » alone while listening: the three others come in muted.
        val voice = Remix.stemsHeard(listOf(false, false, false, true))
        assertEquals(listOf(true, true, true, false), voice.map { it.mute })
        // All on, all off or unknown: all heard, as before.
        assertTrue(Remix.stemsHeard(listOf(true, true, true, true)).none { it.mute })
        assertTrue(Remix.stemsHeard(listOf(false, false, false, false)).none { it.mute })
        assertTrue(Remix.stemsHeard(null).none { it.mute })
        // The project heard with the voice alone ends with the voice.
        val p = Project(tempoBpm = 120.0, remix = song.copy(stems = voice))
        assertEquals(20, p.remixEndBars())
    }
}
