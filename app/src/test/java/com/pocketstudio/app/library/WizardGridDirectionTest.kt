package com.pocketstudio.app.library

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.PocketStudioTheme
import com.pocketstudio.ui.layoutDirectionOf
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 5, lot « langues » (promise t5-Bd-2, seat B d in Arabic: the assistant's little grid had its step 1 on the right,
 * the Rythme tab's grid on the left). Music reads left to right in every language (com.pocketstudio.ui.LeftToRight): the
 * assistant's grid too, its beat « 1 » left of « 2 », in Arabic as in French.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class WizardGridDirectionTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun back() {
        I18n.lang = Lang.FR
    }

    private fun ui(): WizardUi {
        val style = StarterWizard.style("house")!!
        val rows = style.lines.associate { l ->
            val cats = soundCategories(l.role, style.id, emptyList())
            l.role to WizardRow(true, cats.first().id, cats.first().options.first())
        }
        val parts = StarterWizard.Part.entries.associateWith { PartRow(it.byDefault, StarterWizard.soundsFor(it, style).first()) }
        return WizardUi(true, 2, style.id, rows, StarterWizard.Mood.Happy, parts)
    }

    private fun beatX(n: String): Float =
        compose.onAllNodes(hasText(n) and hasAnyAncestor(hasTestTag("wizard_pattern")), useUnmergedTree = true)[0].getBoundsInRoot().left.value

    @Test
    fun theAssistantsGridRunsLeftToRightInArabic() {
        I18n.lang = Lang.AR
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirectionOf(true)) {
                PocketStudioTheme { WizardScreen(ui(), PacksUi(IndexState.Unavailable), null, WizardActions()) }
            }
        }
        compose.onAllNodesWithTag("wizard_pattern")[0].performScrollTo()
        assertTrue("beat 1 at ${beatX("1")}, beat 2 at ${beatX("2")}", beatX("1") < beatX("2"))
    }
}
