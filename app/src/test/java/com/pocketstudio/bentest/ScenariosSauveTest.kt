package com.pocketstudio.bentest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The « save, projects and export » theme of the catalogue: in the run, each one with a gesture the run knows. */
class ScenariosSauveTest {
    private val sauve = Scenarios.LISTE.filter { it.id.startsWith("sav_") }

    @Test
    fun saveThemeIsInTheRunWithItsOwnIds() {
        assertTrue(sauve.size.toString(), sauve.size >= 15)
        assertEquals(ScenariosSauve.LISTE.map { it.id }, sauve.map { it.id })
        val ids = Scenarios.LISTE.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun eachExportsARealStyleInAKnownFormatNoRepeat() {
        val styles = com.pocketstudio.model.StarterWizard.STYLES.map { it.id }.toSet()
        val deja = Scenarios.EXPORTS_STYLES.toSet() + listOf(Scenarios.STYLE to "m4a", Scenarios.STYLE to "wav", Scenarios.STYLE to "flac")
        for (s in sauve) {
            val g = s.geste!!.split(':')
            assertEquals(s.id, "export", g[0])
            assertTrue(s.id, g[1] in styles)
            assertTrue(s.id, g[2] in ToutTester.PREVU.keys && g[2] != "video")
            assertTrue(s.id, (g[1] to g[2]) !in deja)
            assertTrue(s.id, s.oracle.contains("disque") && s.estimeS > 0 && s.estimeS * 1000L <= s.maxMs)
        }
        assertEquals(sauve.size, sauve.map { it.geste }.toSet().size)
        // Every format and the three levels it reads are there.
        assertEquals(setOf("wav", "flac", "m4a"), sauve.map { it.geste!!.split(':')[2] }.toSet())
    }

    @Test
    fun eachIsPlayedInTheFullPackOnly() {
        val complet = Scenarios.liste(Scenarios.COMPLET).map { it.id }
        val rapide = Scenarios.liste(Scenarios.RAPIDE).map { it.id }
        for (s in sauve) {
            assertTrue(s.id, s.id in complet)
            assertTrue(s.id, s.id !in rapide)
        }
    }
}
