package com.pocketstudio.app.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 9, D08 (~19 seats): « écran Structure, liste « Ajouter à la fin » : après chaque ajout de partie, la liste saute,
 * on doit la rechercher. » Acceptance: after an add, the list keeps its place (the button touched stays under the finger)
 * and the new part is in sight, whole, without a gesture.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Tour9AddKeepsPlaceTest {
    @get:Rule val compose = createComposeRule()

    private fun show(parts: Int) = compose.setContent {
        var song by remember { mutableStateOf(List(parts) { SongPart(scene = it % 2) }) }
        PocketStudioTheme {
            SessionScreen(
                SessionScreenUi(
                    scenes = List(Scene.MAX) { if (it < 2) Scene(name = Scene.NAMES[it]) else null }, current = 0, pending = null,
                    song = song, songIndex = -1, playing = false, tempoBpm = 120.0,
                ),
                SessionActions(addPart = { s -> song = song + SongPart(scene = s) }, newPart = { _, _ -> song = song + SongPart(scene = 0) }),
                arrangeOnly = true,
            )
        }
    }

    private fun top(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.top

    private fun addsKeepThePlace(button: String) {
        show(parts = 5)
        // The add buttons in the middle of the screen, the way a hand leaves them after a scroll.
        compose.onNodeWithTag("song_what_plays").performScrollTo()
        compose.waitForIdle()
        val screen = compose.onRoot().fetchSemanticsNode().boundsInRoot
        repeat(3) { round ->
            val before = top(button)
            compose.onNodeWithTag(button).performClick()
            compose.mainClock.advanceTimeBy(500)
            compose.waitForIdle()
            val after = top(button)
            assertTrue("round $round: « $button » moved from $before to $after px after the add (the list jumped)", kotlin.math.abs(after - before) <= 2f)
            val last = compose.onNodeWithTag("part_${5 + round}", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertTrue("round $round: the new part ($last) is not whole on the screen ($screen)", last.top >= screen.top - 1f && last.bottom <= screen.bottom + 1f)
        }
    }

    @Test @Config(qualifiers = "w360dp-h640dp")
    fun aSmallPhoneKeepsTheButtonUnderTheFinger() = addsKeepThePlace("add_part_0")

    @Test @Config(qualifiers = "w360dp-h640dp")
    fun aNewEmptyPartKeepsTheButtonsInPlaceToo() = addsKeepThePlace("add_part_new_empty")

    @Test @Config(qualifiers = "w411dp-h891dp")
    fun aUsualPhoneKeepsTheButtonUnderTheFinger() = addsKeepThePlace("add_part_1")
}
