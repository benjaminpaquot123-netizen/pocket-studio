package com.pocketstudio.app.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.pocketstudio.app.audit.LayoutScan
import com.pocketstudio.app.projects.ProjectStarter
import com.pocketstudio.app.projects.ProjectsActions
import com.pocketstudio.app.projects.ProjectsScreen
import com.pocketstudio.app.projects.ProjectsUi
import com.pocketstudio.app.projects.StarterGroup
import com.pocketstudio.app.ui.UiScaled
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Round 5, chantier 6 (hands-on audit G9; Benjamin, 2026-11-06: « la carte suivante est coupée en bas mais rien ne le
 * signale »): « Bienvenue », « Que veux-tu faire ? » and « Nouveau projet » say that they scroll. While a card is below
 * the fold, the list carries « ↓ n autres » (n: the cards not wholly in view), a touch on it brings them up, and at the
 * end of the list it is gone. A list that fits carries nothing. Rule 17 of docs/DESIGN.md, read by [LayoutScan.unhintedScrolls].
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-xxhdpi")
class HomeScreensSayTheyScrollTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun french() { I18n.lang = Lang.FR }

    private val welcomeCards = listOf(
        "welcome_guided", "welcome_learn", "welcome_demo", "welcome_empty", "welcome_remix", "launch_padlive", "welcome_voice", "launch_challenges",
    )
    private val launchCards = listOf(
        "launch_continue", "launch_new", "launch_projects", "launch_remix", "launch_padlive", "launch_challenges", "launch_voice", "launch_learn", "launch_missions",
    )

    private fun show(scale: Float = 1f, screen: @Composable () -> Unit) {
        I18n.lang = Lang.FR
        compose.setContent { UiScaled(scale) { PocketStudioTheme { screen() } } }
        compose.waitForIdle()
    }

    private fun node(tag: String): SemanticsNode? =
        compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes(atLeastOneRootRequired = false).firstOrNull()

    private fun hintWords(): String? = node("scroll_hint_words")?.config?.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }

    private fun scrolled(list: String): Float = node(list)!!.config[SemanticsProperties.VerticalScrollAxisRange].value()

    private fun more(list: String): Boolean = node(list)!!.config[SemanticsProperties.VerticalScrollAxisRange].let { it.value() < it.maxValue() - 0.5f }

    /** The cards of [cards] not wholly inside the frame of [list] (counted from the layout, not from the hint). */
    private fun below(list: String, cards: List<String>): Int {
        val frame: Rect = node(list)!!.boundsInRoot
        return cards.mapNotNull { node(it) }.count { c ->
            val top = c.positionInRoot.y
            top + c.size.height > frame.bottom + 1f
        }
    }

    private fun expected(n: Int) = if (n == 1) "↓ 1 autre" else "↓ $n autres"

    /** The rule on one screen, from its top to its end. */
    private fun saysItScrolls(list: String, cards: List<String>) {
        assertTrue("$list: the list goes on below (a phone of 360 × 780 dp)", more(list))
        assertEquals("$list: a list that goes on carries the hint", emptyList<String>(), LayoutScan.unhintedScrolls(compose))
        val n = below(list, cards)
        assertTrue("$list: cards below the fold", n > 0)
        assertEquals("$list: the hint counts the cards below", expected(n), hintWords())
        // A touch on it brings the next one up.
        val before = scrolled(list)
        compose.onNodeWithTag("scroll_hint").performClick()
        compose.waitForIdle()
        assertTrue("$list: the touch scrolled", scrolled(list) > before + 1f)
        // At the end of the list, nothing more is said.
        repeat(6) { if (more(list)) { compose.onNodeWithTag(list).performTouchInput { swipeUp() }; compose.waitForIdle() } }
        assertTrue("$list: at the end", !more(list))
        assertEquals("$list: no hint at the end of the list", null, node("scroll_hint"))
    }

    @Test
    fun theWelcomeSaysItScrolls() {
        show { WelcomeScreen(WelcomeActions()) }
        saysItScrolls("welcome", welcomeCards)
    }

    @Test
    fun whatDoYouWantToDoSaysItScrolls() {
        show { LaunchScreen(LaunchUi(name = "Mon morceau", style = "Techno", length = "3:05"), LaunchActions(voice = {}, learn = {})) }
        saysItScrolls("launch", launchCards)
    }

    @Test
    fun whatDoYouWantToDoWithoutProjectAt130PercentSaysItScrolls() {
        show(1.3f) { LaunchScreen(LaunchUi(name = "", hasProject = false), LaunchActions(voice = {}, learn = {})) }
        saysItScrolls("launch", launchCards)
    }

    @Test
    fun newProjectSaysItScrolls() {
        show {
            ProjectsScreen(
                ProjectsUi(loading = false), "p-0",
                listOf(ProjectStarter("Rythme de départ", "Un rythme", "starter", StarterGroup.Style), ProjectStarter("Projet vide", "Vierge", "empty", StarterGroup.Empty)),
                ProjectsActions(), newOnly = true,
            )
        }
        compose.onNodeWithTag("start_others").performClick()
        compose.waitForIdle()
        assertTrue("« Autres façons de commencer » open: the list goes on", more("new_project_list"))
        assertEquals(emptyList<String>(), LayoutScan.unhintedScrolls(compose))
        assertTrue("the hint is there: ${hintWords()}", hintWords()?.startsWith("↓ ") == true)
    }

    /** A tablet lying down: the two columns hold every card, nothing is said. */
    @Test
    @Config(qualifiers = "w1280dp-h800dp-mdpi")
    fun aListThatFitsSaysNothing() {
        show { LaunchScreen(LaunchUi(name = "Mon morceau"), LaunchActions()) }
        assertTrue("everything fits", !more("launch"))
        assertEquals(null, node("scroll_hint"))
    }
}
