package com.pocketstudio.app.audit3

import android.app.Application
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
            // Retour to the window that holds the field, not to the top one (tour 5 « fuzz »: a popup over it took Retour,
            // the field's window stayed open and Compose never rested).
            val w = runCatching { com.pocketstudio.app.human.BenchWindows.textFieldDialog() }.getOrNull() ?: return
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

    private fun run(seed: Long, steps: Int, slowSaveMs: Long = 0) {
        val rnd = Random(seed)
        var controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val until = System.currentTimeMillis() + 15_000
        while (compose.onAllNodesWithTag("space_bar", useUnmergedTree = true).fetchSemanticsNodes().isEmpty()) {
            check(System.currentTimeMillis() < until) { "the app never opened" }
            idle(); Thread.sleep(20)
        }
        val trail = ArrayDeque<String>()
        var done = 0
        var reopened = 0
        // Tour 6, lot « stable » (CI 43498458467: « too few gestures made (133) », nothing else said): the gestures not made, by kind.
        val missed = sortedMapOf<String, Int>()
        // Things that leave the app or wait on the network are not touched (the rest of the app is).
        val avoid = Regex("(?i)share|partag|github|retour|report|download|télécharg|feedback|update|mise à jour")
        // Gestures that fell on a screen still opening its project (« Ouverture du projet… »): the finger's pace against the
        // disk's, not the app's screens; there must be none, however slow the disk (aSlowDiskDoesNotEatTheGestures).
        var onTheOpening = 0
        for (i in 0 until steps) {
            val opening = compose.onAllNodesWithTag("project_opening", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
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
            if (opening) onTheOpening++
            if (what.startsWith("touch") || what.startsWith("long") || what.startsWith("swipe") || what == "back") done++
            else missed.merge(what.substringBefore(" (").take(40), 1, Int::plus)
            try {
                idle()
                // Back on the home screen may close the app: open it again, as the person would.
                if (controller.get().isFinishing) {
                    controller.pause().stop().destroy()
                    // A slow disk (a loaded runner): the closed screen's last save takes [slowSaveMs] to write, and the next
                    // opening waits for it (« Ouverture du projet… », ProjectOpening.prepare).
                    if (slowSaveMs > 0) com.pocketstudio.app.rhythm.ClosingSaves.launch(slowSaveMs + 10_000) { Thread.sleep(slowSaveMs) }
                    reopened++
                    controller = Robolectric.buildActivity(MainActivity::class.java).setup()
                    idle()
                }
            } catch (e: Throwable) {
                if (e is AssertionError || !standIn(e)) {
                    throw AssertionError("seed $seed, after gesture $i « $what »: crash (${e.javaClass.name}); last gestures: ${trail.joinToString(" → ")}", e)
                }
            }
        }
        System.err.println("GestureFuzzTest seed $seed: $done gestures made of $steps ($textWindowsClosed text windows closed with Retour; $reopened times opened again; $onTheOpening on the opening screen; not made: $missed)")
        assertEquals("seed $seed: gestures drawn while the project was still opening (not made: $missed)", 0, onTheOpening)
        assertTrue("seed $seed: too few gestures made ($done; opened again $reopened times; not made: $missed; last gestures: ${trail.joinToString(" → ")})", done > steps / 2)
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

    private fun fuzz(defaultSeed: Long, slowSaveMs: Long = 0) {
        val seed = System.getenv("PS_GESTURE_SEED")?.toLongOrNull() ?: defaultSeed
        val steps = System.getenv("PS_GESTURE_STEPS")?.toIntOrNull() ?: 300
        run(seed, steps, slowSaveMs)
        if (standInLimits.isNotEmpty()) System.err.println("GestureFuzzTest: native readings without a stand-in: $standInLimits")
    }

    /**
     * Tour 5 « fuzz » (CI 43229551237, seed 11 after « acid_sounds_save »): a window holding a text field, with a small
     * window over it (a popup: the field's own handles, a menu). Retour went to the top window, the popup, never to the
     * window with the field: it stayed open and Compose never rested (AppNotIdleException after 60 s). Retour must go
     * to the window that holds the field, and the wait must come back.
     */
    @Test
    fun aTextWindowUnderAPopupIsClosedWithRetour() {
        val controller = Robolectric.buildActivity(androidx.activity.ComponentActivity::class.java).setup()
        controller.get().setContent {
            var name by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("Mon son 1") }
            var open by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
            if (open) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { open = false },
                    confirmButton = { androidx.compose.material3.Text("OK") },
                    text = { androidx.compose.material3.OutlinedTextField(name, { name = it }) },
                )
            }
            androidx.compose.ui.window.Popup { androidx.compose.material3.Text("bulle") }
        }
        val waited = runCatching { idle() }.exceptionOrNull()
        assertTrue("the wait never came back with a text window under a popup: $waited", waited == null)
        assertFalse("the window with the text field is still open", com.pocketstudio.app.human.BenchWindows.textFieldInADialog())
        controller.pause().stop().destroy()
    }

    @Test
    fun aRandomFingerNeverCrashesTheApp() = fuzz(11L)

    @Test
    fun anotherRandomFingerNeverCrashesTheApp() = fuzz(20261103L)

    /**
     * Tour 6, lot « stable » (CI 43498458467: seed 11, « too few gestures made (133) » in 2.1 s, 299 here in 5.6 s): the
     * finger went on at the loop's own pace while the app, opened again, waited for its disk (« Ouverture du projet… »,
     * nothing to touch): on a loaded runner the wait is longer and more gestures fell on the empty screen. Here the disk is
     * slow on purpose (each closing save 8 s): no gesture is drawn while the project still opens, however slow the disk.
     */
    @Test
    fun aSlowDiskDoesNotEatTheGestures() = fuzz(11L, slowSaveMs = 8_000)
}
