package com.pocketstudio.app.export

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 6, seat A c (E6): on the share page the « projet .zip » line read « Sauvegarder le morceau en .zip », yet its
 * one touch opens Android's share menu (it sends the file, it does not save it on its own). A button that sends is
 * named « Partager » or « Envoyer »: the word on screen matches what the touch does.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-port-hdpi", fontScale = 1.0f)
class Tour6ExportsSaveLabelTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun french() { I18n.lang = Lang.FR }

    private fun projectLineText(simple: Boolean): String {
        I18n.lang = Lang.FR
        val ui = ShareSheetUi(name = "Trap", seconds = 120, parts = 3, checks = emptyList(), projectName = "Trap")
        compose.setContent {
            PocketStudioTheme { ShareSheetContent(ui, ShareSheetActions(sendProject = {}), page = simple, simple = simple) }
        }
        compose.waitForIdle()
        // Merged tree: LinkRow is one node (clickable, role Button), its title and subtitle merged into it.
        val node = compose.onAllNodesWithTag("share_project").fetchSemanticsNodes().first()
        return listOfNotNull(
            node.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text },
            node.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" "),
        ).joinToString(" ")
    }

    @Test
    fun theProjectZipLineIsNamedAfterWhatItDoesWhichIsToSend() {
        listOf(true, false).forEach { simple ->
            val text = projectLineText(simple)
            assertTrue("the project line has a title: « $text »", text.isNotBlank())
            assertTrue(
                "« $text » must be named « Partager » or « Envoyer » (its touch opens the share menu)",
                text.contains("Partager", ignoreCase = true) || text.contains("Envoyer", ignoreCase = true),
            )
            assertFalse(
                "« $text » must not say « Sauvegarder » for an action that sends the file",
                text.contains("Sauvegarder", ignoreCase = true),
            )
        }
    }
}
