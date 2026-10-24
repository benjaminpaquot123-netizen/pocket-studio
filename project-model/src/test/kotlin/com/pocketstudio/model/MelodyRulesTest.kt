package com.pocketstudio.model

import com.pocketstudio.model.StarterWizard.Mood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The melodies of every style and mood (37 × 4) obey the pro melody rules M1–M20 of
 * docs/hits/autres-et-melodie.md § B13: leads, basses, counter-melodies, arpeggios, and none of
 * the machine « tells » of § B12.
 */
class MelodyRulesTest {
    private fun assertNoFailure(judged: List<MelodyAudit.Judged>) {
        val failed = judged.filter { it.failures.isNotEmpty() }
        val report = failed.joinToString("\n") { "${it.style.id} ${it.mood.id} ${it.line}: ${it.failures.joinToString("; ")}" }
        assertTrue("${failed.size} of ${judged.size} lines fail:\n${report.lines().take(80).joinToString("\n")}", failed.isEmpty())
    }

    @Test
    fun everyLeadIsAnEightBarPhraseThatBreathes() {
        val judged = StarterWizard.STYLES.filter { MelodyAudit.hasLeadLine(it) }.flatMap { s -> Mood.entries.map { MelodyAudit.judgeLead(s, it, withSong = false) } }
        assertTrue("no written lead", judged.size >= 30 * 4)
        assertNoFailure(judged)
    }

    @Test
    fun everyBassHasAFormAndRests() {
        assertNoFailure(StarterWizard.STYLES.flatMap { s -> Mood.entries.map { MelodyAudit.judgeBass(s, it) } })
    }

    @Test
    fun theHookIsHeardInThreeSectionsAndTheCounterAnswersInItsRests() {
        val judged = StarterWizard.STYLES.filter { MelodyAudit.hasLeadLine(it) }.flatMap { s ->
            Mood.entries.flatMap { m -> listOfNotNull(MelodyAudit.judgeLead(s, m, withSong = true), MelodyAudit.judgeCounter(s, m)) }
        }
        assertTrue("no counter-melody judged", judged.any { it.line == "counter" })
        assertNoFailure(judged)
    }

    @Test
    fun arpeggiosAreFixedPatternsOverTwoOctaves() {
        val judged = StarterWizard.STYLES.flatMap { s -> Mood.entries.flatMap { MelodyAudit.judgeArps(s, it) } }
        assertTrue("no arpeggio judged", judged.isNotEmpty())
        assertNoFailure(judged)
        // Never a triad going up and down as the only lead.
        for (s in StarterWizard.STYLES) for (m in Mood.entries) {
            val perf = MelodyAudit.lead(s, m).performance
            assertTrue("${s.id} $m", perf.arp != ArpMode.UpDown && perf.arp != ArpMode.Random)
        }
    }

    @Test
    fun noMachineTells() {
        // No bar copied four times in a lead; a bass loops its bar but varies by the fill, the turn and the approach note.
        for (s in StarterWizard.STYLES) for (m in Mood.entries) {
            if (MelodyAudit.hasLeadLine(s)) assertTrue("${s.id} $m lead", MelodyAudit.mostRepeatedBar(MelodyAudit.lead(s, m)) <= 3)
            assertTrue("${s.id} $m bass", MelodyAudit.mostRepeatedBar(MelodyAudit.bass(s, m)) <= 5)
        }
        // No rhythm cell shared by three styles (the tresillo of eight recipes).
        val cells = StarterWizard.STYLES.filter { MelodyAudit.hasLeadLine(it) }.map { it.id to MelodyAudit.rhythmCell(MelodyAudit.lead(it, it.melody.mood)) }
        val shared = cells.groupBy({ it.second }, { it.first }).filterValues { it.size >= 3 }
        assertEquals("rhythm cells shared by three styles: $shared", emptyMap<List<Pair<Int, Int>>, List<String>>(), shared)
    }

