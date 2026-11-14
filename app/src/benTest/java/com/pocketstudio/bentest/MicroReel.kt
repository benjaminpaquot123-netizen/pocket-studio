package com.pocketstudio.bentest

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.engine.NativeStems
import kotlin.math.roundToInt

/**
 * The phone's microphone for « Écoute au micro » (T1b): 48 kHz mono float, the « unprocessed » source when the phone
 * has one (no automatic gain nor noise suppression to colour the bands), else the plain microphone. The blocks go
 * straight to the analysis: nothing is written here.
 */
class MicroReel(private val context: Context) : Micro {
    override val rate = 48_000

    override fun autorise() = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    /** The source used (the report says it). */
    fun source(): Int {
        val audio = context.getSystemService(AudioManager::class.java)
        val raw = runCatching { audio?.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true" }.getOrDefault(false)
        return if (raw) MediaRecorder.AudioSource.UNPROCESSED else MediaRecorder.AudioSource.MIC
    }

    @SuppressLint("MissingPermission")
    override fun ecouter(ms: Long, avant: () -> Unit, recevoir: (FloatArray, Int) -> Unit) {
        check(autorise()) { "micro refusé" }
        // 2 s of room: [avant] (the engine starting the sweep) may take a moment before the first read.
        val rec = ouvrir(source()) ?: ouvrir(MediaRecorder.AudioSource.MIC) ?: error("micro indisponible")
        try {
            val buf = FloatArray(BLOC)
            var left = ms * rate / 1000
            var empty = 0
            val until = System.currentTimeMillis() + ms + MARGE_MS
            rec.startRecording()
            avant()
            while (left > 0) {
                check(System.currentTimeMillis() < until) { "micro : l'écoute ne finit pas" }
                val n = rec.read(buf, 0, minOf(buf.size.toLong(), left).toInt(), AudioRecord.READ_BLOCKING)
                check(n >= 0) { "micro : lecture refusée ($n)" }
                if (n == 0) { check(++empty < 200) { "micro muet" }; continue }
                empty = 0
                recevoir(buf, n)
                left -= n
            }
        } finally {
            runCatching { rec.stop() }
            rec.release()
        }
    }

    /** A recorder on [source], null when the phone refuses it (the « unprocessed » one announced but not given). */
    @SuppressLint("MissingPermission")
    private fun ouvrir(source: Int): AudioRecord? {
        val min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT)
        val rec = runCatching { AudioRecord(source, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT, maxOf(min, rate * 4 * 2)) }.getOrNull() ?: return null
        if (rec.state == AudioRecord.STATE_INITIALIZED) return rec
        rec.release()
        return null
    }

    private companion object {
        const val BLOC = 960
        /** The listening may last this much longer than asked (the system's own start) before it is called stuck. */
        const val MARGE_MS = 5_000L
    }
}

/** The engine's preview player for the sweep (the Guide's « Écouter » plays its sounds the same way), and the music volume. */
class HautParleurReel(private val context: Context) : HautParleur {
    private var stems: NativeStems? = null

    /** Benjamin's own volume, kept on disk until put back: BenStudio Test killed in a sound, the next run puts back his, not its 60 %. */
    private val volumeFile = java.io.File(java.io.File(context.filesDir, "bentest"), "volume.txt")

    override fun jouer(pcm: FloatArray) {
        arreter()
        check(NativeEngine.isAvailable) { "moteur absent" }
        val stereo = FloatArray(pcm.size * 2) { pcm[it / 2] }
        val s = NativeStems.fromPcm(stereo) ?: error("le moteur refuse le balayage")
        if (!NativeEngine.setPreviewStems(s)) { s.close(); error("le moteur refuse le balayage") }
        stems = s
        NativeEngine.playPreview(true)
        val until = System.currentTimeMillis() + 3_000
        while (NativeEngine.previewPositionNow() < 0) {
            check(System.currentTimeMillis() < until) { "le moteur ne joue pas le balayage" }
            Thread.sleep(10)
        }
    }

    override fun arreter() {
        val s = stems ?: return
        stems = null
        runCatching {
            NativeEngine.playPreview(false)
            NativeEngine.setPreviewStems(null)
        }
        s.close()
    }

    /** The engine's own count of sixteenths since ▶ (-1 stopped): where the song is when the microphone starts. */
    override fun pasJoues(): Int = if (NativeEngine.isAvailable) NativeEngine.absoluteStep() else -1

    override fun reglerVolume() {
        val audio = context.getSystemService(AudioManager::class.java) ?: return
        if (!volumeFile.isFile) runCatching { volumeFile.parentFile?.mkdirs(); volumeFile.writeText(audio.getStreamVolume(AudioManager.STREAM_MUSIC).toString()) }
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, (max * VOLUME).roundToInt(), 0)
    }

    override fun remettreVolume() {
        val audio = context.getSystemService(AudioManager::class.java) ?: return
        runCatching { volumeFile.readText().trim().toInt() }.getOrNull()?.let { audio.setStreamVolume(AudioManager.STREAM_MUSIC, it, 0) }
        volumeFile.delete()
    }

    private companion object {
        /** 60 % of the music volume: loud enough over a quiet room, under the speaker's own limiter. */
        const val VOLUME = 0.6
    }
}
