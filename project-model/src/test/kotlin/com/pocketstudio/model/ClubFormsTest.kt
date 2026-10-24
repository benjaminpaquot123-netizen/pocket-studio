package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private typealias CR = StarterWizard.Role
private typealias CP = StarterWizard.Part

/** Club songs whose Drop really drops: a Montée before it, layers held back for it, a Break of the style's own. */
class ClubFormsTest {
    private fun style(id: String) = StarterWizard.style(id)!!

    private fun full(s: StarterWizard.Style): Project {
        val lines = s.lines.associate { it.role to null as PadSample? }
        val parts = CP.entries.associateWith { StarterWizard.soundsFor(it, s).first() }
        return StarterWizard.build(s, lines, s.melody.mood, parts)
    }

    private fun Project.lane(scene: Int, role: CR) = allScenes[scene].drums.lanes[role.lane].hits
    private fun Project.notes(scene: Int, track: Int) = allScenes[scene].let { if (track == 0) it.synthNotes else it.trackNotes.getValue(track) }.notes

    @Test
    fun bassMusicBuildsUpWithoutKickThenDrops() {
        for (id in listOf("dnb", "liquid_dnb", "neurofunk", "jump_up", "dubstep", "riddim", "gabber")) {
            val s = style(id)
            assertEquals(id, StarterWizard.SongForm.Build, s.form)
            val p = full(s)
            assertEquals(id, listOf("Intro", "Montée", "Drop", "Break"), p.allScenes.take(4).map { it.label })
            // Intro, Montée, Drop, Break, Montée, Drop (and in a long song a third Break, Montée and Drop), the Intro to end;
            // the riddim's first Montée ends on a fake drop.
            val sections = StarterWizard.songSections(p).map { p.allScenes[it.scene].label }
            val expected = listOf("Intro", "Montée") + (if (id == "riddim") listOf(SongShape.FAKE_DROP) else emptyList()) + listOf("Drop", "Break", "Montée", "Drop")
            assertEquals(id, expected, sections.take(expected.size))
            assertEquals(id, "Intro", sections.last())
            // The Montée: no kick, no bass, the snare (or the clap) rolling faster and faster over its 8 bars, in two scenes of 4.
            assertTrue(id, p.lane(1, CR.Kick).isEmpty())
            val bass = p.instrumentTracks.first { it.name == "Basse" }.id
            assertTrue(id, p.notes(1, bass).isEmpty() && p.notes(0, bass).isEmpty())
            assertTrue(id, p.notes(2, bass).isNotEmpty() && p.lane(2, CR.Kick).isNotEmpty())
            assertEquals(id, StarterWizard.BUILD_STEPS, p.allScenes[1].drums.steps)
            val roll = listOf(CR.Snare, CR.Clap).first { it in s.lines.map { l -> l.role } }
            // Its top follows the style's backbeat (S5b): a bar of it weighs twice a bar of the Drop's.
            val steps = StarterWizard.drumSteps(s)
            val top = StarterWizard.rollTop(StarterWizard.hits(s.lines.first { it.role == roll }, steps), steps)
            assertTrue(id, top in StarterWizard.ROLL_TOP_MIN..StarterWizard.ROLL_TOP)
            assertEquals(id, StarterWizard.rollHalf(0, top), p.lane(1, roll))
            val end = p.allScenes.indexOfFirst { it.label == "Montée 2" }
            assertEquals(id, StarterWizard.rollHalf(1, top), p.lane(end, roll))
            assertEquals(id, listOf(1, end), p.song.map { it.scene }.let { song -> song.subList(song.indexOf(1), song.indexOf(1) + 2) })
            // Its last beat is silent, every track, before the Drop.
            assertTrue(id, p.allScenes[end].drums.lanes.all { l -> l.hits.keys.none { it >= 60 } })
            assertTrue(id, p.allScenes[end].trackNotes.values.all { c -> c.notes.none { it.end > 60 * NoteClip.TICKS_PER_STEP } })
            // Techno and bass music last 4 min 30 to 6 min.
            val seconds = p.songBars * 4 * 60.0 / p.tempoBpm
            assertTrue("$id $seconds", seconds in 270.0..SongShape.LONG_SECONDS)
        }
        // The roll over 8 bars: quarter notes, eighths, sixteenths, then rolls of 2, 3 and 4 hits; louder and louder
        // (a step of several hits weighs what a single hit would: S5b, faster rather than louder at its end),
        // held under the Drop; silent on the last beat before it.
        val a = StarterWizard.rollHalf(0)
        val b = StarterWizard.rollHalf(1)
        assertEquals((0 until 32 step 4).toList(), a.keys.filter { it < 32 })
        assertEquals((32 until 64 step 2).toList(), a.keys.filter { it >= 32 })
        assertEquals((0 until 60).toList(), b.keys.sorted())
        assertEquals(listOf(1, 1, 2, 3, 4), listOf(0, 16, 32, 48, 56).map { b.getValue(it).repeats })
        val all = a.values + b.values
        fun weight(h: Hit) = h.velocity * h.velocity * StarterWizard.rollPower(h.repeats)
        assertTrue(all.zipWithNext().all { (x, y) -> weight(y) >= weight(x) - 1e-6f })
        assertTrue(all.all { it.velocity <= StarterWizard.ROLL_TOP })
        // The last bar's 4 hits a step together weigh no more than one hit at the roll's top.
        assertTrue(weight(b.getValue(56)) <= StarterWizard.ROLL_TOP * StarterWizard.ROLL_TOP + 1e-6f)
        assertEquals(1f, StarterWizard.rollPower(1), 0f)
        assertEquals(1.25f, StarterWizard.rollPower(2), 1e-6f)
    }

