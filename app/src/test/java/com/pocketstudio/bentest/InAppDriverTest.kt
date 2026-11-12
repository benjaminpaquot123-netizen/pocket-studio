package com.pocketstudio.bentest

import android.os.Looper
import android.view.inspector.WindowInspector
import androidx.activity.ComponentActivity
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pocketstudio.app.journey.ComposeRuleDriver
import com.pocketstudio.app.journey.Journey
import com.pocketstudio.app.nav.AppHarness
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import kotlin.concurrent.thread

/**
 * BenStudio Test's in-app driver (docs/PLAN-TEST-APK.md, T0), proven without an emulator: it finds
 * the tags in every window through Compose's semantics tree and its touches are real ones (the
 * targets here only listen to the finger: no click action to call), then the app's own paths and
 * the essai éclair's screens through it, as on the phone.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class InAppDriverTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The test's thread is the app's main thread: each look made at once, each pause lets the frames and the looper run. */
    private val here = object : MainThread {
        override fun <T> run(block: () -> T): T = block()
        override fun pause(ms: Long) = ComposeRuleDriver(compose).idle(ms)
    }
    private val driver = InAppDriver(here)
    private var harness: AppHarness? = null

    @After
    fun back() {
        harness?.finish()
    }

    @Test
    fun realTouchesInEveryWindowAndListsScrolledToTheirTarget() {
        var plain = 0
        var inDialog = 0
        var far = 0
        // The dialog comes after the touch on « plain »: a modal window over it would take the finger.
        var dialog by mutableStateOf(false)
        compose.setContent {
            Column {
                Box(Modifier.size(100.dp).testTag("plain").pointerInput(Unit) { detectTapGestures { plain++ } })
                Column(Modifier.height(200.dp).verticalScroll(rememberScrollState())) {
                    repeat(30) { i -> Box(Modifier.fillMaxWidth().height(48.dp).testTag("item_$i").pointerInput(Unit) { detectTapGestures { if (i == 29) far++ } }) }
                }
            }
            if (dialog) Dialog(onDismissRequest = {}) { Box(Modifier.size(120.dp).testTag("in_dialog").pointerInput(Unit) { detectTapGestures { inDialog++ } }) }
        }
        compose.waitForIdle()
        driver.click("plain")
        assertEquals(30, driver.tags(Regex("item_\\d+")).size)
        val before = driver.bounds("item_29")
        Journey(driver).tap("item_29")
        assertEquals("the list scrolled to its last row (from $before to ${driver.bounds("item_29")}), then touched", 1, far)
        dialog = true
        compose.waitForIdle()
        assertTrue("two windows seen: ${WindowInspector.getGlobalWindowViews().size}", WindowInspector.getGlobalWindowViews().size >= 2)
        assertEquals(1, driver.count("in_dialog", merged = false))
        driver.click("in_dialog")
        assertEquals("a real touch on the activity's window", 1, plain)
        assertEquals("a real touch on the dialog's window", 1, inDialog)
    }

    /**
     * A touch on what another window covers fails at once, as a finger could not reach it (the phone's
     * assistant over the studio, 2026-11-12: five touches « under another window » taken as done);
     * what the top window holds is touched.
     */
    @Test
    fun aTouchUnderAnotherWindowFails() {
        var under = 0
        var onTop = 0
        compose.setContent {
            Box(Modifier.size(100.dp).testTag("under").pointerInput(Unit) { detectTapGestures { under++ } })
            Dialog(onDismissRequest = {}, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
                Box(Modifier.fillMaxSize().testTag("cover")) {
                    Box(Modifier.size(80.dp).testTag("on_top").pointerInput(Unit) { detectTapGestures { onTop++ } })
                }
            }
        }
        compose.waitForIdle()
        val failed = runCatching { driver.click("under") }
        assertEquals("no touch sent", 0, under)
        assertTrue("$failed", failed.exceptionOrNull()?.message?.contains("sous une autre fenêtre") == true)
        driver.click("on_top")
        assertEquals(1, onTop)
    }

    /** « Que veux-tu faire ? » › Nouveau morceau › the assistant › jump up, as JourneyPathsTest, through the in-app driver. */
    @Test
    fun theAppsPathsWalkThroughTheInAppDriver() {
        val h = AppHarness(compose).also { harness = it }
        h.start(launchChoice = true)
        val j = Journey(driver)
        val walked = runCatching {
            j.newSongInAssistant()
            j.chooseStyle("jump_up", "Bass music")
        }
        assertTrue("walked: $walked", walked.isSuccess && j.exists("wizard_create_now"))
    }

    /** The essai éclair's five screens, each recognised and pictured, then back to the song. */
    @Test
    fun theEssaiEclairsScreensThroughTheInAppDriver() {
        val h = AppHarness(compose).also { harness = it }
        h.start()
        val dir = File(h.app.cacheDir, "captures-test").apply { deleteRecursively() }
        val carnet = Carnet(File(dir, "carnet.tsv"))
        val appli = object : Appli {
            override fun ouvrirStudio() = Unit
            override fun export() = Appli.ExportVu(false, emptyList(), null)
            override fun capture(file: File) = Captures.windows(WindowInspector.getGlobalWindowViews(), file)
            override fun demander(question: String, maxMs: Long): Boolean? = null
        }
        val seen = runCatching { EssaiEclair(Journey(driver), NoSound, appli, carnet, dir).ecrans() }
        assertEquals(Result.success("vus : structure, sons, mix, live, projets"), seen)
        val shots = dir.listFiles { f -> f.extension == "jpg" }.orEmpty()
        assertEquals(5, shots.size)
        assertTrue(shots.all { it.length() > 0 })
    }

    /** The preparation screen: the question's ✅ answers, a run the app died in offers « Reprendre ». */
    @Test
    fun theBenchScreenAsksAndPicksUp() {
        var answered: Boolean? = null
        var started = 0
        var tout = 0
        var etat by mutableStateOf(Robot.Etat(question = EssaiEclair.QUESTION))
        compose.setContent {
            BenTestScreen(etat, listOf(Triple("android.permission.RECORD_AUDIO", "Autoriser le micro", false)), BenTestActions(answer = { answered = it }, start = { started++ }, startTout = { tout++ }, resume = { started++ }))
        }
        val j = Journey(driver)
        j.tap("bentest_oui")
        assertEquals(true, answered)
        etat = Robot.Etat(interrompu = "Style Jump up")
        j.await("bentest_interrompu")
        assertTrue(j.exists("bentest_allow_android.permission.RECORD_AUDIO"))
        j.tap("bentest_reprendre")
        assertEquals(1, started)
        // Nothing run yet: « Tout tester » first; once over, its verdict in lights.
        etat = Robot.Etat()
        j.await("bentest_tout")
        j.tap("bentest_tout")
        assertEquals(1, tout)
        etat = Robot.Etat(verdict = "34 verts, 2 oranges, 1 rouge", passage = Robot.TOUT)
        j.await("bentest_verdict")
        assertTrue(j.exists("bentest_envoyer"))
    }

    /** On the phone the robot has a thread of its own: each look is posted to the main thread and its answer (or its failure) comes back. */
    @Test
    fun theRobotsThreadReachesTheMainThread() {
        var onMain: Boolean? = null
        var failure: Throwable? = null
        val robot = thread {
            onMain = LooperMainThread(5_000).run { Looper.myLooper() == Looper.getMainLooper() }
            failure = runCatching { LooperMainThread(5_000).run { error("boum") } }.exceptionOrNull()
        }
        while (robot.isAlive) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
        }
        assertEquals(true, onMain)
        assertEquals("boum", failure?.message)
    }

    private object NoSound : Sonde {
        override fun lire() = Sonde.Lecture(0, 0f, 0f, false, false)
    }
}
