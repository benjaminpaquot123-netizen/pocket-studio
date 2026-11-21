package com.pocketstudio.app.human

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.mixer.MixerActions
import com.pocketstudio.app.mixer.MixerScreen
import com.pocketstudio.model.Project
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 9 (seats t9-04, 14, 24, 34, 28, 18: « curseurs immobiles » at the Mix's « levels » step). The Mix sizes its
 * faders with two consoles it measures and never places (`FitFaders`' probes); their nodes stay in the unmerged
 * semantics tree, at the shown console's own place. The bench's finger took one for a card drawn over the fader
 * (« #74 COUVERT « Drums » couvre « Volume van Akkoorden » ») and refused every drag on a fader and every touch on a
 * strip's name. Without the bench, the same touches move the fader and open the strip's sheet: the app is right.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h852dp-port-hdpi")
class BenchMixFaderCoverTest {
    @get:Rule
    val compose = createComposeRule()

    private val volumes = mutableListOf<Pair<Int, Float>>()

    private fun showMix() {
        val channels = Project().mixer.channels
        val actions = MixerActions(setVolume = { ch, v -> volumes += ch to v })
        compose.setContent {
            PocketStudioTheme { Box(Modifier.fillMaxSize()) { MixerScreen(channels, { FloatArray(17) }, actions) } }
        }
        compose.waitForIdle()
    }

    private fun centerOf(tag: String): Offset {
        val screen = ApplicationProvider.getApplicationContext<android.app.Application>().resources.displayMetrics
        return BenchWindows.drawnBounds(compose.onNodeWithTag(tag).fetchSemanticsNode(), screen).center
    }

    private fun finger(tag: String): ScreenText.Finger {
        val screen = ApplicationProvider.getApplicationContext<android.app.Application>().resources.displayMetrics
        val at = centerOf(tag)
        return ScreenText(compose, screen, reads = true, rtl = false).finger(74, at.x, at.y)
    }

    /**
     * Tour 9 L2, F024 option A: a fader is taken only near its cap. The reading gives the cap's place on the fader's
     * line (« poignée @ x,y »), so a seat aims at it.
     */
    private fun capOf(tag: String): Offset {
        val screen = ApplicationProvider.getApplicationContext<android.app.Application>().resources.displayMetrics
        val at = centerOf(tag)
        val lines = ScreenText(compose, screen, reads = true, rtl = false).read().lines
        val line = lines.firstOrNull { it.startsWith("curseur") && " @ ${at.x.toInt()},${at.y.toInt()}" in it }
        assertTrue("no line for the fader $tag at $at: $lines", line != null)
        val m = Regex("poignée @ (\\d+),(\\d+)").find(line!!)
        assertTrue("the reading does not give the fader's cap: $line", m != null)
        return Offset(m!!.groupValues[1].toFloat(), m.groupValues[2].toFloat())
    }

    /** No bench here: a plain finger drags the fader from its cap (where the reading puts it), and the app moves the volume. */
    @Test
    fun withoutTheBenchADragFromTheCapTheReadingGivesMovesTheFader() {
        showMix()
        val cap = capOf("volume_1")
        val screen = ApplicationProvider.getApplicationContext<android.app.Application>().resources.displayMetrics
        val box = BenchWindows.drawnBounds(compose.onNodeWithTag("volume_1").fetchSemanticsNode(), screen)
        val local = cap - box.topLeft
        compose.onNodeWithTag("volume_1").performTouchInput {
            swipe(local, local + Offset(0f, 50f), durationMillis = 200)
        }
        compose.waitForIdle()
        assertTrue("the drag from the cap did not move the volume of strip 1: $volumes", volumes.any { it.first == 1 })
    }

    /** No bench here: a plain touch on a strip's name opens the strip's sheet (its effects). */
    @Test
    fun withoutTheBenchATouchOnAStripsNameOpensItsSheet() {
        showMix()
        compose.onNodeWithTag("strip_more_1").performTouchInput { click() }
        compose.waitForIdle()
        compose.onNodeWithTag("strip_sheet").assertExists()
    }

    @Test
    fun theBenchsFingerIsNotRefusedOnAFader() {
        showMix()
        val f = finger("volume_1")
        assertFalse("the finger was refused on a fader nothing covers: ${f.lines}", f.refused)
        assertTrue(f.under, f.under.startsWith("volume_1"))
    }

    @Test
    fun theBenchsFingerIsNotRefusedOnAStripsName() {
        showMix()
        val f = finger("strip_more_1")
        assertFalse("the finger was refused on a strip's name nothing covers: ${f.lines}", f.refused)
        assertTrue(f.under, f.under.startsWith("strip_more_1"))
    }
}
