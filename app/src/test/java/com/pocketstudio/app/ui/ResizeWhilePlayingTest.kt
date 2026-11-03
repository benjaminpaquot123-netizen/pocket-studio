package com.pocketstudio.app.ui

import android.app.Application
import android.content.ComponentName
import android.content.pm.ActivityInfo
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.MainActivity
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Lot D (USAGE-TABLETTE-SMARTPHONE §5 point 3): the window changes size while the song plays
 * (split screen, a foldable opened, the phone turned, a tablet's keyboard): the activity is
 * drawn again in place (`configChanges`), never rebuilt; the music never stops, the screen keeps
 * what was open and follows the new size (the spaces' bar becomes a rail lying down).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class ResizeWhilePlayingTest {
    @get:Rule
    val compose = createEmptyComposeRule()
    private val app: Application get() = ApplicationProvider.getApplicationContext()

    /** In the engine's place: the transport's starts and stops. */
    private class Sink : CommandSink {
        private var next = 1L
        val transport = java.util.Collections.synchronizedList(ArrayList<Float>())
        override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) { if (target == 0L && type == SET_TRANSPORT) transport += value }
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
    }

    private val sink = Sink()

    @Before
    fun setUp() {
        NativeEngine.sink = sink
        File(app.filesDir, "projects").deleteRecursively()
        val id = "resize-" + System.nanoTime()
        ProjectStore(File(app.filesDir, "projects")).save(id, Project(name = "Taille", drums = DrumPattern().toggle(0, 0).toggle(0, 8)).sanitized())
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

    @Test
    fun theActivityHandlesItsOwnSizeChanges() {
        val info = app.packageManager.getActivityInfo(ComponentName(app, MainActivity::class.java), 0)
        val needed = ActivityInfo.CONFIG_SCREEN_SIZE or ActivityInfo.CONFIG_SMALLEST_SCREEN_SIZE or ActivityInfo.CONFIG_SCREEN_LAYOUT or
            ActivityInfo.CONFIG_ORIENTATION or ActivityInfo.CONFIG_DENSITY or ActivityInfo.CONFIG_KEYBOARD or ActivityInfo.CONFIG_KEYBOARD_HIDDEN
        assertEquals("configChanges", needed, info.configChanges and needed)
    }

    @Test
    fun theSongPlaysOnAndTheScreenFollowsWhenTheWindowChangesSize() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        await("the project open, the spaces' bar") { shown("space_bar") }
        val vm = ViewModelProvider(activity)[RhythmViewModel::class.java]
        compose.runOnIdle { vm.togglePlay() }
        assertTrue(vm.transport.value.playing)
        val started = sink.transport.toList()
        // The project as it was: nothing read again from the disk.
        val before = vm.project.value
        // Split screen on a tablet lying down, then a narrow half, then a foldable opened.
        for (size in listOf("w1280dp-h800dp-land", "w360dp-h700dp-port", "w673dp-h841dp-port", "w400dp-h890dp-port")) {
            RuntimeEnvironment.setQualifiers(size)
            controller.configurationChange()
            compose.waitForIdle()
            assertSame("$size: the same activity, drawn again in place", activity, controller.get())
            assertTrue("$size: still playing", vm.transport.value.playing)
            assertEquals("$size: the transport never stopped nor started again", started, sink.transport.toList())
            assertSame("$size: the project as it was", before, vm.project.value)
            val lying = size.endsWith("land")
            await("$size: the screen at its new size") { if (lying) shown("space_rail") else shown("space_bar") }
        }
        compose.runOnIdle { vm.togglePlay() }
        controller.pause().stop().destroy()
    }

    private companion object {
        /** NativeEngine's CMD_SET_TRANSPORT. */
        const val SET_TRANSPORT = 5
    }
}
