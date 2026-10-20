package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenresTest {
    @Test
    fun everyTemplateIsReadyToPlay() {
        assertEquals(6, Genres.ALL.size)
        Genres.ALL.forEach { (title, description, build) ->
            val p = build()
            assertEquals(title, p.name)
            assertTrue(description.isNotBlank())
            assertEquals(p, p.sanitized())
            assertTrue("$title: drums", p.drums.lanes.any { it.hits.isNotEmpty() })
            assertTrue("$title: something melodic", p.synthNotes.notes.isNotEmpty() || p.acid.steps.any { it.gate } || p.tracks.any { it.notes.notes.isNotEmpty() })
            assertEquals(p, ProjectJson.decode(ProjectJson.encode(p)))
        }
    }

    @Test
    fun tempoAndKeyFollowTheGenre() {
        val byName = Genres.ALL.associate { it.first to it.third() }
        assertEquals(174.0, byName.getValue("Drum & bass").tempoBpm, 0.0)
        assertEquals(Scale.Minor, byName.getValue("Trap").harmony.scale)
        assertTrue(byName.getValue("Lo-fi").swing > 0.0)
        assertEquals(1, byName.getValue("Trap").tracks.size)
        assertTrue(byName.getValue("Trap").drums.lanes[5].hits.values.any { it.repeats > 1 })
    }
}
