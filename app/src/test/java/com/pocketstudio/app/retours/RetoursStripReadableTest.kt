package com.pocketstudio.app.retours

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
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
 * Tour 9, F135: « La barre de retours se lit ». Seats (ja and it at 200 %, nl) read its words cut. On a 360 dp phone,
 * at 100 % and 200 %, in every language: no word of the strip cut.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-port-hdpi")
class RetoursStripReadableTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun french() { I18n.lang = Lang.FR }

    @Test
    fun noWordOfTheStripIsCut() {
        val cases = Lang.entries.flatMap { l -> listOf(1f, 2f).map { l to it } }
        var case by mutableStateOf(cases.first())
        compose.setContent {
            key(case) {
                I18n.lang = case.first
                UiScaled(case.second) { PocketStudioTheme { RetoursHost(show = true) { Box(Modifier.fillMaxSize()) } } }
            }
        }
        val density = compose.density.density
        val faults = mutableListOf<String>()
        cases.forEach { c ->
            compose.runOnIdle { case = c }
            LayoutScan.scan(compose, density, setOf(LayoutScan.Kind.CUT)).forEach { faults += "${c.first.code} ${c.second}: $it" }
        }
        assertTrue(faults.joinToString("\n"), faults.isEmpty())
    }
}
