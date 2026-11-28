package com.pocketstudio.app.mixer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.github.takahirom.roborazzi.captureRoboImage
import com.pocketstudio.app.nav.acidTechnoSong
import com.pocketstudio.app.ui.simpleTracks
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.Project
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Tour 9, lot L2: the touched screens, whole, on a 360 dp phone with its font at 200 % (pro Mix, its pads' sheet, Mode simple). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-port-hdpi", fontScale = 2.0f)
class Tour9MixCapturesTest {
    @get:Rule
    val compose = createComposeRule()

    private val five = BASIC_STRIPS + StripInfo(Mixer.TRACK_STRIPS[5], "Mélodie")

    private fun showPro() {
        val channels = Project().mixer.channels
        compose.setContent {
            PocketStudioTheme { Box(Modifier.fillMaxSize()) { MixerScreen(channels, { FloatArray(NativeEngine.METERS) }, MixerActions(), strips = five) } }
        }
        compose.waitForIdle()
    }

    @Test
    fun proMix() {
        showPro()
        compose.onRoot().captureRoboImage("screenshots/tour9/mix_pro.png")
    }

    @Test
    fun padsSheet() {
        showPro()
        compose.onNodeWithTag("console_pads").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("screenshots/tour9/mix_pads.png")
    }

    @Test
    fun simpleMix() {
        val meters = FloatArray(NativeEngine.METERS)
        val loudness = FloatArray(NativeEngine.LOUDNESS) { NativeEngine.LOUDNESS_NONE }
        val ui = SimpleMixUi("Acid techno", simpleTracks(acidTechnoSong()), emptyMap(), 0f, false, null, null)
        compose.setContent { PocketStudioTheme { SimpleMixScreen(ui, { meters }, { loudness }, SimpleMixActions(), words = { OutputWords.GOOD }) } }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("screenshots/tour9/mix_simple.png")
    }
}
