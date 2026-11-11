package com.pocketstudio.app.projects

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.settings.ProjectModes
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Round 5, chantier 6 (hands-on audit G11; promise P009 « Un style en une touche », not kept four rounds in a row: 4 to 5
 * touches through « Autres façons de commencer », a family, a card): on « Nouveau projet » five styles are in view, and
 * one touch on one of them makes its song; « Simple ou Pro ? » comes first only until a mode was chosen on this phone.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-xxhdpi")
class OneTouchStyleTest {
    @get:Rule
    val compose = createComposeRule()

    private val app: android.content.Context get() = ApplicationProvider.getApplicationContext()
    private val made = mutableListOf<String>()
    private val styles = listOf("techno", "house", "trap", "pop", "lofi")

    @Before
    fun fresh() {
        I18n.lang = Lang.FR
        app.getSharedPreferences("project_modes", 0).edit().clear().commit()
    }

    private fun show() {
        compose.setContent {
            PocketStudioTheme {
                ProjectsScreen(
                    ProjectsUi(loading = false), "p-0",
                    listOf(ProjectStarter("Rythme de départ", "Un rythme", "starter", StarterGroup.Style), ProjectStarter("Projet vide", "Vierge", "empty", StarterGroup.Empty)),
                    ProjectsActions(quickStyle = { made += "quick:$it" }, customStyle = { made += "custom:$it" }, wizard = { made += "wizard" }),
                    newOnly = true,
                )
            }
        }
        compose.waitForIdle()
    }

    private fun node(tag: String): SemanticsNode? =
        compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes(atLeastOneRootRequired = false).firstOrNull()

    /** Wholly inside the page's list as it opens: seen, touched, no scroll before. */
    private fun inView(tag: String): Boolean {
        val n = node(tag) ?: return false
        val frame = node("new_project_list")!!.boundsInRoot
        val top = n.positionInRoot.y
        return top >= frame.top - 1f && top + n.size.height <= frame.bottom + 1f
    }

    @Test
    fun fiveStylesAreInViewAsThePageOpens() {
        show()
        val missing = styles.filterNot { inView("one_touch_$it") }
        assertEquals("styles not in view on a 360 × 780 phone", emptyList<String>(), missing)
    }

    @Test
    fun aModeChosenOnceOneTouchMakesTheSong() {
        ProjectModes.choose(app, true)
        show()
        assertTrue("« House » to touch on the page", node("one_touch_house") != null)
        compose.onNodeWithTag("one_touch_house").performClick()
        compose.waitForIdle()
        assertEquals("one touch, the song", listOf("quick:house"), made)
        assertTrue("« Simple ou Pro ? » not asked", node("mode_step") == null)
    }

    @Test
    fun theVeryFirstTimeTheModeIsAskedOnceThenTheSong() {
        show()
        assertTrue("« Trap » to touch on the page", node("one_touch_trap") != null)
        compose.onNodeWithTag("one_touch_trap").performClick()
        compose.waitForIdle()
        assertEquals(emptyList<String>(), made)
        compose.onNodeWithTag("mode_card_simple").performClick()
        compose.waitForIdle()
        assertEquals(listOf("quick:trap"), made)
        // The next song: no question.
        compose.onNodeWithTag("one_touch_lofi").performClick()
        compose.waitForIdle()
        assertEquals(listOf("quick:trap", "quick:lofi"), made)
    }
}
