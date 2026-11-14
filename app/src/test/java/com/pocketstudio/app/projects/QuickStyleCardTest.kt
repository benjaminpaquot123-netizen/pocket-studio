package com.pocketstudio.app.projects

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import com.github.takahirom.roborazzi.captureRoboImage
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

    private fun tap(tag: String) {
        val node = compose.onNodeWithTag(tag)
        runCatching { node.performScrollTo() }  // a page that does not scroll has everything in view
        node.performClick()
        compose.waitForIdle()
    }

    /** « Nouveau morceau » › « Autres façons de commencer » › the style's family opened. */
    private fun openFamily() {
        tap("project_new")
        tap("start_others")
        tap("quick_family_${style.family}")
        compose.onNodeWithTag("quick_style_${style.id}").performScrollTo()
    }

    private fun wordsOf(tag: String, unmerged: Boolean = false): String =
        compose.onNodeWithTag(tag, useUnmergedTree = unmerged).fetchSemanticsNode().config.getOrNull(SemanticsProperties.Text).orEmpty().joinToString(" ") { it.text }

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

    @Test
    fun noWordOfAStyleCardIsCutAt130PercentOn360dpInAnyLanguage() {
        var lang by mutableStateOf(Lang.FR)
        show(1.3f) { lang }
        val problems = ArrayList<String>()
        for (l in Lang.entries) {
            lang = l
            compose.waitForIdle()
            openFamily()
            for (part in listOf("quick_custom_text_", "quick_create_text_")) {
                if (cut(part + style.id)) problems += "${l.code}: $part « ${wordsOf(part + style.id, unmerged = true)} »"
            }
        }
        assertTrue("Cut on a style card:\n" + problems.joinToString("\n"), problems.isEmpty())
    }

    /**
     * Tour 5, lot « moyens » C (« restes », out of lot): the opened « Hip-hop » header was pink on pink, its name light on
     * a pale wash. Its panel was see-through (the family's colour at 16 %), so it read only on the page's dark: an opened
     * header is read on its own panel, whatever lies under it (here, nothing: the page's own background is not drawn).
     */
    @Test
    fun anOpenedFamilyHeaderReadsWhateverLiesUnderIt() {
        show(1f) { Lang.FR }
        openFamily()
        compose.onNodeWithTag("quick_family_${style.family}").performScrollTo()
        val density = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>().resources.displayMetrics.density
        val found = com.pocketstudio.app.audit.LayoutScan.scan(compose, density, setOf(com.pocketstudio.app.audit.LayoutScan.Kind.CONTRAST))
            .filter { it.what == "Hip-hop" || it.what == "7 styles" }
        assertEquals("the opened header's words under AA contrast", emptyList<Any>(), found)
    }

    /** The images: the opened family, the style's card with its two buttons (the phone, then the 130 % size). */
    @Test
    fun cardImage() {
        show(1f) { Lang.FR }
        openFamily()
        compose.onNodeWithTag("quick_custom_${style.id}").performScrollTo()
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("screenshots/projects_new_style_card.png")
    }

    @Test
    fun cardImageAt130Percent() {
        show(1.3f) { Lang.FR }
        openFamily()
        compose.onNodeWithTag("quick_custom_${style.id}").performScrollTo()
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("screenshots/scale130/fr_projects_new_style_card.png")
    }
}
