package com.pocketstudio.app

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pocketstudio.ui.LocalFoldStore
import com.pocketstudio.ui.MemoryFoldStore
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.PocketStudioTheme
import com.pocketstudio.ui.SectionLabel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class FoldTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aTouchOnTheTitleFoldsTheCardAndAnotherOpensIt() {
        val store = MemoryFoldStore()
        compose.setContent {
            PocketStudioTheme {
                CompositionLocalProvider(LocalFoldStore provides store) {
                    NeonCard(Modifier.testTag("card")) {
                        SectionLabel("Scènes")
                        repeat(8) { Text("Ligne $it du contenu") }
                    }
                }
            }
        }
        val open = compose.onNodeWithTag("card").getBoundsInRoot().let { it.bottom - it.top }
        compose.onNodeWithTag("card").assertHeightIsAtLeast(150.dp)

        compose.onNodeWithTag("fold_Scènes").performClick()
        compose.waitForIdle()
        assertTrue(store.isFolded("Scènes"))
        val folded = compose.onNodeWithTag("card").getBoundsInRoot().let { it.bottom - it.top }
        assertTrue("folded $folded, open $open", folded < open / 2)

        compose.onNodeWithTag("fold_Scènes").performClick()
        compose.waitForIdle()
        assertFalse(store.isFolded("Scènes"))
    }
}
