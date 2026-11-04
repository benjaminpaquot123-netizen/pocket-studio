package com.pocketstudio.app.human

import android.content.Context
import android.media.AudioFormat
import android.media.MediaFormat
import android.net.Uri
import org.robolectric.shadows.ShadowMediaCodec
import org.robolectric.shadows.ShadowMediaExtractor
import org.robolectric.shadows.util.DataSource
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.sin

/**
 * The downloaded packs' FLAC sounds as a phone reads them (audit ultra tour 0, seat a: « Des sons du pack… n'ont
 * pas pu être lus : ton morceau prend les sons intégrés »): Robolectric has no FLAC decoder, so every pack sound
 * was unreadable on the bench. Each FLAC of [dir] (read from its STREAMINFO: rate, channels, length) is given to
 * Android's extractor as a soft tone of its length and channels, decoded by a pass-through codec; a file that is
 * no FLAC stays unreadable, as on a phone. Bench only.
 */
internal class BenchFlac(private val context: Context, private val dir: File) {
    private val known = HashSet<String>()
    private var decoder = false

    /** The FLACs that came since the last look (a pack just downloaded). */
    fun register() {
        val files = dir.listFiles()?.filter { it.isDirectory }?.flatMap { d -> d.listFiles()?.filter { it.name.endsWith(".flac") }.orEmpty() }.orEmpty()
        for (f in files) {
            val key = f.path + ":" + f.length() + ":" + f.lastModified()
            if (!known.add(key)) continue
            val info = streamInfo(f) ?: continue
            if (!decoder) {
                ShadowMediaCodec.addDecoder(MediaFormat.MIMETYPE_AUDIO_FLAC, ShadowMediaCodec.CodecConfig(64 * 1024, 64 * 1024, passThrough))
                decoder = true
            }
            val frames = minOf(info.frames, info.rate * MAX_SECONDS).toInt().coerceAtLeast(1)
            val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_FLAC, info.rate.toInt(), info.channels).apply {
                setLong(MediaFormat.KEY_DURATION, frames * 1_000_000L / info.rate)
                setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_8BIT)
            }
            // 8 bits a sample (unsigned): the tone's shape matters to no one, the memory a pack's 40 sounds take does.
            val data = ByteArray(frames * info.channels) { i ->
                val t = i / info.channels
                (128 + 40 * sin(t * 2.0 * Math.PI * 220.0 / info.rate) * (1.0 - t.toDouble() / frames)).toInt().toByte()
            }
            ShadowMediaExtractor.addTrack(DataSource.toDataSource(context, Uri.fromFile(f), null), format, data)
        }
    }

    private class Info(val rate: Long, val channels: Int, val frames: Long)

    /** « fLaC », then its STREAMINFO block: 20 bits of rate, 3 of channels − 1, 5 of bits − 1, 36 of frames. */
    private fun streamInfo(f: File): Info? = runCatching {
        RandomAccessFile(f, "r").use { r ->
            val head = ByteArray(26)
            r.readFully(head)
            if (String(head, 0, 4, Charsets.US_ASCII) != "fLaC" || (head[4].toInt() and 0x7F) != 0) return null
            var v = 0L
            for (k in 18 until 26) v = (v shl 8) or (head[k].toLong() and 0xFF)
            val rate = (v ushr 44) and 0xFFFFF
            val channels = ((v ushr 41) and 7).toInt() + 1
            val frames = v and 0xFFFFFFFFFL
            if (rate !in 4_000..768_000 || frames <= 0) null else Info(rate, channels, frames)
        }
    }.getOrNull()

    private companion object {
        /** A pack's loop lasts a few bars: what is past this is not given (memory). */
        const val MAX_SECONDS = 16L

        val passThrough = ShadowMediaCodec.CodecConfig.Codec { input, output ->
            val n = minOf(input.remaining(), output.remaining())
            output.put(input.duplicate().also { it.limit(it.position() + n) })
            input.position(input.position() + n)
        }
    }
}