    @Test
    fun aStyleHoldsLayersBackForTheDropAndHasItsOwnBreak() {
        // Speed garage: the bass drops (not in the Groove); techno: the 909 open hat arrives with the Drop.
        val garage = full(style("speed_garage"))
        val bass = garage.instrumentTracks.first { it.name == "Basse" }.id
        assertEquals(listOf(false, false, true), (0..2).map { garage.notes(it, bass).isNotEmpty() })
        val plan = StarterWizard.planFor(style("speed_garage"), style("speed_garage").lines.map { it.role }.toSet(), CP.entries.toSet(), acid = false)
        assertEquals("le groove, la basse attend le Drop", plan.role(1))
        val techno = full(style("techno"))
        assertEquals(listOf(false, false, true, false), (0..3).map { techno.lane(it, CR.OpenHat).isNotEmpty() })
        // Tech house: the Break keeps the bass and the percussion, never the kick.
        val tech = full(style("tech_house"))
        val techBass = tech.instrumentTracks.first { it.name == "Basse" }.id
        assertTrue(tech.notes(3, techBass).isNotEmpty() && tech.lane(3, CR.Perc).isNotEmpty() && tech.lane(3, CR.Kick).isEmpty())
        val techPlan = StarterWizard.planFor(style("tech_house"), style("tech_house").lines.map { it.role }.toSet(), CP.entries.toSet(), acid = false)
        assertEquals("une pause sans kick, la basse continue", techPlan.role(3))
        // Deep house: the Break is the Drop without its kick.
        val deep = full(style("deep_house"))
        val deepLines = style("deep_house").lines.map { it.role } - CR.Kick
        assertTrue(deepLines.all { deep.lane(3, it).isNotEmpty() } && deep.lane(3, CR.Kick).isEmpty())
        // A style without these fields keeps its plan (house: the open hat in the Groove, the bass out of the Break).
        val house = full(style("house"))
        assertTrue(house.lane(1, CR.OpenHat).isNotEmpty())
        assertTrue(house.notes(3, house.instrumentTracks.first { it.name == "Basse" }.id).isEmpty())
    }
}
