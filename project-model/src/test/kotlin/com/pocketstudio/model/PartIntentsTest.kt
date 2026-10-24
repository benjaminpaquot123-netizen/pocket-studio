package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** The part sheet's intents (phase 34): each writes ordinary scenes, parts and song curves. */
class PartIntentsTest {
    private val kick = 0
    private val snare = 2
    private val hat = 5

    /**
     * Scenes A (Intro) and B (Drop), one bar each, with a kick, a snare, a hat, a bass (track 1)
     * and a pad (track 2); the song Intro ×4, Drop ×4, Intro ×2 (10 bars).
     */
    private fun song(): Project {
        var p = Project().sanitized().addTrack("Basse")!!.addTrack("Nappe")!!
        val drums = DrumPattern().toggle(kick, 0).toggle(kick, 8).toggle(snare, 4).toggle(snare, 12).toggle(hat, 2)
        p = p.copy(drums = drums)
            .withTrack(p.track(1).copy(notes = NoteClip(notes = listOf(Note(0, 24, 36)))))
            .withTrack(p.track(2).copy(notes = NoteClip(notes = listOf(Note(0, 96, 60)))))
        p = p.sanitized().renameScene(0, "Intro").copySceneTo(1).renameScene(1, "Drop")
        return p.copy(song = listOf(SongPart(0, 4), SongPart(1, 4), SongPart(0, 2))).sanitized()
    }

    private fun Project.lane(t: AutoTarget) = automation.first { it.target == t }

    @Test
    fun theBassAndThePadsAreToldApart() {
        val p = song()
        assertEquals(listOf(1), p.bassTrackIds())
        assertEquals(listOf(2), p.padTrackIds())
        assertEquals(setOf(SongPart.ACID, SongPart.inst(1)), p.bassKeys())
        assertNull(p.riserTrackId())
    }

    @Test
    fun aVariationIsTheSceneCopiedForThisPartOnly() {
        val p = song()
        assertEquals(2, p.partPlayedTimes(2))
        val v = p.variationOfPart(2)
        val slot = v.song[2].scene
        assertNotEquals(0, slot)
        assertEquals(0, v.song[0].scene)
        assertEquals("Intro 2", v.sceneLabel(slot))
        assertEquals(1, v.partPlayedTimes(2))
        // Shown for editing, the same notes as the original.
        assertEquals(slot, v.scene)
        assertEquals(p.allScenes[0].drums.lanes.map { it.hits }, v.allScenes[slot].drums.lanes.map { it.hits })
        // Editing it leaves the Intro as it was.
        val edited = v.copy(drums = v.drums.toggle(hat, 6)).sanitized()
        assertEquals(p.allScenes[0].drums.lanes[hat].hits, edited.allScenes[0].drums.lanes[hat].hits)
        // A second variation is numbered on.
        assertEquals("Intro 3", v.variationOfPart(0).sceneLabel(v.variationOfPart(0).song[0].scene))
        assertEquals(p.song, ProjectJson.decode(ProjectJson.encode(p)).song)
    }

    @Test
    fun aFillTakesTheLastBarOnTheDrumsOnly() {
        val p = song()
        val f = p.fillAtEnd(1, "Techno")
        assertEquals(4, f.song.size)
        assertEquals(p.songBars, f.songBars)
        assertEquals(SongPart(1, 3), f.song[1])
        val last = f.song[2]
        assertEquals(1, last.bars)
        assertEquals(1, last.scene)
        val fill = last.play.getValue(SongPart.DRUMS)
        assertEquals(PartIntents.FILL_LABEL, f.sceneLabel(fill))
        // The other tracks carry on into their fourth bar.
        assertEquals(3, last.fromBars(SongPart.ACID))
        assertEquals(3, last.fromBars(SongPart.inst(1)))
        assertEquals(0, last.fromBars(SongPart.DRUMS))
        // The kick stays; a snare roll climbs over the second half, its last two hits doubled and rising.
        val d = f.allScenes[fill].drums
        assertEquals(16, d.steps)
        assertEquals(setOf(0, 8), d.lanes[kick].hits.keys)
        val roll = d.lanes[snare].hits
        // The first half keeps its own snare (step 4).
        assertTrue(4 in roll && roll.keys.count { it >= 8 } >= 5)
        assertEquals(2, roll.getValue(15).repeats)
        assertEquals(1, roll.getValue(15).ramp)
        assertTrue(roll.getValue(15).velocity > roll.getValue(8).velocity)
        assertTrue(f.isFillPart(last))
        // Already ending on a fill: nothing more.
        assertSame(f, f.fillAtEnd(2))
        // Saved and read back.
        val back = ProjectJson.decode(ProjectJson.encode(f))
        assertEquals(f.song, back.song)
        assertEquals(d.lanes[snare].hits, back.allScenes[fill].drums.lanes[snare].hits)
    }

