package com.pocketstudio.app.remix

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.EmptyCoroutineContext

class StemBoardModelTest {
    private val rate = BOARD_RATE

    // --- Peaks ---

    /** A stem of [frames] frames, mono, whose sample i is [value] (i). */
    private fun peaksOf(frames: Long, value: (Long) -> Float) =
        StemPeaks.of(frames, 1) { start, count -> FloatArray(count) { value(start + it) } }

    @Test
    fun peaksAreReadOnceAStemOffTheCallerAndScaledToTheirLoudest() = runBlocking {
        val asked = mutableListOf<Int>()
        val peaks = BoardPeaks()
        // Stem s peaks at 0.1 * (s + 1); the voice (3) is all silence but for a whisper.
        val source = StemPeakSource { stem ->
            asked += stem
            if (stem == 3) peaksOf(10_000) { 0.01f } else peaksOf(48_000) { i -> if (i == 20_000L) 0.1f * (stem + 1) else 0.05f }
        }
        assertNull(peaks.of(0))
        peaks.load(source, EmptyCoroutineContext)
        peaks.load(source, EmptyCoroutineContext)  // again: nothing read twice
        assertEquals(listOf(0, 1, 2, 3), asked)
        assertEquals(4, peaks.ready)
        for (stem in 0..2) {
            assertEquals(1f / (0.1f * (stem + 1)), peaks.scale(stem), 1e-4f)
            val out = FloatArray(4)
            peaks.of(stem)!!.fill(0, 48_000, out)
            // The slot with the peak reaches 1 once scaled; the others stay low.
            assertEquals(1f, out[1] * peaks.scale(stem), 1e-4f)
            assertTrue(out[3] * peaks.scale(stem) < 0.6f)
        }
        // Silence is not blown up into noise.
        assertEquals(1f, peaks.scale(3), 0f)
    }

    @Test
    fun aStemThatCannotBeReadStaysFlatAndTheOthersShow() = runBlocking {
        val peaks = BoardPeaks()
        peaks.load(StemPeakSource { stem -> if (stem == 1) error("closed") else peaksOf(1_000) { 0.5f } }, EmptyCoroutineContext)
        assertEquals(4, peaks.ready)
        assertNull(peaks.of(1))
        assertEquals(2f, peaks.scale(2), 1e-6f)
    }

    // --- Beats ---

    @Test
    fun theGridSnapsToTheNearestBeatFromTheFirstOne() {
        val grid = BeatGrid.of(120.0, 0.25, rate)!!
        assertEquals(24_000.0, grid.beat, 0.0)
        assertEquals(96_000.0, grid.bar, 0.0)
        assertEquals(12_000L, grid.snap(0))           // the first beat, 0.25 s in
        assertEquals(12_000L, grid.snap(23_999))
        assertEquals(36_000L, grid.snap(24_001))
        assertEquals(12_000L + 24_000L * 10, grid.snap(12_000L + 24_000L * 10 + 11_000))
        assertEquals(1, grid.barOpenedBy(0))
        assertEquals(0, grid.barOpenedBy(3))
        assertEquals(3, grid.barOpenedBy(8))
        assertEquals(0, grid.barOpenedBy(-4))
        assertEquals(0L, grid.firstBeatFrom(12_000.0))
        assertEquals(1L, grid.firstBeatFrom(12_001.0))
        assertEquals(0L, grid.firstBeatFrom(0.0))
        assertEquals(-2L, BeatGrid.of(120.0, 1.0, rate)!!.firstBeatFrom(0.0))
        assertNull(BeatGrid.of(0.0, 0.0, rate))
    }

    // --- What is on screen ---

    @Test
    fun theFirstLayoutShowsTheWholeSongAndPinchZoomsAroundTheFingers() {
        val frames = 200L * rate
        val s = StemBoardView(frames)
        s.layout(1000f)
        assertEquals(frames / 1000.0, s.framesPerPx, 1e-9)
        assertEquals(0.0, s.start, 0.0)
        assertFalse(s.zoomedIn)
        // Zooming in 4 times around x = 600: the frame under the fingers stays there.
        val under = s.frameAt(600f)
        s.zoomBy(4f, 600f)
        assertEquals(frames / 4000.0, s.framesPerPx, 1e-9)
        assertEquals(under.toDouble(), s.start + 600 * s.framesPerPx, 2.0)
        assertTrue(s.zoomedIn)
        // Never deeper than the deepest zoom, never further out than the whole song.
        s.zoomBy(1e6f, 0f)
        assertEquals(StemBoardView.MIN_FRAMES_PER_PX, s.framesPerPx, 0.0)
        s.zoomBy(1e-6f, 500f)
        assertEquals(frames / 1000.0, s.framesPerPx, 1e-9)
        assertEquals(0.0, s.start, 0.0)
    }

