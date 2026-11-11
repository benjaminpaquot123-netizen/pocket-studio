package com.pocketstudio.app.nav

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.isDisplayed
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * Audit ultra round 1 (lot A, seats a and d): ▶ then a tab, « Retour », ■: every touch left the screen as it
 * was. The app with an engine that stays, as a phone's does (no bench): the song plays from its part, the
 * position under the top bar is polled from the engine, and every tab touched is the one shown, while it
 * plays and after ■. (The freeze was the bench's: it let its engine go under the running poll.)
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w392dp-h852dp-hdpi")
class PlayThenTabsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    /** The phone's engine as the screens read it: the song and the loop move on, a step every 120 ms. */
    private class Engine : CommandSink {
        private var next = 1L
        @Volatile var polls = 0
        private var startedAt = -1L
        private fun step(): Int = if (startedAt < 0) -1 else ((android.os.SystemClock.uptimeMillis() - startedAt) / 120).toInt()
        override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) {
            if (target == 0L && type == SET_TRANSPORT) startedAt = if (value > 0f) android.os.SystemClock.uptimeMillis() else -1L
        }
        override fun scheduleFrom(target: Long, frame: Long) = Unit
        override fun install(target: Long, kind: CommandSink.Install, index: Int, sample: Long) = Unit
        @Synchronized override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long = next++
        override fun sampleFrames(handle: Long): Long = 48_000L
        override fun sampleRelease(handle: Long) = Unit
        @Synchronized override fun exportCreate(): Long = next++
        override fun exportRender(target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean, sampleRate: Int, bits: Int) =
            ExportResult.Cancelled
        override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray = FloatArray(0)
        override fun exportDestroy(target: Long) = Unit
        override fun songStep(): Int { polls++; return step() }
        override fun absoluteStep(): Int { polls++; return step() }
        override fun currentStep(): Int = step().coerceAtLeast(0) % 16
    }

    private val engine = Engine()

    @Before
    fun engineThere() {
        NativeEngine.sink = engine
    }

    @After
    fun done() {
        NativeEngine.sink = null
        h.finish()
    }

    /** [ms] of the phone's time: the main thread's clock and the screen's. */
    private fun idle(ms: Long = 200) {
        var left = ms
        while (left > 0) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            compose.mainClock.advanceTimeBy(50)
            left -= 50
        }
        compose.waitForIdle()
    }

    private fun tap(tag: String) {
        val node = compose.onNodeWithTag(tag)
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
        idle(300)
    }

    /** The tab shown (the bench's ROUTE reads the same: the tab whose entry is selected). */
    private fun tabShown(): String? =
        compose.onAllNodes(SemanticsMatcher("a tab") { n -> n.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("tab_") == true })
            .fetchSemanticsNodes().firstOrNull { it.config.getOrNull(SemanticsProperties.Selected) == true }
            ?.config?.getOrNull(SemanticsProperties.TestTag)

    private fun goesTo(tag: String, what: String) {
        tap(tag)
        assertEquals("$what: the tab touched is the one shown", tag, tabShown())
    }

    private fun playThenTabs(simple: Boolean, tabs: List<String>) {
        h.start(simple = simple)
        idle()
        assertFalse(h.vm.transport.value.playing)
        // « ▶ le morceau » (tour 5, chantier 4): the song.
        tap("thumb_play")
        assertTrue("▶ plays", h.vm.transport.value.playing)
        assertTrue("the song plays, from its part", h.vm.sessionUi.value.songIndex >= 0)
        idle(1_000)
        assertTrue("the position under the top bar is read from the engine", engine.polls > 0)
        for (t in tabs.drop(1)) goesTo(t, "playing")
        idle(1_000)
        assertTrue("still playing", h.vm.transport.value.playing)
        tap("thumb_play")
        assertFalse("■ stops", h.vm.transport.value.playing)
        for (t in tabs.reversed().drop(1)) goesTo(t, "after ■")
    }

    @Test
    fun modeSimpleTabsFollowTheFingerWhileTheSongPlaysAndAfterStop() =
        playThenTabs(simple = true, tabs = listOf("tab_simple_Morceau", "tab_simple_Rythme", "tab_simple_Clavier"))

    @Test
    fun proTabsFollowTheFingerWhileTheSongPlaysAndAfterStop() =
        playThenTabs(simple = false, tabs = listOf("tab_Structure", "tab_Sons", "tab_Plus"))

    private companion object {
        /** NativeEngine's CMD_SET_TRANSPORT. */
        const val SET_TRANSPORT = 5
    }
}