    @Test
    fun eachFamilyHasItsFill() {
        assertEquals(PartIntents.Fill.Snare, PartIntents.fillOf("House"))
        assertEquals(PartIntents.Fill.Hats, PartIntents.fillOf("Hip-hop"))
        assertEquals(PartIntents.Fill.Toms, PartIntents.fillOf(null))
        val bar = DrumPattern().toggle(kick, 0)
        val toms = PartIntents.withFill(bar, 16, PartIntents.Fill.Toms)
        assertTrue(toms.lanes[9].hits.isNotEmpty() && toms.lanes[7].hits.isNotEmpty())
        assertTrue((toms.lanes[9].hits.keys.max()) < (toms.lanes[7].hits.keys.min()))
        val hats = PartIntents.withFill(bar, 16, PartIntents.Fill.Hats)
        assertEquals((8..15).toSet(), hats.lanes[hat].hits.keys)
        assertEquals(2, hats.lanes[hat].hits.getValue(15).repeats)
        // A one-bar part: the whole part plays the fill's scene on the drums.
        val one = song().copy(song = listOf(SongPart(0), SongPart(1))).sanitized().fillAtEnd(0)
        assertEquals(2, one.song.size)
        assertTrue(one.isFillPart(one.song[0]))
    }

    @Test
    fun aBreakHasNeitherKickNorBass() {
        val b = song().breakAfter(1)
        assertEquals(4, b.song.size)
        val part = b.song[2]
        assertEquals(PartIntents.BREAK_LABEL, b.sceneLabel(part.scene))
        assertEquals(4, b.partSteps(part) / 16)
        val d = b.allScenes[part.scene].drums
        assertTrue(d.lanes[kick].hits.isEmpty())
        assertEquals(setOf(4, 12), d.lanes[snare].hits.keys)
        assertTrue(SongPart.ACID in part.muted && SongPart.inst(1) in part.muted)
        assertTrue(part.plays(SongPart.inst(2)) && part.plays(SongPart.DRUMS))
        // The Drop it comes from keeps its kick; 8 bars on demand.
        assertEquals(setOf(0, 8), b.allScenes[1].drums.lanes[kick].hits.keys)
        assertEquals(8, song().breakAfter(1, 8).let { it.partSteps(it.song[2]) / 16 })
    }

    @Test
    fun aBuildUpRisesOverTheBarsBeforeThePart() {
        val p = song()
        assertSame(p, p.buildUpBefore(0))
        val m = p.buildUpBefore(1)
        val lane = m.lane(AutoTarget.live(AutoTarget.LIVE_BUILD_UP))
        assertEquals(AutomationLane.SONG, lane.mode)
        assertTrue(lane.songOnly)
        val curve = lane.songCurve(10)
        assertEquals(0f, curve[0], 1e-6f)
        assertTrue(curve[1] > 0f && curve[2] > curve[1])
        assertEquals(PartIntents.BUILD_UP_TOP, curve[3], 1e-6f)
        // Dropped on the part's first bar, with a jump (no slide down through the last bar).
        assertEquals(0f, curve[4], 1e-6f)
        assertEquals(PartIntents.BUILD_UP_TOP, lane.songValueAtStep(3 * 16 + 15, 10, 16)!!, 1e-6f)
        // Done twice: the same curve, one lane.
        assertEquals(m.automation, m.buildUpBefore(1).automation)
    }