    @Test
    fun theMetricsReadAHookAsTheRulesSay() {
        // A tiny hand-written hook in A minor: i – VI – III – VII, two bars, repeated twice with rests.
        val h = Harmony(9, Scale.Minor, lock = true)
        val ctx = MelodyMetrics.Context(h, listOf(0, 5, 2, 6))
        val t = NoteClip.TICKS_PER_STEP
        fun n(step: Int, len: Int, pitch: Int, v: Float) = Note(step * t, len * t, pitch, v)
        val clip = NoteClip(
            steps = 128,
            notes = listOf(
                n(0, 2, 69, 1f), n(2, 2, 71, 0.7f), n(4, 4, 72, 0.9f), n(8, 4, 69, 0.8f),
                n(16, 2, 72, 0.95f), n(18, 2, 71, 0.7f), n(20, 8, 69, 0.85f),
                n(64, 2, 69, 1f), n(66, 2, 71, 0.7f), n(68, 4, 72, 0.9f), n(72, 4, 76, 0.8f),
                n(80, 2, 72, 0.95f), n(82, 2, 71, 0.7f), n(84, 8, 69, 0.85f),
            ),
        )
        val r = MelodyMetrics.analyse(clip, ctx)
        assertEquals(128, r.steps)
        assertEquals(14, r.notes)
        // Bar 1's motif comes back in bars 5 (one note changed) and, over 16 bars, twice that.
        assertEquals(4, r.motifOccurrences)
        assertEquals(0, r.identicalPairs)
        // Each phrase breathes: bars 3–4 and 7–8 are rests of two beats and more.
        assertTrue(r.restsPerPhrase.all { it >= 1 })
        assertTrue(r.phraseEndRest.all { it })
        assertTrue(r.phraseEndOk.all { it })
        // The peak (76) sounds once, in bar 5, on beat 3.
        assertEquals(1, r.peakCount)
        assertEquals(72 * t, r.peakStart)
        assertTrue(r.peakStrong)
        assertEquals(7, r.ambitus)
        assertTrue(r.conjunct > 0.6f)
        assertTrue(r.velocityRange >= 0.25f)
        assertEquals(3, r.distinctLengths)
        // On the grid: nothing humanised.
        assertEquals(0, r.maxShift)
        assertEquals(0f, r.shiftedShare)
        assertEquals(1f, r.strongChordTones)
        // Two clips: the counter answers in the rests, never with the lead.
        val counter = NoteClip(steps = 128, notes = listOf(n(32, 2, 57, 0.6f), n(36, 2, 60, 0.6f), n(96, 2, 57, 0.6f)))
        val resp = MelodyMetrics.response(clip, counter)
        assertEquals(2, resp.gaps)
        assertEquals(2, resp.gapsAnswered)
        assertEquals(0f, resp.simultaneous)
        // A verse of half the notes is a subset.
        val verse = clip.copy(notes = clip.notes.filterIndexed { i, _ -> i % 2 == 0 })
        assertTrue(MelodyMetrics.variant(verse, clip).subset)
        assertEquals(0.5f, MelodyMetrics.variant(verse, clip).share)
        // Edit distance.
        assertEquals(0, MelodyMetrics.editDistance(listOf(1, 2, 3), listOf(1, 2, 3)))
        assertEquals(1, MelodyMetrics.editDistance(listOf(1, 2, 3), listOf(1, 2)))
        assertEquals(2, MelodyMetrics.editDistance(listOf(1, 2, 3), listOf(3, 2, 1)))
    }

    @Test
    fun aPatternArpeggioWalksTwoOctavesWithAccentsAndGate() {
        val chord = listOf(60, 64, 67).map { Note(0, 16 * NoteClip.TICKS_PER_STEP, it, 0.7f) }
        val clip = NoteClip(steps = 16, notes = chord, performance = Performance(arp = ArpMode.Pattern, arpRate = 24, pattern = listOf(0, 2, 3, 5, 4, 3, 2, 1), gate = 0.5f, accentEvery = 4))
        val played = clip.performance.expand(clip)
        assertEquals(16, played.size)
        assertEquals(listOf(60, 67, 72, 79, 76, 72, 67, 64), played.take(8).map { it.pitch })
        assertTrue(played.all { it.length == 12 })
        assertEquals(0.85f, played[0].velocity, 0.001f)
        assertEquals(0.7f, played[1].velocity, 0.001f)
        val arp = MelodyMetrics.arp(clip)
        assertEquals(6, arp.distinctPitches)
        assertEquals(19, arp.range)
        assertTrue(arp.fixedPattern)
        assertTrue(MelodyRules.arpFailures(arp, 120.0).isEmpty())
        // An old project without the new fields reads as before.
        val old = ProjectJson.decode(ProjectJson.encode(Project(name = "x").withTrack(Project().track(0).copy(notes = clip))))
        assertEquals(clip.performance, old.track(0).notes.performance)
    }
}
