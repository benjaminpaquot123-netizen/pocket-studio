package com.pocketstudio.app.export

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pocketstudio.app.mixer.ExportCard
import com.pocketstudio.app.mixer.MixerActions
import com.pocketstudio.app.nav.AppHarness
import com.pocketstudio.app.rhythm.ExportUi
import com.pocketstudio.app.rhythm.ExportWhat
import com.pocketstudio.app.ui.GestureBudget
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Hands-on audit, chantier 1 (« partager et retrouver en 3 gestes », G1 and G2; the owner's trials tel-03 and tel-06 of
 * 2026-11-08): from the home screen of the Mode simple on a small phone, the length of what is shared and the files
 * already shared are three touches away at most, and the length is no longer under Mix's « Avancé ».
 */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp")
class SharePathTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)

    @After
    fun end() = h.finish()

    private fun settle() {
        repeat(4) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            compose.mainClock.advanceTimeBy(400)
            compose.waitForIdle()
        }
    }

    /** A project with no song yet (a part goes out looped), opened from « Que veux-tu faire ? » in the Mode simple. */
    private fun fromHomeWithoutASong(): GestureBudget {
        h.start(simple = true, launchChoice = true, build = {})
        h.awaitTag("launch_page")
        assertTrue("a part looped, not a song", h.vm.project.value.song.isEmpty())
        return GestureBudget(compose, "partager")
    }

    /** G1 (tel-06): the loop's length is picked in « Prêt à partager ? », three touches from home, and the duration follows. */
    @Test
    fun theSheetsChipsSetTheLoopLengthAndTheDuration() {
        val g = fromHomeWithoutASong()
        g.tap("launch_continue"); settle()
        g.tap("tab_simple_Envoyer"); settle()
        // 120 BPM: the default loop of 8 bars lasts 16 s.
        compose.onNodeWithTag("share_duration").assertTextEquals("0:16")
        g.tap("share_bars_16"); settle()
        g.assertWithin(3)
        assertEquals("the same setting as Mix's « Longueur de la boucle »", 16, h.vm.export.value.bars)
        assertTrue(h.vm.export.value.barsChosen)
        compose.onNodeWithTag("share_duration").assertTextEquals("0:32")
        g.tap("share_bars_4"); settle()
        assertEquals(4, h.vm.export.value.bars)
        compose.onNodeWithTag("share_duration").assertTextEquals("0:08")
    }

    /** G2 (tel-03): « Mes fichiers » is in sight under « Partager » (no scroll), and opens the list: three touches from home. */
    @Test
    fun myFilesIsInSightUnderTheShareButton() {
        val g = fromHomeWithoutASong()
        g.tap("launch_continue"); settle()
        g.tap("tab_simple_Envoyer"); settle()
        assertTrue("« Mes fichiers » is not in sight on the page", compose.onNodeWithTag("share_my_exports").isDisplayed())
        val go = compose.onNodeWithTag("share_go").fetchSemanticsNode().boundsInRoot
        val files = compose.onNodeWithTag("share_my_exports").fetchSemanticsNode().boundsInRoot
        assertTrue("« Mes fichiers » ($files) is not under « Partager » ($go)", files.top >= go.bottom)
        g.tap("share_my_exports"); settle()
        g.assertWithin(3)
        compose.onNodeWithTag("my_exports").assertIsDisplayed()
    }
}

/** The same chantier, the pieces on their own (no app around them). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h2400dp")
class SharePiecesTest {
    @get:Rule
    val compose = androidx.compose.ui.test.junit4.createComposeRule()

    /** G1 (tel-06: « le repli Avancé cache un réglage dont dépend l'export »): Mix shows the loop's length without « Avancé ». */
    @Test
    fun theMixCardShowsTheLoopLengthWithoutAvance() {
        val picked = ArrayList<Int>()
        compose.setContent {
            PocketStudioTheme { ExportCard(ExportUi(what = ExportWhat.Loop, bars = 8, sceneBars = 4), MixerActions(setExportBars = { picked += it }), hasSong = false) }
        }
        // « Avancé » is closed, never touched.
        compose.onNodeWithTag("export_bars_16").assertIsDisplayed().performClick()
        assertEquals(listOf(16), picked)
    }
}