    @Test
    fun scrollingFollowsTheFingerAndStopsAtBothEnds() {
        val frames = 100L * rate
        val s = StemBoardView(frames)
        s.layout(1000f)
        s.zoomBy(10f, 0f)  // a tenth of the song on screen
        s.scrollBy(-300f)  // finger to the left: later frames come in
        assertEquals(300 * s.framesPerPx, s.start, 1e-6)
        s.scrollBy(100f)
        assertEquals(200 * s.framesPerPx, s.start, 1e-6)
        s.scrollBy(1e6f)
        assertEquals(0.0, s.start, 0.0)
        s.scrollBy(-1e7f)
        assertEquals(frames - 1000 * s.framesPerPx, s.start, 1e-6)
        assertEquals(frames, s.frameAt(1000f))
        // « Tout voir »: the whole song again.
        s.fit()
        assertFalse(s.zoomedIn)
        assertEquals(0.0, s.start, 0.0)
    }

    @Test
    fun turningThePhoneKeepsTheSamePartOfTheSong() {
        val s = StemBoardView(100L * rate)
        s.layout(1000f)
        s.zoomBy(5f, 0f)
        s.scrollBy(-400f)
        val first = s.frameAt(0f)
        val last = s.frameAt(1000f)
        s.layout(2000f)
        assertEquals(first, s.frameAt(0f))
        assertEquals(last.toDouble(), s.frameAt(2000f).toDouble(), 2.0)
    }

    // --- The stretch ---

    @Test
    fun aSlideChoosesTheBeatsItCoversWithTheMagnet() {
        val grid = BeatGrid.of(120.0, 0.0, rate)!!
        val frames = 60L * rate
        // From just after beat 2 to just before beat 6: beats 2 to 6.
        assertEquals(48_000L until 144_000L, BoardSelect.drag(50_000, 140_000, grid, frames))
        // Right to left gives the same.
        assertEquals(48_000L until 144_000L, BoardSelect.drag(140_000, 50_000, grid, frames))
        // A tiny slide still gives a beat, in the direction of the finger.
        assertEquals(48_000L until 72_000L, BoardSelect.drag(48_100, 49_000, grid, frames))
        assertEquals(24_000L until 48_000L, BoardSelect.drag(48_100, 47_000, grid, frames))
        // Never past the end.
        assertEquals(frames - 24_000 until frames, BoardSelect.drag(frames - 100, frames + 9_000, grid, frames))
    }

    @Test
    fun withoutTheMagnetTheStretchIsExactlyWhereTheFingerWent() {
        assertEquals(50_123L until 140_456L, BoardSelect.drag(50_123, 140_456, null, 1_000_000))
        assertEquals(1_000L until 1_480L, BoardSelect.drag(1_000, 1_001, null, 1_000_000))
        assertEquals(0L until 10_000L, BoardSelect.drag(-500, 10_000, null, 1_000_000))
    }

    @Test
    fun aHandleMovesOneEdgeSnappedAndKeepsAtLeastABeat() {
        val grid = BeatGrid.of(120.0, 0.0, rate)!!
        val frames = 60L * rate
        assertEquals(24_000L until 144_000L, BoardSelect.moveEdge(48_000, 144_000, -1, 30_000, grid, frames))
        assertEquals(48_000L until 192_000L, BoardSelect.moveEdge(48_000, 144_000, 1, 200_000, grid, frames))
        // Dragged past the other edge: stops a beat before it.
        assertEquals(120_000L until 144_000L, BoardSelect.moveEdge(48_000, 144_000, -1, 500_000, grid, frames))
        assertEquals(48_000L until 72_000L, BoardSelect.moveEdge(48_000, 144_000, 1, 0, grid, frames))
        // Without the magnet: exact.
        assertEquals(31_234L until 144_000L, BoardSelect.moveEdge(48_000, 144_000, -1, 31_234, null, frames))
    }

    @Test
    fun theSelectionCarriesTheFadesChosenAndHandlesAreFoundUnderTheFinger() {
        val s = StemBoardView(60L * rate)
        s.layout(1000f)
        assertNull(s.selection("song"))
        s.select(3, 288_000, 96_000)
        s.fade = BoardFade.Medium
        assertEquals(StemSelection("song", 3, 96_000, 288_000, 15, 60), s.selection("song"))
        s.fade = BoardFade.None
        assertEquals(0, s.selection("song")!!.fadeInMs)
        val x0 = s.x(96_000L)
        val x1 = s.x(288_000L)
        assertEquals(-1, s.edgeAt(3, x0 + 5f, 20f))
        assertEquals(1, s.edgeAt(3, x1 - 5f, 20f))
        assertEquals(0, s.edgeAt(3, (x0 + x1) / 2, 20f))
        assertEquals(0, s.edgeAt(2, x0, 20f))  // another stem: no handle
        s.clearSelection()
        assertFalse(s.hasSelection)
    }

    @Test
    fun clockSaysMinutesSecondsAndTenths() {
        assertEquals("0:00.0", boardClock(0))
        assertEquals("0:03.9", boardClock(187_200))
        assertEquals("1:05.3", boardClock(65L * rate + 14_400))
    }
}
