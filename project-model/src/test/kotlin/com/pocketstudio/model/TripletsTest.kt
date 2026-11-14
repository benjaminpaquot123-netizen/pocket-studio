package com.pocketstudio.model

import com.pocketstudio.model.tuto.TutoChecks
import com.pocketstudio.model.tuto.TutoStep
import com.pocketstudio.model.tuto.TutoTarget
import com.pocketstudio.model.tuto.TutoValidation
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Triplets (« Triolet »): a roll of N hits spread over M steps ([Hit.span]): 3 over 2 cells for
 * the trap and drill hats. The file stays readable by older versions, the steps a triplet covers
 * hold nothing of their own, the recipes and the tutorial's « triplet:<role> » know them.
 */
class TripletsTest {
    private val hat = StarterWizard.Role.Hat.lane

    private fun pattern(vararg hits: Pair<Int, Hit>) = DrumPattern().let { d ->
        d.copy(lanes = d.lanes.mapIndexed { i, l -> if (i == hat) l.copy(hits = hits.toMap()) else l })
    }

    @Test
    fun aTripletCoversTheNextCellAndTakesItsHit() {
        val d = pattern(4 to Hit(0.6f), 5 to Hit(0.3f), 6 to Hit(1f)).withSpread(hat, 4, 3, 2)
        val hits = d.lanes[hat].hits
        assertEquals(Hit(0.6f, repeats = 3, span = 2), hits[4])
        assertNull("the covered cell is the triplet's", hits[5])
        assertEquals(Hit(1f), hits[6])
        assertTrue(hits.getValue(4).isTriplet)
        assertEquals(4, d.lanes[hat].covering(5))
        assertNull(d.lanes[hat].covering(6))
        assertNull(d.lanes[hat].covering(4))
        // 6 over the beat covers its 3 next cells.
        val beat = pattern(12 to Hit(0.6f), 14 to Hit(0.6f)).withSpread(hat, 12, 6, 4)
        assertEquals(mapOf(12 to Hit(0.6f, repeats = 6, span = 4)), beat.lanes[hat].hits)
        assertTrue(beat.lanes[hat].hits.getValue(12).isTriplet)
        // A roll of 4 over 2 cells is no triplet (two 32nds a cell).
        assertFalse(Hit(repeats = 4, span = 2).isTriplet)
    }

    @Test
    fun noTripletPastThePadsLoopNorWithoutAHit() {
        val d = pattern(15 to Hit(), 12 to Hit())
        assertEquals("the loop's last cell has no next one", d, d.withSpread(hat, 15, 3, 2))
        assertEquals("no hit, no triplet", d, d.withSpread(hat, 10, 3, 2))
        assertTrue(d.canSpread(hat, 14, 2))
        assertFalse(d.canSpread(hat, 15, 2))
        // A pad on its own loop of 6 steps: its 5th cell has no room for a triplet.
        val own = d.withLaneLength(hat, 6).let { it.copy(lanes = it.lanes.mapIndexed { i, l -> if (i == hat) l.copy(hits = mapOf(5 to Hit(), 4 to Hit())) else l }) }
        assertFalse(own.canSpread(hat, 5, 2))
        assertTrue(own.canSpread(hat, 4, 2))
    }

    @Test
    fun touchingACoveredCellActsOnTheTriplet() {
        val d = pattern(4 to Hit(0.6f, repeats = 3, span = 2), 8 to Hit())
        // A touch on the covered cell takes the triplet away, as on its first cell.
        assertEquals(mapOf(8 to Hit()), d.toggle(hat, 5).lanes[hat].hits)
        assertEquals(mapOf(8 to Hit()), d.toggle(hat, 4).lanes[hat].hits)
        // A finger painting over it leaves it; erasing over it removes it.
        assertEquals(d, d.setHit(hat, 5, true))
        assertEquals(mapOf(8 to Hit()), d.setHit(hat, 5, false).lanes[hat].hits)
        // The « Roulements » tool turns it back into one hit; a ×N roll is in its own cell again.
        assertEquals(Hit(0.6f), d.cycleRepeats(hat, 4).lanes[hat].hits[4])
        assertEquals(Hit(0.6f, repeats = 3), d.withRoll(hat, 4, 3).lanes[hat].hits[4])
        // Its ramp changes, it stays a triplet.
        assertEquals(Hit(0.6f, repeats = 3, span = 2, ramp = 1), d.withRoll(hat, 4, 3, ramp = 1, span = 2).lanes[hat].hits[4])
    }

    @Test
    fun aHitOnACoveredCellIsDroppedWhenSanitized() {
        // A file where the covered cell holds a hit (another version, a pattern moved under it).
        val d = pattern(4 to Hit(repeats = 3, span = 2), 5 to Hit(), 6 to Hit(repeats = 3, span = 2), 7 to Hit(repeats = 3, span = 2), 9 to Hit())
        val clean = d.sanitized().lanes[hat].hits
        assertEquals(setOf(4, 6, 9), clean.keys)
        assertEquals(clean, clean.let { DrumLane.uncovered(it) })
        assertEquals("an already clean pattern is kept as it is", d.sanitized(), d.sanitized().sanitized())
        // A span out of range is brought back.
        assertEquals(Hit.MAX_SPAN, Hit(repeats = 6, span = 9).sanitized().span)
        assertEquals(1, Hit(span = 0).sanitized().span)
    }

