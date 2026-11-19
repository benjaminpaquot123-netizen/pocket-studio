package com.pocketstudio.app.export

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.pocketstudio.app.rhythm.KitChoice
import com.pocketstudio.app.rhythm.KitSheetContent
import com.pocketstudio.app.ui.TempoActions
import com.pocketstudio.app.ui.TempoSheet
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.PocketStudioTheme
import com.pocketstudio.ui.UiScaled
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 9, lot L8: the screens this lot touched (Partager, Tempo, Kit) on a 360 dp phone, at 200 % and in Arabic.
 * Images in app/screenshots/tour9-L8/, looked at by hand.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-xxhdpi")
class Tour9L8CapturesTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun end() {
        I18n.lang = Lang.FR
    }

    @OptIn(ExperimentalRoborazziApi::class)
    private fun shoot(name: String, lang: Lang, scale: Float, sheet: @Composable () -> Unit) {
        I18n.lang = lang
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides if (lang.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                PocketStudioTheme { UiScaled(scale) { Box(Modifier.fillMaxSize().background(Neon.Surface)) { sheet() } } }
            }
        }
        compose.waitForIdle()
        captureScreenRoboImage("screenshots/tour9-L8/$name.png")
    }

    private val share: @Composable () -> Unit = {
        ShareSheetContent(ShareSheetUi("Mon tube", seconds = 180, parts = 3, checks = emptyList()), ShareSheetActions())
    }
    private val tempo: @Composable () -> Unit = { TempoSheet(124.0, TempoActions(), onClose = {}) }
    private val kit: @Composable () -> Unit = {
        KitSheetContent(
            "Studio",
            listOf(
                KitChoice("builtin:2", "Studio", KitChoice.BUILT_IN),
                KitChoice("pack:house", "House", KitChoice.PACK),
                KitChoice("gm:0", "Standard", KitChoice.GM),
            ),
            choose = {}, close = {},
        )
    }

    @Test fun share200() = shoot("share_fr_200", Lang.FR, 2f, share)
    @Test fun shareAr() = shoot("share_ar", Lang.AR, 1f, share)
    @Test fun tempo200() = shoot("tempo_fr_200", Lang.FR, 2f, tempo)
    @Test fun tempoAr() = shoot("tempo_ar", Lang.AR, 1f, tempo)
    @Test fun kit200() = shoot("kit_fr_200", Lang.FR, 2f, kit)
    @Test fun kitAr() = shoot("kit_ar", Lang.AR, 1f, kit)
}
