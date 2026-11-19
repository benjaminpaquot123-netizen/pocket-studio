package com.pocketstudio.app.session

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.pocketstudio.app.learn.LocalSpots
import com.pocketstudio.app.learn.SpotRegistry
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.Scene
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 9, lot L6, F106 (seats t9-12, t9-36): the tuto brought the add buttons above its panel and pushed the
 * Structure's help sentence under the top bar, cut.
 * Acceptance: « La phrase d'aide de la Structure se lit sous la barre du haut. »
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Tour9HelpUnderTopTest {
    @get:Rule val compose = createComposeRule()

    private fun helpStaysInSight(scenes: Int, simple: Boolean) {
        val spots = SpotRegistry()
        compose.setContent {
            CompositionLocalProvider(LocalSpots provides spots) {
                PocketStudioTheme {
                    Box(Modifier.fillMaxSize().onGloballyPositioned { spots.windowHeight = it.size.height.toFloat() }) {
                        SessionScreen(
                            SessionScreenUi(
                                scenes = List(Scene.MAX) { i ->
                                    if (i < scenes) Scene(name = Scene.NAMES[i], drums = DrumPattern(steps = 16).toggle(0, 0)) else null
                                },
                                current = 0, pending = null, song = emptyList(), songIndex = -1, playing = false, tempoBpm = 120.0,
                            ),
                            SessionActions(),
                            arrangeOnly = true,
                            simple = simple,
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
        val height = compose.onRoot().fetchSemanticsNode().size.height.toFloat()
        // The tuto's panel over the lower 45 % of the screen.
        spots.coverTop = height * 0.55f
        compose.runOnIdle { assertTrue(spots.reveal("song_add")) }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
        val help = compose.onNode(hasText("La ligne du temps met tes parties", substring = true), useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val add = spots.find("song_add")!!
        assertTrue("the add buttons were not brought above the panel: $add, panel at ${spots.coverTop}", add.top < spots.coverTop)
        assertTrue("the help sentence went above the screen's top: $help", help.top >= 0f)
    }

    @Test @Config(qualifiers = "w360dp-h640dp")
    fun theHelpStaysInSightWhenTheTutoShowsTheAddButtons() = helpStaysInSight(scenes = 4, simple = true)

    @Test @Config(qualifiers = "w360dp-h640dp", fontScale = 2f)
    fun theHelpStaysInSightWithBigText() = helpStaysInSight(scenes = 3, simple = true)

    @Test @Config(qualifiers = "w360dp-h640dp")
    fun theHelpStaysInSightWithManyParts() = helpStaysInSight(scenes = 8, simple = false)
}
