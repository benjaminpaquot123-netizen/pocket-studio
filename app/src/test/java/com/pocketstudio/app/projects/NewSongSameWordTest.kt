package com.pocketstudio.app.projects

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 7 lot 5 D4: « Mes projets » said « Nouveau projet » where the home screen (and the empty list's button) say
 * « Nouveau morceau ». One word for one thing: the list's button starts with the home screen's « Nouveau morceau ».
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class NewSongSameWordTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theListSaysNewSongLikeTheHomeScreen() {
        val open = com.pocketstudio.model.ProjectInfo("p-a", "Mon morceau", 1_800_000_000_000L, 120.0, 16, 1, false)
        compose.setContent {
            PocketStudioTheme {
                ProjectsScreen(ProjectsUi(loading = false, projects = listOf(open)), open.id, emptyList(), ProjectsActions())
            }
        }
        val node = compose.onNodeWithTag("project_new", useUnmergedTree = false).fetchSemanticsNode()
        val words = node.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text }.orEmpty()
        assertTrue("the button reads « $words »", words.trim().startsWith(tr("Nouveau morceau")))
    }
}
