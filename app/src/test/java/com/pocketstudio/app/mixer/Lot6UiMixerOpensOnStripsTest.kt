package com.pocketstudio.app.mixer

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.pocketstudio.app.rhythm.ExportFormat
import com.pocketstudio.app.rhythm.ExportUi
import com.pocketstudio.app.rhythm.ExportWhat
import com.pocketstudio.model.Project
import com.pocketstudio.model.TargetChoice
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Lot 6, R23 (seat C a #24-25, C b #30): an export already made (in « Envoyer », Simple) made the Mix Pro open at its
 * bottom, on « Autres fichiers », the strips out of sight. The Mix opens at its top (the console); the file made is
 * brought into view only when it is made while the Mix is on screen ([ExportResultInViewTest]).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-port-hdpi", fontScale = 1.3f)
class Lot6UiMixerOpensOnStripsTest {
    @get:Rule
    val compose = createComposeRule()

    private val made = ExportUi(
        what = ExportWhat.Song, files = listOf(File("Mon-morceau-20261110-201500.m4a")), savedInMusic = true, loudness = -8.6, truePeakDb = -1.2,
        seconds = 158.0, writtenFormat = ExportFormat.M4a, targetLufs = -8.5f.toDouble(), targetChoice = TargetChoice.AUTO, targetWholeSong = false,
    )

    private val scrolls = SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)

    private fun settle() = repeat(10) { compose.mainClock.advanceTimeBy(200); compose.waitForIdle() }

    private fun scrolled(): Float = compose.onAllNodes(scrolls, useUnmergedTree = true).fetchSemanticsNodes()
        .maxOf { it.config[SemanticsProperties.VerticalScrollAxisRange].value() }

    @Test
    fun anExportAlreadyMadeOpensTheMixOnItsStrips() {
        val p = Project()
        compose.setContent { PocketStudioTheme { MixerScreen(p.mixer.channels, { FloatArray(32) }, MixerActions(), export = made, hasSong = true) } }
        settle()
        assertEquals("the Mix opens at its top", 0f, scrolled(), 0.5f)
        compose.onNodeWithTag("automation_entry").assertIsDisplayed()
    }

    @Test
    fun aFileMadeWithTheMixOpenStillComesIntoView() {
        val p = Project()
        val export = mutableStateOf(ExportUi(what = ExportWhat.Song))
        compose.setContent { PocketStudioTheme { MixerScreen(p.mixer.channels, { FloatArray(32) }, MixerActions(), export = export.value, hasSong = true) } }
        settle()
        export.value = made
        settle()
        assertTrue("scrolled down to the file", scrolled() > 0f)
        compose.onNodeWithTag("export_result").assertIsDisplayed()
    }
}
