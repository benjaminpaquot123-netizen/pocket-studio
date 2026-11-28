package com.pocketstudio.app.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import com.pocketstudio.app.ui.PartHeader
import com.pocketstudio.app.ui.partHeader
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.Project
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 9, lot L6, Structure's gestures:
 * - F101 « La liste des parties défile d'où que parte mon doigt. »
 * - F102 « Un appui long sur une partie ouvre son menu, sans l'ajouter. » (the « + A · Intro » add buttons)
 * - F103 « Le bouton dit ce qu'il fait : copie ou partie vide. »
 * - F104 « Toucher une partie ouvre ses réglages (longueur) ; la lecture a son propre bouton. »
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp")
class Tour9StructureGesturesTest {
    @get:Rule val compose = createComposeRule()

    private val played = mutableListOf<Int>()
    private val asked = mutableListOf<Int?>()
    private var songSize = 0

    private fun show(parts: Int = 6, current: Int = 0, emptyCurrent: Boolean = false) {
        compose.setContent {
            var song by remember { mutableStateOf(List(parts) { SongPart(scene = it % 3) }) }
            songSize = song.size
            PocketStudioTheme {
                SessionScreen(
                    SessionScreenUi(
                        scenes = List(Scene.MAX) {
                            when {
                                it == current && emptyCurrent -> Scene(name = Scene.NAMES[it])
                                it < 3 -> filledScene(it)
                                else -> null
                            }
                        },
                        current = current, pending = null, song = song, songIndex = -1, playing = false, tempoBpm = 120.0,
                    ),
                    SessionActions(
                        addPart = { s -> song = song + SongPart(scene = s) },
                        playPart = { played += it },
                        whatPlays = { asked += it },
                    ),
                    arrangeOnly = true,
                )
            }
        }
        compose.waitForIdle()
    }

    private fun filledScene(i: Int): Scene {
        val d = DrumPattern(steps = 16)
        return Scene(name = Scene.NAMES[i], label = listOf("Intro", "Couplet", "Refrain")[i], drums = d.toggle(0, 0))
    }

    // F104 -----------------------------------------------------------------------------------------------------------

    @Test
    fun touchingAPartOpensItsSettingsWithoutPlaying() {
        show()
        compose.onNodeWithTag("part_open_1").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("part_sheet").assertIsDisplayed()
        assertEquals("a touch on the part played it", emptyList<Int>(), played)
    }

    /** Review G2: a touch on the part opens it, so its ▶ is the only « play from here »: a thumb wide (48 dp). */
    @Test @Config(qualifiers = "w360dp-h640dp", fontScale = 2f)
    fun thePlayButtonIsAThumbWide() {
        show()
        compose.onNodeWithTag("part_play_1").performScrollTo().assertWidthIsAtLeast(48.dp)
    }

    @Test
    fun thePlayButtonPlaysFromThePart() {
        show()
        compose.onNodeWithTag("part_play_1").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(listOf(1), played)
        compose.onNodeWithTag("part_sheet").assertDoesNotExist()
    }

    @Test
    fun aLongPressOnPlayStillOpensTheSheet() {
        show()
        compose.onNodeWithTag("part_play_2").performScrollTo().performTouchInput { longClick() }
        compose.waitForIdle()
        compose.onNodeWithTag("part_sheet").assertIsDisplayed()
        assertEquals(emptyList<Int>(), played)
    }

    @Test
    fun theHelpSaysWhatATouchDoes() {
        show()
        // The old words promised a touch plays the part.
        compose.onNode(hasText("Touche une partie pour écouter", substring = true)).assertDoesNotExist()
        compose.onNode(hasText("longueur", substring = true)).assertExists()
    }

    // F102 -----------------------------------------------------------------------------------------------------------

