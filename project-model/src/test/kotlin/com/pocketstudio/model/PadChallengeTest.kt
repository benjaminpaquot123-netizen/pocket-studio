package com.pocketstudio.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pad Live's challenges (lot B3, « refais ce rythme »): the rhythms, the score, the stars kept; the take's pads kept for « Pistes séparées ». */
class PadChallengeTest {
    private val shots = listOf(8, 9, 10)

    /** [c] played back by a hand [lateMs] late on its first touch and [driftMs] off on every other, [pads] as they come. */
    private fun play(c: PadChallenge.Challenge, lateMs: Long = 0, driftMs: (Int) -> Long = { 0 }, pads: (Int) -> Int = { c.hits[it].pad }) =
        c.hits.indices.map { i -> PadChallenge.Tap(pads(i), 10_000 + lateMs * (if (i == 0) 1 else 0) + Math.round((c.hits[i].beat - c.hits[0].beat) * c.beatMs) + driftMs(i)) }

    @Test
    fun eachLevelIsABarOnTheCoupPadsAtATempoAChildKeepsUpWith() {
        assertEquals(6, PadChallenge.LEVELS.size)
        PadChallenge.LEVELS.forEach { l ->
            assertTrue(l.notes.all { it.beat >= 0.0 && it.beat < 4.0 && it.voice in 0..2 })
            assertEquals(l.notes.sortedBy { it.beat }, l.notes)
        }
        val c = PadChallenge.make(2, shots, 124.0)!!
        assertEquals(listOf(8, 9, 8, 9), c.hits.map { it.pad })
        assertEquals(120.0, c.bpm, 0.0)
        assertEquals(80.0, PadChallenge.make(1, shots, 60.0)!!.bpm, 0.0)
        // One « coup » pad: every voice on it.
        assertEquals(setOf(8), PadChallenge.make(6, listOf(8), 100.0)!!.hits.map { it.pad }.toSet())
        assertNull(PadChallenge.make(1, emptyList(), 100.0))
        // Past the last level: the last one.
        assertEquals(6, PadChallenge.make(99, shots, 100.0)!!.level)
    }

    @Test
    fun playedInTimeIsThreeStarsAndAFirstTouchALittleLateIsForgiven() {
        val c = PadChallenge.make(3, shots, 100.0)!!
        val perfect = PadChallenge.score(c, play(c))
        assertEquals(c.hits.size, perfect.right)
        assertEquals(3, perfect.stars)
        assertTrue(perfect.won)
        // Everything 60 ms behind the first touch (it came 60 ms early): lined up, still three stars.
        val shifted = PadChallenge.score(c, play(c, driftMs = { if (it == 0) 0 else 60 }))
        assertEquals(3, shifted.stars)
        assertEquals(0, shifted.tendency)
    }

    @Test
    fun aWrongPadOrAMissedHitCostsStarsAndRushingIsSaid() {
        val c = PadChallenge.make(2, shots, 100.0)!!
        // The third hit on the wrong pad: 3 of 4, two stars.
        val wrong = PadChallenge.score(c, play(c, pads = { if (it == 2) 10 else c.hits[it].pad }))
        assertEquals(3, wrong.right)
        assertEquals(listOf(true, true, false, true), wrong.matched)
        assertEquals(2, wrong.stars)
        // Half of it: one star; nothing right: none.
        assertEquals(1, PadChallenge.score(c, play(c).take(2)).stars)
        assertEquals(0, PadChallenge.score(c, listOf(PadChallenge.Tap(10, 0), PadChallenge.Tap(10, 600))).stars)
        assertEquals(0, PadChallenge.score(c, emptyList()).stars)
        // Each touch a little earlier than the last (rushing): still right, and said.
        val rushed = PadChallenge.score(c, play(c, driftMs = { -it * 50L }))
        assertEquals(4, rushed.right)
        assertEquals(-1, rushed.tendency)
        val dragged = PadChallenge.score(c, play(c, driftMs = { it * 50L }))
        assertEquals(1, dragged.tendency)
        // Far off its place (more than a sixteenth): not counted.
        val late = PadChallenge.score(c, play(c, driftMs = { if (it == 3) 400 else 0 }))
        assertFalse(late.matched[3])
    }

    @Test
    fun theBestStarsAreKeptAndTheNextChallengeIsTheFirstNotWon() {
        var set = PadLiveSet()
        assertEquals(1, set.nextChallenge)
        set = set.withStars(1, 3)
        assertEquals(2, set.nextChallenge)
        set = set.withStars(2, 1)
        assertEquals(2, set.nextChallenge)
        assertEquals(set, set.withStars(1, 2))  // never worse
        set = set.withStars(2, 2)
        assertEquals(3, set.nextChallenge)
        assertEquals(listOf(3, 2), set.stars)
        assertEquals(set, set.withStars(0, 3))
        assertEquals(set, set.withStars(99, 3))
        // Every challenge won: the last one again.
        PadChallenge.LEVELS.indices.forEach { set = set.withStars(it + 1, 3) }
        assertEquals(PadChallenge.LEVELS.size, set.nextChallenge)
    }

    @Test
    fun theStarsAndTheTakesPadsAreSavedAndLeftOutAtTheirDefaults() {
        val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
        assertEquals("{}", json.encodeToString(PadLiveSet.serializer(), PadLiveSet()))
        val take = PadTakeRecord(
            track = 2, start = 16, end = 48, bpm = 124.0,
            spans = listOf(PadSpanRecord.of(LivePadTake.Span(0, 32.0, 48.0, velocity = 0.5f)), PadSpanRecord.of(LivePadTake.Span(9, 36.0, Double.POSITIVE_INFINITY, oneShot = true))),
            clips = mapOf(0 to LiveClip("k.wav", "Kick", 4), 9 to LiveClip("h.wav", "Klaxon", 1, oneShot = true, release = 0.5f)),
        )
        val set = PadLiveSet(home = true, stars = listOf(3, 1), take = take)
        val back = json.decodeFromString(PadLiveSet.serializer(), json.encodeToString(PadLiveSet.serializer(), set))
        assertEquals(set, back)
        assertEquals(listOf(LivePadTake.Span(0, 32.0, 48.0, velocity = 0.5f), LivePadTake.Span(9, 36.0, Double.POSITIVE_INFINITY, oneShot = true)), back.take!!.spans.map { it.span() })
        assertEquals(listOf(0, 9), back.take!!.pads)
        assertEquals(0.5f, back.take!!.clipList()[9]!!.release, 0f)
    }
}
