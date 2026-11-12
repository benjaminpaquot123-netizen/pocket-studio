package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.NativeEngine
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 5 « mixwake », review finding 1: the numbers and words that warn (« Ça sature », the limiter's line) are shown at
 * most 4 times a second, and showed only the latest reading: a clip of one reading (33 ms) falling between two samples
 * was never seen. They keep the worst reading of the period, and the Mode simple's tip follows the same value.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class ClipHoldTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @After
    fun finish() = h.finish()

    @Suppress("UNCHECKED_CAST")
    private fun <T> flow(name: String): MutableStateFlow<T> =
        RhythmViewModel::class.java.getDeclaredField(name).apply { isAccessible = true }.get(h.vm) as MutableStateFlow<T>

    private fun reading(limiter: Float, into: Float) = FloatArray(NativeEngine.METERS).also {
        it[NativeEngine.METER_LIMITER] = limiter
        it[NativeEngine.METER_MASTER_IN] = into
    }

    /**
     * Quiet readings (the first one starts a sample's pause), ONE clipping reading 33 ms later, quiet ones again: the
     * clip falls between two samples. [seen] is asked after every reading of the next 600 ms (the shown value changes
     * at most 4 times a second): it must say yes at least once.
     */
    private fun oneShortClip(seen: () -> Boolean): Boolean {
        val meters = flow<FloatArray>("_meters")
        meters.value = reading(1f, 0.30f)
        compose.mainClock.advanceTimeBy(600)
        compose.waitForIdle()
        // The clock moves by the readings only (a wait for idle would jump it to the next sample).
        compose.mainClock.autoAdvance = false
        meters.value = reading(1f, 0.31f)
        compose.mainClock.advanceTimeBy(33)
        meters.value = reading(0.4f, 2.5f)
        compose.mainClock.advanceTimeBy(33)
        var yes = false
        for (i in 0 until 18) {
            meters.value = reading(1f, 0.32f + i * 0.001f)
            compose.mainClock.advanceTimeBy(33)
            if (seen()) yes = true
        }
        return yes
    }

    private fun text(tag: String) = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().single().config
        .let { it[androidx.compose.ui.semantics.SemanticsProperties.Text] }.joinToString { it.text }

    @Test
    fun aSingleShortClipShowsInTheLimiterLine() {
        h.start()
        compose.onNodeWithTag("tab_Mix").performClick()
        compose.waitForIdle()
        assertTrue(oneShortClip { text("limiter_line").contains("retenu") })
    }

    @Test
    fun aSingleShortClipShowsInTheWordsAndTheTipAgrees() {
        h.start(simple = true)
        compose.onNodeWithTag("tab_simple_Mix").performClick()
        compose.waitForIdle()
        var tipSeen = false
        val words = oneShortClip {
            // The tip (« Ça sature » is what it is made for) follows the same held value as the words, a moment after.
            tipSeen = tipSeen || compose.onAllNodesWithTag("mix_tip_text").fetchSemanticsNodes().isNotEmpty()
            text("mix_out_words") == "Ça sature"
        }
        assertTrue("the words never said « Ça sature »", words)
        for (i in 0 until 8) {
            compose.mainClock.advanceTimeBy(33)
            tipSeen = tipSeen || compose.onAllNodesWithTag("mix_tip_text").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue("the tip never came with the words", tipSeen)
    }
}
