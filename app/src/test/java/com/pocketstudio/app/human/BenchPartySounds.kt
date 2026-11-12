package com.pocketstudio.app.human

import android.content.Context
import android.media.AudioFormat
import android.media.MediaFormat
import android.net.Uri
import com.pocketstudio.model.PartySounds
import org.robolectric.shadows.ShadowMediaCodec
import org.robolectric.shadows.ShadowMediaExtractor
import org.robolectric.shadows.util.DataSource
import java.io.File
import kotlin.math.sin

/**
 * The party sounds (klaxon, laser, siren…) as a phone reads them. Audit method v2: with the bench's engine always there,
 * Pad Live's ready-to-play set is the app's own, and its party pads came out empty on the bench: they are OGG files the
 * app copies to `cache/party/` then decodes, and Robolectric has no Vorbis decoder (a phone has). Each one is put where
 * the app copies it and given to Android's extractor as a short soft tone, decoded by a pass-through codec (as
 * [BenchFlac] does for the packs). Bench only.
 */
internal class BenchPartySounds(private val context: Context) {
    private var done = false

    fun register() {
        if (done) return
        done = true
        ShadowMediaCodec.addDecoder(MediaFormat.MIMETYPE_AUDIO_VORBIS, ShadowMediaCodec.CodecConfig(64 * 1024, 64 * 1024, passThrough))
        val dir = File(context.cacheDir, "party").apply { mkdirs() }
        for (sound in PartySounds.ALL) {
            val file = File(dir, "${sound.id}.ogg")
            // The app copies the asset only when the file is missing: the bench's copy is the asset itself.
            if (!file.isFile || file.length() == 0L) {
                runCatching { context.assets.open(sound.asset).use { i -> file.outputStream().use { i.copyTo(it) } } }
            }
            if (!file.isFile) continue
            val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_VORBIS, RATE, 2).apply {
                setLong(MediaFormat.KEY_DURATION, FRAMES * 1_000_000L / RATE)
                setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_8BIT)
            }
            val data = ByteArray(FRAMES * 2) { i ->
                val t = i / 2
                (128 + 40 * sin(t * 2.0 * Math.PI * 440.0 / RATE) * (1.0 - t.toDouble() / FRAMES)).toInt().toByte()
            }
            ShadowMediaExtractor.addTrack(DataSource.toDataSource(context, Uri.fromFile(file), null), format, data)
        }
    }

    private companion object {
        const val RATE = 48_000
        const val FRAMES = 24_000

        val passThrough = ShadowMediaCodec.CodecConfig.Codec { input, output ->
            val n = minOf(input.remaining(), output.remaining())
            output.put(input.duplicate().also { it.limit(it.position() + n) })
            input.position(input.position() + n)
        }
    }
}
