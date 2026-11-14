package com.pocketstudio.app.ui

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pocketstudio.app.live.LiveActions
import com.pocketstudio.app.live.LiveScreen
import com.pocketstudio.app.live.LiveScreenUi
import com.pocketstudio.app.remix.CutWithStructure
import com.pocketstudio.app.session.LivePadsActions
import com.pocketstudio.app.session.LivePadsUi
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LivePads
import com.pocketstudio.model.LiveState
import com.pocketstudio.model.Project
import com.pocketstudio.ui.PocketStudioTheme
import com.pocketstudio.ui.widthClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.compose.ui.unit.dp

/**
 * Lot D (USAGE-TABLETTE-SMARTPHONE §5 point 2): on an expanded width (a tablet lying down), the
 * Live page opens with the bricks and the pads side by side (the Pads tab stays beside the grid,
 * never the pads on the whole screen), and « Découper la chanson » has the Structure beside it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1280dp-h800dp-land-mdpi")
class TabletSideBySideTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theFiveSizeClasses() {
        assertEquals(listOf(0, 1, 2, 3, 4), listOf(360, 700, 1000, 1280, 1700).map { widthClass(it.dp) })
        assertEquals(listOf(0, 1, 2), listOf(599, 600, 840).map { widthClass(it.dp) })
    }

    @Test
    fun theLiveShowsTheBricksAndThePadsSideBySide() {
        var fullScreen = 0
        val pads = LivePads(8).with(0, LiveClip("k.wav", "Kick 909", 1)).with(1, LiveClip("c.wav", "Charley", 2))
        compose.setContent {
            PocketStudioTheme {
                LiveScreen(
                    LiveScreenUi(Project(drums = DrumPattern().toggle(0, 0)).sanitized(), LiveState.on(0, 0), clock = 0, playing = false),
                    LiveActions(),
                    pads = LivePadsUi(pads, List(16) { 0 }, editing = null),
                    padsActions = LivePadsActions(fullScreen = { fullScreen++ }),
                )
            }
        }
        compose.onNodeWithTag("live_side_by_side").assertIsDisplayed()
        compose.onNodeWithTag("live_panel").assertIsDisplayed()
        compose.onNodeWithText("Kick 909", substring = true).assertIsDisplayed()
        // Another page, then Pads again: beside the grid, not on the whole screen.
        compose.onAllNodesWithTag("live_segment_Effets", useUnmergedTree = true).fetchSemanticsNodes().let { effets ->
            if (effets.isNotEmpty()) compose.onNodeWithTag("live_segment_Effets", useUnmergedTree = true).performClick()
        }
        compose.onNodeWithText("Pads").performClick()
        compose.waitForIdle()
        assertEquals(0, fullScreen)
        compose.onNodeWithText("Kick 909", substring = true).assertIsDisplayed()
    }

    @Test
    fun theCutHasTheStructureBesideIt() {
        compose.setContent {
            PocketStudioTheme { CutWithStructure(structure = { Text("Le morceau") }) { Text("Découper") } }
        }
        compose.onNodeWithTag("cut_side_by_side").assertIsDisplayed()
        compose.onNodeWithText("Le morceau").assertIsDisplayed()
        compose.onNodeWithText("Découper").assertIsDisplayed()
        assertTrue(compose.onAllNodesWithTag("cut_structure").fetchSemanticsNodes().isNotEmpty())
    }
}

/** A phone: the cut alone (no room beside it), the Live as before. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class PhoneNotSideBySideTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aPhoneShowsTheCutAlone() {
        compose.setContent {
            PocketStudioTheme { CutWithStructure(structure = { Text("Le morceau") }) { Text("Découper") } }
        }
        compose.onNodeWithText("Découper").assertIsDisplayed()
        assertTrue(compose.onAllNodesWithText("Le morceau").fetchSemanticsNodes().isEmpty())
    }
}