    /** The project as an older version (no « span ») reads it: the step's roll ×3, the covered cell empty. */
    @Serializable
    private data class OldHit(val velocity: Float = 1f, val repeats: Int = 1, val pan: Float = 0f, val pitch: Int = 0)

    @Serializable
    private data class OldLane(val hits: Map<Int, OldHit> = emptyMap())

    @Serializable
    private data class OldDrums(val steps: Int = 16, val lanes: List<OldLane> = emptyList())

    @Serializable
    private data class OldProject(val name: String = "", val drums: OldDrums = OldDrums())

    @Test
    fun theFileStaysReadableByOlderVersions() {
        val p = Project(drums = pattern(4 to Hit(0.6f), 0 to Hit()).withSpread(hat, 4, 3, 2))
        val text = ProjectJson.encode(p)
        // Read back the same.
        assertEquals(p.sanitized().drums, ProjectJson.decode(text).drums)
        // A plain hit writes no « span » (files saved before read the same; a triplet adds one key).
        assertEquals(1, Regex("\"span\"").findAll(text).count())
        // An older version: it ignores the key, plays 3 hits in the step, the covered cell empty.
        val old = Json { ignoreUnknownKeys = true }.decodeFromString(OldProject.serializer(), text)
        val oldHits = old.drums.lanes[hat].hits
        assertEquals(3, oldHits.getValue(4).repeats)
        assertNull(oldHits[5])
        assertEquals(1, oldHits.getValue(0).repeats)
    }

    @Test
    fun theRecipesWriteTripletsWhereTheSheetsSayIt() {
        assertEquals(mapOf(0 to Hit(1f, repeats = 3, span = 2), 2 to Hit(0.6f, repeats = 3, span = 2)), StarterWizard.hits("T.t."))
        // Trap: the last beat of the first bar in triplets (trap.md, bar 3: « TTTT »), 6 hits over the beat.
        val trap = StarterWizard.style("trap")!!
        val trapHats = StarterWizard.hits(trap.lines.first { it.role == StarterWizard.Role.Hat }.pattern)
        assertEquals(listOf(12, 14), trapHats.filterValues { it.isTriplet }.keys.sorted())
        // Drill: one triplet before the bar's end (drill.md § 8: the triplets on 2 steps).
        val drill = StarterWizard.style("drill")!!
        val drillHats = StarterWizard.hits(drill.lines.first { it.role == StarterWizard.Role.Hat }.pattern)
        assertEquals(listOf(28), drillHats.filterValues { it.isTriplet }.keys.sorted())
        // Every triplet of every recipe has its next cell free, inside its bar.
        for (s in StarterWizard.STYLES) for (l in s.lines) {
            val hits = StarterWizard.hits(l.pattern)
            hits.filterValues { it.isSpread }.forEach { (st, h) ->
                assertTrue("${s.id} ${l.role}: $st", (st + 1 until st + h.span).none { it in hits } && (st + h.span - 1) / 16 == st / 16)
            }
        }
        // The songs built keep them, and nothing on their covered cells.
        for (id in listOf("trap", "drill")) {
            val style = StarterWizard.style(id)!!
            val p = StarterWizard.build(style, style.lines.associate { it.role to null }).sanitized()
            val lane = p.drums.lanes[hat]
            assertTrue(id, lane.hits.values.any { it.isTriplet })
            assertTrue(id, lane.hits.keys.none { lane.covering(it) != null })
        }
    }

    @Test
    fun theTutorialDetectsAndShowsATriplet() {
        val target = TutoTarget.of("trap")!!
        val step = TutoStep("trip", "Un triolet", check = "triplet:hat")
        var p = target.blank()
        assertFalse(TutoChecks.done(step, p, target))
        // A ×3 roll in one cell is not it.
        p = p.copy(drums = p.drums.setHit(hat, 4, true).withRoll(hat, 4, 3))
        assertFalse(TutoChecks.done(step, p, target))
        // The triplet is.
        p = p.copy(drums = p.drums.withSpread(hat, 4, 3, 2))
        assertTrue(TutoChecks.done(step, p, target))
        // « Montre-moi »: the style's triplets on the hats (trap: the first bar's last beat).
        val shown = TutoChecks.show(step, target.blank(), target)
        assertNotNull(shown)
        assertTrue(TutoChecks.done(step, shown!!, target))
        assertEquals(listOf(12, 14), shown.drums.lanes[hat].hits.filterValues { it.isTriplet }.keys.sorted())
        // A style whose line has none: one on the first bar's last beat.
        val house = TutoTarget.of("house")!!
        val shownHouse = TutoChecks.show(step, house.blank(), house)!!
        assertTrue(TutoChecks.done(step, shownHouse, house))
    }

    @Test
    fun theCourseValidationKnowsTheDetector() {
        val ok = TutoValidation.problems(course("triplet:hat"), "trap.json")
        assertTrue(ok.toString(), ok.none { "trip" in it })
        val bad = TutoValidation.problems(course("triplet:piano"), "trap.json")
        assertTrue(bad.toString(), bad.any { it.startsWith("trip:") && "triplet:<drum role>" in it })
    }

    private fun course(check: String) = com.pocketstudio.model.tuto.TutoCourse(
        style = "trap",
        chapters = listOf(
            com.pocketstudio.model.tuto.TutoChapter(
                "hats", "Charleys", levels = "fp",
                steps = listOf(TutoStep("trip", "Un triolet sur les charleys", check = check)),
            ),
        ),
    )
}
