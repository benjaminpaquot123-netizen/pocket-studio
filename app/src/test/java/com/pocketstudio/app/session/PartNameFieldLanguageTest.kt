package com.pocketstudio.app.session

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 5, lot « langues » (seat D c in Italian, D c #38: touching « Ritornello » to name a part wrote « Refrain » in the field
 * « Oppure un altro nome »). The field shows the section as the chip says it, in the person's language; the part keeps its
 * section (« Refrain » stored, « Ritornello » shown everywhere).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
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
    fun theChosenSectionIsWrittenInItalianAndKeptAsItsSection() {
        I18n.lang = Lang.IT
        var done: String? = null
        // The field's cursor blinks for ever: the clock moved by hand.
        compose.mainClock.autoAdvance = false
        compose.setContent { PocketStudioTheme { RenameSceneDialog(2, "Couplet", onDone = { done = it }, onDismiss = {}) } }
        compose.mainClock.advanceTimeBy(300)
        assertEquals("Strofa", field())
        compose.onNodeWithTag("section_Refrain").performClick()
        compose.mainClock.advanceTimeBy(300)
        assertEquals("Ritornello", field())
        compose.onNodeWithTag("scene_name_ok").performClick()
        assertEquals("Refrain", done)
    }
}
