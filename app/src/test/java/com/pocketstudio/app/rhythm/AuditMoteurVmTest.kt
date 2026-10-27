package com.pocketstudio.app.rhythm

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Duration

/**
 * Final audit, lot « moteur », through the ViewModel. Audit 06: a Pompe set to « Le son du kick »
 * in a project made by hand (no sidechain key) was silent: the engine now hears the kick's pad, at
 * once when the pump is switched to it, and on load. A loop curve over the whole part reaches the
 * engine with its length.
 */
@RunWith(RobolectricTestRunner::class)
class AuditMoteurVmTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    private class Sink : CommandSink {
        val keys = ArrayList<Int>()  // the live engine's sidechain keys, in order
        val modes = ArrayList<Pair<Int, Int>>()  // the live engine's automation modes: lane, b
        @Synchronized override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) {
            if (target == 0L && type == CMD_SIDECHAIN && a == 0) keys += value.toInt()  // a 0: the key (1..5: the Musique bus)
            if (target == 0L && type == CMD_AUTOMATION_MODE) modes += a to b
        }
        override fun scheduleFrom(target: Long, frame: Long) = Unit
        override fun install(target: Long, kind: CommandSink.Install, index: Int, sample: Long) = Unit
        private var next = 1L
        @Synchronized override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long = next++
        override fun sampleFrames(handle: Long): Long = 0L
        override fun sampleRelease(handle: Long) = Unit
        @Synchronized override fun exportCreate(): Long = 1000L + next++
        override fun exportRender(target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean, sampleRate: Int, bits: Int) =
            ExportResult.Ok
        override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long) = FloatArray(0)
        override fun exportDestroy(target: Long) = Unit
        @Synchronized fun last() = keys.lastOrNull()
    }

    private val sink = Sink()

    @After
    fun tearDown() {
        NativeEngine.sink = null
        com.pocketstudio.app.projects.CurrentProject.set(app, RhythmViewModel.PROJECT_ID)
    }

    private fun open(p: Project): RhythmViewModel {
        val id = "pumpkey-${System.nanoTime()}"
        ProjectStore(File(app.filesDir, "projects")).save(id, p)
        com.pocketstudio.app.projects.CurrentProject.set(app, id)
        NativeEngine.sink = sink
        val vm = RhythmViewModel(app)
        val until = System.currentTimeMillis() + 60_000
        while (vm.project.value.name != p.name) {
            assertTrue("the project never opened", System.currentTimeMillis() < until)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            Thread.sleep(5)
        }
        return vm
    }

    @Test
    fun aPumpSwitchedToTheKicksSoundHearsTheKick() {
        val kick = Mixer.padChannel(Mixer.KICK_LANE)
        val vm = open(Project(name = "Pompe à la main"))
        vm.engineStarted()
        assertEquals(-1, sink.last())
        // A Pompe on the bass strip: on the grid's hits by default, no key.
        vm.setInsertType(1, 0, EffectType.PUMP)
        assertEquals(-1, sink.last())
        // « Réagit à : Le son du kick »: the kick's pad is the key at once.
        vm.setInsertParam(1, 0, 3, 1f)
        assertEquals(kick, sink.last())
        // Back to the grid: no key again.
        vm.setInsertParam(1, 0, 3, 0f)
        assertEquals(-1, sink.last())
        // A project saved that way, opened: the key is sent with it.
        vm.setInsertParam(1, 0, 3, 1f)
        val again = open(vm.project.value.copy(name = "Pompe rouverte"))
        sink.keys.clear()
        again.engineStarted()
        assertEquals(kick, sink.last())
    }

    @Test
    fun aLoopCurveOverTheWholePartReachesTheEngine() {
        val vm = open(Project(name = "Courbe sur la partie"))
        vm.engineStarted()
        val pan = com.pocketstudio.model.AutoTarget(com.pocketstudio.model.AutoTarget.PAN, 1)
        vm.addAutomation(pan)
        assertEquals(com.pocketstudio.model.AutomationLane.LOOP, vm.project.value.automation[0].mode)
        assertEquals(0, vm.project.value.automation[0].loopSteps)
        vm.setAutomationLoopSteps(0, 32)
        assertEquals(32, vm.project.value.automation[0].loopSteps)
        assertEquals(32, sink.modes.last { it.first == 0 }.second shr 8)
        // Back on the drum loop.
        vm.setAutomationLoopSteps(0, 0)
        assertEquals(0, vm.project.value.automation[0].loopSteps)
        assertEquals(0, sink.modes.last { it.first == 0 }.second shr 8)
    }

    private companion object {
        const val CMD_SIDECHAIN = 102
        const val CMD_AUTOMATION_MODE = 58
    }
}
