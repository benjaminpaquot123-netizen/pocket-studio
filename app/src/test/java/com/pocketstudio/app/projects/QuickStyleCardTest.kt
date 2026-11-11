package com.pocketstudio.app.projects

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import com.pocketstudio.app.ui.UiScaled
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 5, « restes » (second review of the « textes » lot): a style card of « Nouveau morceau » has two ways in. A touch
 * on the card makes the song at once; its second button opens « Fais-moi un morceau » on that style, to set the sounds
 * and parts first. Its words say that difference (« Régler avant de créer », not « Choisir ce style », which is the
 * assistant's own button), and neither the button nor « Créer le morceau » is ever cut: 360 dp, the 130 % size, any language.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-xxhdpi")
class QuickStyleCardTest {
    @get:Rule
    val compose = createComposeRule()

    private val style = StarterWizard.STYLES.first { it.family == "Hip-hop" }

    private fun show(scale: Float, lang: () -> Lang) {
        compose.setContent {
            I18n.lang = lang()
            androidx.compose.runtime.key(lang()) {
                UiScaled(scale) {
                    PocketStudioTheme {
                        ProjectsScreen(
                            ProjectsUi(loading = false), "p-0",
                            listOf(ProjectStarter("Rythme de départ", "Un rythme", "starter", StarterGroup.Style)),
                            ProjectsActions(),
                        )
                    }
                }
            }
        }
    }

    /** « Nouveau morceau » › « Autres façons de commencer » › the style's family opened. */
    private fun openFamily() {
        compose.onNodeWithTag("project_new").performClick()
        compose.onNodeWithTag("start_others").performClick()
        compose.onNodeWithTag("quick_family_${style.family}").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("quick_style_${style.id}").performScrollTo()
    }

    private fun wordsOf(tag: String): String =
        compose.onNodeWithTag(tag).fetchSemanticsNode().config.getOrNull(SemanticsProperties.Text).orEmpty().joinToString(" ") { it.text }

    /** The words of a text node as laid out (cut: its layout overflows or ends in « … »). */
    private fun cut(tag: String): Boolean {
        val node = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()
        val results = ArrayList<TextLayoutResult>()
        node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        check(results.isNotEmpty()) { "$tag: no text laid out" }
        val r = results.first()
        return r.multiParagraph.maxIntrinsicWidth > r.size.width + 0.5f || r.didOverflowHeight || (0 until r.lineCount).any(r::isLineEllipsized)
    }

    @Test
    fun theSecondButtonSaysItSetsThingsBeforeCreating() {
        show(1f) { Lang.FR }
        openFamily()
        assertEquals("Régler avant de créer", wordsOf("quick_custom_${style.id}"))
        assertTrue(wordsOf("quick_custom_${style.id}"), "Choisir ce style" !in wordsOf("quick_custom_${style.id}"))
    }
}
