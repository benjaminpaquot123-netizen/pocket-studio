package com.pocketstudio.model.tuto

import com.pocketstudio.model.Harmony
import com.pocketstudio.model.Hit
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.Project
import com.pocketstudio.model.Scale
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.TrackKind
import com.pocketstudio.model.track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The tutorial's ground for every style (chantier « tuto pour tous les styles »): the 303, the 808
 * as the bass, lines that follow the chords, the course's own key, the hits early or late, the
 * swing, the rolls, the gaps and fills of any part, the curves that go up and down.
 */
class TutoSocleTest {
    private fun step(check: String, target: String = "", part: String = "", length: Int = 0) = TutoStep("x", "x", check = check, target = target, part = part, length = length)

    private fun withHits(p: Project, lane: Int, hits: Map<Int, Hit>) =
        p.copy(drums = p.drums.copy(lanes = p.drums.lanes.mapIndexed { i, l -> if (i == lane) l.copy(hits = hits) else l }))

    /** The 303: its steps dotted a bar at a time, written by hand, « Copier », ticked only with its accents and slides. */
    @Test
    fun theAcidLineIsWrittenThenCopied() {
        val target = TutoTarget.of("acid_techno")!!
        val acid = target.acid!!
        val s = step("notes:acid")
        var p = target.blank()
        // The empty project: the style's 303 sound, no note.
        assertEquals(acid.sound, p.acid.sound)
        assertTrue(p.acid.steps.none { it.gate })
        assertFalse(TutoChecks.done(s, p, target))
        // The dotted steps: those of the first bar (or bars) to write by hand.
        val dotted = TutoChecks.acidWanted(s, p, target)!!
        assertTrue(dotted.isNotEmpty())
        p = p.copy(acid = p.acid.copy(steps = p.acid.steps.mapIndexed { i, st -> dotted[i] ?: st }))
        var rounds = 0
        while (!TutoChecks.done(s, p, target)) {
            assertTrue("rounds", ++rounds < 8)
            val need = TutoChecks.need(s, p, target)
            p = if (need is TutoNeed.Copy) TutoChecks.fulfil(need, s, p, target)!!.first
            else TutoChecks.acidWanted(s, p, target)!!.let { w -> p.copy(acid = p.acid.copy(length = maxOf(p.acid.length, acid.length), steps = p.acid.steps.mapIndexed { i, st -> w[i] ?: st })) }
        }
        // An accent taken away: no longer the line.
        val i = acid.steps.indexOfFirst { it.gate && it.accent }
        if (i >= 0) assertFalse(TutoChecks.done(s, p.copy(acid = p.acid.withStep(i, p.acid.steps[i].copy(accent = false))), target))
        // « Montre-moi » from empty writes it.
        assertTrue(generateSequence(target.blank()) { q -> TutoChecks.show(s, q, target)?.takeIf { !TutoChecks.done(s, q, target) } }.take(10).any { TutoChecks.done(s, it, target) })
    }

    /** Trap: the bass is the 808, on an 808 track (written in the piano roll), its notes the line's. */
    @Test
    fun the808StylesWriteTheirBassOnAn808Track() {
        val target = TutoTarget.of("trap")!!
        assertTrue(target.bass808)
        assertEquals(TrackKind.Bass808, target.trackKind(StarterWizard.Part.Bass))
        val (p, id) = TutoChecks.partTrack(target.blank(), "bass", target)!!
        assertEquals(InstrumentTrack.ENGINE_808, p.track(id).engine)
        assertEquals("bass", TutoChecks.roleOf(p, id))
        val s = step("notes:bass")
        val shown = TutoChecks.show(s, p, target)!!
        assertTrue(target.notes(StarterWizard.Part.Bass).notes.isNotEmpty())
        // The whole line, a show at a time (its bars to write, then « Copier »).
        var q = shown
        repeat(8) { if (!TutoChecks.done(s, q, target)) q = (TutoChecks.need(s, q, target)?.let { TutoChecks.fulfil(it, s, q, target)?.first } ?: TutoChecks.show(s, q, target) ?: q).sanitized() }
        assertTrue(TutoChecks.done(s, q, target))
        // The trap kick is the kick's pad alone: its second pad is the 808's.
        assertEquals(setOf(0), TutoChecks.lanesOf("kick", target))
    }