    @Test
    fun aLongPressOnAnAddButtonOpensItsMenuWithoutAdding() {
        show(parts = 2)
        val before = songSize
        compose.onNodeWithTag("add_part_1").performScrollTo().performTouchInput { longClick() }
        compose.waitForIdle()
        assertEquals("a long press added the part", before, songSize)
        compose.onNodeWithTag("add_part_menu").assertIsDisplayed()
        compose.onNodeWithTag("add_part_menu_what_plays").performClick()
        compose.waitForIdle()
        assertEquals(listOf<Int?>(1), asked)
        assertEquals(before, songSize)
    }

    @Test
    fun aTapOnAnAddButtonStillAdds() {
        show(parts = 2)
        compose.onNodeWithTag("add_part_1").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(3, songSize)
    }

    // F103 -----------------------------------------------------------------------------------------------------------

    @Test
    fun anEmptyPartIsNotOfferedAsACopy() {
        show(parts = 2, current = 0, emptyCurrent = true)
        compose.onNodeWithTag("add_part_new_copy").assertDoesNotExist()
        compose.onNodeWithTag("add_part_new_empty").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun aFilledPartIsOfferedAsACopy() {
        show(parts = 2, current = 1)
        compose.onNodeWithTag("add_part_new_copy").performScrollTo()
        compose.onNode(hasText("copie de B", substring = true), useUnmergedTree = true).assertExists()
    }

    @Test
    fun theEditorsMenuSaysAnEmptyPartIsNew() {
        val p = Project().let { it.copy(scenes = listOf(Scene(name = "A"))) }
        val ui = partHeader(p, playing = false, songPlaying = false, loopPart = false, armedBySelf = false)
        compose.setContent { PocketStudioTheme { PartHeader(ui, parts = listOf("A"), current = 0, newPart = {}) } }
        compose.onNodeWithTag("part_header_pick").performClick()
        compose.waitForIdle()
        val label = compose.onNodeWithTag("part_new", useUnmergedTree = true).fetchSemanticsNode().let { n ->
            generateSequence(listOf(n)) { l -> l.flatMap { it.children }.takeIf { it.isNotEmpty() } }.flatten()
                .mapNotNull { it.config.getOrElseNullable(androidx.compose.ui.semantics.SemanticsProperties.Text) { null }?.joinToString("") { t -> t.text } }.joinToString(" ")
        }
        assertFalse("the menu offers a copy of an empty part: « $label »", "copie" in label)
        assertTrue("« $label »", "vide" in label)
    }

    // F101 -----------------------------------------------------------------------------------------------------------

    private fun top(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.top

    /** Drags 300 px up (or down) from the middle of [tag]; returns how far [tag] moved in the drag's direction. */
    private fun drag(tag: String, restMs: Long, dy: Float): Float {
        val before = top(tag)
        compose.onNodeWithTag(tag).performTouchInput {
            down(center)
            if (restMs > 0) advanceEventTime(restMs)
            repeat(10) { moveBy(androidx.compose.ui.geometry.Offset(0f, dy / 10)) }
            up()
        }
        compose.waitForIdle()
        return (top(tag) - before) * kotlin.math.sign(dy)
    }

    /** A drag starting on [tag], quick or after a short rest of the finger, moves the screen one way or the other. */
    private fun dragFrom(tag: String, restMs: Long) {
        show(parts = 8)
        compose.onNodeWithTag(tag).performScrollTo()
        compose.waitForIdle()
        val up = drag(tag, restMs, -300f)
        val down = if (up > 100f) up else drag(tag, restMs, 300f)
        assertTrue("a drag from « $tag » (finger resting $restMs ms) did not scroll: up $up, down $down", up > 100f || down > 100f)
    }

    @Test fun aDragFromAPartScrolls() = dragFrom("part_open_3", 0)
    @Test fun aDragFromAPartAfterARestScrolls() = dragFrom("part_open_3", 250)
    @Test fun aDragFromAnAddButtonScrolls() = dragFrom("add_part_1", 0)
    @Test fun aDragFromAnAddButtonAfterARestScrolls() = dragFrom("add_part_1", 250)
    @Test fun aDragFromTheRepeatsScrolls() = dragFrom("song_repeats_help", 0)
    @Test fun aDragFromAPlayButtonScrolls() = dragFrom("part_play_3", 0)
}
