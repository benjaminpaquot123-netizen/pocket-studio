package com.pocketstudio.app.export

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.rhythm.RhythmViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tour 5, lot « moyens » C (review of the share, second « moyen »; « restes », minor): when the project's .zip could not
 * be copied to Documents › BenStudio, « C'est prêt » no longer offers « Voir mes fichiers », which opened a folder the
 * .zip is not in. Kept, it does.
 */
@RunWith(RobolectricTestRunner::class)
class ZipSavedWindowTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun seeFiles(kept: Boolean): Int {
        val app: Application = ApplicationProvider.getApplicationContext()
        CurrentProject.set(app, "zip-window-${System.nanoTime()}")
        val vm = RhythmViewModel(app)
        var sender: ProjectZipSender? = null
        compose.setContent { sender = rememberProjectZipSender(vm) }
        compose.waitForIdle()
        sender!!.saved = "a.pocketstudio.zip" to kept
        compose.waitForIdle()
        assertEquals("« C'est prêt » is shown", 1, compose.onAllNodesWithTag("share_bilan", useUnmergedTree = true).fetchSemanticsNodes().size)
        return compose.onAllNodesWithTag("share_bilan_files", useUnmergedTree = true).fetchSemanticsNodes().size
    }

    @Test
    fun noMyFilesWhenTheCopyInDocumentsFailed() {
        assertEquals("« Voir mes fichiers » would open Documents, where the .zip is not", 0, seeFiles(kept = false))
    }

    @Test
    fun myFilesWhenTheCopyIsKept() {
        assertEquals(1, seeFiles(kept = true))
    }
}
