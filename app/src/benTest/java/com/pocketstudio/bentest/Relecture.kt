package com.pocketstudio.bentest

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.pocketstudio.app.audio.AudioDecoder
import com.pocketstudio.app.export.TruePeakMeter
import java.io.File

/**
 * An exported file read back as a listener's player reads it: its length, its true peak (dBTP),
 * and for a video its picture's size (9:16 standing). WAV by the app's own reader, the others
 * decoded by Android (MediaCodec) into a scratch file then measured.
 */
object Relecture {
    data class Relu(val secondes: Double, val creteDbtp: Double, val largeur: Int = 0, val hauteur: Int = 0) {
        val portrait916: Boolean get() = largeur > 0 && kotlin.math.abs(hauteur.toDouble() / largeur - 16.0 / 9.0) < 0.02
    }

    /** A WAV's length and true peak. */
    fun wav(file: File): Relu = TruePeakMeter.ofWav(file).let { (peak, seconds) -> Relu(seconds, peak) }

    /** Any exported file (wav, m4a, flac, mp4); null: could not be read. [scratch]: a folder for the decoded sound. */
    fun lire(context: Context, file: File, scratch: File): Relu? = runCatching {
        if (file.extension.lowercase() == "wav") return@runCatching wav(file)
        scratch.mkdirs()
        val raw = File(scratch, "relu.raw")
        try {
            val audio = AudioDecoder.decodeToFile(context, Uri.fromFile(file), raw, maxSeconds = 1_800.0, isCancelled = { false })
            val peak = TruePeakMeter.ofRawFloats(audio.file, audio.channels)
            var w = 0
            var h = 0
            if (file.extension.lowercase() == "mp4") {
                val r = MediaMetadataRetriever()
                try {
                    r.setDataSource(file.path)
                    w = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                    h = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                    val rot = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                    if (rot == 90 || rot == 270) { val t = w; w = h; h = t }
                } finally {
                    r.release()
                }
            }
            Relu(audio.frames.toDouble() / audio.sampleRate, peak, w, h)
        } finally {
            raw.delete()
        }
    }.getOrNull()
}
