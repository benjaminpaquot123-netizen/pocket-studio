package com.pocketstudio.app.export

import android.app.Application
import android.media.AudioFormat
import android.media.MediaFormat
import android.net.Uri
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.human.BenchEngine
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.rhythm.ExportFormat
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
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
import org.robolectric.shadows.ShadowMediaCodec
import org.robolectric.shadows.ShadowMediaExtractor
import org.robolectric.shadows.util.DataSource
import java.io.File
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tour 5, moyens2 (the phone runs: the export's screen read « −9,2 LUFS intégrés » above « Volume intégré : −9,4 LUFS »
 * under « Mesures pro », for one file). An M4A that goes over −1 dBTP once decoded is rendered again with its limiter
 * lower ([M4aGuard]): the file shared is that last render. Its loudness was taken from it, but the pro readings kept the
 * first render's report. Here the decoded file always reads 0 dBTP (the guard renders again) and each render reports
 * its own loudness (−9,4 the first, −9,2 after): the card must say one loudness, the file's.
 */
@RunWith(RobolectricTestRunner::class)
class OneLoudnessExportTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    /** The bench's engine; each render reports its own loudness, and its M4A decodes at 0 dBTP (over −1: rendered again). */
    private inner class Sink(private val inner: BenchEngine = BenchEngine()) : CommandSink by inner {
        val renders = AtomicInteger()
        private val lufsOf = HashMap<Long, Float>()

        override fun exportRender(
            target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean,
            sampleRate: Int, bits: Int,
        ): ExportResult {
            val n = renders.incrementAndGet()
            synchronized(lufsOf) { lufsOf[target] = if (n == 1) -9.4f else -9.2f }
            // The M4A made from this WAV, as a phone's decoder would read it back: a full-scale square, 0 dBTP.
            val wav = File(path)
            loudM4a(File(wav.parentFile, wav.nameWithoutExtension + ".m4a"))
            return inner.exportRender(target, path, frames, tailFrames, fadeOutFrames, autoTail, floatWav, sampleRate, bits)
        }

        override fun exportReport(target: Long): FloatArray? {
            val lufs = synchronized(lufsOf) { lufsOf[target] } ?: return null
            return FloatArray(14) { -120f }.also {
                it[NativeEngine.REPORT_LUFS] = lufs
                it[NativeEngine.REPORT_TRUE_PEAK] = -2.1f
                it[NativeEngine.REPORT_SECONDS] = 8f
                it[NativeEngine.REPORT_RATE] = 48_000f
                it[NativeEngine.REPORT_DRIVE] = 0f
                it[NativeEngine.REPORT_LOSS] = 0f
                it[NativeEngine.REPORT_LRA] = 4f
                it[NativeEngine.REPORT_CORRELATION] = 0.9f
            }
        }
    }

    private val passThrough = ShadowMediaCodec.CodecConfig.Codec { input, output ->
        val n = minOf(input.remaining(), output.remaining())
        output.put(input.duplicate().also { it.limit(it.position() + n) })
        input.position(input.position() + n)
    }

    private fun loudM4a(file: File) {
        val frames = 48_000
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 48_000, 2).apply {
            setLong(MediaFormat.KEY_DURATION, 1_000_000L)
            setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
        }
        val data = ByteArray(frames * 4)
        for (i in 0 until frames * 2) {
            val v = if ((i / 2 / 48) % 2 == 0) Short.MAX_VALUE.toInt() else -Short.MAX_VALUE
            data[2 * i] = (v and 0xFF).toByte()
            data[2 * i + 1] = ((v shr 8) and 0xFF).toByte()
        }
        ShadowMediaExtractor.addTrack(DataSource.toDataSource(app, Uri.fromFile(file), null), format, data)
    }

    @After
    fun end() {
        NativeEngine.sink = null
        ShadowMediaCodec.clearCodecs()
        CurrentProject.set(app, RhythmViewModel.PROJECT_ID)
    }

    private fun until(what: String, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + 120_000
        while (!ok()) {
            check(System.currentTimeMillis() < end) { what }
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(20))
            Thread.sleep(5)
        }
    }

    /** « −9,2 » out of « −9,2 LUFS intégrés · crête … » or « Volume intégré : −9,2 LUFS ». */
    private fun lufsIn(line: String): String = Regex("""(−?[0-9]+,[0-9]) LUFS""").find(line)?.groupValues?.get(1) ?: error("no LUFS in « $line »")

    @Test
    fun anM4aRenderedAgainSaysOneLoudness() {
        ShadowMediaCodec.addEncoder(MediaFormat.MIMETYPE_AUDIO_AAC, ShadowMediaCodec.CodecConfig(16 * 1024, 16 * 1024, passThrough))
        ShadowMediaCodec.addDecoder(MediaFormat.MIMETYPE_AUDIO_AAC, ShadowMediaCodec.CodecConfig(16 * 1024, 16 * 1024, passThrough))
        val sink = Sink()
        NativeEngine.sink = sink
        val id = "one-loudness-" + System.nanoTime()
        ProjectStore(File(app.filesDir, "projects")).save(id, Project(name = "Une mesure", song = listOf(SongPart(0), SongPart(0))))
        CurrentProject.set(app, id)
        val vm = RhythmViewModel(app)
        until("never opened") { vm.project.value.name == "Une mesure" }
        vm.setExportFormat(ExportFormat.M4a)
        vm.exportWav()
        until("never exported: ${vm.export.value.error}") { !vm.export.value.running && vm.export.value.files.isNotEmpty() }
        val export = vm.export.value
        assertTrue("the decoded M4A went over −1 dBTP: the mix was rendered again (${sink.renders.get()} renders)", sink.renders.get() >= 2)
        // The file is the last render's: −9,2.
        assertEquals(-9.2, export.loudness!!, 0.01)
        val card = lufsIn(export.bilan!!.levelLine())
        val pro = ProReport.lines(export.proReport, export.decodedPeakDb).first { "Volume intégré" in it }
        assertEquals("the card's « $card LUFS intégrés » and its pro reading « $pro » are one loudness", card, lufsIn(pro))
        assertEquals("−9,2", card)
    }
}
