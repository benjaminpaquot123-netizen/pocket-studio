package com.pocketstudio.app.padlive

import android.app.Application
import android.os.Looper
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.audio.FloatWav
import com.pocketstudio.app.audio.PadBounce
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.live.TakeUi
import com.pocketstudio.app.rhythm.ExportFormat
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LivePadTake
import com.pocketstudio.model.PadLive
import com.pocketstudio.model.PadLiveSet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Duration
import kotlin.math.abs

/**
 * The export of a performance (n° 25, lot B3): a Live take (● REC) keeps the pads it played on its
 * « Pads live » track and how each one played; « Pistes séparées » gives one WAV per pad, and the
 * files add up to the track exactly; « Partager le son » is the song's own export, the take's pads
 * in it. A pad's « Fin » (release) is in what the take keeps (the B2 leftover).
 */
@RunWith(RobolectricTestRunner::class)
class PadLiveExportTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    /** In the engine's place: the sounds made, the audio clips an export installs. */
    private class Sink : CommandSink {
        private var next = 1L
        val created = HashMap<Long, Int>()
        val exportClips = ArrayList<Long>()
        override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) = Unit
        override fun scheduleFrom(target: Long, frame: Long) = Unit
        @Synchronized override fun install(target: Long, kind: CommandSink.Install, index: Int, sample: Long) {
            if (target != 0L && kind == CommandSink.Install.AudioClip && sample != 0L) exportClips += sample
        }
        @Synchronized override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long = next++.also { created[it] = data.size }
        @Synchronized override fun sampleFrames(handle: Long): Long = (created[handle] ?: 96_000).toLong() / 2
        override fun sampleRelease(handle: Long) = Unit
        @Synchronized override fun exportCreate(): Long = next++
        override fun exportRender(target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean, sampleRate: Int, bits: Int) =
            ExportResult.Cancelled
        override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray = FloatArray(0)
        override fun exportDestroy(target: Long) = Unit
        /** The transport's step (sixteenths since ▶) and where the engine marked the take's start. */
        @Volatile var step = -1
        @Volatile var mark = -1
        override fun transportStep(): Int = step
        override fun transportPosition(): Double = step.toDouble()
        override fun songMarkStep(): Int = mark
    }

    private val sink = Sink()

    private fun waitFor(what: String, done: () -> Boolean) {
        val until = System.currentTimeMillis() + 20_000
        while (!done()) {
            assertTrue("never: $what", System.currentTimeMillis() < until)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            Thread.sleep(5)
        }
    }

    @Before
    fun setUp() {
        File(app.filesDir, "projects").deleteRecursively()
        NativeEngine.sink = sink
    }

    @After
    fun tearDown() {
        NativeEngine.sink = null
    }

    @Test
    fun aPadsFinRingsOnInTheKeptTakeAsTheEngineFadesIt() {
        // A loud steady loop of 1 beat, stopped at step 8 (frame 1000 at 120 BPM, 1000 frames a second).
        val loop = Pcm(FloatArray(2000) { 1f }, 1, 1000)
        val spans = listOf(LivePadTake.Span(0, 0.0, 8.0))
        val dry = PadBounce.render(spans, listOf(LiveClip("a.wav", "Nappe", beats = 1)), mapOf(0 to loop), 0, 16, 120.0, 1000)!!
        assertEquals(0f, dry.data[1010], 0f)  // no « Fin »: cut (the short fade) at the stop
        val tail = PadBounce.render(spans, listOf(LiveClip("a.wav", "Nappe", beats = 1, release = 0.4f)), mapOf(0 to loop), 0, 16, 120.0, 1000)!!
        // With a « Fin » of 0.4 s: it rings on from the stop, fading to nothing over 400 frames.
        assertEquals(1f, tail.data[999], 1e-6f)
        assertEquals(0.75f, tail.data[1100], 0.01f)
        assertEquals(0.25f, tail.data[1300], 0.01f)
        assertEquals(0f, tail.data[1420], 0f)
        // Its passes before the stop are cut as before (the next pass starts at full level).
        assertEquals(1f, tail.data[500], 1e-6f)
    }

    /** A « Live » project with two pads whose sounds are on disk: a loop (A1) and a hit (A9) with a « Fin ». */
    private fun liveWithSounds(): RhythmViewModel {
        val vm = RhythmViewModel(app)
        vm.livePadsSoundingForTest = emptySet()
        var files: Pair<String, String>? = null
        vm.viewModelScope.launch {
            val loop = withContext(Dispatchers.IO) { vm.padSamples.add(Pcm(FloatArray(2 * 24_000) { k -> 0.3f * kotlin.math.sin(k * 0.01f) }, 2, 48_000), "Basse") }
            val hit = withContext(Dispatchers.IO) { vm.padSamples.add(Pcm(FloatArray(2 * 12_000) { k -> 0.5f * kotlin.math.exp(-k / 9000f) }, 2, 48_000), "Klaxon") }
            vm.padSamples.register(loop.second)
            vm.padSamples.register(hit.second)
            files = loop.first.file to hit.first.file
        }
        waitFor("the pads' sounds") { files != null }
        vm.session.edit { p ->
            PadLive.newProject().let { n ->
                p.copy(
                    name = "Ma soirée", tempoBpm = 120.0, padLive = PadLiveSet(home = true),
                    live = n.live.withAt(0, LiveClip(files!!.first, "Basse", 4)).withAt(8, LiveClip(files!!.second, "Klaxon", 1, oneShot = true, release = 0.3f)),
                )
            }
        }
        return vm
    }

    /** ● REC, the loop and the hit played, Fin: the take written, its pads kept on their track. */
    private fun take(vm: RhythmViewModel) {
        vm.togglePlay()
        sink.step = 5
        vm.togglePerformance()
        sink.step = 16
        sink.mark = 16
        vm.tickPerformance()
        assertTrue("recording: ${vm.take.value}", vm.take.value is TakeUi.Recording)
        sink.step = 18
        vm.toggleLive(0)  // a loop: waits for its bar line (step 32)
        sink.step = 36
        vm.toggleLive(8)  // a hit: on its beat (step 36)
        sink.step = 40
        vm.toggleLive(0)  // the loop stopped
        sink.step = 60
        vm.togglePerformance()  // ends at the bar line after: 64
        assertTrue("written: ${vm.take.value}", vm.take.value is TakeUi.Done)
        waitFor("the pads' track") { vm.project.value.padLive.take != null }
    }

    @Test
    fun theTakeKeepsEachPadAndItsSeparateFilesAddUpToItsTrack() {
        val vm = liveWithSounds()
        take(vm)
        val p = vm.project.value
        val record = PerfExport.takeOf(p)
        assertNotNull("the take's pads are kept", record)
        assertEquals(listOf(0, 8), record!!.pads)
        val track = p.audioTracks.first { it.id == record.track }
        assertEquals("Pads live", track.name)
        // « Pistes séparées »: one WAV per pad, copied to Musique and handed to the share menu.
        var shared: List<File>? = null
        vm.padLive.perf.shareForTest = { shared = it }
        vm.padLive.perf.shareStems(app)
        waitFor("the separate files") { shared != null || vm.padLive.perf.stems.value is PerfStemsUi.Failed }
        val files = shared!!
        assertTrue(vm.padLive.perf.stems.value is PerfStemsUi.Done)
        assertEquals(2, files.size)
        assertTrue(files[0].name, files[0].name.contains("a1-basse"))
        assertTrue(files[1].name, files[1].name.contains("a9-klaxon"))
        // Together they are the track, sample for sample (the hit's « Fin » in both).
        val whole = vm.padSamples.read(track.clip!!.file)
        val parts = files.map { FloatWav.read(it) }
        parts.forEach { assertEquals(whole.frames, it.frames); assertEquals(whole.channels, it.channels) }
        var worst = 0f
        for (k in whole.data.indices) worst = maxOf(worst, abs(whole.data[k] - parts.sumOf { it.data[k].toDouble() }.toFloat()))
        assertTrue("the files add up to the track (worst $worst)", worst < 1e-5f)
        assertTrue("each file has its pad", parts.all { pcm -> pcm.data.any { abs(it) > 0.01f } })
        // Saved with the project, read back as the app reopened finds it.
        var saved: Boolean? = null
        vm.viewModelScope.launch { saved = vm.flushNow() }
        waitFor("saved") { saved != null }
        assertTrue(saved!!)
        assertEquals(record, RhythmViewModel(app).project.value.padLive.take)
        // One undo takes the take back, its pads' record with it.
        vm.undo()
        assertNull(vm.project.value.padLive.take)
        assertNull(PerfExport.takeOf(vm.project.value))
    }

    @Test
    fun theSoundOfThePerfIsTheSongsExportWithItsPads() {
        val vm = liveWithSounds()
        take(vm)
        val track = vm.project.value.audioTracks.first { it.name == "Pads live" }
        // The engine's sound of the pads' track (by its size: the sink only knows sizes).
        val size = vm.padSamples.read(track.clip!!.file).data.size
        val made = sink.created.filterValues { it == size }.keys
        assertTrue("the pads' track sound was made", made.isNotEmpty())
        assertTrue(perfSeconds(vm.project.value) > 0)
        vm.padLive.perf.shareSound(ExportFormat.M4a)
        waitFor("the song's export") { sink.exportClips.isNotEmpty() }
        assertTrue("the take's pads are in the shared sound", sink.exportClips.any { it in made })
    }

    @Test
    fun withoutATakeTheSeparateFilesSayHowToMakeOne() {
        val vm = liveWithSounds()
        var shared: List<File>? = null
        vm.padLive.perf.shareForTest = { shared = it }
        vm.padLive.perf.shareStems(app)
        val failed = vm.padLive.perf.stems.value as PerfStemsUi.Failed
        assertTrue(failed.message, failed.message.contains("REC"))
        assertNull(shared)
        assertEquals(0, perfSeconds(vm.project.value))
    }
}
