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
import com.pocketstudio.model.partSteps
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
        val (vm, files) = share(ExportFormat.Wav)
        val f = files.single()
        assertEquals("wav", f.extension)
        // Audit ultra round 3 (lot A d, « Length: 3 s » for any song): the song's own length, float stereo at 48 kHz
        // (its tail cut once the sound died away: at most 0,5 s more).
        val p = vm.project.value
        val song = p.song.sumOf { p.partSteps(it) } * 15.0 / p.tempoBpm
        val seconds = (f.length() - 44) / 8.0 / 48_000
        assertTrue("$seconds s for a song of $song s", song > 3.5 && seconds in song - 0.01..song + 0.51)
    }

    /** A render of [frames] (and [tail]) by [engine], its project described by [describe] (round 3, lot A d). */
    private fun render(engine: BenchEngine, frames: Long, tail: Long = 0L, autoTail: Boolean = false, describe: (Long) -> Unit): File {
        val t = engine.exportCreate()
        describe(t)
        val f = File(app.cacheDir, "render-$t-${System.nanoTime()}.wav")
        assertEquals(com.pocketstudio.engine.ExportResult.Ok, engine.exportRender(t, f.path, frames, tail, 0L, autoTail, true, 48_000, 32))
        engine.exportDestroy(t)
        return f
    }

    private fun seconds(f: File) = (f.length() - 44) / 8.0 / 48_000

    /** A 1-bar loop: 4 kicks at 120 BPM. */
    private fun loop(e: BenchEngine, t: Long) {
        e.post(t, 6, 120f, 0, 0)
        for (s in 0 until 4) e.post(t, 10, 1f, 0, s * 4)
    }

    /** A 4-bar song: the drums busier, a bass, its channels a little lower. */
    private fun song(e: BenchEngine, t: Long) {
        e.post(t, 6, 120f, 0, 0)
        for (part in 0 until 4) for (s in 0 until 16) e.post(t, 10, 0.9f, s % 3, s)
        for (ch in 0 until 4) e.post(t, 20, 0.7f, ch, 0)
    }

    /**
     * Audit ultra round 3 (lot A d): every export said « 3 s » and the same LUFS, a loop of 1 bar as a song of 4.
     * The bench's render lasts what the app asks (its tail cut once the sound died away) and sounds like the project
     * described to it: two projects measure differently with the app's own meter; a target still brings them to it.
     */
    @Test
    fun aRenderLastsWhatIsAskedAndSoundsLikeItsProject() {
        val e = BenchEngine().apply { renderMs = 0L }
        val bar = 96_000L  // 2 s at 120 BPM
        val a = render(e, bar) { loop(e, it) }
        val b = render(e, 4 * bar) { song(e, it) }
        assertEquals(2.0, seconds(a), 0.01)
        assertEquals(8.0, seconds(b), 0.01)
        val la = com.pocketstudio.app.export.Loudness.integrated(a)!!
        val lb = com.pocketstudio.app.export.Loudness.integrated(b)!!
        assertTrue("the loop $la LUFS, the song $lb LUFS", kotlin.math.abs(la - lb) > 1.0)
        assertEquals("the same project, the same sound", la, com.pocketstudio.app.export.Loudness.integrated(render(e, bar) { loop(e, it) })!!, 0.05)
        // A song's tail: at most 0,5 s once the sound died away (autoTail), the whole tail asked without it.
        assertEquals(2.5, seconds(render(e, bar, tail = 10 * 48_000L, autoTail = true) { loop(e, it) }), 0.01)
        assertEquals(3.0, seconds(render(e, bar, tail = 48_000L) { loop(e, it) }), 0.01)
        // Round 2's targets, each project brought to them (the master's two passes): club −8, streaming −14.
        for (target in listOf(-8f, -14f)) for (describe in listOf<(Long) -> Unit>({ loop(e, it) }, { song(e, it) })) {
            val f = render(e, 4 * bar) { t -> describe(t); e.post(t, 67, target, 4, 0) }
            assertEquals("target $target", target.toDouble(), com.pocketstudio.app.export.Loudness.integrated(f)!!, 0.5)
        }
        // Rendered to memory (a frozen track, a pad's loop): the length asked too.
        val t = e.exportCreate()
        loop(e, t)
        assertEquals(2 * (bar + 4_800), e.exportRenderFloat(t, bar, 4_800L).size.toLong())
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
