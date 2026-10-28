package com.pocketstudio.app.human

import android.app.Application
import android.media.MediaFormat
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.rhythm.ExportFormat
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.sceneSteps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowMediaCodec
import java.io.File
import java.time.Duration

/**
 * Lot « tuto 4 », É0: the « testeur humain » bench's stand-ins for the phone's engine. « Partager »
 * gives a file to share (no « le moteur audio n'est pas prêt »), and the play head moves with time.
 */
@RunWith(RobolectricTestRunner::class)
class BenchEngineTest {
    private val app: Application get() = ApplicationProvider.getApplicationContext()

    @After
    fun clear() {
        NativeEngine.sink = null
        ShadowMediaCodec.clearCodecs()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(20))

    private fun open(p: Project): RhythmViewModel {
        val id = "bench-${System.nanoTime()}"
        ProjectStore(File(app.filesDir, "projects")).save(id, p)
        CurrentProject.set(app, id)
        val vm = RhythmViewModel(app)
        val until = System.currentTimeMillis() + 10_000
        while (vm.project.value.song.size != p.song.size) {
            assertTrue("the project never opened", System.currentTimeMillis() < until)
            idle(); Thread.sleep(5)
        }
        return vm
    }

    private fun share(format: ExportFormat): Pair<RhythmViewModel, List<File>> {
        val passThrough = ShadowMediaCodec.CodecConfig.Codec { input, output ->
            val n = minOf(input.remaining(), output.remaining())
            output.put(input.duplicate().also { it.limit(it.position() + n) })
            input.position(input.position() + n)
        }
        ShadowMediaCodec.addEncoder(MediaFormat.MIMETYPE_AUDIO_AAC, ShadowMediaCodec.CodecConfig(16 * 1024, 16 * 1024, passThrough))
        val vm = open(Project(song = listOf(SongPart(0), SongPart(0))))
        val engine = BenchEngine()
        NativeEngine.sink = engine
        val shared = ArrayList<List<File>>()
        val scope = CoroutineScope(Dispatchers.Main.immediate)
        scope.launch { vm.shareRequests.collect { shared += it } }
        vm.shareSong(format)
        val until = System.currentTimeMillis() + 60_000
        while (shared.isEmpty() && vm.export.value.error == null) {
            assertTrue("the share never ended (${vm.export.value})", System.currentTimeMillis() < until)
            idle(); Thread.sleep(10)
        }
        scope.cancel()
        assertNull("no « moteur audio » error", vm.export.value.error)
        assertTrue(engine.renders >= 1)
        return vm to shared.single()
    }

    @Test
    fun anM4aIsMadeAndShared() {
        val (vm, files) = share(ExportFormat.M4a)
        assertEquals("m4a", files.single().extension)
        assertTrue(files.single().length() > 1000)
        assertEquals(false, vm.export.value.sharing)  // the progress window is gone, Android's menu opens
    }

    @Test
    fun aWavIsMadeAndShared() {
        val (_, files) = share(ExportFormat.Wav)
        val f = files.single()
        assertEquals("wav", f.extension)
        // The tone, 3 s of float stereo at 48 kHz.
        assertTrue(f.length() >= BenchEngine.SECONDS * 48_000L * 2 * 4)
    }

    @Test
    fun thePlayHeadMovesWithTheTempo() {
        val vm = open(Project(song = listOf(SongPart(0))).copy(tempoBpm = 120.0))
        val loop = vm.project.value.let { it.sceneSteps(it.scene) }
        val spb = vm.project.value.stepsPerBar
        val head = BenchPlayhead()
        head.tick(vm, 0L)
        assertEquals(-1, vm.playBar.value)  // stopped: nothing said
        vm.togglePlay()
        idle()
        assertTrue(vm.transport.value.playing)
        head.tick(vm, 1_000L)
        assertEquals(0, vm.playBar.value)
        // At 120 BPM a bar lasts 2 s: half a bar later, the head is on step 8 of bar 1.
        head.tick(vm, 2_000L)
        assertEquals(8 % loop, vm.live.value.currentStep)
        head.tick(vm, 3_000L)
        assertEquals(16 % loop / spb, vm.playBar.value)
        assertEquals(16 % loop, vm.live.value.currentStep)
        assertEquals(16, vm.live.value.absoluteStep)
    }
}
