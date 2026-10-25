package com.pocketstudio.model

import com.pocketstudio.model.StarterWizard.Part
import com.pocketstudio.model.video.VideoPlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * S6 son: « Écouter » starts about 8 bars before the first Drop (decision 2A), and the live gain is
 * estimated on the fullest Drop, on the songs the assistant really makes.
 */
class FirstListenTest {
    private fun song(id: String): Project {
        val s = StarterWizard.style(id)!!
        val lines = s.lines.filter { !it.off }.associate { it.role to null as PadSample? }
        val parts: Set<Part> = StarterWizard.defaultParts(s)
        return StarterWizard.build(s, lines, s.melody.mood, parts.associateWith { StarterWizard.soundsFor(it, s).first() })
    }

    private fun Project.seconds(steps: Int) = steps * 15.0 / tempoBpm

    @Test
    fun listenStartsAboutEightBarsBeforeTheFirstDropInEveryClubStyle() {
        for (style in StarterWizard.STYLES) {
            val p = song(style.id)
            val main = FirstListen.firstMain(p)
            if (main < 0) continue
            val start = FirstListen.startPart(p)
            assertTrue("${style.id}: starts before or on the Drop", start in 0..main)
            val bars = (p.songStepOf(main) - p.songStepOf(start)) / p.stepsPerBar
            // On a part's start, as near 8 bars as the parts allow (a Montée of 4 + 4, of 8, a Groove of 16…).
            if (main > 0) assertTrue("${style.id}: $bars bars before its first Drop", bars in 2..16)
            // The Drop comes within half a minute of « Écouter », whatever the form (2 min of intro in techno).
            assertTrue("${style.id}: Drop after ${p.seconds(p.songStepOf(main) - p.songStepOf(start))} s", p.seconds(p.songStepOf(main) - p.songStepOf(start)) <= 30.0)
        }
    }

    @Test
    fun aTechnoListenSkipsItsLongIntro() {
        val p = song("techno")
        val main = FirstListen.firstMain(p)
        assertEquals("Drop", VideoPlanner.baseSection(p.sceneLabel(p.song[main].scene)))
        // Its Drop is more than a minute and a half in: « Écouter » does not play all that.
        assertTrue(p.seconds(p.songStepOf(main)) > 90.0)
        val start = FirstListen.startPart(p)
        assertTrue(FirstListen.startsLater(p))
        assertEquals(FirstListen.BARS_BEFORE_DROP.toDouble(), (p.songStepOf(main) - p.songStepOf(start)).toDouble() / p.stepsPerBar, 4.0)
    }

    @Test
    fun aSongWithoutDropOrWhoseDropComesFirstPlaysFromItsStart() {
        val p = song("techno")
        val drop = FirstListen.firstMain(p)
        // Only its Drop: nothing before it.
        val only = p.copy(song = listOf(p.song[drop]))
        assertEquals(0, FirstListen.startPart(only))
        assertFalse(FirstListen.startsLater(only))
        // A long part before the Drop (32 bars of Groove): « Écouter » still starts on it, never on the Drop.
        val longBefore = p.copy(song = listOf(p.song[drop - 1].copy(bars = 32), p.song[drop]))
        assertEquals(0, FirstListen.startPart(longBefore))
        // No section name at all: from the start.
        assertEquals(0, FirstListen.startPart(Project()))
    }

    @Test
    fun theLiveGainIsEstimatedOnTheFullestDropForEightToSixteenBars() {
        for (id in listOf("techno", "house", "jump_up", "trap")) {
            val p = song(id)
            val i = FirstListen.loudPart(p)
            assertTrue("$id: a part", i in p.song.indices)
            val base = VideoPlanner.baseSection(p.sceneLabel(p.song[i].scene))
            assertTrue("$id: $base", base == "Drop" || base == "Refrain")
            // The fullest of them.
            val same = p.song.filter { VideoPlanner.baseSection(p.sceneLabel(it.scene)) == base }
            assertEquals(same.maxOf { VideoPlanner.energy(p, it) }, VideoPlanner.energy(p, p.song[i]))
            val bars = FirstListen.loudSteps(p, i) / p.stepsPerBar
            assertTrue("$id: $bars bars", bars in FirstListen.MIN_LOUD_BARS..FirstListen.MAX_LOUD_BARS)
        }
        assertEquals(-1, FirstListen.loudPart(Project()))
    }
}
