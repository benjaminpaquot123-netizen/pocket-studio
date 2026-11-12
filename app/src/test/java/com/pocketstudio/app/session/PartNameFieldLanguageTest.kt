package com.pocketstudio.app.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pocketstudio.model.Scene
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 5, lot « langues » (seat D c in Italian, D c #38: touching « Ritornello » to name a part wrote « Refrain » in the field
 * « Oppure un altro nome »). The field shows the section as its chip says it, in the person's language, and that chip stays lit.
 * (The dialog's fields drawn alone: under Robolectric a dialog holding a text field never goes idle.)
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp")
class PartNameFieldLanguageTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun back() {
        I18n.lang = Lang.FR
    }

    private fun field(): String =
        compose.onNodeWithTag("scene_name_field").fetchSemanticsNode().config.getOrNull(SemanticsProperties.EditableText)?.text.orEmpty()

    @Test
    fun theChosenSectionIsWrittenInItalian() {
        I18n.lang = Lang.IT
        var name by mutableStateOf(Scene.sectionName("Couplet"))
        compose.setContent { PocketStudioTheme { RenameSceneFields(name) { name = it } } }
        compose.onNodeWithTag("section_Refrain").performClick()
        compose.waitForIdle()
        assertEquals("Ritornello", field())
        compose.onNodeWithTag("section_Refrain").assertIsSelected()
    }
}
