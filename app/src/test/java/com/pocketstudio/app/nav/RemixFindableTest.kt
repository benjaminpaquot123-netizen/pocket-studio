package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.pocketstudio.app.ui.withinGestures
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Lot 2 (audit 35, gestes.md): Benjamin wants the Remix (a song split into drums, bass, voice and
 * the rest) « mis en avant ». It was the second page of Sons › Voix: 3 gestures from Structure,
 * 4 from the opening, none in the Mode simple. Now: one card on « Que veux-tu faire ? », a
 * « Remix » chip in Sons, a line in the Mode simple's « ⋯ » (docs/NAVIGATION.md).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class RemixFindableTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @After
    fun end() = h.finish()

    companion object {
        /** docs/NAVIGATION.md: the Remix's budget (Benjamin: « en deux gestes »). */
        const val FROM_OPENING = 1
        const val FROM_A_SPACE = 2
    }

    private fun assertRemix() = compose.onNodeWithTag("remix_start").assertIsDisplayed()

    @Test
    fun fromTheOpeningItIsOneTouch() {
        h.start(launchChoice = true)
        h.awaitTag("launch_remix")
        compose.withinGestures("Remixer une chanson (ouverture)", FROM_OPENING) { tap("launch_remix") }
        assertRemix()
    }

    @Test
    fun fromStructureItIsTwoTouches() {
        h.start()
        compose.withinGestures("Remixer une chanson", FROM_A_SPACE) {
            tap("tab_Sons")
            tap("sons_Remix")
        }
        assertRemix()
        // The voice keeps its own chip, and goes back to the audio tracks.
        compose.onNodeWithTag("sons_Audio").assertIsDisplayed()
    }

    @Test
    fun inTheModeSimpleItIsTwoTouches() {
        h.start(simple = true)
        compose.withinGestures("Remixer une chanson (Mode simple)", FROM_A_SPACE) {
            tap("top_more")
            tap("more_remix")
        }
        assertRemix()
        // Its way back to the song.
        compose.onNodeWithTag("remix_back").assertIsDisplayed().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("tab_simple_Morceau").assertIsSelected()
        compose.onNodeWithTag("remix_start").assertDoesNotExist()
    }

    /** The Missions' last card: what comes next, the Remix. */
    @Test
    fun theMissionsEndOnTheRemix() {
        var remixed = 0
        compose.setContent {
            com.pocketstudio.ui.PocketStudioTheme {
                com.pocketstudio.app.learn.MissionCard(null, com.pocketstudio.app.learn.Mission.entries.size, simple = true, skip = {}, stop = {}, remix = { remixed++ })
            }
        }
        compose.onNodeWithTag("mission_remix").assertIsDisplayed().performClick()
        org.junit.Assert.assertEquals(1, remixed)
    }

    @Test
    fun inTheModeSimpleOpeningToo() {
        h.start(simple = true, launchChoice = true)
        h.awaitTag("launch_remix")
        compose.withinGestures("Remixer une chanson (Mode simple, ouverture)", FROM_OPENING) { tap("launch_remix") }
        assertRemix()
    }
}
