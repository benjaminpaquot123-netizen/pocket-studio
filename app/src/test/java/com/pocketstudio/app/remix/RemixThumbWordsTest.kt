package com.pocketstudio.app.remix

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.PocketStudioTheme
import com.pocketstudio.ui.WholeWordsAutoSize
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 6, R1 (seat E c #5, pt-BR at 200 %): the Remix page's thumb button read « Baixar o modelo (174 », its size cut off.
 * The thumb row's words read whole, at 200 %, in Brazilian Portuguese.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class RemixThumbWordsTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun french() {
        I18n.lang = Lang.FR
    }

    private fun whole(text: String): Boolean = compose.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().all { n ->
        val results = ArrayList<TextLayoutResult>()
        n.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        results.isEmpty() || WholeWordsAutoSize.fits(results.first(), text)
    }

    @Test
    fun theModelsButtonReadsWholeInPortugueseAtTwiceTheText() {
        I18n.lang = Lang.PT_BR
        compose.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, 2f)) {
                PocketStudioTheme { RemixScreen(RemixUi(false, SeparationState.Idle, emptyList()), RemixActions()) }
            }
        }
        compose.waitForIdle()
        val label = com.pocketstudio.model.i18n.tr("Télécharger le modèle (%s)", SeparationModel.SPEC.sizeLabel)
        assertTrue("« $label » cut at 200 %", whole(label))
    }
}
