package com.pocketstudio.app.tuto

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.bench.RecordingSink
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.FxPresets
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.PadVoice
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.SoundLibrary
import com.pocketstudio.model.track
import com.pocketstudio.model.tuto.TutoChecks
import com.pocketstudio.model.tuto.TutoStep
import com.pocketstudio.model.tuto.TutoTarget
import com.pocketstudio.model.withFx
import com.pocketstudio.model.withTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/**
 * The tutorial's default sounds heard through the engine core (`ps_render`): the kicks, the snares
 * and the bass sounds built in that the jump up project could start with, each rendered alone at
 * 174 BPM and measured (how long a hit rings, how bright it is, how much is low end), each also
 * written as a WAV to listen to. What the tutorial loads (TutoTarget.blank, the bass of
 * TutoChecks.partTrack) was chosen from this report (docs/DECISIONS.md, 2026-10-28).
 *
 * Needs the engine: off unless `-Pbench.render=<ps_render>`.
 */
@RunWith(RobolectricTestRunner::class)
class TutoDefaultSoundBench {
    private val app: Application = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        NativeEngine.sink = null
        com.pocketstudio.app.projects.CurrentProject.set(app, RhythmViewModel.PROJECT_ID)
    }

    private fun <T> await(block: suspend () -> T): T {
        var done = false
        var result: Result<T>? = null
        CoroutineScope(Dispatchers.Main).launch {
            result = runCatching { block() }
            done = true
        }
        val until = System.currentTimeMillis() + 300_000
        while (!done) {
            check(System.currentTimeMillis() < until) { "render too long" }
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(2)
        }
        return result!!.getOrThrow()
    }

    @Test
    fun defaultSounds() {
        val renderer = System.getProperty("ps.bench.render")?.takeIf { it.isNotBlank() }?.let(::File)
        assumeTrue("run with -Pbench.render=<ps_render>", renderer != null)
        val dir = File(renderer!!.parentFile, "tuto-sounds").apply { deleteRecursively(); mkdirs() }
        NativeEngine.sink = RecordingSink(File(dir, "samples"), renderer)
        val id = "tuto-snd-${System.nanoTime()}"
        ProjectStore(File(app.filesDir, "projects")).save(id, Project())
        com.pocketstudio.app.projects.CurrentProject.set(app, id)
        val vm = RhythmViewModel(app)
        shadowOf(Looper.getMainLooper()).idle()
        val target = TutoTarget.of("jump_up")!!
        val blank = target.blank().copy(tempoBpm = 174.0, harmony = target.harmony)
        val song = target.song
        val report = StringBuilder()

        fun strip(p: Project, lane: Int, from: Int = lane): Project {
            val ch = Mixer.padChannel(lane)
            val theirs = song.mixer.channels[Mixer.padChannel(from)]
            return p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, c -> if (i == ch) c.copy(inserts = theirs.inserts) else c }))
        }
        fun drums(p: Project, lane: Int, role: String): Project {
            val line = target.line(role)!!
            val hits = com.pocketstudio.model.StarterWizard.hits(line, 32)
            return p.copy(drums = p.drums.copy(steps = 32, lanes = p.drums.lanes.mapIndexed { i, l -> l.copy(hits = if (i == lane) hits else emptyMap()) }))
        }
        fun measure(name: String, p: Project) {
            val pcm = await { vm.renderExample(p, maxBars = 2, minBars = 2) } ?: error("$name: no render")
            writeWav(File(dir, "$name.wav"), pcm)
            val n = pcm.size / 2
            val mono = FloatArray(n) { (pcm[2 * it] + pcm[2 * it + 1]) / 2 }
            val peak = mono.maxOf { abs(it) }
            val rms = sqrt(mono.fold(0.0) { a, v -> a + v * v } / n)
            // How long the first hit rings: 5 ms windows from its peak until 40 dB under it.
            val w = 240
            val env = (0 until n / w).map { k -> sqrt((0 until w).sumOf { j -> (mono[k * w + j] * mono[k * w + j]).toDouble() } / w) }
            val top = env.indices.take(40).maxBy { env[it] }
            val ring = (top until env.size).firstOrNull { env[it] < env[top] * 0.01 }?.let { (it - top) * 5 } ?: -1
            // Low end (< ~150 Hz, one-pole) and brightness (> ~2 kHz, one-pole high-pass) shares of the energy.
            var lp = 0f; var hpPrev = 0f; var hpIn = 0f
            var low = 0.0; var high = 0.0; var all = 0.0
            val aLow = 0.0194f; val aHigh = 0.79f
            for (x in mono) {
                lp += aLow * (x - lp)
                val hp = aHigh * (hpPrev + x - hpIn); hpPrev = hp; hpIn = x
                low += lp * lp; high += hp * hp; all += x * x
            }
            report.appendLine("%-26s peak=%6.1f dBFS rms=%6.1f dBFS ring(-40dB)=%5d ms low=%4.0f%% bright=%4.1f%%".format(
                name, 20 * log10(max(peak.toDouble(), 1e-9)), 20 * log10(max(rms, 1e-9)), ring, 100 * low / all, 100 * high / all))
        }

        // Kicks: the two-step alone.
        val kick = drums(blank, 0, "kick")
        measure("kick_studio", kick)
        measure("kick_studio_punch", strip(kick, 0))
        measure("kick_hard_punch", strip(kick.copy(padVoices = kick.padVoices + (0 to PadVoice(PadVoice.KICK_HARD))), 0))
        measure("kick_hard_short_punch", strip(kick.copy(padVoices = kick.padVoices + (0 to PadVoice(PadVoice.KICK_HARD, mapOf(PadVoice.DECAY to 0.15f, PadVoice.CLICK to 0.6f)))), 0))
        measure("kick_studio_tunedF_punch", strip(kick.copy(padVoices = kick.padVoices + (0 to PadVoice(PadVoice.KICK_STUDIO, mapOf(PadVoice.TUNE to com.pocketstudio.model.StudioDrums.kickHz(5), PadVoice.DECAY to 0.18f, PadVoice.CLICK to 0.7f)))), 0))
        measure("kick_classic", kick.copy(kitVersion = Project.KIT_V1))
        // Snares: 2 and 4.
        val snare = drums(blank, 2, "snare")
        measure("snare_caisse", snare)
        measure("snare_caisse_punch", strip(snare, 2))
        val snare2 = drums(blank, 3, "snare")
        measure("snare_caisse2_punch", strip(snare2, 3, from = 2))
        measure("snare_classic", snare.copy(kitVersion = Project.KIT_V1))
        // Basses: the target's line on the bass track, each sound of the library a DnB producer would try.
        val withBass = TutoChecks.show(TutoStep("b", "", check = "notes:bass"), blank, target)!!
        val bassId = TutoChecks.trackFor(withBass, "bass")!!
        for (name in listOf("Basse growl", "Reese DnB", "Basse reese", "Reese moderne", "Basse neuro", "Basse wobble", "Basse qui parle")) {
            val sound = SoundLibrary.SYNTH.first { it.name == name }.sound
            val q = withBass.withTrack(withBass.track(bassId).copy(synth = sound))
            val tag = name.lowercase().replace(' ', '_')
            measure("bass_$tag", q)
            val ch = InstrumentTrack.mixerChannel(bassId)
            for (fx in listOf("growl", "reese_dnb")) {
                val preset = FxPresets.byId(fx)!!
                measure("bass_${tag}+$fx", q.copy(mixer = q.mixer.copy(channels = q.mixer.channels.mapIndexed { i, c -> if (i == ch) c.withFx(preset) else c })))
            }
        }
        File(dir, "report.txt").writeText(report.toString())
        println(report)
    }

    private fun writeWav(f: File, pcm: FloatArray) {
        val data = ByteBuffer.allocate(pcm.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (v in pcm) data.putShort((v.coerceIn(-1f, 1f) * 32767).toInt().toShort())
        val rate = OfflineExport.SAMPLE_RATE
        val h = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        h.put("RIFF".toByteArray()).putInt(36 + pcm.size * 2).put("WAVE".toByteArray()).put("fmt ".toByteArray())
            .putInt(16).putShort(1).putShort(2).putInt(rate).putInt(rate * 4).putShort(4).putShort(16)
            .put("data".toByteArray()).putInt(pcm.size * 2)
        f.writeBytes(h.array() + data.array())
    }
}