    @Test
    fun theRiserOpensWhenThePartBeforeHoldsIt() {
        var p = song().addTrack(StarterWizard.RISER_TRACK)!!
        val riser = p.riserTrackId()!!
        val ch = InstrumentTrack.mixerChannel(riser)
        p = p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, c ->
            if (i == ch) c.copy(inserts = listOf(Insert(EffectType.FILTER.ordinal, listOf(0f, 0.2f, 0f, 1f))) + List(Mixer.INSERTS - 1) { Insert() }) else c
        }))
        p = p.withTrack(p.track(riser).copy(notes = NoteClip(notes = listOf(Note(0, 16 * NoteClip.TICKS_PER_STEP, StarterWizard.RISER_PITCH))))).sanitized()
        val m = p.buildUpBefore(1)
        val curve = m.lane(AutoTarget(AutoTarget.INSERT_PARAM, ch, 0, 0)).songCurve(10)
        assertEquals(0.9f, curve[3], 1e-6f)
        assertEquals(1f, curve[4], 1e-6f)
        assertEquals(0f, curve[5], 1e-6f)
    }

    @Test
    fun fadesWriteTheWholeMixVolumeOverThePart() {
        val p = song().fadeIn(0)
        val lane = p.lane(AutoTarget.masterVolume())
        assertTrue(lane.target.isMasterVolume && lane.target.isValid)
        val curve = lane.songCurve(10)
        assertEquals(0f, curve[0], 1e-6f)
        assertEquals(1f, curve[3], 1e-6f)
        assertEquals(1f, curve[9], 1e-6f)
        // Looped on its own, the Intro plays at its level (the fade is the song's).
        assertTrue(p.songCurvesAt(0).isEmpty())
        // The master fader itself is untouched.
        assertEquals(p, p.withSetting(AutoTarget.masterVolume(), 0.2f))
        val out = p.fadeOut(2).lane(AutoTarget.masterVolume()).songCurve(10)
        assertEquals(1f, out[8], 1e-6f)
        assertEquals(PartIntents.FADE_FLOOR, out[9], 1e-6f)
        assertEquals(0f, out[0], 1e-6f)
        // A fade out in the middle: the next part comes back at full level, at once.
        val mid = song().fadeOut(0).lane(AutoTarget.masterVolume())
        assertEquals(PartIntents.FADE_FLOOR, mid.songCurve(10)[3], 1e-6f)
        assertEquals(1f, mid.songCurve(10)[4], 1e-6f)
        assertTrue(4 in mid.songJumps)
    }

    @Test
    fun anEndingIsAddedQuietAndFadingAndTheSongStops() {
        val p = song().copy(songLoops = true).withEnding()
        assertEquals(4, p.song.size)
        assertFalse(p.songLoops)
        val end = p.song.last()
        assertEquals(PartIntents.ENDING_LABEL, p.sceneLabel(end.scene))
        assertEquals(4, p.partSteps(end) / 16)
        assertTrue(p.allScenes[end.scene].drums.lanes[kick].hits.isEmpty())
        assertTrue(SongPart.ACID in end.muted && SongPart.inst(1) in end.muted)
        val curve = p.lane(AutoTarget.masterVolume()).songCurve(p.songBars)
        assertEquals(1f, curve[10], 1e-6f)
        assertEquals(PartIntents.FADE_FLOOR, curve[13], 1e-6f)
    }

    @Test
    fun energyTakesLayersOutAndPutsThemBack() {
        val p = song()
        assertEquals(3, p.partEnergy(1))
        val one = p.withPartEnergy(1, 1)
        assertEquals(1, one.partEnergy(1))
        val muted = one.song[1].muted
        assertTrue(SongPart.DRUMS in muted && SongPart.ACID in muted && SongPart.inst(1) in muted && SongPart.inst(0) in muted)
        assertTrue(one.song[1].plays(SongPart.inst(2)))
        // Darker over the part only.
        val filter = one.lane(AutoTarget.live(AutoTarget.LIVE_FILTER)).songCurve(10)
        assertEquals(PartIntents.ENERGY_1_FILTER, filter[5], 1e-6f)
        assertEquals(PartIntents.FILTER_OPEN, filter[0], 1e-6f)
        assertEquals(PartIntents.FILTER_OPEN, filter[8], 1e-6f)
        val two = one.withPartEnergy(1, 2)
        assertEquals(2, two.partEnergy(1))
        assertTrue(two.song[1].plays(SongPart.inst(1)) && !two.song[1].plays(SongPart.DRUMS))
        val three = two.withPartEnergy(1, 3)
        assertEquals(3, three.partEnergy(1))
        assertTrue(three.song[1].muted.isEmpty())
        assertEquals(PartIntents.FILTER_OPEN, three.lane(AutoTarget.live(AutoTarget.LIVE_FILTER)).songCurve(10)[5], 1e-6f)
        val four = three.withPartEnergy(1, 4)
        assertEquals(4, four.partEnergy(1))
        val layer = four.song[1].play.getValue(SongPart.DRUMS)
        assertEquals("Drop +", four.sceneLabel(layer))
        assertTrue(four.allScenes[layer].drums.lanes[hat].hits.size > p.allScenes[1].drums.lanes[hat].hits.size)
        // The Drop itself is unchanged; back to 3 drops the layer.
        assertEquals(p.allScenes[1].drums.lanes[hat].hits, four.allScenes[1].drums.lanes[hat].hits)
        assertEquals(3, four.withPartEnergy(1, 3).partEnergy(1))
        // Énergie 3 on a song with no filter curve writes none.
        assertTrue(p.withPartEnergy(1, 3).automation.isEmpty())
    }

    @Test
    fun aPartIsLengthenedByAPassOrSetInBars() {
        val p = song()
        assertEquals(5, p.lengthenPart(0, 1).song[0].repeats)
        assertEquals(3, p.lengthenPart(0, -1).song[0].repeats)
        assertEquals(SongPart(0, 6), p.withPartBars(0, 6).song[0])
        assertSame(p, p.withPartBars(0, 4))
        // A two-bar scene asked for 3 bars: a length of its own.
        val two = p.copy(drums = p.drums.withSteps(32)).sanitized()
        val odd = two.withPartBars(0, 3).song[0]
        assertEquals(3, odd.bars)
        assertEquals(5, two.withPartBars(0, 3).lengthenPart(0, 1).song[0].bars)
    }

    @Test
    fun rollsGoToEightAndRampAndOldHitsReadTheSame() {
        var d = DrumPattern().toggle(snare, 4)
        d = d.withRoll(snare, 4, 8, 1)
        assertEquals(Hit(1f, 8, ramp = 1), d.lanes[snare].hits[4])
        assertEquals(d, d.sanitized())
        // No hit there: nothing.
        assertSame(d, d.withRoll(snare, 5, 4))
        // The tool still cycles ×2 ×3 ×4 and back.
        val c = DrumPattern().toggle(0, 0).cycleRepeats(0, 0).cycleRepeats(0, 0).cycleRepeats(0, 0).cycleRepeats(0, 0)
        assertEquals(1, c.lanes[0].hits.getValue(0).repeats)
        // A plain hit is saved as before (no "ramp"); an old file reads the same.
        val json = ProjectJson.encode(Project(drums = DrumPattern().toggle(0, 0)))
        assertFalse(json.contains("ramp"))
        val rolled = ProjectJson.decode(ProjectJson.encode(Project(drums = d)))
        assertEquals(d.lanes[snare].hits, rolled.drums.lanes[snare].hits)
        // A 32nd burst over the beat.
        val b = DrumPattern().toggle(hat, 5).burst(hat, 5)
        assertEquals(setOf(4, 5, 6, 7), b.lanes[hat].hits.keys)
        assertTrue(b.lanes[hat].hits.values.all { it.repeats == 2 && it.ramp == 1 })
    }

    @Test
    fun paintingSetsAndNeverToggles() {
        val d = DrumPattern().setHit(0, 3, true)
        assertEquals(setOf(3), d.lanes[0].hits.keys)
        assertSame(d, d.setHit(0, 3, true))
        assertTrue(d.setHit(0, 3, false).lanes[0].hits.isEmpty())
        // Past the loop: nothing.
        assertSame(d, d.setHit(0, 40, true))
    }

    @Test
    fun withNoRoomNothingChanges() {
        var p = song()
        // 24 scenes in use.
        p = p.copy(scenes = p.allScenes + List(Scene.MAX - 2) { i -> p.allScenes[0].copy(label = "S$i") }).sanitized()
        assertEquals(Scene.MAX, p.allScenes.size)
        assertSame(p, p.variationOfPart(0))
        assertSame(p, p.breakAfter(0))
        assertSame(p, p.fillAtEnd(0))
        assertSame(p, p.withEnding())
    }
}
