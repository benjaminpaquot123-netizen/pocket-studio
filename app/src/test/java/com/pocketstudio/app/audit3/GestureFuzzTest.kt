package com.pocketstudio.app.audit3

import android.app.Application
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.click
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.MainActivity
import com.pocketstudio.app.human.BenchEngine
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectJson
import com.pocketstudio.model.ProjectStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.random.Random

/**
 * Audit stage 3, « milliers de gestes au hasard rejouables »: the whole app under a seeded random
 * finger (touches, long touches, swipes on what scrolls, Back), from the home screen; the trail of
 * the last gestures is in the failure, and PS_GESTURE_SEED / PS_GESTURE_STEPS replay one run.
 * Invariants: no crash, the open project still saves and reads back the same.
 */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class GestureFuzzTest {
    @get:Rule
    val compose = createEmptyComposeRule()
    private val app: Application get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        val c = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory::class.java
        c.declaredFields.filter { java.lang.reflect.Modifier.isStatic(it.modifiers) && c.isAssignableFrom(it.type) }.forEach { it.isAccessible = true; it.set(null, null) }
        NativeEngine.sink = BenchEngine()
        File(app.filesDir, "projects").deleteRecursively()
        listOf("nav", "missions", "settings", "next_steps", "song_ready", "share", "tools", "launch", "project_modes").forEach {
            app.getSharedPreferences(it, 0).edit().clear().commit()
        }
        val id = "gestes-" + System.nanoTime()
        ProjectStore(File(app.filesDir, "projects")).save(id, Project(name = "Gestes", drums = DrumPattern().toggle(0, 0).toggle(2, 4)).sanitized())
        CurrentProject.set(app, id)
        SettingsStore.forgetForTests()
        SettingsStore.update(app) { it.copy(guideSeen = true) }
        com.pocketstudio.app.onboarding.LaunchRule.askAtStartForTest = false
    }

    @After
    fun end() {
        NativeEngine.sink = null
        com.pocketstudio.app.onboarding.LaunchRule.askAtStartForTest = true
        SettingsStore.forgetForTests()
        CurrentProject.set(app, RhythmViewModel.PROJECT_ID)
    }

    private fun idle() {
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        closeTextWindows()
        compose.waitForIdle()
    }

    /**
     * Round 3 (Zone3DialogDiagTest): a window holding a text field (« Renommer ») never lets Compose rest under
     * Robolectric, without one line of the app. The finger met one: it is closed with Retour, as a person who
     * does not want to type, and the run goes on (counted at the end).
     */
    private fun closeTextWindows() {
        // A window just opened is composed on the next frames: looked at once they are drawn.
        repeat(3) {
            runCatching { compose.mainClock.advanceTimeByFrame() }
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
        }
        repeat(3) {
            val w = com.pocketstudio.app.human.BenchWindows.windows()
                .firstOrNull { it.params.type != android.view.WindowManager.LayoutParams.TYPE_BASE_APPLICATION } ?: return
            if (!com.pocketstudio.app.human.BenchWindows.textFieldInADialog()) return
            for (action in listOf(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.ACTION_UP)) {
                w.view.dispatchKeyEvent(android.view.KeyEvent(action, android.view.KeyEvent.KEYCODE_BACK))
            }
            textWindowsClosed++
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
        }
    }

    private var textWindowsClosed = 0

    /** Native readings the stand-in engine does not answer, met on the way (printed at the end). */
    private val standInLimits = HashSet<String>()

    /**
     * Robolectric has no native engine: a reading the bench's stand-in does not answer (the CPU
     * load…) is the test's limit, noted, not the app's crash.
     */
    private fun standIn(e: Throwable): Boolean {
        val link = linkErrors(e)
        standInLimits += link
        return link.isNotEmpty()
    }

    /** The native calls [e] (its causes, what it suppressed) failed to link, empty when it is not that. */
    private fun linkErrors(e: Throwable, seen: MutableSet<Throwable> = HashSet()): Set<String> {
        if (!seen.add(e)) return emptySet()
        val own = if (e is UnsatisfiedLinkError) setOf(e.message.orEmpty()) else emptySet()
        return own + (listOfNotNull(e.cause) + e.suppressed).flatMap { linkErrors(it, seen) }
    }

    private fun label(n: SemanticsNode): String =
        n.config.getOrNull(SemanticsProperties.TestTag) ?: n.config.getOrNull(SemanticsProperties.Text)?.joinToString()?.take(30)
            ?: n.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString()?.take(30) ?: "#${n.id}"

    private fun run(seed: Long, steps: Int) {
        val rnd = Random(seed)
        var controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val until = System.currentTimeMillis() + 15_000
        while (compose.onAllNodesWithTag("space_bar", useUnmergedTree = true).fetchSemanticsNodes().isEmpty()) {
            check(System.currentTimeMillis() < until) { "the app never opened" }
            idle(); Thread.sleep(20)
        }
        val trail = ArrayDeque<String>()
        var done = 0
        // Things that leave the app or wait on the network are not touched (the rest of the app is).
        val avoid = Regex("(?i)share|partag|github|retour|report|download|télécharg|feedback|update|mise à jour")
        for (i in 0 until steps) {
            val action = rnd.nextInt(100)
            val what = try {
                when {
                    action < 4 -> {
                        controller.get().onBackPressedDispatcher.onBackPressed()
                        "back"
                    }
                    action < 14 -> {
                        val nodes = compose.onAllNodes(hasScrollAction()).fetchSemanticsNodes()
                        if (nodes.isEmpty()) "no scroll" else {
                            val k = rnd.nextInt(nodes.size)
                            val dir = rnd.nextInt(4)
                            compose.onAllNodes(hasScrollAction())[k].performTouchInput {
                                when (dir) { 0 -> swipeUp(); 1 -> swipeDown(); 2 -> swipeLeft(); else -> swipeRight() }
                            }
                            "swipe $dir on ${label(nodes[k])}"
                        }
                    }
                    else -> {
                        val clickable = hasClickAction() and !SemanticsMatcher("not leaving") { n -> avoid.containsMatchIn(label(n)) }
                        val nodes = compose.onAllNodes(clickable).fetchSemanticsNodes()
                        if (nodes.isEmpty()) "nothing to touch" else {
                            val k = rnd.nextInt(nodes.size)
                            val long = action >= 92 && nodes[k].config.getOrNull(SemanticsActions.OnLongClick) != null
                            val node = compose.onAllNodes(clickable)[k]
                            if (long) node.performTouchInput { longClick() } else node.performTouchInput { click(center) }
                            (if (long) "long " else "") + "touch ${label(nodes[k])}"
                        }
                    }
                }
            } catch (e: AssertionError) {
                // The test's own lookups (a node gone between finding and touching it): not the app's.
                "skipped (${e.message?.take(60)})"
            } catch (e: Throwable) {
                if (!standIn(e)) throw AssertionError("seed $seed, gesture $i crashed the app (${e.javaClass.name}); last gestures: ${trail.joinToString(" → ")}", e)
                "stand-in limit"
            }
            trail.addLast(what)
            if (trail.size > 15) trail.removeFirst()
            if (what.startsWith("touch") || what.startsWith("long") || what.startsWith("swipe") || what == "back") done++
            try {
                idle()
                // Back on the home screen may close the app: open it again, as the person would.
                if (controller.get().isFinishing) {
                    controller.pause().stop().destroy()
                    controller = Robolectric.buildActivity(MainActivity::class.java).setup()
                    idle()
                }
            } catch (e: Throwable) {
                if (e is AssertionError || !standIn(e)) {
                    throw AssertionError("seed $seed, after gesture $i « $what »: crash (${e.javaClass.name}); last gestures: ${trail.joinToString(" → ")}", e)
                }
            }
        }
        System.err.println("GestureFuzzTest seed $seed: $done gestures made of $steps ($textWindowsClosed text windows closed with Retour)")
        assertTrue("seed $seed: too few gestures made ($done)", done > steps / 2)
        val vm = ViewModelProvider(controller.get())[RhythmViewModel::class.java]
        runCatching { idle() }.exceptionOrNull()?.let { if (!standIn(it)) throw it }
        val p = vm.project.value
        val text = ProjectJson.encode(p)
        assertFalse("seed $seed: NaN written", text.contains("NaN"))
        assertEquals("seed $seed: reads back the same", p, ProjectJson.decode(text))
        runCatching {
            if (vm.transport.value.playing) vm.togglePlay()
            controller.pause().stop().destroy()
        }.exceptionOrNull()?.let { if (!standIn(it)) throw it }
    }

    private fun fuzz(defaultSeed: Long) {
        val seed = System.getenv("PS_GESTURE_SEED")?.toLongOrNull() ?: defaultSeed
        val steps = System.getenv("PS_GESTURE_STEPS")?.toIntOrNull() ?: 300
        run(seed, steps)
        if (standInLimits.isNotEmpty()) System.err.println("GestureFuzzTest: native readings without a stand-in: $standInLimits")
    }

    @Test
    fun aRandomFingerNeverCrashesTheApp() = fuzz(11L)

    @Test
    fun anotherRandomFingerNeverCrashesTheApp() = fuzz(20261103L)
}
