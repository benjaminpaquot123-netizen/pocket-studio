package com.pocketstudio.bentest

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * « Retester les corrections » (chef, 2026-11-24): the red and interrupted steps and the never played new
 * tests ticked on opening, the green ones not; every scenario offered under its group; a chosen one runs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class RetestCandidatsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val tmp = TemporaryFolder()

    private val ran = ArrayList<String>()
    private fun etape(name: String) = Etape(name, "Étape $name", 1_000) { ran += name; "ok" }

    /** « Tout tester »'s ids as the robot has them (no work: only the names count here). */
    private val tout = listOf("ouverture", "style_house", "perte_son", "latence", "ecran_eteint").map(::etape)

    /** The scenarios as the robot has them: [Scenarios.liste] (every id, the complete pack). */
    private val scenarios = Scenarios.liste(Scenarios.COMPLET).map { etape(it.id) }

    @Test
    fun everyScenarioIsOfferedOnceAndTheLoadTestsAreThere() {
        val all = Retest.avecScenarios(tout, scenarios)
        val ids = Retest.proposees(all).map { it.name }
        assertTrue(ids.toString(), "jouer_arreter_10" in ids && "fond_reprise_5" in ids)
        // An id « Tout tester » already has is offered once (its « Tout tester » version).
        assertEquals(ids.toString(), ids.size, ids.toSet().size)
        assertTrue(Scenarios.liste(Scenarios.COMPLET).all { it.id in ids })
        assertEquals("Charge", Retest.groupe("jouer_arreter_10"))
        assertEquals("Charge", Retest.groupe("fond_reprise_5"))
        assertEquals("Styles", Retest.groupe("style_house"))
        assertEquals("Épreuves du téléphone", Retest.groupe("perte_son"))
    }

    @Test
    fun aChosenScenarioRunsAfterTheOpeningAndBeforeTheScreenOff() {
        val all = Retest.avecScenarios(tout, scenarios)
        val steps = Robot.subset(all, Robot.TOUT, setOf("jouer_arreter_10", "fond_reprise_5", "ecran_eteint"))
        Passage(Carnet(File(tmp.root, "c.tsv")), steps).run()
        assertEquals(listOf("ouverture", "jouer_arreter_10", "fond_reprise_5", "ecran_eteint"), ran)
    }

    @Test
    fun theRedIsTickedTheGreenIsNotTheNewTestIs() {
        val carnet = Carnet(File(tmp.root, "c.tsv"))
        val steps = listOf(etape("ouverture"), Etape("style_house", "Style house", 1_000) { error("raté exprès") }, etape("latence"))
        Passage(carnet, steps).run()
        val last = Rapport.feux(carnet.lines(), steps).filterValues { it != null }
        val all = Retest.avecScenarios(tout, scenarios)
        val c = Robot.candidatsDe(Retest.proposees(all), last).associateBy { it.id }
        assertTrue(c["style_house"]!!.coche)
        assertTrue(!c["latence"]!!.coche)
        // Never played: the new tests are ticked; an old scenario never played is not.
        assertTrue(c["jouer_arreter_10"]!!.coche && c["perte_son"]!!.coche)
        assertTrue(!c["effets"]!!.coche)
        assertEquals("Charge", c["jouer_arreter_10"]!!.groupe)
    }

    @Test
    fun theScreenPutsTheTickedFirstAndHasItsButtons() {
        val candidats = listOf(
            Robot.Candidat("latence", "Latence", Feu.VERT, false, "Autres"),
            Robot.Candidat("style_house", "Style house", Feu.ROUGE, true, "Styles"),
            Robot.Candidat("jouer_arreter_10", "10 fois jouer", null, true, "Charge"),
        )
        var launched: Set<String>? = null
        compose.setContent { BenTestScreen(Robot.Etat(), emptyList(), BenTestActions(startRetest = { launched = it }), candidats) }
        compose.onNodeWithTag("bentest_retest").performScrollTo().performClick()
        compose.onNodeWithTag("bentest_retest_lancer").assertTextEquals("Lancer les 2 cochées")
        compose.onNodeWithTag("bentest_retest_style_house").assertIsOn()
        compose.onNodeWithTag("bentest_retest_latence").assertIsOff()
        // Ticked first: the red and the new test above the green.
        val top = { tag: String -> compose.onNodeWithTag(tag).getBoundsInRoot().top }
        assertTrue(top("bentest_retest_style_house") < top("bentest_retest_latence"))
        assertTrue(top("bentest_retest_jouer_arreter_10") < top("bentest_retest_latence"))
        compose.onNodeWithTag("bentest_retest_tout").performClick()
        compose.onNodeWithTag("bentest_retest_lancer").assertTextEquals("Lancer les 3 cochées")
        compose.onNodeWithTag("bentest_retest_rien").performClick()
        compose.onNodeWithTag("bentest_retest_lancer").assertTextEquals("Lancer les 0 cochées")
        compose.onNodeWithTag("bentest_retest_jouer_arreter_10").performScrollTo().performClick()
        compose.onNodeWithTag("bentest_retest_lancer").performScrollTo().performClick()
        assertEquals(setOf("jouer_arreter_10"), launched)
    }
}
