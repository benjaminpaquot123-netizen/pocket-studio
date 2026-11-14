package com.pocketstudio.app.live

import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import com.pocketstudio.app.ScreenshotTest
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.LiveSlots
import com.pocketstudio.model.LiveState
import com.pocketstudio.model.Project
import com.pocketstudio.model.Remix
import com.pocketstudio.model.RemixCell
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.addTrack
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Lot D2 (« case Remix du Live »), on a small phone (360 dp): the remix column with a passage in
 * bricks B and C (« Voix · 2 », « Batterie · 4 »), B playing; and the cell's menu opened by a long
 * press (app/screenshots/).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-xxhdpi")
class LiveRemixCellScreenshotTest : ScreenshotTest() {
    override fun wanted(name: String) = name in setOf("live_remix_cells", "live_remix_menu")

    private fun project(): Project {
        val drums = DrumPattern().toggle(0, 0).toggle(0, 8).toggle(2, 4).toggle(2, 12)
        val p = Project(
            tempoBpm = 113.0, drums = drums,
            scenes = listOf(Scene("A", drums, label = "Intro"), Scene("B", drums, label = "Couplet"), Scene("C", drums, label = "Refrain")),
            remix = Remix(
                "boys", "Boys", songBpm = 113.0, seconds = 180.0,
                cells = listOf(RemixCell(1, "v.wav", "Voix", 2, 113.0, 3, 42.0), RemixCell(2, "d.wav", "Batterie", 4, 113.0, 0, 64.0)),
            ),
        ).addTrack("Basse")!!.sanitized()
        return p
    }

    private fun ui(): LiveScreenUi {
        val g = LiveState.on(0, 0).launched(LiveSlots.REMIX, 1, 16)
        return LiveScreenUi(project(), g, clock = 22, playing = true)
    }

    @Test
    fun remixCells() = shoot("live_remix_cells", fullScreen = true) {
        LiveScreen(ui(), LiveActions())
    }

    @Test
    fun remixMenu() = shoot("live_remix_menu", fullScreen = true, prepare = {
        compose.onNodeWithTag("live_pad_${SongPart.REMIX}_1").performTouchInput { longClick() }
        compose.waitForIdle()
    }) {
        LiveScreen(ui(), LiveActions())
    }
}
