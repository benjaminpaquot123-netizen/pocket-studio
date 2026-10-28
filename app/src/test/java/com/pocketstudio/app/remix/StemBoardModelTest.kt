package com.pocketstudio.app.remix

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.EmptyCoroutineContext

class StemBoardModelTest {
    private val rate = BOARD_RATE

    // --- Peaks read from the audio ---

    @Test
    fun peaksAreTheLoudestSampleOfEachSliceAcrossReads() {
        // 3 s of stereo silence with three clicks: one per slice they fall in, in either channel.
        val frames = 3L * rate
        val audio = FloatArray((frames * 2).toInt())
        audio[(1000 * 2)] = 0.5f                // slice 0 (left)
        audio[(70_000 * 2) + 1] = -0.8f         // slice 1 (right, negative)
        audio[(140_000 * 2)] = 1.7f             // slice 2, clipped to 1
        val reads = mutableListOf<Pair<Long, Long>>()
        val source = stemPeaksFrom(frames) { stem, start, count ->
            assertEquals(2, stem)
            reads += start to count
            audio.copyOfRange((start * 2).toInt(), ((start + count) * 2).toInt())
        }
        val peaks = source.peaks(2, 65_536)
        assertEquals(3, peaks.size)  // 144 000 frames in slices of 65 536: the last one shorter
        assertEquals(0.5f, peaks[0], 0f)
        assertEquals(0.8f, peaks[1], 0f)
        assertEquals(1f, peaks[2], 0f)
        // Read a few seconds at a time, never the whole stem, and every frame exactly once.
        assertTrue(reads.size > 1)
        assertTrue(reads.all { it.second <= 1L shl 17 })
        assertEquals(frames, reads.sumOf { it.second })
        assertEquals(0L, reads.first().first)
    }

    @Test
    fun peaksStopAtTheEndOfWhatCanBeRead() {
        val source = stemPeaksFrom(10_000) { _, _, _ -> FloatArray(0) }
        assertArrayEquals(FloatArray(2), source.peaks(0, 5_000), 0f)
    }

    // --- Zoom levels ---

    @Test
    fun levelsHalveFromTheFinestAndScaleTheLoudestPeakToOne() {
        val fine = floatArrayOf(0.1f, 0.4f, 0.2f, 0.05f, 0.3f)
        val levels = PeakLevels.build(fine, 3)
        assertArrayEquals(floatArrayOf(0.25f, 1f, 0.5f, 0.125f, 0.75f), levels[0], 1e-6f)
        assertArrayEquals(floatArrayOf(1f, 0.5f, 0.75f), levels[1], 1e-6f)
        assertArrayEquals(floatArrayOf(1f, 0.75f), levels[2], 1e-6f)
        // Silence is not blown up into noise.
        assertArrayEquals(floatArrayOf(0.01f, 0f), PeakLevels.build(floatArrayOf(0.01f, 0f), 1)[0], 0f)
    }

    @Test
    fun aThreeMinuteSongHasFewBucketsAndManyLevels() {
        val levels = PeakLevels(3L * 60 * rate)
        assertEquals(128, levels.base)  // 8 640 000 / 64 would be over 131 072 buckets
        assertTrue(levels.count >= 10)
        assertTrue(3L * 60 * rate / levels.framesPerBucket(levels.count - 1) > PeakLevels.MIN_COARSE / 2)
        // The coarsest level no wider than a column: each column reads a bucket or two.
        assertEquals(0, levels.levelFor(100.0))
        assertEquals(0, levels.levelFor(255.0))
        assertEquals(1, levels.levelFor(256.0))
        assertEquals(3, levels.levelFor(1500.0))
        assertEquals(levels.count - 1, levels.levelFor(1e12))
    }

    @Test
    fun peaksAreComputedOnceAStemOffTheCallerThenOnlyRead() = runBlocking {
        val frames = 60L * rate
        val asked = mutableListOf<Pair<Int, Int>>()
        val levels = PeakLevels(frames)
        val source = StemPeaks { stem, fpb ->
            asked += stem to fpb
            // Stem s: loud in its s-th quarter of the song only.
            FloatArray(((frames + fpb - 1) / fpb).toInt()) { i -> if ((i * 4L) / ((frames + fpb - 1) / fpb) == stem.toLong()) 0.5f else 0.05f }
        }
        assertFalse(levels.has(0))
        assertEquals(0f, levels.columnPeak(0, 0, 0, frames), 0f)
        levels.load(source, EmptyCoroutineContext)
        levels.load(source, EmptyCoroutineContext)  // again: nothing read twice
        assertEquals((0 until 4).map { it to levels.base }, asked)
        assertEquals(4, levels.ready)
        val quarter = frames / 4
        for (stem in 0 until 4) {
            for (l in 0 until levels.count) {
                // The loud quarter reads 1 (scaled), the others 0.1, at every zoom level.
                assertEquals(1f, levels.columnPeak(stem, l, stem * quarter + 1000, stem * quarter + 2000), 1e-6f)
                val other = (stem + 2) % 4
                assertEquals(0.1f, levels.columnPeak(stem, l, other * quarter + quarter / 2, other * quarter + quarter / 2 + 10), 1e-6f)
            }
        }
        // Out of the song: nothing.
        assertEquals(0f, levels.columnPeak(0, 0, frames, frames + 5000), 0f)
        assertEquals(0f, levels.columnPeak(0, 0, -5000, 0), 0f)
    }

    @Test
    fun aStemThatCannotBeReadStaysFlatAndTheOthersShow() = runBlocking {
        val levels = PeakLevels(10L * rate)
        levels.load(StemPeaks { stem, fpb -> if (stem == 1) error("closed") else FloatArray((10 * rate / fpb) + 1) { 0.5f } }, EmptyCoroutineContext)
        assertEquals(4, levels.ready)
        assertEquals(0f, levels.columnPeak(1, 0, 0, 10_000), 0f)
        assertEquals(1f, levels.columnPeak(2, 0, 0, 10_000), 0f)
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
        val s = StemBoardState(frames)
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
        assertEquals(StemBoardState.MIN_FRAMES_PER_PX, s.framesPerPx, 0.0)
        s.zoomBy(1e-6f, 500f)
        assertEquals(frames / 1000.0, s.framesPerPx, 1e-9)
        assertEquals(0.0, s.start, 0.0)
    }

    @Test
    fun scrollingFollowsTheFingerAndStopsAtBothEnds() {
        val frames = 100L * rate
        val s = StemBoardState(frames)
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
        val s = StemBoardState(100L * rate)
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
        val s = StemBoardState(60L * rate)
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
