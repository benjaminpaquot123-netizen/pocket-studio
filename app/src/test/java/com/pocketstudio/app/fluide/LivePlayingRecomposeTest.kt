package com.pocketstudio.app.fluide

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.tooling.CompositionObserver
import androidx.compose.runtime.tooling.CompositionObserverHandle
import androidx.compose.runtime.tooling.CompositionRegistrationObserver
import androidx.compose.runtime.tooling.ObservableComposition
import androidx.compose.runtime.tooling.observe
import androidx.compose.ui.platform.findViewTreeCompositionContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pocketstudio.app.nav.AppHarness
import com.pocketstudio.app.rhythm.LiveUi
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.rhythm.TransportUi
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
 * Tour 9, C6 (robot v7: slow frames on Mix and Live « 100 % » while playing). The whole app on the Live page, the
 * engine's readings moving as they do while playing (the transport step 30 times a second, the pads' states, the
 * lanes' lights, the meters): every composable scope entered again is counted. A reading redraws what shows it; it
 * does not compose the page again.
 */
@OptIn(ExperimentalComposeRuntimeApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class LivePlayingRecomposeTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @After
    fun finish() = h.finish()

    private class Counter : CompositionRegistrationObserver, CompositionObserver {
        var total = 0
        val byScope = HashMap<String, Int>()
        private val handles = ArrayList<CompositionObserverHandle>()
        override fun onCompositionRegistered(composition: ObservableComposition) {
            composition.setObserver(this)?.let { handles += it }
        }
        override fun onCompositionUnregistered(composition: ObservableComposition) {}
        override fun onBeginComposition(composition: ObservableComposition) {}
        override fun onScopeEnter(scope: RecomposeScope) {
            total++
            val name = Throwable().stackTrace.firstOrNull { f ->
                f.className.startsWith("com.pocketstudio") && !f.className.contains("RecomposeTest")
            }?.let { "${it.className.substringAfterLast('.')}.${it.methodName}:${it.lineNumber}" } ?: "?"
            byScope[name] = (byScope[name] ?: 0) + 1
        }
        override fun onReadInScope(scope: RecomposeScope, value: Any) {}
        override fun onScopeExit(scope: RecomposeScope) {}
        override fun onEndComposition(composition: ObservableComposition) {}
        override fun onScopeInvalidated(scope: RecomposeScope, value: Any?) {}
        override fun onScopeDisposed(scope: RecomposeScope) {}
        fun reset() { total = 0; byScope.clear() }
        fun top(n: Int = 25) = byScope.entries.sortedByDescending { it.value }.take(n).joinToString("\n") { "  ${it.value} ${it.key}" }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> flow(name: String): MutableStateFlow<T> =
        RhythmViewModel::class.java.getDeclaredField(name).apply { isAccessible = true }.get(h.vm) as MutableStateFlow<T>

    private fun recomposer(): Recomposer {
        val root = compose.activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        (root.findViewTreeCompositionContext() as? Recomposer)?.let { return it }
        fun field(o: Any, name: String): Any? {
            var c: Class<*>? = o.javaClass
            while (c != null) {
                runCatching { return c.getDeclaredField(name).apply { isAccessible = true }.get(o) }
                c = c.superclass
            }
            return null
        }
        val wrapped = field(root, "composition") ?: error("no composition on $root")
        val original = field(wrapped, "original") ?: wrapped
        return field(original, "parent") as? Recomposer ?: error("no recomposer")
    }

    /** [readings] readings 33 ms apart (the step moves every 3, a 16th at 140 BPM), then the scopes composed again. */
    private fun playing(readings: Int = 60): Counter {
        flow<TransportUi>("_transport").value = h.vm.transport.value.copy(playing = true)
        compose.waitForIdle()
        val counter = Counter()
        val handle = recomposer().observe(counter)
        val clock = flow<Int>("_liveClock")
        val live = flow<LiveUi>("_live")
        val states = flow<List<Int>>("_liveStates")
        val meters = flow<FloatArray>("_meters")
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        counter.reset()
        for (i in 0 until readings) {
            val step = i / 3
            clock.value = step
            live.value = LiveUi(step % 16, if (step % 2 == 0) setOf(0) else emptySet(), step, step)
            // The pads' states stay (nothing launched meanwhile): a new list equal to the last one.
            states.value = states.value.toList()
            meters.value = FloatArray(NativeEngine.METERS) { ch -> 0.2f + 0.6f * ((i * 7 + ch * 3) % 10) / 10f }
            compose.mainClock.advanceTimeBy(33)
        }
        compose.waitForIdle()
        handle.dispose()
        return counter
    }

    private fun tap(tag: String) {
        compose.onNodeWithTag(tag).performClick()
        compose.waitForIdle()
    }

    @Test
    fun liveComposesLittleWhilePlaying() {
        h.start()
        tap("tab_Live")
        compose.onNodeWithTag("live_screen").assertExists()
        val c = playing()
        val report = "Live : ${c.total} scopes composed again in 60 readings\n${c.top()}"
        println(report)
        assertTrue(report, c.total <= LIVE_MAX)
        // Its ring fills at each step: drawn in its own layer, not the whole top bar again.
        assertTrue("the counter draws in the top bar's layer", hasOwnLayer(compose.onNodeWithTag("live_counter", useUnmergedTree = true).fetchSemanticsNode()))
    }

    // C6: a playing pad redraws each frame (its light, its ring). Without its own layer, that redraw re-records
    // the whole grid's layer, every cell with it, 60 times a second.
    @Test
    fun liveCellsDrawInOwnLayers() {
        h.start()
        tap("tab_Live")
        compose.onNodeWithTag("live_screen").assertExists()
        playing()
        val nodes = compose.onAllNodes(
            SemanticsMatcher("tag live_pad_…") { it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("live_pad_") == true },
            useUnmergedTree = true,
        ).fetchSemanticsNodes()
        assertTrue("no live pad shown", nodes.isNotEmpty())
        val shared = nodes.filterNot(::hasOwnLayer).map { it.config.getOrNull(SemanticsProperties.TestTag) }
        assertTrue("pads draw in the grid's layer: $shared", shared.isEmpty())
    }

    private fun hasOwnLayer(node: SemanticsNode): Boolean {
        val layout = node.layoutInfo
        val inner = layout.javaClass.methods.first { it.name.startsWith("getInnerLayerCoordinator") }
        return inner.invoke(layout) != null
    }

    companion object {
        // Measured 70 before C6 (2026-11-23): the bar counter composed again at each step (20), its two FitTexts with
        // it (50). Its text changes once a beat: composed again then only (5 beats in these readings).
        const val LIVE_MAX = 25
    }
}
