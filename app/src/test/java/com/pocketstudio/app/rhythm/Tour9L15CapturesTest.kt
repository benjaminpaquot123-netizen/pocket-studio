package com.pocketstudio.app.rhythm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import com.github.takahirom.roborazzi.captureRoboImage
import com.pocketstudio.app.ui.UiScaled
import com.pocketstudio.model.Project
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 9, lot L15: the whole Rythme screen on a 360 dp phone at 100 %, Réglages › Taille 130 % and the phone's font
 * at 200 %, the Kick chosen, for the lot's review (written only when Roborazzi records).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h1800dp-xxhdpi")
class Tour9L15CapturesTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun tearDown() = RuntimeEnvironment.setFontScale(1f)

    private fun shot(name: String, uiScale: Float = 1f, fontScale: Float = 1f) {
        if (fontScale != 1f) RuntimeEnvironment.setFontScale(fontScale)
        val start = Project()
        val project = start.copy(drums = start.drums.withSteps(32))
        compose.setContent {
            UiScaled(uiScale) {
                PocketStudioTheme {
                    Box(Modifier.background(Neon.Background)) {
                        RhythmScreen(project = project, transport = TransportUi(selectedPad = 0), live = { LiveUi() }, canUndo = false, canRedo = false, actions = RhythmActions())
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/tour9-L15/$name.png")
    }

    private fun line(name: String, fontScale: Float, here: Int, elsewhere: Pair<Int, Int>?, toPad: Pair<String, () -> Unit>? = null) {
        RuntimeEnvironment.setFontScale(fontScale)
        compose.setContent {
            PocketStudioTheme {
                Box(Modifier.background(Neon.Background).padding(16.dp)) {
                    androidx.compose.foundation.layout.Column(Modifier.width(328.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                        com.pocketstudio.app.tuto.TutoGridLine(bar = 6, bars = 8, pad = "Claquement de mains", here = here, elsewhere = elsewhere, go = {}, copy = null, toPad = toPad)
                        com.pocketstudio.app.tuto.TutoGridLine(bar = 0, bars = 1, pad = "Kick", here = 0, elsewhere = null, go = {}, copy = {})
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/tour9-L15/$name.png")
    }

    @Test fun lineAt100() = line("ligne_tuto_100", 1f, here = 0, elsewhere = 3 to 5)
    @Test fun lineAt200() = line("ligne_tuto_200", 2f, here = 0, elsewhere = 3 to 5)
    @Test fun lineAt200OtherPad() = line("ligne_tuto_200_autre_pad", 2f, here = 0, elsewhere = null, toPad = "Caisse claire" to {})
    @Test fun lineAt200Here() = line("ligne_tuto_200_ici", 2f, here = 3, elsewhere = null)

    /** F057: the Swing with its « − » and « + » (the groove card, scrolled into view). */
    private fun swing(name: String, fontScale: Float) {
        RuntimeEnvironment.setFontScale(fontScale)
        val project = Project().copy(swing = 0.25 * Project.MAX_SWING)
        compose.setContent {
            PocketStudioTheme {
                Box(Modifier.background(Neon.Background)) {
                    RhythmScreen(project = project, transport = TransportUi(selectedPad = 0), live = { LiveUi() }, canUndo = false, canRedo = false, actions = RhythmActions())
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag(SWING_SPOT).performScrollTo().captureRoboImage("build/outputs/roborazzi/tour9-L15/$name.png")
    }

    @Test fun swingAt100() = swing("swing_100", 1f)
    @Test fun swingAt200() = swing("swing_200", 2f)

    @Test fun at100() = shot("rythme_360_100")
    @Test fun at130() = shot("rythme_360_130", uiScale = 1.3f)
    @Test fun at200() = shot("rythme_360_200", fontScale = 2f)
}
