package com.pocketstudio.app.ui

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** The play strip is made for fingers (DESIGN.md, rule 4): each control is touchable over 48 dp, lying down too. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class PlayStripTouchTargetsTest {
    @get:Rule
    val compose = createComposeRule()

    private fun strip(soloed: List<String> = emptyList(), clearSolos: () -> Unit = {}) = compose.setContent {
        PocketStudioTheme {
            Box(Modifier.width(360.dp)) {
                Column {
                    PlayStrip(
                        PlayStripUi(
                            124.0, hasSong = true, loopPart = false, playing = false, position = { PlayPosition("Couplet", 9, 60) },
                            parts = listOf("Intro", "Couplet"), soloed = soloed,
                        ),
                        PlayStripActions(clearSolos = clearSolos),
                    )
                    SoloBanner(listOf("Batterie"), {})
                }
            }
        }
    }

    private fun assertTouchable(tag: String) {
        val b = compose.onNodeWithTag(tag).assertIsDisplayed().getBoundsInRoot()
        assertTrue("$tag is ${b.bottom - b.top} high", b.bottom - b.top >= 47.5.dp)
        assertTrue("$tag is ${b.right - b.left} wide", b.right - b.left >= 47.5.dp)
    }

    @Test
    fun everyControlIsAtLeast48dp() {
        strip(soloed = listOf("Batterie"))
        listOf("top_tempo", "top_position", "top_loop_part", "solo_chip", "solo_banner").forEach(::assertTouchable)
        val strip = compose.onNodeWithTag("play_strip").getBoundsInRoot()
        assertTrue("the strip is ${strip.bottom - strip.top} high", strip.bottom - strip.top >= STRIP_HEIGHT - 0.5.dp)
    }

    @Test
    fun theSoloChipClearsEverySoloInOneTouchAndShowsOnlyWhenAsked() {
        var cleared = 0
        strip(soloed = listOf("Batterie", "Basse"), clearSolos = { cleared++ })
        compose.onNodeWithTag("solo_chip").performClick()
        assertEquals(1, cleared)
    }

    @Test
    fun withoutSoloedTracksGivenThereIsNoChip() {
        strip()
        compose.onNodeWithTag("solo_chip").assertDoesNotExist()
    }

    /** Lying down, the banner would cost 40 dp more under the bar: the solo folds into the strip's chip. */
    @Test
    @Config(qualifiers = "w780dp-h360dp-land")
    fun lyingDownTheSoloBannerFoldsIntoTheStripsChip() {
        val app: Application = ApplicationProvider.getApplicationContext()
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        vm.toggleHit(0, 0)
        vm.toggleSolo(0)
        compose.setContent {
            PocketStudioTheme { UnderTopBar(vm, tempoOpen = false, setTempoOpen = {}, renaming = false, closeRename = {}) }
        }
        compose.onNodeWithTag("solo_banner").assertDoesNotExist()
        compose.onNodeWithTag("solo_chip").assertIsDisplayed().performClick()
        compose.waitForIdle()
        assertTrue(vm.project.value.mixer.channels.none { it.solo })
        compose.onNodeWithTag("solo_chip").assertDoesNotExist()
    }
}
