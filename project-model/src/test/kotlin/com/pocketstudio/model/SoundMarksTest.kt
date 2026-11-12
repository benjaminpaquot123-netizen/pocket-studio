package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** « Récents » and « Favoris » kept on the phone: safe to read back whatever is in the file. */
class SoundMarksTest {
    @Test
    fun recentsFirstOnceAndCapped() {
        var list = emptyList<String>()
        (0 until 40).forEach { list = SoundMarks.touched(list, "kit:${it % 16}") }
        assertEquals(16, list.size)
        list = SoundMarks.touched(list, "party:air_horn")
        assertEquals("party:air_horn", list.first())
        (0 until 30).forEach { list = SoundMarks.touched(list, "pack:tr909/kick_%02d.flac".format(it)) }
        assertEquals(SoundMarks.MAX_RECENTS, list.size)
    }

    @Test
    fun favoritesToggle() {
        val one = SoundMarks.toggled(emptyList(), "mine:abc123")
        assertEquals(listOf("mine:abc123"), one)
        assertEquals(emptyList<String>(), SoundMarks.toggled(one, "mine:abc123"))
    }

    @Test
    fun onlyWellFormedKeysAreReadBack() {
        val text = "kit:3\n../../etc/passwd\npack:tr909/kick_01.flac\nkit:3\n\npack:a/../b\nparty:air_horn\n" + "x".repeat(5000)
        assertEquals(listOf("kit:3", "pack:tr909/kick_01.flac", "party:air_horn"), SoundMarks.read(text, 10))
        assertEquals(emptyList<String>(), SoundMarks.read(null, 10))
        assertFalse(SoundMarks.isKey("pack:tr909/../x"))
        assertTrue(SoundMarks.isKey("pack:bruitages_maison/foley_cartes_03.flac"))
        assertEquals(listOf("kit:3"), SoundMarks.read(SoundMarks.write(listOf("kit:3", "bad key")), 10))
    }
}
