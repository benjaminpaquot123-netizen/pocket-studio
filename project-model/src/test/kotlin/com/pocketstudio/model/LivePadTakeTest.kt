package com.pocketstudio.model

import com.pocketstudio.model.LivePadTake.Span
import com.pocketstudio.model.LivePadTake.Tap
import org.junit.Assert.assertEquals
import org.junit.Test

/** Phase 35: the live pads of a Live take, when each one sounded, as the engine plays them. */
class LivePadTakeTest {
    private val clips = listOf(
        LiveClip("a.wav", "Batterie", beats = 4),
        LiveClip("b.wav", "Impact", beats = 1, oneShot = true),
        LiveClip("c.wav", "Nappe", beats = 16),
        null,
    )

    @Test
    fun aTapWaitsForItsGridAndASecondOneStopsAtOnce() {
        // The loop of 4 beats waits for the next bar line (16 steps); 16 beats: a bar at most.
        val spans = LivePadTake.spans(clips, emptySet(), listOf(Tap(0, 3.0), Tap(2, 20.0), Tap(0, 41.5)), start = 0, end = 64, beatsPerBar = 4)
        assertEquals(listOf(Span(0, 16.0, 41.5), Span(2, 32.0, 64.0)), spans)
    }

    @Test
    fun aOneShotPlaysOnceOnItsBeatAndStopAllStopsTheLoops() {
        val spans = LivePadTake.spans(clips, setOf(0), listOf(Tap(1, 5.0), Tap(-1, 30.0), Tap(3, 31.0)), start = 0, end = 64, beatsPerBar = 4)
        assertEquals(listOf(Span(0, 0.0, 30.0), Span(1, 8.0, Double.POSITIVE_INFINITY, oneShot = true)), spans)
    }

    @Test
    fun aPadStoppedBeforeItStartsLeavesNothingAndNothingPastTheEnd() {
        val spans = LivePadTake.spans(clips, emptySet(), listOf(Tap(0, 3.0), Tap(0, 10.0), Tap(2, 70.0)), start = 0, end = 64, beatsPerBar = 4)
        assertEquals(emptyList<Span>(), spans)
        // Started before the take was armed (on the transport's clock): from the take's start.
        val before = LivePadTake.spans(clips, emptySet(), listOf(Tap(2, 1.0)), start = 32, end = 64, beatsPerBar = 4)
        assertEquals(listOf(Span(2, 32.0, 64.0)), before)
    }

    @Test
    fun aRowLaunchedStartsOnTheBarAndMovesAWaitingPadThere() {
        // Pad Live B2: the 4-beat loop and the hit of a row launched at step 3 start on the bar line
        // (16); the 1-beat loop tapped alone at step 2 waited for its beat (4): it is moved to the
        // bar line with its row; a row launched again stops the loop at once (plain taps).
        val withBeat = clips.toMutableList().apply { set(3, LiveClip("d.wav", "Charley", beats = 1)) }
        val taps = listOf(Tap(3, 2.0), Tap(0, 3.0, bar = true), Tap(1, 3.0, bar = true), Tap(3, 3.0, bar = true), Tap(0, 40.0))
        val spans = LivePadTake.spans(withBeat, emptySet(), taps, start = 0, end = 64, beatsPerBar = 4)
        assertEquals(listOf(Span(0, 16.0, 40.0), Span(1, 16.0, Double.POSITIVE_INFINITY, oneShot = true), Span(3, 16.0, 64.0)), spans)
        // A loop already playing when its row is launched goes on, from where it started.
        val playing = LivePadTake.spans(clips, setOf(0), listOf(Tap(0, 21.0, bar = true)), start = 0, end = 64, beatsPerBar = 4)
        assertEquals(listOf(Span(0, 0.0, 64.0)), playing)
        // On the bar line itself: at once; a 3/4 bar is 12 steps.
        val waltz = LivePadTake.spans(clips, emptySet(), listOf(Tap(1, 13.0, bar = true)), start = 0, end = 64, beatsPerBar = 3)
        assertEquals(listOf(Span(1, 24.0, Double.POSITIVE_INFINITY, oneShot = true)), waltz)
    }
}
