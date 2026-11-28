package com.pocketstudio.app.export

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 9, F126: « Le rond coché veut dire quelque chose ». The green check beside « Tout est prêt » is read with its
 * sentence, as one line: never a lone picture without a meaning (TalkBack, a touch on it).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class ShareCheckOkMeaningTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theCheckIsReadWithItsSentence() {
        compose.setContent {
            PocketStudioTheme {
                ShareSheetContent(ShareSheetUi("Mon tube", seconds = 180, parts = 3, checks = emptyList()), ShareSheetActions())
            }
        }
        val node = compose.onNodeWithTag("share_check_ok").fetchSemanticsNode()
        val said = node.config.getOrNull(SemanticsProperties.Text).orEmpty().joinToString(" ") { it.text }
        assertTrue("the check says nothing of its own: « $said »", said.contains("Tout est prêt"))
    }
}
