package com.pocketstudio.app.remix

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.AutoCut
import com.pocketstudio.model.SongCuts
import com.pocketstudio.ui.PocketStudioTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Tour 9, L11 (the rest of L10's F139, its review: the path the router really takes). A message from before stays on
 * the screen (it lives until « OK »); the screen's own note (« Aucune coupe trouvée ici… », found on a stem never cut)
 * comes over it and the column runs down to it, as it should; the user scrolls back up and touches the curve: the note
 * goes and the old message shows again under it. Acceptance: « L'écran de découpe ne défile que vers le message du
 * moment » — the old message coming back does not pull the column down again.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class CutLocalNoteScrollTest {
    @get:Rule
    val compose = createComposeRule()

    private val frames = 60L * BOARD_RATE

    /** The router as it is: a message from before, and auto cuts that find nothing (a stem almost silent). */
    private class Shown(message: String?) : StemBoardActions by NoStemBoardActions {
        override val state = MutableStateFlow(StemBoardState(message = message))
        override fun savedCuts(setId: String, frames: Long) = SongCuts("song", emptyList())
        override suspend fun autoCuts(setId: String, stem: Int, mode: AutoCut, from: Long, to: Long): List<LongRange> = emptyList()
    }

    private val column get() = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange), useUnmergedTree = true)
    private fun scrolled(): Float = column.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
    private fun settle() {
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
    }
    private fun said(): String = compose.onAllNodesWithTag("cut_message", useUnmergedTree = true).fetchSemanticsNodes()
        .joinToString { n -> n.config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }.orEmpty() }

    @Test
    fun theOldMessageBackUnderTheNoteDoesNotPullTheColumnDown() {
        val old = "« Voix » est sur les pads 9 à 14."
        val shown = Shown(old)
        val ui = StemBoardUi("song", "Ma chanson", frames, 120.0, 0.0, StemPeakSource { StemPeaks.of(frames, 1) { _, n -> FloatArray(n) } }, peaksContext = EmptyCoroutineContext)
        compose.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, 2f)) {
                PocketStudioTheme {
                    Box(Modifier.fillMaxWidth().height(430.dp)) {
                        val model = remember { CutModel("song", frames, 120.0, 0.0, SongCuts("song", emptyList()), mode = AutoCut.Selection) }
                        CutScreen(ui, shown, onClose = {}, model = model)
                    }
                }
            }
        }
        settle()
        // The screen's first cuts found nothing: its note is said, and the column came down to it (a message of now).
        assertTrue("the note « Aucune coupe trouvée » is not said (said: ${said()})", "Aucune coupe" in said())
        assertTrue("the note did not bring the column down to it", scrolled() > 0f)
        // The user goes back up and touches the curve: the note goes, the old message is under it again.
        column.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, -100_000f) }
        settle()
        assertEquals(0f, scrolled())
        compose.onNodeWithTag("cut_wave", useUnmergedTree = true).performTouchInput { click(center) }
        settle()
        assertEquals("the note is gone, the old message shows again", old, said())
        assertEquals("the old message, back under the note, ran the column down to it again", 0f, scrolled())
    }
}
