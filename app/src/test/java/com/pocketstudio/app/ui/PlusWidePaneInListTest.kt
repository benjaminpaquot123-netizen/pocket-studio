package com.pocketstudio.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private fun simpleEntries(): List<PlusEntry> = plusEntries(
    home = {}, projects = {}, glossary = {}, missions = {}, simple = true, setSimple = {}, bugReport = {},
    simpleWays = SimplePlusWays({}, {}, {}, {}, {}),
)

/**
 * Tour 5, lot « moyens » (review of the Plus lot, Mineur): on a tablet the Plus page's right pane opened on the page
 * last seen in Pro (`ToolPage.load`), one the Mode simple's list may not hold (MIDI, Latence…). It opens on a page of
 * the list shown: the last seen if the list holds it, else the list's first.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w800dp-h1280dp")
class PlusWidePaneInListTest {
    @get:Rule
    val compose = createComposeRule()

    private fun shown(page: ToolPage?, widePage: ToolPage): String {
        var args by mutableStateOf(page to widePage)
        compose.setContent {
            PocketStudioTheme {
                PlusSpace(simpleEntries(), page = args.first, open = {}, back = {}, widePage = args.second, leave = {}, tiles = true) { p ->
                    Box(Modifier.testTag("shown_${p.name}")) { Text(p.name) }
                }
            }
        }
        compose.waitForIdle()
        return ToolPage.entries.first { compose.onAllNodesWithTagCount("shown_${it.name}") > 0 }.name
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTagCount(tag: String) =
        onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().size

    @Test
    fun aProPageLastSeenOpensTheSimpleListsFirst() {
        assertEquals(ToolPage.Aide.name, shown(page = null, widePage = ToolPage.Midi))
    }

    @Test
    fun aPageOfTheSimpleListLastSeenOpensAgain() {
        assertEquals(ToolPage.Recette.name, shown(page = null, widePage = ToolPage.Recette))
    }

    @Test
    fun aProPageStillChosenGivesWayToTheSimpleList() {
        assertEquals(ToolPage.Aide.name, shown(page = ToolPage.Latence, widePage = ToolPage.Midi))
    }
}
