package com.pocketstudio.app.padlive

import android.app.Application
import android.os.Looper
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.MainActivity
import com.pocketstudio.app.human.BenchEngine
import com.pocketstudio.app.onboarding.LaunchRule
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.engine.OfflineExport
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.time.Duration

/**
 * Audit ultra round 2, zone 1, n° 2 (« revient », seat A c, tablet11Land): Pad Live opened on 16 empty pads at a
 * real first launch, and again from the home screen after a song was made. The app as installed, with an engine
 * that stays as a phone's does (no human-tester bench, which lets its engine go between gestures): each way into
 * Pad Live (the first launch's card, « Que veux-tu faire ? »'s card, the « Live » project reopened after another
 * project) opens on the ready-to-play pads, the « Live » project's own.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1280dp-h800dp-land-hdpi")
class PadLiveReadyEveryWayTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app: Application get() = ApplicationProvider.getApplicationContext()
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun freshInstall() {
        for (dir in listOf("projects", "projects-trash", "stems")) File(app.filesDir, dir).deleteRecursively()
        File(app.filesDir, "../shared_prefs").listFiles()?.forEach { app.getSharedPreferences(it.name.removeSuffix(".xml"), 0).edit().clear().commit() }
        app.getSharedPreferences("settings", 0).edit().putString("language", "fr").commit()
        SettingsStore.forgetForTests()
        LaunchRule.askAtStartForTest = true
        shadowOf(app).grantPermissions(android.Manifest.permission.RECORD_AUDIO)
        forgetTheViewModelFactory()
        NativeEngine.sink = BenchEngine()
    }

    private fun forgetTheViewModelFactory() {
        ViewModelProvider.AndroidViewModelFactory::class.java.getDeclaredField("_instance").apply { isAccessible = true }.set(null, null)
    }

    @After
    fun end() {
        runCatching { scenario?.close() }
        com.pocketstudio.app.rhythm.ClosingSaves.awaitIdleBlocking(15_000)
        NativeEngine.sink = null
        for (dir in listOf("projects", "projects-trash", "stems")) File(app.filesDir, dir).deleteRecursively()
        LaunchRule.askAtStartForTest = true
        SettingsStore.forgetForTests()
        forgetTheViewModelFactory()
    }

    private fun vm(): RhythmViewModel {
        lateinit var v: RhythmViewModel
        scenario!!.onActivity { a ->
            v = ViewModelProvider(a)[RhythmViewModel::class.java]
            // The built-in sounds as the phone's engine renders them (Robolectric has no engine to render them).
            val lib = ViewModelProvider(a)[com.pocketstudio.app.library.SoundLibraryViewModel::class.java]
            if (lib.kitCache.isEmpty()) for (voice in 0 until 64) {
                lib.kitCache[voice] = com.pocketstudio.app.audio.Pcm(FloatArray(2 * 9600) { k -> 0.5f * kotlin.math.sin(k * 0.01f) }, 2, OfflineExport.SAMPLE_RATE)
            }
        }
        return v
    }

    private fun await(what: String, seconds: Int = 60, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + seconds * 1000L
        while (!runCatching(ok).getOrDefault(false)) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
            runCatching { compose.waitForIdle() }
            if (System.currentTimeMillis() > end) {
                val p = runCatching { vm().project.value }.getOrNull()
                throw AssertionError("$what: not in time (shown ${shown("padlive")}, project « ${p?.name} » home ${p?.padLive?.home}, " +
                    "pads ${p?.live?.used}, offered ${p?.live?.starterOffered})\n" + tags())
            }
            Thread.sleep(5)
        }
        runCatching { compose.waitForIdle() }
    }

    private fun tags(): String = runCatching {
        compose.onAllNodes(androidx.compose.ui.test.SemanticsMatcher("tagged") { it.config.contains(androidx.compose.ui.semantics.SemanticsProperties.TestTag) })
            .fetchSemanticsNodes().map { it.config[androidx.compose.ui.semantics.SemanticsProperties.TestTag] }.take(60).joinToString(" ")
    }.getOrDefault("?")

    private fun shown(tag: String) = runCatching { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)

    private fun tap(tag: String) {
        await("$tag on screen") { shown(tag) }
        val node = compose.onNodeWithTag(tag)
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
    }

    /** Pad Live on screen, its « Live » project open, its pads filled (the ready-to-play set). */
    private fun padsReady(how: String) =
        await("$how: Pad Live's ready pads") { shown("padlive") && vm().project.value.padLive.home && vm().project.value.live.used > 0 }

    @Test
    fun theFirstLaunchsCardOpensReadyPads() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        await("the first launch's welcome") { shown("welcome") }
        vm()
        tap("launch_padlive")
        padsReady("first launch, Pad Live's card")
    }

    @Test
    fun theHomesCardOpensReadyPadsAndAgainAfterASong() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        await("the first launch's welcome") { shown("welcome") }
        // A project first (the welcome's « Plus tard »: its starting beat), then home and Pad Live, as seat A c did.
        tap("welcome_later")
        await("the project") { !shown("welcome") && shown("tab_simple_Rythme") }
        val song = vm().projectId
        vm()
        // « Retour » from the project: « Que veux-tu faire ? ».
        repeat(4) {
            if (!shown("launch_padlive")) {
                scenario!!.onActivity { it.onBackPressedDispatcher.onBackPressed() }
                await("a step back") { true }
            }
        }
        tap("launch_padlive")
        padsReady("« Que veux-tu faire ? », Pad Live's card")
        val live = vm().projectId
        tap("padlive_back")
        await("home again") { shown("launch_padlive") }
        tap("launch_padlive")
        padsReady("Pad Live again")
        assertEquals("the same « Live » project", live, vm().projectId)
        assert(song != live)
    }
}
