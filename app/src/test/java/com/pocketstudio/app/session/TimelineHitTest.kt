package com.pocketstudio.app.session

import androidx.compose.ui.graphics.Color
import com.pocketstudio.model.PartSpan
import com.pocketstudio.model.SongPart
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineHitTest {
    private val rows = listOf(
        TimelineRow(SongPart.DRUMS, "Batterie", Color.Red),
        TimelineRow(SongPart.ACID, "Basse acide", Color.Green),
    )
    private val lanes = listOf(TimelineLane(0, SongPart.DRUMS, "Volume · Batterie"), TimelineLane(3, null, "Volume · Sortie générale"))

    @Test
    fun curvesGoUnderTheirTrackAndTheMixLast() {
        val lines = timelineLines(rows, lanes, showLanes = true)
        assertEquals(listOf(SongPart.DRUMS, null, SongPart.ACID, null), lines.map { it.row?.key })
        assertEquals(listOf(null, 0, null, 3), lines.map { it.lane?.index })
        assertEquals(2, timelineLines(rows, lanes, showLanes = false).size)
    }

    @Test
    fun aTouchFindsItsPartItsTrackAndItsBar() {
        val lines = timelineLines(rows, lanes, showLanes = true)
        val heights = floatArrayOf(64f, 34f, 64f, 34f)
        val tops = floatArrayOf(0f, 64f, 98f, 162f)
        // Two parts: 2 bars, then 1 bar; 2 px a step (32 px a bar).
        val spans = listOf(PartSpan(0, 0, 32), PartSpan(1, 32, 16))
        fun hit(x: Float, y: Float, scroll: Float = 0f) = timelineHit(x, y, scroll, 2f, 16, spans, lines, tops, heights)
        assertEquals(TimelineHit(0, SongPart.DRUMS, null, 0), hit(10f, 10f))
        assertEquals(TimelineHit(0, SongPart.DRUMS, null, 1), hit(40f, 10f))
        assertEquals(TimelineHit(0, null, 0, 0), hit(10f, 70f))
        assertEquals(TimelineHit(0, SongPart.ACID, null, 0), hit(10f, 100f))
        // Scrolled: the second part.
        assertEquals(TimelineHit(1, null, 3, 0), hit(30f, 170f, scroll = 40f))
        // Past the song's end, below the last line: nothing.
        assertNull(hit(100f, 10f))
        assertNull(hit(10f, 300f))
    }

    @Test
    fun theFollowTurnsThePageOnlyWhenThePlayheadLeavesTheView() {
        // A 1000 px view at 0, the song 4000 px long (max scroll 3000).
        assertNull(followScroll(100f, 0f, 1000f, 3000f))
        assertNull(followScroll(849f, 0f, 1000f, 3000f))
        // Past 85 %: the next page, the playhead 8 % into it.
        assertEquals(780f, followScroll(860f, 0f, 1000f, 3000f)!!, 0.01f)
        // The song started again at bar 1 while the view was far on (Stop then Lecture): back to the start.
        assertEquals(0f, followScroll(0f, 1900f, 1000f, 3000f)!!, 0.01f)
        // A jump far ahead (a part chosen to play): straight there.
        assertEquals(2420f, followScroll(2500f, 0f, 1000f, 3000f)!!, 0.01f)
        // At the song's end the page cannot turn: nothing, no glide on the spot.
        assertNull(followScroll(3950f, 3000f, 1000f, 3000f))
        // Nothing to follow before the screen is measured.
        assertNull(followScroll(500f, 0f, 0f, 0f))
    }

    @Test
    fun theRowsShareTheHeightBetweenAMinimumAndAMaximum() {
        // Seven tracks and a curve on 700 dp: the rows fill it.
        assertEquals((700.dp - LANE_H) / 7, timelineRowHeight(700.dp, 7, 1, tall = false))
        // Two tracks: never giant rows.
        assertEquals(ROW_MAX, timelineRowHeight(700.dp, 2, 0, tall = false))
        // A small screen in landscape: never thinner than the minimum (the rows scroll).
        assertEquals(ROW_MIN, timelineRowHeight(250.dp, 7, 1, tall = false))
        // « Pistes plus hautes »: taller, within its own bounds.
        val tall = timelineRowHeight(250.dp, 7, 1, tall = true)
        assertTrue(tall >= ROW_TALL_MIN && tall <= ROW_TALL_MAX)
    }

    @Test
    fun twoTracksOfTheSameColourGetTwoColoursOnTheTimeline() {
        val green = Color(0xFF81C784)
        val blue = Color(0xFF4FC3F7)
        val out = timelineColors(listOf(Color(0xFF7986CB), green, blue, green, Color(0xFFBA68C8)))
        // The first green kept, the second one changed for a colour far from every other track.
        assertEquals(green, out[1])
        assertNotEquals(green, out[3])
        out.forEachIndexed { i, a -> out.forEachIndexed { j, b -> if (i < j) assertTrue("$i $j", colorDistance(a, b) >= COLOR_CLASH) } }
        // Distinct colours stay as they are.
        assertEquals(listOf(green, blue), timelineColors(listOf(green, blue)))
    }

    @Test
    fun theRulerNumbersThinOutAsItZoomsOut() {
        assertEquals(1, numberEvery(100f, 60f))
        assertEquals(4, numberEvery(20f, 60f))
    }
}
