package com.pocketstudio.bentest

import android.app.Application
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.zip.ZipFile

/**
 * Benjamin (2026-11-25): the full scenarios cut into « Partie 1, Partie 2… » of at most 50, each launched alone (the
 * phone not held for an hour), the parts played shown ticked, and at the bottom « Générer le rapport et l'envoyer »:
 * every part played (and the other run) in one zip, straight into Android's share menu.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h2000dp-port-xhdpi")
class PartiesTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun thePartsCoverEveryScenarioOnceAndHoldFiftyAtMost() {
        val ids = Scenarios.LISTE.map { it.id }
        println("Scenarios.LISTE: ${ids.size} scénarios, ${Scenarios.NB_PARTIES} parties")
        assertEquals("the scenario ids are unique", ids.size, ids.toSet().size)
        assertEquals((ids.size + Scenarios.TAILLE_PARTIE - 1) / Scenarios.TAILLE_PARTIE, Scenarios.NB_PARTIES)
        assertTrue("more than one part", Scenarios.NB_PARTIES > 1)
        val parts = (1..Scenarios.NB_PARTIES).map { Scenarios.liste(Scenarios.partie(it)) }
        parts.forEachIndexed { i, p ->
            assertTrue("part ${i + 1}: ${p.size}", p.size in 1..Scenarios.TAILLE_PARTIE)
            if (i < parts.lastIndex) assertEquals("only the last part is shorter", Scenarios.TAILLE_PARTIE, p.size)
            assertTrue(Scenarios.partie(i + 1) in Scenarios.PAQUETS)
            assertEquals(i + 1, Scenarios.numeroPartie(Scenarios.partie(i + 1)))
        }
        // Every scenario exactly once, in the full list's order.
        assertEquals(ids, parts.flatten().map { it.id })
        assertEquals(null, Scenarios.numeroPartie(Scenarios.partie(Scenarios.NB_PARTIES + 1)))
        // A part's logbook is read back as that part (its report lists its own scenarios).
        val p2 = Scenarios.partie(2)
        val lines = listOf(Carnet.Ligne(0, Carnet.NOTE, parts[1][0].id, "paquet;$p2"))
        assertEquals(p2, Scenarios.paquet(lines))
        assertEquals(parts[1].map { it.id }, Scenarios.resultats(lines).map { it.scenario.id })
    }

    @Test
    fun eachPartStartsAloneTheDoneOnesAreTickedAndTheReportButtonIsAtTheBottom() {
        val started = ArrayList<Int>()
        var tout = 0
        compose.setContent {
            BenTestScreen(
                Robot.Etat(), emptyList(),
                BenTestActions(startPartie = { started += it }, sendTout = { tout++ }),
                faites = setOf(1),
            )
        }
        compose.onNodeWithTag("bentest_partie_1").assertTextContains("✓", substring = true)
        compose.onNodeWithTag("bentest_partie_1").assertTextContains("Partie 1", substring = true)
        compose.onNodeWithTag("bentest_partie_2").assertTextContains("Partie 2", substring = true)
        assertFalse(
            "part 2 not ticked",
            compose.onNodeWithTag("bentest_partie_2").fetchSemanticsNode().config.toString().contains("✓"),
        )
        compose.onRoot().captureRoboImage("screenshots/bentest/accueil_parties.png")
        compose.onNodeWithTag("bentest_partie_2").performScrollTo().performClick()
        assertEquals(listOf(2), started)
        // The report button is the screen's last one, under every run's buttons.
        val bas = compose.onNodeWithTag("bentest_rapport_tout").fetchSemanticsNode().boundsInRoot.top
        listOf("bentest_tout", "bentest_retest", "bentest_partie_${Scenarios.NB_PARTIES}").forEach {
            assertTrue("$it above the report button", compose.onNodeWithTag(it).fetchSemanticsNode().boundsInRoot.top < bas)
        }
        compose.onNodeWithTag("bentest_rapport_tout").assertTextContains("Générer le rapport et l'envoyer")
        compose.onNodeWithTag("bentest_rapport_tout").performScrollTo().performClick()
        assertEquals(1, tout)

        // After a part, the next one is offered right under its verdict.
        compose.setContent {
            BenTestScreen(
                Robot.Etat(verdict = "50 passés", passage = Robot.SCENARIOS), emptyList(),
                BenTestActions(startPartie = { started += it }), faites = setOf(1, 2),
            )
        }
        compose.onNodeWithTag("bentest_partie_2").assertTextContains("✓", substring = true)
        compose.onNodeWithTag("bentest_partie_3").performScrollTo().performClick()
        assertEquals(listOf(2, 3), started)
        assertNotNull(compose.onNodeWithTag("bentest_rapport_tout").fetchSemanticsNode())
    }

    @Test
    fun theReportButtonGathersEveryPartPlayedAndOpensTheShareMenu() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Robot.oublierParties(app)
        // Part 1 played and finished, then part 2 played: both kept.
        listOf(1, 2).forEach { n ->
            val c = Robot.carnet(app)
            c.clear()
            val s = Scenarios.liste(Scenarios.partie(n))[0]
            c.add(Carnet.NOTE, s.id, "paquet;${Scenarios.partie(n)}")
            c.add(Carnet.DEBUT, s.id)
            c.add(Carnet.REUSSI, s.id, "ok partie $n")
            c.add(Carnet.FINI, "")
            Robot.archiverPartie(app)
        }
        assertEquals(setOf(1, 2), Robot.partiesFaites(app))
        // « Tout recommencer » forgets the run, never the parts played.
        Robot.reset(app)
        assertEquals(setOf(1, 2), Robot.partiesFaites(app))

        assertTrue("the share menu opened", Robot.shareTout(app))
        val chooser = shadowOf(app).nextStartedActivity
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        @Suppress("DEPRECATION")
        val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals("application/zip", send.type)

        val zip = File(app.cacheDir, "exports").listFiles()!!.single { it.name.startsWith("rapport-benstudio-test-") && it.extension == "zip" }
        ZipFile(zip).use { z ->
            val names = z.entries().toList().map { it.name }
            listOf("parties.md", "parties/partie-1/resume.md", "parties/partie-2/resume.md", "parties/partie-1/carnet.tsv", "parties/partie-2/scenarios.json", "rapport.md").forEach {
                assertTrue("$it in $names", it in names)
            }
            val carnet2 = z.getInputStream(z.getEntry("parties/partie-2/carnet.tsv")).readBytes().toString(Charsets.UTF_8)
            assertTrue(carnet2.contains("ok partie 2"))
            val sommaire = z.getInputStream(z.getEntry("parties.md")).readBytes().toString(Charsets.UTF_8)
            assertTrue(sommaire, sommaire.contains("Partie 1") && sommaire.contains("Partie 2"))
        }
        Robot.oublierParties(app)
        assertEquals(emptySet<Int>(), Robot.partiesFaites(app))
    }
}
