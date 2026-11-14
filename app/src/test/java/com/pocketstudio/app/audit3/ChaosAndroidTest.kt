package com.pocketstudio.app.audit3

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.MainActivity
import com.pocketstudio.app.human.BenchEngine
import com.pocketstudio.app.playback.PlaybackService
import com.pocketstudio.app.playback.PlaybackStatus
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

/**
 * Audit stage 3, « chaos Android » as far as Robolectric goes (the rest: the emulator workflow
 * and Benjamin's phone, docs/lots/AUDIT-3.md): the app killed in the background and brought back,
 * the memory running low while it plays, the headphones pulled out, a call taking the sound, and
 * the app opened in dark mode, at 200 % text and right to left.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class ChaosAndroidTest {
    @get:Rule
    val compose = createEmptyComposeRule()
    private val app: Application get() = ApplicationProvider.getApplicationContext()
    private val store get() = ProjectStore(File(app.filesDir, "projects"))
    private lateinit var id: String

    @Before
    fun setUp() {
        forgetViewModelFactory()
        NativeEngine.sink = BenchEngine()
        File(app.filesDir, "projects").deleteRecursively()
        listOf("nav", "missions", "settings", "next_steps", "song_ready", "share", "tools", "launch", "project_modes").forEach {
            app.getSharedPreferences(it, 0).edit().clear().commit()
        }
        id = "chaos-" + System.nanoTime()
        store.save(id, Project(name = "Chaos", drums = DrumPattern().toggle(0, 0).toggle(0, 8)).sanitized())
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
        PlaybackService.update(PlaybackStatus())
        CurrentProject.set(app, RhythmViewModel.PROJECT_ID)
    }

    /**
     * Robolectric gives each test a new Application, but AndroidX keeps the first one in its
     * AndroidViewModelFactory singleton: the screens' models of a second test would read the first
     * test's files. One Application per process on a phone; here it is forgotten before each test.
     */
    private fun forgetViewModelFactory() {
        val c = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory::class.java
        c.declaredFields.filter { java.lang.reflect.Modifier.isStatic(it.modifiers) && c.isAssignableFrom(it.type) }.forEach {
            it.isAccessible = true
            it.set(null, null)
        }
    }

    /** The project on disk becomes [p] (a stop saves off the main thread). */
    private fun awaitSaved(what: String, id: String, p: () -> Project) {
        val until = System.currentTimeMillis() + 15_000
        while (store.load(id) != p()) {
            check(System.currentTimeMillis() < until) { "never saved: $what" }
            idle()
            Thread.sleep(20)
        }
    }

    private fun shown(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun await(what: String, done: () -> Boolean) {
        val until = System.currentTimeMillis() + 15_000
        while (!done()) {
            check(System.currentTimeMillis() < until) { "never: $what" }
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            compose.waitForIdle()
            Thread.sleep(20)
        }
    }

    private fun idle() {
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        Thread.sleep(10)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
    }

    @Test
    fun killedInTheBackgroundTheProjectAndTheScreenComeBack() {
        val first = Robolectric.buildActivity(MainActivity::class.java).setup()
        await("the project open") { shown("space_bar") }
        val vm = ViewModelProvider(first.get())[RhythmViewModel::class.java]
        await("the project read") { vm.project.value.name.isNotEmpty() }
        val opened = vm.project.value
        compose.runOnIdle { vm.toggleStep(4) }
        val edited = vm.project.value
        assertTrue(edited != opened)
        // To the background (the edit is written there), its state kept, then the process dies.
        first.pause().stop()
        val saved = Bundle()
        first.saveInstanceState(saved)
        first.destroy()
        idle()
        awaitSaved("on disk before the process dies", vm.projectId) { edited }
        // A new process: a new activity from the saved state, a new screen model reading the disk.
        val second = Robolectric.buildActivity(MainActivity::class.java).setup(saved)
        await("the project open again") { shown("space_bar") }
        val again = ViewModelProvider(second.get())[RhythmViewModel::class.java]
        assertNotSame(vm, again)
        assertEquals(vm.projectId, again.projectId)
        await("the project read back") { again.project.value == edited }
        assertEquals("the project whole", edited, again.project.value)
        second.pause().stop().destroy()
    }

    @Test
    fun lowMemoryWhilePlayingKeepsTheSongGoing() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        await("the project open") { shown("space_bar") }
        val vm = ViewModelProvider(controller.get())[RhythmViewModel::class.java]
        compose.runOnIdle { vm.togglePlay() }
        assertTrue(vm.transport.value.playing)
        for (level in listOf(ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW, ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL, ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)) {
            controller.get().onTrimMemory(level)
            app.onTrimMemory(level)
        }
        app.onLowMemory()
        compose.waitForIdle()
        assertTrue("still playing", vm.transport.value.playing)
        compose.runOnIdle { vm.toggleStep(5) }
        compose.runOnIdle { vm.togglePlay() }
        controller.pause().stop()
        idle()
        awaitSaved("after stop", vm.projectId) { vm.project.value }
        controller.destroy()
    }

    @Test
    fun headphonesOutOrACallStopTheSongButANotificationDoesNot() {
        val vm = RhythmViewModel(app)
        await("open") { vm.project.value.name == "Chaos" }
        vm.togglePlay()
        assertTrue(vm.transport.value.playing)
        PlaybackService.update(PlaybackStatus("Chaos", 120.0))
        val service = Robolectric.buildService(PlaybackService::class.java).create().get()
        // Wired, Bluetooth or USB headphones out: Android says « becoming noisy ».
        app.sendBroadcast(Intent(AudioManager.ACTION_AUDIO_BECOMING_NOISY).setPackage(app.packageName))
        idle()
        assertFalse("the song stops with the headphones out", vm.transport.value.playing)

        vm.togglePlay()
        assertTrue(vm.transport.value.playing)
        val focus = shadowOf(app.getSystemService(AudioManager::class.java)).lastAudioFocusRequest
        // A notification's sound lowers us a moment: the song goes on.
        focus.listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)
        idle()
        assertTrue("a notification does not stop the song", vm.transport.value.playing)
        // A call: the song stops.
        focus.listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
        idle()
        assertFalse("a call stops the song", vm.transport.value.playing)
        vm.togglePlay()
        focus.listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)
        idle()
        assertFalse("another player stops the song", vm.transport.value.playing)
        service.onDestroy()
    }

    /** Opens the app with [qualifiers], plays, edits, stops: no crash, the bar shown, the project whole. */
    private fun openIn(qualifiers: String, fontScale: Float = 1f) {
        RuntimeEnvironment.setQualifiers(qualifiers)
        RuntimeEnvironment.setFontScale(fontScale)
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        await("$qualifiers: the project open") { shown("space_bar") || shown("space_rail") }
        val vm = ViewModelProvider(controller.get())[RhythmViewModel::class.java]
        compose.runOnIdle { vm.togglePlay() }
        compose.runOnIdle { vm.toggleStep(3) }
        compose.runOnIdle { vm.togglePlay() }
        controller.pause().stop()
        idle()
        assertEquals("$qualifiers: the project opened", id, vm.projectId)
        awaitSaved("$qualifiers: saved", vm.projectId) { vm.project.value }
        controller.destroy()
    }

    @Test
    fun darkModeOpens() = openIn("+night")

    @Test
    fun rightToLeftOpens() = openIn("+ldrtl")

    @Test
    fun twoHundredPercentTextOpens() = openIn("+notnight", fontScale = 2f)

    @Test
    fun aFoldableOpenedAndASmallSplitScreenOpen() {
        openIn("w673dp-h841dp")
        openIn("w320dp-h480dp-mdpi")
    }
}
