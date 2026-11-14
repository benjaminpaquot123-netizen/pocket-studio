package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** Audit ultra round 1, zone 3: one length for every screen (n° 23), a copy's name never cut (n° 26). */
class Zone3ModelTest {
    private val dir = Files.createTempDirectory("zone3").toFile()
    private val catalog = ProjectCatalog(File(dir, "projects"), File(dir, "trash"))

    /** Seat F d: « Mes projets » said « 1 mes. », the home « 0:05 · 1 partie », the studio « 6 mesures · 0:04 » for one song. */
    @Test
    fun theSongsLengthIsCountedOnceForEveryScreen() {
        // A two-bar part played three times at 300 BPM: 6 bars, 4.8 s.
        val loop = Project(name = "Rapide", tempoBpm = 300.0).let { p -> p.copy(drums = p.drums.copy(steps = 32)) }.sanitized()
        val p = loop.insertSongPart(0, 0).withSongRepeats(0, 3)
        val length = SongLength.of(p)
        assertEquals(6, length.bars)
        assertEquals("rounded, not cut", 5, length.seconds)
        assertEquals(1, length.parts)
        // « Mes projets » reads the same numbers from the saved project.
        val id = catalog.create(p)
        val info = catalog.info(id)!!
        assertEquals(SongLength(info.bars, info.seconds, info.parts), length)
        // Without a song: the loop shown.
        assertEquals(SongLength(2, 2, 0), SongLength.of(loop))
        dir.deleteRecursively()
    }

    /** Seat F d: « Mon premier rythme (copie) (copie) » was cut to « … (copie) » without a word. */
    @Test
    fun aCopysNameKeepsItsWordWithinTheLimit() {
        val name = "Mon premier rythme qui est assez long"  // 37 letters
        val a = catalog.create(Project(name = name))
        val first = catalog.copyName(catalog.info(a)!!.name)
        assertTrue(first, first.endsWith(" (copie)") && first.length <= ProjectCatalog.MAX_NAME)
        catalog.duplicate(a, first)
        val second = catalog.copyName(first)
        assertTrue(second, second.endsWith(" (copie 2)") && second.length <= ProjectCatalog.MAX_NAME)
        assertEquals("Court (copie)", catalog.copyName("Court"))
        dir.deleteRecursively()
    }
}
