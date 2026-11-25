package com.pocketstudio.model.tuto

import com.pocketstudio.model.Project
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 9, control « fable-tuto », seat d (house, Facile, chapter 12): « À toi : dans le Drop, touche la 1re case du
 * pad de la cymbale » never passed, even after « Je le fais »: the check wanted the Drop 2 as well, which only the
 * Pro words name. The rule: at each level, a step checks the parts its words of that level name, no other.
 */
class T9FableLevelPartsTest {
    private val styles: List<String> =
        File("src/main/resources/tutos").listFiles { f -> f.extension == "json" }!!.map { it.nameWithoutExtension }.sorted()

    /** The course of [style] at [level] walked until step [id] (not done yet), each step before made as « Montre-moi » would. */
    private fun upTo(style: String, level: TutoLevel, id: String): Project {
        val target = TutoTarget.of(style)!!
        var p = target.blank()
        for (s in TutoCourse.of(style)!!.chaptersFor(level).flatMap { it.steps }) {
            if (s.id == id) return p
            if (s.listening) continue
            var tries = 0
            while (!TutoChecks.done(s, p, target, null) && tries++ < 8) {
                val need = TutoChecks.need(s, p, target, null)
                val q = need?.let { TutoChecks.fulfil(it, s, p, target)?.first } ?: TutoChecks.show(s, p, target) ?: break
                if (q == p) break
                p = q
            }
        }
        error("no step $id")
    }

    @Test
    fun everyPartAStepChecksIsNamedByItsWordsOfThatLevel() {
        assertTrue("the courses are read", styles.size > 30)
        val bad = ArrayList<String>()
        for (style in styles) for (level in TutoLevel.entries) {
            for (s in TutoCourse.of(style)!!.chaptersFor(level).flatMap { it.steps }) {
                if (s.part.isBlank()) continue
                val words = s.textFor(level)
                val parts = TutoChecks.partsOf(s)
                // « Drop » is in « Drop 2 »: a part counts as named only where its name does not go on into a longer one.
                for (part in parts) {
                    val alone = Regex(Regex.escape(part) + "(?! ?\\d)")
                    if (parts.size > 1 && !alone.containsMatchIn(words)) bad += "$style ${level.name} ${s.id}: checks « $part », words: $words"
                }
            }
        }
        assertEquals("steps that check a part their words do not name", emptyList<String>(), bad)
    }

    /**
     * Seat d, same chapter: « touche 4 cases, une par temps de la mesure 1 » passed at the first cell (then « Déjà
     * fait ! »). The rule: a variation is done at the number of hits its words of that level ask for (no number:
     * one), not before, and « Je le fais » makes that many.
     */
    @Test
    fun aVariationIsDoneAtTheNumberOfHitsItsWordsAskFor() {
        val count = Regex("(\\d+) (cases|coups)")
        val bad = ArrayList<String>()
        var counted = 0
        for (style in styles) for (level in TutoLevel.entries) {
            val target = TutoTarget.of(style)!!
            for (s in TutoCourse.of(style)!!.chaptersFor(level).flatMap { it.steps }) {
                if (!s.check.startsWith("changed:drums:")) continue
                val part = s.check.removePrefix("changed:drums:")
                val n = count.find(s.textFor(level))?.groupValues?.get(1)?.toInt() ?: 1
                if (n > 1) counted++
                val p = upTo(style, level, s.id)
                val armed = TutoKind.of(s.check).arm(p)
                val lane = TutoChecks.laneOf(s.target.substringBefore(',').trim()).takeIf { it in TutoChecks.variationLanes() } ?: TutoChecks.OPEN_HAT_LANE
                fun with(hits: Int): Project = TutoChecks.inScene(p, part) { q ->
                    val free = (0 until q.drums.steps).filter { it !in q.drums.lanes[lane].hits }.take(hits)
                    q.copy(drums = free.fold(q.drums) { d, c -> d.setHit(lane, c, true) })
                }!!
                val id = "$style ${level.name} ${s.id} ($n asked)"
                if (n > 1 && TutoChecks.done(s, with(n - 1), target, armed)) bad += "$id: done at ${n - 1}: ${s.textFor(level)}"
                if (!TutoChecks.done(s, with(n), target, armed)) bad += "$id: not done at $n"
                val shown = TutoChecks.show(s, p, target, level)
                if (shown == null || !TutoChecks.done(s, shown, target, armed)) bad += "$id: « Je le fais » does not make it"
            }
        }
        assertTrue("a variation that counts its hits is read (house, Facile)", counted > 0)
        assertEquals("variation steps done before, or not at, what their words ask", emptyList<String>(), bad)
    }

    @Test
    fun facileCymbalOnTheDropAloneIsDone() {
        for (style in listOf("house", "speed_garage")) {
            val target = TutoTarget.of(style)!!
            val s = TutoCourse.of(style)!!.chaptersFor(TutoLevel.Facile).flatMap { it.steps }.first { it.check == "crash" }
            assertFalse("$style: the Facile words name the Drop only", "Drop 2" in s.textFor(TutoLevel.Facile))
            val p = upTo(style, TutoLevel.Facile, s.id)
            assertFalse("$style: not done before", TutoChecks.done(s, p, target, null))
            val one = TutoChecks.inScene(p, "Drop") { q -> q.copy(drums = q.drums.setHit(TutoChecks.CRASH_LANE, 0, true)) }!!
            assertTrue("$style: the Drop's cymbal, as the words ask: done", TutoChecks.done(s, one, target, null))
            val shown = TutoChecks.show(s, p, target)!!
            assertTrue("$style: « Je le fais »: done", TutoChecks.done(s, shown, target, null))
        }
    }

    @Test
    fun proCymbalStillWantsTheDropAndTheDrop2() {
        for (style in listOf("house", "speed_garage")) {
            val target = TutoTarget.of(style)!!
            val s = TutoCourse.of(style)!!.chaptersFor(TutoLevel.Pro).flatMap { it.steps }.first { it.check == "crash" }
            assertEquals("$style: Pro checks both", listOf("Drop", "Drop 2"), TutoChecks.partsOf(s))
            val p = upTo(style, TutoLevel.Pro, s.id)
            val one = TutoChecks.inScene(p, "Drop") { q -> q.copy(drums = q.drums.setHit(TutoChecks.CRASH_LANE, 0, true)) }!!
            assertFalse("$style: Pro, the Drop alone: not done", TutoChecks.done(s, one, target, null))
        }
    }
}
