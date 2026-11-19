package com.pocketstudio.app.session

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.pocketstudio.app.ui.SceneChipUi
import com.pocketstudio.app.ui.TopBar
import com.pocketstudio.app.ui.TopBarActions
import com.pocketstudio.app.ui.TopBarUi
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Tour 9, lot L6: the screens D08 (Structure after adds) and D10 (« Renommer » in the scene menu) touch, whole. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Tour9L6ScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private fun structureAfterAdds(name: String) {
        compose.setContent {
            var song by remember { mutableStateOf(List(5) { SongPart(scene = it % 2) }) }
            PocketStudioTheme {
                Column {
                    TopBar(TopBarUi(projectName = "Mon morceau", playing = false, canUndo = true, canRedo = false, scene = SceneChipUi(1, listOf("A · Intro", "B · Refrain", "C"), setOf(2))), TopBarActions())
                    SessionScreen(
                        SessionScreenUi(
                            scenes = List(Scene.MAX) { if (it < 2) Scene(name = Scene.NAMES[it]) else null }, current = 1, pending = null,
                            song = song, songIndex = -1, playing = false, tempoBpm = 120.0,
                        ),
                        SessionActions(addPart = { s -> song = song + SongPart(scene = s) }, newPart = { _, _ -> song = song + SongPart(scene = 0) }),
                        arrangeOnly = true,
                    )
                }
            }
        }
        compose.onNodeWithTag("song_what_plays").performScrollTo()
        repeat(2) { compose.onNodeWithTag("add_part_0").performClick(); compose.mainClock.advanceTimeBy(500); compose.waitForIdle() }
        captureScreenRoboImage("screenshots/tour9_l6_${name}_structure_after_adds.png")
        compose.onNodeWithTag("top_scene").performClick()
        compose.waitForIdle()
        captureScreenRoboImage("screenshots/tour9_l6_${name}_scene_menu_rename.png")
    }

    @Test @Config(qualifiers = "w360dp-h640dp")
    fun smallPhone() = structureAfterAdds("360dp")

    @Test @Config(qualifiers = "w360dp-h640dp", fontScale = 2f)
    fun smallPhoneBigText() = structureAfterAdds("360dp_200pc")

    @Test @Config(qualifiers = "w411dp-h891dp")
    fun usualPhone() = structureAfterAdds("411dp")
}
