package com.pocketstudio.app.rhythm

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import com.pocketstudio.app.audit.LayoutScan
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.PocketStudioTheme
import com.pocketstudio.ui.UiScaled
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 9, lot L8 (capture of the kit sheet on a 360 dp phone at 200 %): the framed « Fermer » left the title a
 * sliver, « Kit de batterie » read « Kit / de / bat / teri / e ». Acceptance: at every interface size and in every
 * language, the sheet's top (title, hint, « Fermer ») shows no word cut.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-xxhdpi")
class KitSheetNarrowTopTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun end() {
        I18n.lang = Lang.FR
    }

    @Test
    fun theTopOfTheKitSheetIsNeverCut() {
        var case by mutableStateOf(Lang.FR to 1f)
        compose.setContent {
            key(case) {
                val (lang, scale) = case
                CompositionLocalProvider(LocalLayoutDirection provides if (lang.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                    PocketStudioTheme { UiScaled(scale) { Box(Modifier.fillMaxSize()) { KitSheetContent("builtin:2", listOf(KitChoice("builtin:2", "Studio", KitChoice.BUILT_IN)), {}, {}) } } }
                }
            }
        }
        val faults = mutableListOf<String>()
        Lang.entries.forEach { lang ->
            listOf(1f, 1.3f, 2f).forEach { scale ->
                I18n.lang = lang
                case = lang to scale
                compose.waitForIdle()
                val density = compose.density.density * scale
                LayoutScan.scan(compose, density, setOf(LayoutScan.Kind.CUT))
                    .filter { f -> f.what.length <= 30 }
                    .forEach { faults += "${lang.code} ${(scale * 100).toInt()} %: $it" }
            }
        }
        assertTrue(faults.take(12).joinToString("\n"), faults.isEmpty())
    }
}
