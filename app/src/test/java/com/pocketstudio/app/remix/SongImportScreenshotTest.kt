package com.pocketstudio.app.remix

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.pocketstudio.model.Project
import com.pocketstudio.model.SongSection
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Step 5c: « Mettre Boys dans mon projet », one screen, its safe choices made; a passage asked for. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class SongImportScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun importScreen() {
        val set = StemSet(File("boys"), "Boys", 200.0, 0L, SongInfo(bpm = 113.0, firstBeat = 0.2, root = 9, minor = true))
        val parts = listOf(SongSection("Intro", 0, 8), SongSection("Couplet", 8, 16), SongSection("Refrain", 24, 16), SongSection("Fin", 40, 8))
        var put: com.pocketstudio.model.SongImport? = null
        compose.setContent {
            PocketStudioTheme {
                SongImportSheet(set, Project(tempoBpm = 120.0), sections = { parts }, put = { c, _ -> put = c }, cancel = {})
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("screenshots/remix_import.png")
        compose.onNodeWithTag("import_passage").performClick()
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("screenshots/remix_import_passage.png")
        compose.onNodeWithTag("import_go").performClick()
        compose.waitForIdle()
        // An empty project: the song's tempo and key, the passage asked for.
        assertEquals(com.pocketstudio.model.SongImport(takeTempo = true, setKey = true, place = com.pocketstudio.model.SongPlace.Passage, bricks = false), put)
    }
}
