package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * G0, the « courbes décalées de 16 mesures » of Benjamin's « Jump up DnB »: its song is 216 bars
 * long, not 184 (its Intro and outro scenes are 8 bars: their notes last 128 steps, their beat 64),
 * so its 216-point curves fit it and the second bass opens on the second Drop (bar 96). Every edit
 * of the song's parts keeps each song curve with the part it was drawn in ([followArrangement]).
 */
class SongLanesFollowPartsTest {
    private fun benj() = DropVariationTest.benj()

    /** The value of [lane]'s song curve on [bar] of [p]'s song. */
    private fun Project.at(lane: AutomationLane, bar: Int): Float? {
        val per = lane.pointsPerBar(beatsPerBar)
        return lane.songCurve((songBars + 1) * per).getOrNull(bar * per)
    }

    private fun Project.volume(ch: Int) = automation.first { it.target == AutoTarget(AutoTarget.VOLUME, ch) }

    @Test
    fun benjaminsCurvesFitHisSong() {
        val p = benj()
        assertEquals(216, p.songBars)
        assertEquals(listOf(0, 32, 40, 72, 88, 96, 128, 144, 152, 184), StarterWizard.songSections(p).map { it.start })
        for (lane in p.automation) {
            val points = lane.songValues.size / lane.pointsPerBar(p.beatsPerBar)
            assertTrue("${lane.target}: $points points", points <= p.songBars)
        }
        val drops = DropVariation.drops(p).map { it.start }
        // The bass up to the second Drop, the second bass (« Basse 2 », channel 8) from it on.
        assertTrue(p.at(p.volume(2), drops[1] - 1)!! > 0.5f)
        assertEquals(0f, p.at(p.volume(2), drops[1])!!, 0f)
        assertEquals(0f, p.at(p.volume(8), drops[1] - 1)!!, 0f)
        assertTrue(p.at(p.volume(8), drops[1])!! > 0.5f)
        // The lead out of the first half of the first Drop, in from its second half.
        assertEquals(0f, p.at(p.volume(6), drops[0])!!, 0f)
        assertTrue(p.at(p.volume(6), drops[0] + 16)!! > 0.5f)
    }

    /**
     * After [edit] of [before] (the result [after]), each bar of [after] mapped to a bar of [before]
     * ([map]: -1 for a new bar) reads the same value on every song curve that was there before,
     * except on the [own] targets the edit writes itself.
     */
    private fun assertFollows(name: String, before: Project, after: Project, map: (Int) -> Int, own: Set<AutoTarget> = emptySet()) {
        var checked = 0
        for (lane in before.automation.filter { it.mode == AutomationLane.SONG && it.hasPoints && it.target !in own }) {
            val now = after.automation.firstOrNull { it.target == lane.target } ?: error("$name: ${lane.target} lost")
            for (bar in 0 until after.songBars) {
                val old = map(bar)
                if (old < 0 || old >= before.songBars) continue
                val a = before.at(lane, old) ?: continue
                // A lane emptied by the edit is a loss, not a pass.
                val v = after.at(now, bar) ?: error("$name: ${lane.target} emptied")
                assertEquals("$name ${lane.target} bar $bar (was $old)", a, v, 1e-4f)
                checked++
            }
        }
        assertTrue(name, checked > 0)
    }

    /** Bar [bar] of the song after part [index] became [newBars] bars long (its first bars kept). */
    private fun Project.resized(index: Int, newBars: Int): (Int) -> Int {
        val start = songStepOf(index) / stepsPerBar
        val old = partSteps(song[index]) / stepsPerBar
        return { bar ->
            when {
                bar < start -> bar
                bar < start + newBars -> if (bar - start < old) bar else -1
                else -> bar - newBars + old
            }
        }
    }

    /** Bar [bar] of the song after [bars] new bars were put at [at]. */
    private fun inserted(at: Int, bars: Int): (Int) -> Int = { bar -> if (bar < at) bar else if (bar < at + bars) -1 else bar - bars }

