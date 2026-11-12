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
    override fun ecouter(ms: Long, recevoir: (FloatArray, Int) -> Unit) {
        check(autorise()) { "micro refusé" }
        val min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT)
        val rec = AudioRecord(source(), rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT, maxOf(min, rate * 4 / 5 * 4))
        try {
            check(rec.state == AudioRecord.STATE_INITIALIZED) { "micro indisponible" }
            val buf = FloatArray(BLOC)
            var left = ms * rate / 1000
            var empty = 0
            rec.startRecording()
            while (left > 0) {
                val n = rec.read(buf, 0, minOf(buf.size.toLong(), left).toInt(), AudioRecord.READ_BLOCKING)
                check(n >= 0) { "micro : lecture refusée ($n)" }
                if (n == 0) { check(++empty < 200) { "micro muet" }; continue }
                recevoir(buf, n)
                left -= n
            }
        } finally {
            runCatching { rec.stop() }
            rec.release()
        }
    }

    private companion object {
        const val BLOC = 960
    }
}

/** The engine's preview player for the sweep (the Guide's « Écouter » plays its sounds the same way), and the music volume. */
class HautParleurReel(private val context: Context) : HautParleur {
    private var stems: NativeStems? = null
    private var volumeAvant: Int? = null

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

    override fun reglerVolume() {
        val audio = context.getSystemService(AudioManager::class.java) ?: return
        if (volumeAvant == null) volumeAvant = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, (max * VOLUME).roundToInt(), 0)
    }

    override fun remettreVolume() {
        val audio = context.getSystemService(AudioManager::class.java) ?: return
        volumeAvant?.let { audio.setStreamVolume(AudioManager.STREAM_MUSIC, it, 0) }
        volumeAvant = null
    }

    private companion object {
        /** 60 % of the music volume: loud enough over a quiet room, under the speaker's own limiter. */
        const val VOLUME = 0.6
    }
}
