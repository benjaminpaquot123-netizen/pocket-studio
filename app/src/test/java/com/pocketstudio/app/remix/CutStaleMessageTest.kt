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
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.AutoCut
import com.pocketstudio.model.CutPiece
import com.pocketstudio.model.SongCuts
import com.pocketstudio.model.StemCuts
import com.pocketstudio.ui.PocketStudioTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Tour 9, L10 F139 (review T9): « L'écran de découpe ne défile que vers le message du moment. » A message left from
 * before stays where it is on arrival (tour 8, R); once a work came and went (« Voix vers le pad… ») and the message
 * left under it was still that old one, the column ran down to it again, away from where the user had scrolled.
 */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class CutStaleMessageTest {
    @get:Rule
    val compose = createComposeRule()

    private val frames = 60L * BOARD_RATE
    private val bar = 4 * 24_000L

    private class Shown(private val cuts: SongCuts, message: String?) : StemBoardActions by NoStemBoardActions {
        override val state = MutableStateFlow(StemBoardState(message = message))
        override fun savedCuts(setId: String, frames: Long) = cuts
    }

    private lateinit var shown: Shown

    private fun show(message: String?) {
        val cuts = SongCuts("song", listOf(StemCuts(VOCALS_STEM, listOf(CutPiece(2 * bar, 4 * bar)), auto = "Selection")))
        val ui = StemBoardUi("song", "Ma chanson", frames, 120.0, 0.0, StemPeakSource { StemPeaks.of(frames, 1) { _, n -> FloatArray(n) } }, peaksContext = EmptyCoroutineContext)
        shown = Shown(cuts, message)
        compose.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, 2f)) {
                PocketStudioTheme {
                    Box(Modifier.fillMaxWidth().height(430.dp)) {
                        val model = remember { CutModel("song", frames, 120.0, 0.0, cuts, mode = AutoCut.Selection) }
                        CutScreen(ui, shown, onClose = {}, model = model)
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private val column get() = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange), useUnmergedTree = true)
    private fun scrolled(): Float = column.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
    private fun toTop() {
        column.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, -100_000f) }
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
    }
    private fun set(s: StemBoardState) {
        shown.state.value = s
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
    }

    @Test
    fun theOldMessageLeftUnderAWorkDoesNotPullTheColumnDown() {
        val old = "« Voix » est sur les pads 9 à 14."
        show(old)
        assertEquals("a message from before moved the column on arrival", 0f, scrolled())
        set(StemBoardState(busy = "Voix vers le pad…", message = old))
        toTop()
        set(StemBoardState(message = old))
        assertEquals("the work ended on the same old message: the column ran down to it", 0f, scrolled())
    }

    @Test
    fun aNewMessageStillBringsTheColumnDown() {
        show("« Voix » est sur les pads 9 à 14.")
        set(StemBoardState(message = "« Voix 1 » est sur une nouvelle piste, mesure 3."))
        assertTrue("a new message did not bring the column down to it", scrolled() > 0f)
    }
}
