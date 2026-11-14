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
 * Lot 6, U16 (seat F a #66, Italian): the share closed without sending, the name field read « Nome del brano inviato »
 * (« Nom du morceau envoyé »): « already sent? ». Its label never says « sent »: nothing has left the phone yet.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-port-hdpi", fontScale = 1.3f)
class Lot6UiShareNameLabelTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun french() { I18n.lang = Lang.FR }

    private val name = androidx.compose.runtime.mutableStateOf("")

    private fun show(lang: Lang, projectName: String, simple: Boolean) {
        I18n.lang = lang
        compose.setContent {
            val ui = ShareSheetUi(name = name.value, seconds = 120, parts = 3, checks = emptyList(), projectName = projectName)
            PocketStudioTheme { ShareSheetContent(ui, ShareSheetActions(), page = simple, simple = simple) }
        }
    }

    private fun label(typed: String): String {
        name.value = typed
        compose.waitForIdle()
        val field = compose.onAllNodesWithTag("share_name", useUnmergedTree = false).fetchSemanticsNodes().single()
        return listOfNotNull(
            field.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text },
            field.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" "),
        ).joinToString(" ")
    }

    private fun saysNotSent(lang: Lang, simple: Boolean, sent: String) {
        // A project with a name of its own (it keeps it): the field holds the shared song's name, the same, or another.
        show(lang, "Notte di primavera", simple)
        listOf("Notte di primavera", "Notte di primavera (MR)").forEach { typed ->
            val l = label(typed)
            assertTrue("the field has a label: $l", l.isNotBlank())
            assertFalse("« $l » says the song was sent", l.contains(sent, ignoreCase = true))
        }
    }

    @Test
    fun italianProSheet() = saysNotSent(Lang.IT, simple = false, sent = "inviato")

    @Test
    fun frenchSimplePage() = saysNotSent(Lang.FR, simple = true, sent = "envoyé")
}
