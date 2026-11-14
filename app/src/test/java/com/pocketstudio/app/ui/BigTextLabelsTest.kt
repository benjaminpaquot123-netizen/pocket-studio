package com.pocketstudio.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.audit.LayoutScan
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.Palettes
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Audit ultra tour 0 (seat b, English, 411 dp, text at 130 %): the bottom bar's words broke inside a
 * word (« Keyboar / d », « Structur / e ») and the top bar's mode chip said « Si… ». Each word stays
 * whole, smaller if it must (never under 11 sp in the bar), on the phone of that seat and on a 360 dp one.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BigTextLabelsTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun french() {
        I18n.lang = Lang.FR
    }

    private val shown = androidx.compose.runtime.mutableStateOf(emptyList<String>())

    private fun draw(labels: List<String>, font: Float) {
        val first = shown.value.isEmpty()
        shown.value = labels
        if (!first) return
        compose.setContent {
            val labels = shown.value
            val local = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(local.density, font),
            ) {
                PocketStudioTheme(Palettes.Studio) {
                    Column(Modifier.fillMaxSize().background(Neon.Background), verticalArrangement = Arrangement.SpaceBetween) {
                        ModeChip(simple = true, onClick = {})
                        ModeChip(simple = false, onClick = {})
                        SpaceBar(labels.mapIndexed { i, l -> NavEntry("t$i", l, l, Icons.Filled.Star, i == 0) {} })
                    }
                }
            }
        }
    }

    private fun cuts(): List<LayoutScan.Finding> {
        val density = ApplicationProvider.getApplicationContext<android.content.Context>().resources.displayMetrics.density
        return LayoutScan.scan(compose, density, setOf(LayoutScan.Kind.CUT))
    }

    @Test
    @Config(qualifiers = "w411dp-h914dp-xhdpi")
    fun englishWordsStayWholeAt130() {
        I18n.lang = Lang.EN
        draw(listOf("My song", "Rhythm", "Keyboard", "Mix", "Send"), 1.3f)
        val found = cuts()
        assertTrue("$found", found.isEmpty())
        draw(listOf("My song", "Sounds", "Structure", "Mix", "Live", "Send"), 1.3f)
        val pro = cuts()
        assertTrue("$pro", pro.isEmpty())
    }

    /** Seat b, the pad's sheet: TalkBack read the volume's slider as « curseur », with no name. */
    @Test
    @Config(qualifiers = "w411dp-h914dp-xhdpi")
    fun thePadSheetsSlidersHaveTheirNames() {
        compose.setContent {
            PocketStudioTheme(Palettes.Studio) {
                androidx.compose.foundation.layout.Box(Modifier.background(Neon.Surface)) {
                    com.pocketstudio.app.rhythm.PadSheetContent(
                        com.pocketstudio.app.rhythm.PadSheetUi(0, "Kick", volume = 0.7f, pan = 0f, muted = false, hasSample = true, sounds = listOf("Kick", "Kick 2")),
                        com.pocketstudio.app.rhythm.PadSheetActions(), close = {},
                    )
                }
            }
        }
        // A slider's value (« 0.8 ») is not its name: TalkBack says what it moves.
        val sliders = compose.onAllNodes(androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsActions.SetProgress))
            .fetchSemanticsNodes()
        assertTrue("no slider", sliders.isNotEmpty())
        val unnamed = sliders.filter { n ->
            n.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.ContentDescription) { emptyList() }.joinToString("").isBlank() &&
                n.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.Text) { emptyList() }.joinToString("").isBlank()
        }.map { it.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.TestTag) { "?" } }
        assertTrue("sliders without a name: $unnamed", unnamed.isEmpty())
    }

    @Test
    @Config(qualifiers = "w360dp-h780dp-xhdpi")
    fun frenchWordsStayWholeAt130OnASmallPhone() {
        draw(listOf("Morceau", "Rythme", "Clavier", "Mix", "Envoyer"), 1.3f)
        val found = cuts()
        assertTrue("$found", found.isEmpty())
    }
}