    /** « Copier » that follows the chords: a bar written once, moved along the scale to each bar's chord. */
    @Test
    fun aShortLineFollowsTheChords() {
        val key = Harmony(9, Scale.Minor)
        assertEquals(60, TutoTarget.byDegrees(57, 2, key))  // A3 a third up: C4
        assertEquals(53, TutoTarget.byDegrees(57, -2, key)) // F3
        val bar = listOf(Note(0, 24, 45, 0.8f), Note(48, 24, 52, 0.8f))  // A2, E3
        val clip = TutoTarget.fitted(NoteClip(steps = 64), bar, steps = 16, chords = listOf(0, 5, 2, 6), key = key)
        // Bar 2 on VI: five degrees up, A2 → F3, E3 → C4.
        val bar2 = clip.notes.filter { it.start in 384 until 768 }.map { it.pitch }
        assertEquals(listOf(53, 60), bar2)
        // Without chords: the same bar four times.
        assertEquals(listOf(45, 52), TutoTarget.fitted(NoteClip(steps = 64), bar, steps = 16).notes.filter { it.start in 384 until 768 }.map { it.pitch })
    }

    /** The course's own key and chords (house in minor), the recipe untouched. */
    @Test
    fun aCourseNamesItsKey() {
        assertEquals(9 to Scale.Minor, TutoCourse.keyOf("A minor"))
        assertEquals(6 to Scale.Dorian, TutoCourse.keyOf("F# dorian"))
        assertEquals(3 to Scale.Phrygian, TutoCourse.keyOf("Eb Phrygian"))
        assertEquals(9 to Scale.HarmonicMinor, TutoCourse.keyOf("A harmonic minor"))
        assertNull(TutoCourse.keyOf("H minor"))
        assertEquals(listOf(0, 3), TutoCourse.degreesOf("i iv"))
        assertEquals(listOf(0, 5, 2, 6), TutoCourse.degreesOf("i – VI – III – VII"))
        assertNull(TutoCourse.degreesOf("Am9 D9"))
    }

    /** Lo-fi: the snare played late, a few ms off its cell. */
    @Test
    fun aHitPlayedLate() {
        val target = TutoTarget.of("lofi")!!
        val s = step("offset:snare:late")
        val lane = TutoChecks.laneOf("snare")
        val p = withHits(target.blank(), lane, mapOf(4 to Hit(1f), 12 to Hit(1f)))
        assertFalse(TutoChecks.done(s, p, target))
        assertFalse(TutoChecks.done(s, withHits(p, lane, mapOf(4 to Hit(1f, offset = -12f), 12 to Hit(1f, offset = -12f))), target))
        assertTrue(TutoChecks.done(s, withHits(p, lane, mapOf(4 to Hit(1f, offset = 12f), 12 to Hit(1f, offset = 12f))), target))
        assertTrue(TutoChecks.done(s, TutoChecks.show(s, p, target)!!, target))
        assertTrue(TutoChecks.done(step("offset:snare:early"), withHits(p, lane, mapOf(4 to Hit(1f, offset = -12f), 12 to Hit(1f))), target))
    }

    /** House swings: the style's, not none, not twice as much. */
    @Test
    fun theStylesSwing() {
        val target = TutoTarget.of("house")!!
        val s = step("swing")
        val p = target.blank()
        assertFalse(TutoChecks.done(s, p, target))
        assertTrue(TutoChecks.done(s, p.copy(swing = target.style.swing), target))
        assertFalse(TutoChecks.done(s, p.copy(swing = target.style.swing * 2), target))
        assertTrue(TutoChecks.done(s, TutoChecks.show(s, p, target)!!, target))
    }

