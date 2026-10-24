package com.pocketstudio.app.library

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Lot 1 (APK 181): « Nouveau projet » in the Mode simple opened with « Calme (Chill, Lo-fi) »
 * unfolded. Every family starts folded, and one touch opens one.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class StyleFamiliesFoldedTest {
    @get:Rule
    val compose = createComposeRule()

    private fun cards() = compose.onAllNodesWithTag("card", useUnmergedTree = true).fetchSemanticsNodes().size

    private fun folded(tag: String) = compose.onNodeWithTag(tag).assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "fermé"))

    @Test
    fun everyFamilyStartsFoldedInTheModeSimpleToo() {
        compose.setContent {
            PocketStudioTheme {
                androidx.compose.foundation.layout.Column(Modifier.verticalScroll(rememberScrollState())) {
                    StyleFamilies("quick", style = { Text(it.name, Modifier.testTag("card")) }, calm = true)
                }
            }
        }
        assertEquals("no style shown before a family is opened", 0, cards())
        folded("quick_family_calm")
        StarterWizard.FAMILIES.forEach { folded("quick_family_$it") }
        // One touch opens that family alone.
        compose.onNodeWithTag("quick_family_calm").performScrollTo().performClick()
        assertEquals(StarterWizard.calmStyles().size, cards())
        compose.onNodeWithTag("quick_family_Techno").performScrollTo().performClick()
        assertEquals(StarterWizard.calmStyles().size + StarterWizard.stylesOf("Techno").size, cards())
    }

    @Test
    fun theFamilyOfAChosenStyleIsTheOnlyOneOpen() {
        val chosen = StarterWizard.stylesOf("House").first()
        compose.setContent {
            PocketStudioTheme {
                androidx.compose.foundation.layout.Column(Modifier.verticalScroll(rememberScrollState())) {
                    StyleFamilies("wizard", style = { Text(it.name, Modifier.testTag("card")) }, open = setOf(chosen.family), calm = true)
                }
            }
        }
        assertEquals(StarterWizard.stylesOf("House").size, cards())
        folded("wizard_family_calm")
    }
}
