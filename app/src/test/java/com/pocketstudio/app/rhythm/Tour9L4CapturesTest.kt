package com.pocketstudio.app.rhythm

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.LayoutDirection
import com.github.takahirom.roborazzi.captureRoboImage
import com.pocketstudio.app.ui.UiScaled
import com.pocketstudio.model.Project
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
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
 * Tour 9, lot L4: the whole Rythme screen on a 360 dp phone at 100 %, Réglages › Taille 130 %, the phone's font at
 * 200 % and in Arabic, for the lot's review (written only when Roborazzi records).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h1800dp-xxhdpi")
class Tour9L4CapturesTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun tearDown() {
        I18n.lang = Lang.FR
        RuntimeEnvironment.setFontScale(1f)
    }

    private fun shot(name: String, uiScale: Float = 1f, fontScale: Float = 1f, lang: Lang = Lang.FR) {
        if (fontScale != 1f) RuntimeEnvironment.setFontScale(fontScale)
        I18n.lang = lang
        val start = Project()
        val project = start.copy(drums = start.drums.withSteps(32))
        compose.setContent {
            val dir = if (lang == Lang.AR) LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides dir) {
                UiScaled(uiScale) {
                    PocketStudioTheme {
                        RhythmScreen(project = project, transport = TransportUi(selectedPad = 0), live = { LiveUi() }, canUndo = false, canRedo = false, actions = RhythmActions())
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/tour9-L4/$name.png")
    }

    @Test fun at100() = shot("rythme_360_100")
    @Test fun at130() = shot("rythme_360_130", uiScale = 1.3f)
    @Test fun at200() = shot("rythme_360_200", fontScale = 2f)
    @Test fun arabic() = shot("rythme_360_ar", lang = Lang.AR)
}
