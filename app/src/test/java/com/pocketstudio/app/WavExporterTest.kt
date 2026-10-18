package com.pocketstudio.app

import com.pocketstudio.app.export.WavExporter
import com.pocketstudio.engine.OfflineExport
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class WavExporterTest {
    private val time = LocalDateTime.of(2026, 10, 18, 22, 15, 3)

    @Test
    fun fileNameIsSafeAndDated() {
        assertEquals("Mon-premier-rythme-20261018-221503.wav", WavExporter.fileName("Mon premier rythme", time))
        assertEquals("Acideété-20261018-221503.wav", WavExporter.fileName("Acide/été?:*", time))
        assertEquals("pocket-studio-20261018-221503.wav", WavExporter.fileName("  ../// ", time))
    }

    @Test
    fun framesForBars() {
        assertEquals(96000L, OfflineExport.framesFor(1, 120.0))  // 2 s
        assertEquals(4 * 4 * 60 * 48000L / 90, OfflineExport.framesFor(4, 90.0))
    }
}
