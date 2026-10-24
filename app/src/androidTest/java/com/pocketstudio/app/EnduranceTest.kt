package com.pocketstudio.app

import android.accessibilityservice.AccessibilityService
import android.content.pm.ActivityInfo
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.pocketstudio.app.RealUse.log
import com.pocketstudio.app.RealUse.waitFor
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.journey.Journey
import com.pocketstudio.app.library.SoundLibraryViewModel
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.projects.ProjectsViewModel
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.Demos
import com.pocketstudio.model.Project
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * « Endurance » (docs/TESTS-REELS.md): the app played for a long time as a user plays it, round
 * and round: open another project from « Mes projets », play its song, loop the part, the Live
 * page and its pads, the app sent to the background (the music goes on) and back, the phone
 * turned sideways and back. Each round looks at what wears out: a crash (the test process dies
 * with the app), the main thread stuck (an ANR is 5 s), the audio callbacks stopping, a storm of
 * underruns, memory (Java and native) growing round after round, a NaN.
 *
 * How long: the instrumentation argument `enduranceMinutes` (3 by default, the PR's run; the
 * nightly gives 10, a manual run 30).
 */
@RunWith(AndroidJUnit4::class)
class EnduranceTest {
    @get:Rule(order = 0)
    val mic: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.RECORD_AUDIO)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    private val j = Journey(compose) { log(it) }
    private val app get() = RealUse.app

    @After
    fun cleanUp() {
        com.pocketstudio.app.onboarding.LaunchRule.askAtStartForTest = true
        RealUse.freshInstall()
    }

    private class Round(val index: Int, val javaMb: Double, val nativeMb: Double, val callbacks: Long, val xruns: Int, val mainMs: Long, val peak: Float)

    @Test
    fun playsForMinutesAsAUserDoes() {
        assertTrue("native engine must load", NativeEngine.isAvailable)
        val minutes = InstrumentationRegistry.getArguments().getString("enduranceMinutes")?.toDoubleOrNull() ?: 3.0
        log("endurance: $minutes min")
        RealUse.freshInstall()
        SettingsStore.update(app) { it.copy(guideSeen = true, simpleMode = false) }
        com.pocketstudio.app.onboarding.LaunchRule.askAtStartForTest = false

        // Two projects to go between: the assistant's jump up and a demo (each opened from « Mes projets »).
        val ids = ArrayList<String>()
        var made: Pair<Project, Map<String, Pcm>>? = null
        lateinit var library: SoundLibraryViewModel
        RealUse.instrumentation.runOnMainSync {
            library = SoundLibraryViewModel(app)
            library.quickCreate("jump_up") { project, sounds, _ -> made = project to sounds }
        }
        waitFor("the assistant's song", 240) { made != null || library.quick.value.error != null }
        val (song, sounds) = checkNotNull(made) { "jump_up: ${library.quick.value.error}" }
        RealUse.instrumentation.runOnMainSync {
            library.quickDone()
            val projects = ProjectsViewModel(app)
            projects.create(song, sounds) { ids += it }
            projects.create(Demos.ALL.first().third()) { ids += it }
        }
        waitFor("the projects", 60) { ids.size == 2 }
        CurrentProject.set(app, ids[0])

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitFor("the audio stream", 20) { NativeEngine.streamInfo().let { it.running && it.callbacks > 0 } }
            RealUse.describeStream()
            val rounds = ArrayList<Round>()
            val end = System.currentTimeMillis() + (minutes * 60_000).toLong()
            var k = 0
            while (System.currentTimeMillis() < end || rounds.size < 2) {
                val before = NativeEngine.streamInfo()
                var peak = 0f
                // Mes projets › the next project (the screen is rebuilt on it).
                if (k > 0) {
                    val id = ids[k % ids.size]
                    val old = activityOf(scenario)
                    j.tap("top_projects")
                    j.await("projects")
                    j.tap("project_$id")
                    waitFor("project $id opened", 30) { CurrentProject.get(app) == id && activityOf(scenario) !== old }
                    j.await("space_bar", 20_000)
                }
                Thread.sleep(2000)  // its sounds loading
                // ▶: the song (or the loop of a project without one).
                j.tap("top_play")
                waitFor("the transport", 10) { NativeEngine.currentStep() >= 0 }
                peak = maxOf(peak, listen(8))
                // Loop the part: one touch, then back to the song.
                if (j.exists("top_loop_part")) {
                    j.tap("top_loop_part")
                    peak = maxOf(peak, listen(4))
                    j.tap("top_loop_part")
                }
                // The Live page: a few pads, then back.
                j.tap("tab_Live")
                j.await("live_screen")
                j.tagsMatching(Regex("live_pad_.+")).take(3).forEach { pad ->
                    runCatching { compose.onNodeWithTag(pad).performClick() }
                    peak = maxOf(peak, listen(1))
                }
                j.tap("tab_Structure")
                // The app in the background, the music going on; then back.
                val home = RealUse.instrumentation.uiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
                if (!home) scenario.moveToState(Lifecycle.State.CREATED)
                waitFor("the app in the background", 10) { scenario.state != Lifecycle.State.RESUMED }
                val inBackground = NativeEngine.streamInfo().callbacks
                Thread.sleep(4000)
                assertTrue("the audio callbacks run on in the background", NativeEngine.streamInfo().callbacks > inBackground)
                scenario.moveToState(Lifecycle.State.RESUMED)
                j.await("space_bar", 20_000)
                // Turned sideways and back (the screen rebuilt twice).
                scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
                j.idle(2500)
                peak = maxOf(peak, listen(2))
                scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
                j.idle(2500)
                j.await("space_bar", 20_000)
                peak = maxOf(peak, listen(2))
                // ■ (whatever plays now).
                val playingNow = rhythmOf(scenario)
                if (playingNow.transport.value.playing) RealUse.instrumentation.runOnMainSync { if (playingNow.transport.value.playing) playingNow.stop() }
                waitFor("the stop", 10) { NativeEngine.currentStep() == -1 }

                val after = NativeEngine.streamInfo()
                val mainMs = RealUse.mainThreadDelayMs()
                val (java, native) = RealUse.memoryMb()
                val round = Round(k, java, native, after.callbacks - before.callbacks, after.xruns - before.xruns, mainMs, peak)
                rounds += round
                log("round $k: project ${CurrentProject.get(app)}, java %.1f MB, native %.1f MB, callbacks +%d, xruns +%d, main thread %d ms, peak %.3f, running=%s".format(
                    java, native, round.callbacks, round.xruns, mainMs, peak, after.running))
                assertTrue("round $k: the audio stream runs", after.running)
                assertTrue("round $k: the audio callbacks went on (+${round.callbacks})", round.callbacks > 0)
                assertTrue("round $k: the main thread answered in $mainMs ms (an ANR is 5 s)", mainMs < 5000)
                assertTrue("round $k: no NaN in the output", !after.nonFinite)
                // A storm of underruns: more than one callback in 20 late in a round (the CI emulator has some).
                if (after.xruns >= 0 && before.xruns >= 0) {
                    assertTrue("round $k: ${round.xruns} underruns for ${round.callbacks} callbacks", round.xruns <= maxOf(50L, round.callbacks / 20))
                }
                assertTrue("round $k: music heard (peak $peak)", peak > 0.01f)
                k++
            }
            // Memory: after the first round (sounds and caches filled once), no growth round after round.
            val settled = rounds.drop(1)
            val java0 = settled.first().javaMb
            val native0 = settled.first().nativeMb
            val javaMax = settled.maxOf { it.javaMb }
            val nativeMax = settled.maxOf { it.nativeMb }
            log("memory: java %.1f..%.1f MB, native %.1f..%.1f MB over ${rounds.size} rounds".format(java0, javaMax, native0, nativeMax))
            assertTrue("Java heap grew from %.1f to %.1f MB".format(java0, javaMax), javaMax <= java0 + 64)
            assertTrue("native heap grew from %.1f to %.1f MB".format(native0, nativeMax), nativeMax <= native0 + 128)
            if (settled.size >= 6) {
                // A leak a round: the last third against the second third (a slow leak over 30 minutes).
                val third = settled.size / 3
                val mid = settled.subList(third, 2 * third)
                val last = settled.takeLast(third)
                assertTrue("Java heap creeps up", last.map { it.javaMb }.average() <= mid.map { it.javaMb }.average() + 24)
                assertTrue("native heap creeps up", last.map { it.nativeMb }.average() <= mid.map { it.nativeMb }.average() + 48)
            }
        }
    }

    private fun activityOf(scenario: ActivityScenario<MainActivity>): MainActivity {
        lateinit var activity: MainActivity
        scenario.onActivity { activity = it }
        return activity
    }

    private fun rhythmOf(scenario: ActivityScenario<MainActivity>): RhythmViewModel {
        lateinit var vm: RhythmViewModel
        scenario.onActivity { vm = ViewModelProvider(it)[RhythmViewModel::class.java] }
        return vm
    }

    /** The output's peak over [seconds]. */
    private fun listen(seconds: Int): Float {
        RealUse.peakNow()
        var peak = 0f
        repeat(seconds * 10) {
            Thread.sleep(100)
            peak = maxOf(peak, RealUse.peakNow())
        }
        return peak
    }
}
