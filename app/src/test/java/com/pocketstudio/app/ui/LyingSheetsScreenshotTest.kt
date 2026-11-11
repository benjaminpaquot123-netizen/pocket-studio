package com.pocketstudio.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.pocketstudio.app.padlive.PadLiveActions
import com.pocketstudio.app.padlive.PlayedTakeSheet
import com.pocketstudio.app.tuto.TutoKeySheet
import com.pocketstudio.model.Harmony
import com.pocketstudio.model.Scale
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.PocketStudioTheme
import com.pocketstudio.ui.UiScaled
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Hands-on audit, chantier 9 (G10; rule 22 of docs/DESIGN.md: « une capture par feuille, phoneLand 1,0 et 1,3 »): the
 * real sheets, their window included, on a phone lying down (800 × 360 dp). Images in app/screenshots/lying/: the tempo
 * sheet at 100 %, with big text, at 130 % (as it opens, then scrolled to « Taper en rythme »), and the two sheets that
 * did not scroll (the take of Pad Live, the tutorial's key) at 130 %.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w800dp-h360dp-land-xxhdpi")
class LyingSheetsScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun end() = RuntimeEnvironment.setFontScale(1f)

    @OptIn(ExperimentalRoborazziApi::class)
    private fun shoot(name: String, scale: Float = 1f, prepare: () -> Unit = {}, sheet: @Composable () -> Unit) {
        compose.setContent {
            PocketStudioTheme { UiScaled(scale) { Box(Modifier.fillMaxSize().background(Neon.Background)) { sheet() } } }
        }
        compose.waitForIdle()
        prepare()
        compose.waitForIdle()
        captureScreenRoboImage("screenshots/lying/$name.png")
    }

    @Test
    fun tempo() = shoot("tempo_sheet") { TempoSheet(124.0, TempoActions(), onClose = {}) }

    @Test
    fun tempoBigText() {
        RuntimeEnvironment.setFontScale(1.3f)
        shoot("tempo_sheet_text130") { TempoSheet(124.0, TempoActions(), onClose = {}) }
    }

    @Test
    fun tempoAt130() = shoot("tempo_sheet_ui130", scale = 1.3f) { TempoSheet(124.0, TempoActions(), onClose = {}) }

    @Test
    fun tempoAt130Scrolled() = shoot("tempo_sheet_ui130_scrolled", scale = 1.3f, prepare = { compose.onNodeWithTag("tempo_tap").performScrollTo() }) {
        TempoSheet(124.0, TempoActions(), onClose = {})
    }

    @Test
    fun takeAt130() = shoot("take_sheet_ui130", scale = 1.3f) { PlayedTakeSheet(4, "A12", PadLiveActions()) }

    @Test
    fun keyAt130() = shoot("key_sheet_ui130", scale = 1.3f) { TutoKeySheet(Harmony(), Harmony(2, Scale.entries.last()), set = {}, close = {}) }
}
