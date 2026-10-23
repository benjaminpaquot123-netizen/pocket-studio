package com.pocketstudio.app.session

import androidx.compose.ui.graphics.Color
import com.pocketstudio.model.PartSpan
import com.pocketstudio.model.SongPart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