    /** Trap's hats: "lane:hat:rolls" wants each roll of the line rolled; "lane:hat" the cells alone. */
    @Test
    fun theRollsOfTrapHats() {
        val target = TutoTarget.of("trap")!!
        val line = target.line("hat")!!
        val p = target.blank().let { it.copy(drums = it.drums.withSteps(line.pattern.length)) }
        val lane = TutoChecks.laneOf("hat")
        val flat = withHits(p, lane, StarterWizard.hits(line, p.drums.steps).mapValues { Hit(0.8f) })
        assertTrue(TutoChecks.done(step("lane:hat"), flat, target))
        assertFalse(TutoChecks.done(step("lane:hat:rolls"), flat, target))
        assertTrue(TutoChecks.done(step("lane:hat:rolls"), TutoChecks.show(step("lane:hat:rolls"), p, target)!!, target))
        // Techno's hats: their own pad, the open hat's line is another.
        assertEquals(setOf(5), TutoChecks.lanesOf("hat", TutoTarget.of("techno")))
    }

    /** A gap that silences the hats alone, a tom fill, a filter up and down: in the Break of the techno course. */
    @Test
    fun gapsFillsAndCurvesOfAnyPart() {
        val target = TutoTarget.of("techno")!!
        val course = TutoCourse.of("techno")!!
        val steps = course.chapters.flatMap { it.steps }.associateBy { it.id }
        assertEquals(setOf(5), TutoChecks.gapLanes(steps.getValue("gap"), target))
        assertEquals(listOf("Break"), TutoChecks.partsOf(steps.getValue("toms")))
        assertEquals("tom", TutoChecks.fillRole(steps.getValue("toms")))
        assertEquals(4, TutoChecks.fillEnd(steps.getValue("toms"), target))
        // The crash: the Drop the step names, the Drop and the Drop 2 by default.
        assertEquals(listOf("Drop", "Drop 2"), TutoChecks.partsOf(step("crash")))
        assertEquals(listOf("Montée"), TutoChecks.partsOf(step("gap")))
        // A slow style's silence before a part: half a beat.
        assertEquals(2, TutoChecks.gapCells(step("gap"), TutoTarget.of("lofi")!!))
        assertEquals(4, TutoChecks.gapCells(step("gap"), target))
        assertEquals(16, TutoChecks.gapCells(step("gap", length = 16), target))
    }

    /** A course's notes: chords, strengths, the 303's accents and slides; errors said. */
    @Test
    fun theLinesNotation() {
        val notes = TutoTarget.parseLine("0:F3+Ab3+C4:16 16:C3:2:100", 0.8f)
        assertEquals(listOf(53, 56, 60, 48), notes.map { it.pitch })
        assertEquals(0.8f, notes[0].velocity)
        assertEquals(100f / 127f, notes[3].velocity, 0.001f)
        val acid = TutoTarget.parseAcid("0:A2:1 2:A3:1:as", 45)
        assertTrue(acid.getValue(2).accent && acid.getValue(2).slide && acid.getValue(2).octave == 1)
        assertFalse(acid.getValue(0).accent)
        assertTrue(runCatching { TutoTarget.entries("0:H2:1") }.isFailure)
        assertTrue(runCatching { TutoTarget.entries("0:F2") }.isFailure)
        assertEquals(1, TutoTarget.phraseBarsOf(14))
        assertEquals(2, TutoTarget.phraseBarsOf(16))
        assertEquals(4, TutoTarget.phraseBarsOf(58))
    }

    /** The validation names what is wrong. */
    @Test
    fun theValidationSaysWhatIsWrong() {
        val bad = TutoCourse.parse(
            """{"style":"techno","lines":{"bass":"0:Z2:1","drums":"0:C2:1"},"chapters":[{"id":"c","title":"T","steps":[
              {"id":"a","text":"t","check":"lane:snare"},
              {"id":"b","text":"t","check":"part:Couplet:loud"},
              {"id":"c2","text":"t","check":"fill","part":"Pont"},
              {"id":"d","text":"t","check":"listen","listen":"part:Refrain"},
              {"id":"e","text":"t","check":"tempo","spot":"nowhere","keep":"guitar"}
            ]}]}""",
        )
        val problems = TutoValidation.problems(bad, "techno.json").joinToString("\n")
        listOf("lines.bass", "role « drums »", "a: the style has no « snare » line", "b: modes", "c2: part « Pont »", "d: listen", "spot « nowhere »", "keep « guitar »")
            .forEach { assertTrue("$it in\n$problems", it in problems) }
    }
}