    private fun checkEdits(p: Project, label: String) {
        val bar = { i: Int -> p.songStepOf(i) / p.stepsPerBar }
        val len = { i: Int -> p.partSteps(p.song[i]) / p.stepsPerBar }
        // The Intro's last part (or the first one that repeats) shortened, lengthened, removed.
        val i = p.song.indices.firstOrNull { p.song[it].repeats > 1 } ?: 0
        val cut = p.withSongRepeats(i, 1)
        assertFollows("$label repeats", p, cut, p.resized(i, len(i) / p.song[i].repeats))
        val short = p.withPartBars(i, 4)
        assertFollows("$label withPartBars", p, short, p.resized(i, 4))
        val long = p.lengthenPart(i, 1)
        assertFollows("$label lengthen", p, long, p.resized(i, long.partSteps(long.song[i]) / long.stepsPerBar))
        assertFollows("$label remove", p, p.removeSongPart(0), { b -> b + len(0) })
        assertFollows("$label insert", p, p.insertSongPart(0, p.song[0].scene), inserted(0, len(0)))
        assertFollows("$label duplicate", p, p.duplicateSongPart(0), { b -> if (b < len(0)) b else b - len(0) })
        assertFollows("$label move", p, p.moveSongPart(1, 0), { b -> if (b < len(1)) b + len(0) else if (b < len(0) + len(1)) b - len(1) else b })
        assertFollows("$label split", p, p.splitSongPart(i, 1), { it })
        // The part sheet: a variation, a fill, a Break after, a Montée before, the energy, an ending.
        val drop = p.song.indices.first { p.allScenes[p.song[it].scene].label in DropVariation.LABELS }
        assertFollows("$label variation", p, p.variationOfPart(drop), { it })
        assertFollows("$label fill", p, p.fillAtEnd(i), { it })
        assertFollows("$label break", p, p.breakAfter(i), inserted(bar(i + 1), PartIntents.BARS))
        assertFollows("$label energy", p, p.withPartEnergy(drop, 4), { it }, own = setOf(AutoTarget.live(AutoTarget.LIVE_FILTER)))
        val riser = p.riserTrackId()?.let { setOf(AutoTarget(AutoTarget.INSERT_PARAM, InstrumentTrack.mixerChannel(it), 0, 0)) }.orEmpty()
        assertFollows("$label build-up", p, p.buildUpBefore(drop), { it }, own = riser + AutoTarget.live(AutoTarget.LIVE_BUILD_UP))
        // The ending: the song as it was, then the new part (its fade and its riser on it).
        val end = p.withEnding()
        assertFollows("$label ending", p, end, { b -> if (b < p.songBars) b else -1 }, own = riser + AutoTarget.masterVolume())
    }

    @Test
    fun everyPartEditKeepsTheCurvesWithTheirParts() {
        checkEdits(benj(), "benj")
        for (id in listOf("jump_up", "techno", "house", "pop", "trap")) checkEdits(DropVariationTest.song(StarterWizard.style(id)!!), id)
    }

    @Test
    fun shorteningBenjaminsIntroKeepsTheSwitchOnTheSecondDrop() {
        // Intro 32 → 16 (its first two parts out), outro 32 → 16 (its last two).
        var p = benj().removeSongPart(0).removeSongPart(0)
        p = p.removeSongPart(p.song.lastIndex)
        p = p.removeSongPart(p.song.lastIndex)
        assertEquals(184, p.songBars)
        val drops = DropVariation.drops(p).map { it.start }
        assertEquals(listOf(24, 80, 136), drops)
        assertEquals(0f, p.at(p.volume(8), drops[1] - 1)!!, 0f)
        assertTrue(p.at(p.volume(8), drops[1])!! > 0.5f)
        assertEquals(0f, p.at(p.volume(6), drops[0])!!, 0f)
        assertTrue(p.at(p.volume(6), drops[0] + 16)!! > 0.5f)
    }
}
