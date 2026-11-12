package com.pocketstudio.app.export

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.rhythm.ExportFormat
import com.pocketstudio.app.rhythm.ExportWhat
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.rhythm.WavLength
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.SongPart
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Duration

/**
 * Lot E (« Master pro », §1 bis point 5): separate tracks for pros. « Sans les envois » gives each strip
 * dry (the engine's stem mode 1) plus the reverb's and the echo's returns alone (modes 2 and 3, no
 * strip soloed, no limiter, no master effects); all the files have one length (the longest stem's,
 * the others padded with silence) from the song's frame 0, numbered in order. Their sum equals the mix
 * (here: each stand-in render a part of a known signal, the files summed back under -60 dB); the
 * engine's own sum is proven by the host test mp_stems_add_up_to_the_mix and the export matrix.
 */
@RunWith(RobolectricTestRunner::class)
class StemNullTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val store = ProjectStore(File(app.filesDir, "projects"))

    /** Each private engine's posts; its render writes a 24-bit WAV, as long as its index asks, of its share of a ramp. */
    private class Sink : CommandSink {
        private var next = 1L
        val posts = HashMap<Long, MutableList<IntArray>>()
        val order = ArrayList<Long>()
        @Synchronized override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) {
            posts.getOrPut(target) { ArrayList() } += intArrayOf(type, Math.round(value * 100), a, b)
        }
        override fun scheduleFrom(target: Long, frame: Long) = Unit
        override fun install(target: Long, kind: CommandSink.Install, index: Int, sample: Long) = Unit
        @Synchronized override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long = next++
        override fun sampleFrames(handle: Long): Long = 48_000
        override fun sampleRelease(handle: Long) = Unit
        @Synchronized override fun exportCreate(): Long = (next++).also { order += it }
        override fun exportRender(target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean, sampleRate: Int, bits: Int): ExportResult {
            val k = synchronized(this) { order.indexOf(target) }
            // Each stem rings for a different time: 1000, 1500, 2000… frames.
            writeWav(File(path), 1000 + 500 * k) { i -> share(k, i) }
            return ExportResult.Ok
        }
        override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray = FloatArray(0)
        override fun exportDestroy(target: Long) = Unit
    }

    private val sink = Sink()

    @After
    fun tearDown() {
        NativeEngine.sink = null
    }

    private fun waitUntil(what: String, check: () -> Boolean) {
        val until = System.currentTimeMillis() + 60_000
        while (!check()) {
            assertTrue(what, System.currentTimeMillis() < until)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            Thread.sleep(5)
        }
    }

    @Test
    fun stemsWithoutTheirSendsComeAtOneLengthWithTheReturnsAndAddUpToTheMix() {
        val id = "stems-${System.nanoTime()}"
        val drums = DrumPattern().toggle(0, 0)
        val acid = com.pocketstudio.model.AcidPattern().let { a -> a.copy(steps = a.steps.mapIndexed { i, s -> if (i == 0) s.copy(gate = true) else s }) }
        val mixer = com.pocketstudio.model.Mixer().let { m ->
            m.copy(channels = m.channels.mapIndexed { i, c -> if (i == 0) c.copy(sends = listOf(0.4f, 0f)) else if (i == 1) c.copy(sends = listOf(0f, 0.3f)) else c })
        }
        store.save(id, Project(name = id, drums = drums, acid = acid, mixer = mixer, song = listOf(SongPart(0))))
        com.pocketstudio.app.projects.CurrentProject.set(app, id)
        NativeEngine.sink = sink
        val vm = RhythmViewModel(app)
        waitUntil("the project never opened") { vm.project.value.name == id }
        vm.setExportWhat(ExportWhat.Stems)
        vm.setExportFormat(ExportFormat.Wav)
        vm.setExportOptions(ExportOptions(stemsDry = true))
        vm.exportWav()
        waitUntil("the export never ended") { !vm.export.value.running && (vm.export.value.files.isNotEmpty() || vm.export.value.error != null) }
        val files = vm.export.value.files
        assertEquals(vm.export.value.error, null, vm.export.value.error)
        val names = files.map { it.name }
        // Drums and acid dry, then the reverb's and the echo's returns, numbered in order.
        assertEquals(names.toString(), 4, files.size)
        assertTrue(names.toString(), names[0].contains("-01-batterie") && names[1].contains("-02-acide"))
        assertTrue(names.toString(), names[2].contains("-03-reverbe") && names[3].contains("-04-echo"))
        // One length (the longest), from frame 0.
        files.forEach { assertEquals(it.name, 1000L + 500 * 3, WavLength.frames(it)) }
        // The engine was asked: dry strips (mode 1, each soloed), each return alone (modes 2 and 3).
        val modes = sink.order.map { t -> sink.posts[t].orEmpty().lastOrNull { it[0] == 190 && it[2] == 5 }?.get(1) }
        assertEquals(listOf(100, 100, 200, 300), modes)
        // The returns: no strip soloed; the limiter and the master's effects off.
        for (t in sink.order.drop(2)) {
            val p = sink.posts[t].orEmpty()
            assertTrue(p.none { it[0] == SOLO && it[1] == 100 })
            assertTrue(p.any { it[0] == MASTER && it[2] == 1 && it[1] == 0 })
        }
        // Their sum gives back the whole signal (the mix), under -60 dB.
        val sum = DoubleArray(2500)
        files.forEach { f -> read24(f).forEachIndexed { i, v -> sum[i] += v } }
        var worst = 0.0
        for (i in sum.indices) worst = maxOf(worst, kotlin.math.abs(sum[i] - (0 until 4).sumOf { share(it, i) }))
        assertTrue("residual ${20 * kotlin.math.log10(worst + 1e-12)} dB", 20 * kotlin.math.log10(worst / 0.5 + 1e-12) < -60)
        files.forEach { it.delete() }
    }

    @Test
    fun paddingKeepsTheSoundAndPatchesTheHeader() {
        val f = File(app.cacheDir, "pad-${System.nanoTime()}.wav")
        writeWav(f, 300) { 0.25 }
        WavLength.padTo(f, 1000)
        assertEquals(1000L, WavLength.frames(f))
        val bytes = f.readBytes()
        assertEquals(bytes.size - 8, ByteBuffer.wrap(bytes, 4, 4).order(ByteOrder.LITTLE_ENDIAN).int)
        val v = read24(f)
        assertEquals(0.25, v[299], 1e-5)
        assertEquals(0.0, v[300], 0.0)
        WavPcm.open(f).use { assertEquals(1000L, it.framesLeft) }
        f.delete()
    }

    companion object {
        /** NativeEngine's CMD_CHANNEL_SOLO and CMD_MASTER (CommandIdsTest keeps them in step). */
        const val SOLO = 23
        const val MASTER = 67

        /** Stem [k]'s part of frame [i] of the « mix ». */
        fun share(k: Int, i: Int): Double = if (i < 1000 + 500 * k) 0.1 * ((i * (k + 3)) % 97) / 97.0 else 0.0

        fun writeWav(file: File, frames: Int, v: (Int) -> Double) {
            val b = ByteBuffer.allocate(44 + frames * 6).order(ByteOrder.LITTLE_ENDIAN)
            b.put("RIFF".toByteArray()).putInt(36 + frames * 6).put("WAVEfmt ".toByteArray()).putInt(16).putShort(1).putShort(2)
                .putInt(48000).putInt(48000 * 6).putShort(6).putShort(24).put("data".toByteArray()).putInt(frames * 6)
            repeat(frames) { i ->
                val q = Math.round(v(i) * 8388607).toInt()
                repeat(2) { b.put(q.toByte()).put((q shr 8).toByte()).put((q shr 16).toByte()) }
            }
            file.writeBytes(b.array())
        }

        /** The left channel of a 24-bit stereo WAV. */
        fun read24(file: File): List<Double> {
            val pcm = WavPcm.open(file)
            val out = ArrayList<Double>()
            pcm.use {
                val buf = FloatArray(2 * 1024)
                while (true) {
                    val n = it.readFloat(buf, 1024)
                    if (n == 0) break
                    for (i in 0 until n) out += buf[2 * i].toDouble()
                }
            }
            return out
        }
    }
}
