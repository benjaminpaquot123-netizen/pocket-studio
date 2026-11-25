package com.pocketstudio.bentest

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** « Test 3 sur 12 » on « Le robot travaille… » and in the logbook (chef, 2026-11-24, asked by Benjamin). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class CompteurTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val tmp = TemporaryFolder()

    private fun etape(name: String, fails: Boolean = false) = Etape(name, "Étape $name", 1_000) { if (fails) error("raté exprès") else "ok" }

    @Test
    fun theCounterAdvancesAndTheLogbookSaysEachResult() {
        val carnet = Carnet(File(tmp.root, "c.tsv"))
        val steps = listOf(etape("a"), etape("b", fails = true), etape("c"))
        val passage = Passage(carnet, steps, compteur = true)
        val seen = ArrayList<Pair<Int, Int>>()
        passage.run { e -> seen += passage.numero(e) to passage.total }
        assertEquals(listOf(1 to 3, 2 to 3, 3 to 3), seen)
        val notes = carnet.lines().filter { it.kind == Carnet.NOTE }.map { it.text }
        assertEquals(listOf("Test 1 sur 3 : réussi", "Test 2 sur 3 : raté", "Test 3 sur 3 : réussi"), notes)
    }

    @Test
    fun theTotalIsTheLaunchedList() {
        val all = listOf("ouverture", "x", "y", "z").map { etape(it) }
        val retest = Passage(Carnet(File(tmp.root, "r.tsv")), Robot.subset(all, Robot.TOUT, setOf("y")), compteur = true)
        // The opening always comes first: 2 tests launched, not 4.
        assertEquals(2, retest.total)
        assertEquals(2, retest.numero(all[2]))
        assertEquals(4, Passage(Carnet(File(tmp.root, "t.tsv")), Robot.subset(all, Robot.TOUT, null), compteur = true).total)
    }

    @Test
    fun theScreenShowsTheCounterAboveTheStep() {
        compose.setContent { BenTestScreen(Robot.Etat(running = true, step = "Étape b", numero = 3, total = 12), emptyList(), BenTestActions()) }
        compose.onNodeWithTag("bentest_compteur").assertTextEquals("Test 3 sur 12")
        compose.onNodeWithTag("bentest_compteur_barre").assertExists()
        compose.onNodeWithTag("bentest_step").assertExists()
    }
}
