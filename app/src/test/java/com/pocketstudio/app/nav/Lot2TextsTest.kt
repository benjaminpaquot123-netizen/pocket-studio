package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pocketstudio.app.mixer.HardLimiting
import com.pocketstudio.app.mixer.limiterShown
import com.pocketstudio.app.report.Journal
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.StarterWizard
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Lot 2 (audit 35): the words the reports found wrong. « Briques » for the grid of A to H (two
 * « Parties » side by side), a limiter « retenu de 214748368,0 dB », « 1 erreurs », « … dans Mix »
 * in the Mode simple (no Mix there), « Plus · Aide » in the Mode simple (no Plus there), and
 * « environ 3 minutes » for songs of 5 to 6.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp")
class Lot2TextsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @After
    fun end() = h.finish()

    private fun tap(tag: String) {
        compose.onNodeWithTag(tag).performClick()
        compose.waitForIdle()
    }

    @Test
    fun theLimiterNeverSaysAnAbsurdNumber() {
        assertEquals(0 to false, limiterShown(0f))
        assertEquals(0 to false, limiterShown(Float.NaN))
        assertEquals(0 to false, limiterShown(Float.NEGATIVE_INFINITY))
        assertEquals(0 to false, limiterShown(1f))
        assertEquals(60 to true, limiterShown(0.5f))
        // Nothing read for a long while (the engine closed) is not « the limiter forces ».
        val watch = HardLimiting()
        var shown = false
        for (t in 0L..10_000L step HardLimiting.READ_MS) shown = shown || watch.update(0f, t)
        assertFalse(shown)
        // The meters start with the limiter at rest.
        h.start()
        assertEquals(1f, h.vm.meters.value[NativeEngine.METER_LIMITER], 0f)
        tap("tab_Mix")
        compose.onNodeWithTag("limiter_line").assertTextContains("Pas de saturation", substring = true)
    }

    @Test
    fun oneErrorIsOneError() {
        Journal.clear()
        Journal.error("test", "boum")
        val lines = com.pocketstudio.app.report.bugSummary(com.pocketstudio.app.report.BugReportState(), null)
        assertTrue(lines.toString(), lines.any { it.endsWith("et 1 erreur") })
        assertTrue(lines.none { "1 erreurs" in it })
        Journal.clear()
        assertTrue(com.pocketstudio.app.report.bugSummary(com.pocketstudio.app.report.BugReportState(), null).any { it.endsWith("et aucune erreur") })
    }

    @Test
    fun theGridOfAToHIsCalledBriques() {
        h.start()
        tap("structure_parts")
        compose.onNode(hasText("BRIQUES") or hasText("Briques")).assertExists()
    }

    @Test
    fun theModeSimpleSaysWhereTheOtherExportsAre() {
        h.start(simple = true)
        tap("tab_simple_Envoyer")
        compose.onNodeWithTag("share_other_where", useUnmergedTree = true).assertTextContains("mode Pro", substring = true)
    }

    @Test
    fun theModeSimplesHelpGoesBackToTheSong() {
        h.start(simple = true)
        tap("top_more")
        tap("more_help")
        compose.onNodeWithTag("plus_back").assertTextContains("Retour", substring = true)
        compose.onNodeWithText("Plus").assertDoesNotExist()
        tap("plus_back")
        compose.onNodeWithTag("tab_simple_Morceau").assertExists()
        assertTrue(compose.onNodeWithTag("plus_back").let { runCatching { it.assertExists() }.isFailure })
    }

    @Test
    fun eachStyleSaysItsRealLength() {
        val techno = StarterWizard.style("techno")!!
        val lofi = StarterWizard.style("lofi_hiphop") ?: StarterWizard.STYLES.first { it.family == StarterWizard.STYLES.last().family }
        val t = com.pocketstudio.app.library.StyleLengths.seconds(techno)
        assertTrue("techno lasts $t s", t >= 4 * 60)
        val (from, to) = com.pocketstudio.app.library.StyleLengths.range()
        assertTrue("$from to $to minutes", from <= 3 && to >= 5)
        assertTrue(com.pocketstudio.app.library.StyleLengths.label(lofi).contains("min"))
    }
}
