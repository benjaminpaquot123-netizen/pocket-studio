package com.pocketstudio.app.export

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 6, U22 (seat A c #27, its real part): while « Mes fichiers » reads the phone's files, the window showed only
 * where they are, no sign of waiting (« vide, sans même aucun fichier »). A sign of waiting shows meanwhile, then the
 * list, or « Rien ici pour l'instant » (nothing in Robolectric's MediaStore), and the sign is gone.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-xxhdpi")
class MyFilesWaitTest {
    @get:Rule
    val compose = createComposeRule()

    private val waiting = SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate)

    @Test
    fun whileTheFilesAreReadAWaitSignShowsThenNothingHereYet() {
        // The clock held: the window as drawn before the reading comes back.
        compose.mainClock.autoAdvance = false
        compose.setContent { PocketStudioTheme { MyExportsDialog {} } }
        compose.onNodeWithTag("my_exports").assertExists()
        val signs = compose.onAllNodes(waiting, useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue("« Mes fichiers » being read shows no sign of waiting", signs.isNotEmpty())
        compose.mainClock.autoAdvance = true
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("my_exports_empty").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(waiting, useUnmergedTree = true).assertCountEquals(0)
    }
}
