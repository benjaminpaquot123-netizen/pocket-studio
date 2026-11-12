package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Step 4e (Benjamin's list n° 16): the first sound with a tempo sets the project's tempo. */
class AutoTempoTest {
    private val blank = Project().sanitized()

    @Test
    fun theFirstLoopSetsTheTempoOfAnEmptyProject() {
        val bpm = AutoTempo.tempoOf(seconds = 8 * 60.0 / 113, known = 113.0)
        assertEquals(113.0, bpm, 0.0)
        val lead = AutoTempo.leadFor(blank, "Boys", bpm)
        assertNotNull(lead)
        val led = AutoTempo.led(blank, lead!!)
        assertEquals(113.0, led.tempoBpm, 0.0)
        assertEquals(TempoLead("Boys", 113.0), led.tempoLead)
        // The second loop never moves it again.
        assertNull(AutoTempo.leadFor(led, "Autre boucle", 90.0))
    }

    @Test
    fun aHitOrAShoutHasNoTempoAndSetsNothing() {
        assertEquals(0.0, AutoTempo.tempoOf(seconds = 0.4, known = 0.0, detected = 128.0), 0.0)  // too short: a hit
        assertEquals(0.0, AutoTempo.tempoOf(seconds = 2.0, detected = 128.0, oneShot = true), 0.0)  // « Go go go! »
        assertEquals(0.0, AutoTempo.tempoOf(seconds = 3.0), 0.0)  // nothing rhythmic found
        assertNull(AutoTempo.leadFor(blank, "Klaxon", 0.0))
    }

    @Test
    fun aDetectedTempoIsMovedOntoWholeBeatsOfTheLoop() {
        // A 2-bar loop at 120 (4 s) the tracker reads at 118.4: 120, the loop lasts 8 whole beats.
        assertEquals(120.0, AutoTempo.tempoOf(seconds = 4.0, detected = 118.4), 1e-9)
        // Too far from whole beats (a phrase cut anywhere): the tracker's own tempo.
        assertEquals(97.0, AutoTempo.tempoOf(seconds = 3.3, detected = 97.0), 1e-9)
    }

    @Test
    fun aProjectWithMusicOrASongKeepsItsTempo() {
        val withDrums = Project(drums = DrumPattern().toggle(0, 0)).sanitized()
        assertTrue(AutoTempo.hasTempo(withDrums))
        assertNull(AutoTempo.leadFor(withDrums, "Boucle", 90.0))
        val song = blank.copy(remix = Remix("s1", "Boys", 113.0, 0.0))
        assertTrue(AutoTempo.hasTempo(song))
        // A song waiting silent on its Remix page (« Dans un nouveau projet ») gives no tempo yet.
        val waiting = blank.copy(remix = Remix("s1", "Boys", 113.0, 0.0, stems = List(Remix.STEMS) { StemMix(mute = true) }))
        assertFalse(AutoTempo.hasTempo(waiting))
        assertNotNull(AutoTempo.leadFor(waiting, "Voix de Boys", 113.0))
    }

    @Test
    fun annulerGivesThePreviousTempoBackAndTheNextSoundSetsNothing() {
        val led = AutoTempo.led(blank, TempoLead("Boys", 113.0))
        val back = AutoTempo.undone(led, blank.tempoBpm)
        assertEquals(120.0, back.tempoBpm, 0.0)
        assertNull(AutoTempo.leadFor(back, "Autre boucle", 90.0))
        assertEquals(TempoLead("Boys", 113.0), back.tempoLead)  // « Revenir au tempo de Boys » stays offered
    }

    @Test
    fun loopsAreStretchedToTheProjectsTempoWithinHalfToTwice() {
        assertEquals(90_400L, AutoTempo.fittedFrames(96_000L, 113.0, 120.0))
        assertTrue(AutoTempo.needsFit(113.0, 120.0))
        assertFalse(AutoTempo.needsFit(120.0, 120.02))
        assertFalse(AutoTempo.canFit(60.0, 130.0))
    }

    @Test
    fun theNewFieldsAreLeftOutOfTheFileAndReadBack() {
        val plain = ProjectJson.encode(blank)
        assertFalse(plain.contains("tempoLead"))
        val clip = LiveClip("a.wav", "Boucle", 8, bpm = 113.0)
        assertTrue(clip.follows)
        assertFalse(clip.copy(oneShot = true).follows)
        val audio = AudioClip("b.wav", "Boucle", seconds = 4.0, bpm = 113.0, fit = true)
        assertTrue(audio.follows)
        assertFalse(audio.copy(fit = false).follows)
        val p = AutoTempo.led(blank, TempoLead("Boys", 113.0)).copy(live = blank.live.with(0, clip))
        val back = ProjectJson.decode(ProjectJson.encode(p))
        assertEquals(TempoLead("Boys", 113.0), back.tempoLead)
        assertEquals(113.0, back.live.clips[0]!!.bpm, 0.0)
        assertFalse(ProjectJson.encode(blank.copy(live = blank.live.with(0, clip.copy(bpm = 0.0)))).contains("\"bpm\""))
    }
}
