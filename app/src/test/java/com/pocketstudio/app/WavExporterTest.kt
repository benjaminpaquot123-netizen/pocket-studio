package com.pocketstudio.app

import com.pocketstudio.app.export.WavExporter
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.takeWhole
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

    /**
     * Audit ultra round 1 (n° 17, seats A c, C a, E d, F c, B b): every script's letters stay (the
     * « ? » the seats saw came from the bench's JVM, whose file names were ASCII: see
     * [benchFileNamesKeepTheirLetters]), Hindi's vowel signs too, « & » stays without « -- », and no
     * word comes twice (« Mon-morceau-morceau »).
     */
    @Test
    fun everyLetterStaysAndNoWordTwice() {
        assertEquals("Mój-pierwszy-bit-pętla-20261018-221503.wav", WavExporter.fileName("Mój pierwszy bit-pętla", time))
        assertEquals("テクノ-曲-20261018-221503.wav", WavExporter.fileName("テクノ 曲", time))
        assertEquals("मेरा-गाना-20261018-221503.wav", WavExporter.fileName("मेरा गाना", time))
        assertEquals("Drum-&-Bass-song-20261018-221503.wav", WavExporter.fileName("Drum & Bass-song", time))
        assertEquals("Mon morceau", WavExporter.nameWith("Mon morceau", "morceau"))
        assertEquals("My song", WavExporter.nameWith("My song", "song"))
        assertEquals("Ma Mix-morceau-club", WavExporter.nameWith("Ma Mix", "morceau", "club"))
        assertEquals("Club night-morceau", WavExporter.nameWith("Club night", "morceau", "club", null))
    }

    /** Relecture tour 1 (mineur): the 40 chars kept never cut a letter from its sign, nor a surrogate pair. */
    @Test
    fun theCutKeepsLettersWhole() {
        val a39 = "a".repeat(39)
        assertEquals("$a39-20261018-221503.wav", WavExporter.fileName(a39 + "e\u0301xyz", time))
        assertEquals("$a39-20261018-221503.wav", WavExporter.fileName(a39 + "\uD840\uDC00xyz", time))
        assertEquals("ab", "ab\uD840\uDC00".takeWhole(3))
    }

    /** The bench runs with LC_ALL=C.UTF-8 (app/build.gradle.kts): a file written with « ó » is listed with it, as on a phone. */
    @Test
    fun benchFileNamesKeepTheirLetters() {
        val dir = java.nio.file.Files.createTempDirectory("names").toFile()
        try {
            java.io.File(dir, "Mój-テクノ.wav").writeText("x")
            assertEquals(listOf("Mój-テクノ.wav"), dir.list()!!.toList())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun framesForBars() {
        assertEquals(96000L, OfflineExport.framesFor(1, 120.0))  // 2 s
        assertEquals(4 * 4 * 60 * 48000L / 90, OfflineExport.framesFor(4, 90.0))
    }
}
