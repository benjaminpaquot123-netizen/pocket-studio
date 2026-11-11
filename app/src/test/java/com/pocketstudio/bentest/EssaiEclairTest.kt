package com.pocketstudio.bentest

import androidx.compose.ui.geometry.Rect
import com.pocketstudio.app.journey.Journey
import com.pocketstudio.app.journey.JourneyDriver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** The essai éclair's listening: 4 readings a second in the logbook, the dropouts counted from the start of the song. */
class EssaiEclairTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private object Waits : JourneyDriver {
        override fun count(tag: String, merged: Boolean) = 0
        override fun bounds(tag: String) = Rect.Zero
        override fun scrollTo(tag: String) = Unit
        override fun click(tag: String) = Unit
        override fun idle(ms: Long) = Thread.sleep(ms)
        override fun tags(pattern: Regex) = emptyList<String>()
    }

    @Test
    fun theSoundIsReadFourTimesASecond() {
        var reads = 0
        val sonde = object : Sonde {
            // Two dropouts before the song (not counted), then one every 4 readings.
            override fun lire() = Sonde.Lecture(xruns = 2 + reads++ / 4, cpu = 0.4f, peak = 0.5f, nonFinite = false, running = true)
        }
        val carnet = Carnet(java.io.File(tmp.root, "c.tsv"))
        val m = EssaiEclair(Journey(Waits), sonde, object : Appli {
            override fun ouvrirStudio() = Unit
            override fun export() = Appli.ExportVu(false, emptyList(), null)
            override fun capture(file: java.io.File) = false
            override fun demander(question: String, maxMs: Long): Boolean? = null
        }, carnet, tmp.root).mesurer("style_x", 2_000)
        assertTrue("${m.reads} readings in 2 s", m.reads in 7..8)
        assertEquals(m.reads, carnet.lines().count { it.kind == Carnet.MESURE })
        assertEquals((m.reads) / 4, m.xruns)
        assertTrue(m.text(), m.loudSeconds >= 1 && m.text().contains("décrochage"))
    }
}
