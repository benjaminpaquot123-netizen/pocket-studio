package com.pocketstudio.app.nav

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
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pocketstudio.app.rhythm.LiveUi
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.rhythm.TransportUi
import com.pocketstudio.engine.NativeEngine
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 5 « jank » (BenStudio Test on the HONOR, R8 build): with the song playing, slow frames were 31 % on Sons,
 * 48 % on Mix, 25 % under « Mes projets ». Here the whole app (AppRoot) is shown on each of those screens while the
 * engine's readings move as they do while playing (the step 30 times a second, the meters 20 times a second) and
 * every composable scope entered again is counted. A reading must redraw what shows it, not compose the screen.
 */
@OptIn(ExperimentalComposeRuntimeApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class PlayingRecomposeTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @After
    fun finish() = h.finish()

    /** Every scope composed again (any composition of the window, subcompositions included), by its block's class. */
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
            // The composable entered: the first frame of the app's own code under the runtime's.
            val name = Throwable().stackTrace.firstOrNull { f ->
                f.className.startsWith("com.pocketstudio") && !f.className.contains("PlayingRecomposeTest")
            }?.let { "${it.className.substringAfterLast('.')}.${it.methodName}:${it.lineNumber}" }
                ?: runCatching {
                    // Recomposed on its own (the frame's first): its block's class, and what it holds.
                    val block = scope.javaClass.getDeclaredField("block").apply { isAccessible = true }.get(scope)
                    val held = block?.javaClass?.declaredFields?.take(3)?.joinToString(",") { f ->
                        f.isAccessible = true
                        f.get(block)?.javaClass?.name?.substringAfterLast('.') ?: "null"
                    }
                    "? ${block?.javaClass?.name?.substringAfterLast('.')} [$held]"
                }.getOrNull() ?: "?"
            byScope[name] = (byScope[name] ?: 0) + 1
        }
        override fun onReadInScope(scope: RecomposeScope, value: Any) {}
        override fun onScopeExit(scope: RecomposeScope) {}
        override fun onEndComposition(composition: ObservableComposition) {}
        override fun onScopeInvalidated(scope: RecomposeScope, value: Any?) {}
        override fun onScopeDisposed(scope: RecomposeScope) {}
        fun reset() { total = 0; byScope.clear() }
        fun top(n: Int = 12) = byScope.entries.sortedByDescending { it.value }.take(n).joinToString("\n") { "  ${it.value} ${it.key}" }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> flow(name: String): MutableStateFlow<T> =
        RhythmViewModel::class.java.getDeclaredField(name).apply { isAccessible = true }.get(h.vm) as MutableStateFlow<T>

    /** The window's recomposer: the app's composition's parent (the root view's, else through its composition). */
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
        return field(original, "parent") as? Recomposer ?: error("no recomposer: ${field(original, "parent")}")
    }

    /**
     * The song playing for [readings] readings of the engine (33 ms apart: the step moves every 3 readings, a 16th
     * at 140 BPM; the meters move at each), then the scopes composed again meanwhile.
     */
    private fun playing(readings: Int = 60): Counter {
        flow<TransportUi>("_transport").value = h.vm.transport.value.copy(playing = true)
        compose.waitForIdle()
        val counter = Counter()
        val handle = recomposer().observe(counter)
        val live = flow<LiveUi>("_live")
        val meters = flow<FloatArray>("_meters")
        val loudness = flow<FloatArray>("_loudness")
        // Settled first: the scopes the start of playing composes are not the readings'.
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        counter.reset()
        for (i in 0 until readings) {
            val step = i / 3
            live.value = LiveUi(step % 16, if (step % 2 == 0) setOf(0) else emptySet(), step, step)
            meters.value = FloatArray(NativeEngine.METERS) { ch -> 0.2f + 0.6f * ((i * 7 + ch * 3) % 10) / 10f }
            // The readings move at each one; the gain the target adds and what the limiter takes off stay (they
            // follow the loudest part, not the beat).
            loudness.value = FloatArray(NativeEngine.LOUDNESS) { k ->
                when (k) {
                    NativeEngine.LOUDNESS_DRIVE -> 2.5f
                    NativeEngine.LOUDNESS_LOSS -> 0.4f
                    else -> -14f - (i % 5) - k
                }
            }
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

    private fun check(screen: String, counter: Counter, max: Int) {
        val report = "$screen : ${counter.total} scopes composed again in 60 readings\n${counter.top(25)}"
        println(report)
        assertTrue(report, counter.total <= max)
    }

    @Test
    fun sonsComposesLittleWhilePlaying() {
        h.start()
        tap("tab_Sons")
        compose.onNodeWithTag("rhythm").assertExists()
        check("Sons", playing(), max = SONS_MAX)
    }

    @Test
    fun mixComposesLittleWhilePlaying() {
        h.start()
        tap("tab_Mix")
        compose.onNodeWithTag("mixer").assertExists()
        check("Mix", playing(), max = MIX_MAX)
    }

    @Test
    fun projectsComposeLittleWhilePlaying() {
        h.start()
        // As BenStudio Test does: from the studio's top bar (the Live page has its own bar).
        tap("tab_Structure")
        tap("top_projects")
        h.awaitTag("projects")
        check("Mes projets", playing(), max = PROJECTS_MAX)
        // Nothing composed again is not enough: the studio's screen under the projects drew its playhead at each step
        // (read when drawn), the window of the studio drawing 30 times a second unseen. It is not kept there.
        val under = compose.onAllNodesWithTag("timeline_screen", useUnmergedTree = true).fetchSemanticsNodes().size
        assertEquals("the Structure's timeline still under « Mes projets »", 0, under)
        // Closed: the studio as it was, on the same tab.
        tap("projects_back")
        // Back through « Que veux-tu faire ? » when the app shows it, as BenStudio Test does.
        if (compose.onAllNodesWithTag("launch_continue").fetchSemanticsNodes().isNotEmpty()) tap("launch_continue")
        compose.onNodeWithTag("timeline_screen").assertExists()
    }

    companion object {
        /** Before (2026-11-12): 2296 on Sons, 1934 on Mix (the whole drum screen at each step, the loudness panel at each reading). */
        const val SONS_MAX = 400
        const val MIX_MAX = 450
        const val PROJECTS_MAX = 0
    }
}
