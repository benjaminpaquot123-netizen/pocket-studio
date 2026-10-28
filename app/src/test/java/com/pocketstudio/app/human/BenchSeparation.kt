package com.pocketstudio.app.human

import android.content.Context
import android.net.Uri
import com.pocketstudio.app.audio.AudioDecoder
import com.pocketstudio.app.audio.FloatWav
import com.pocketstudio.app.download.VerifiedFile
import com.pocketstudio.app.remix.SeparationModel
import com.pocketstudio.app.remix.SeparationService
import com.pocketstudio.app.remix.SongInfo
import com.pocketstudio.separation.Demucs
import com.pocketstudio.separation.SeparationResult
import com.sun.net.httpserver.HttpServer
import java.io.Closeable
import java.io.DataInputStream
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

/**
 * The Remix as a phone does it, without the model nor native code (« testeur humain » bench):
 * the separation service runs for real (its states, its notification, its files), with this
 * stand-in for the heavy work ([SeparationService.Work], the service's own seam). The song is
 * read from the file the picker gave (a WAV), the separation takes a few seconds with its
 * progress, and gives back the four parts of [BenchSong] (the « Voix » stem is the sung voice,
 * clean); any other song lands whole in « Autres ». The model's download (174 MB) comes from a
 * local server, at the size and with the progress a phone sees, in about six seconds. Bench only.
 */
internal class BenchSeparation(
    private val context: Context,
    /** 1: the times a tester sees (a 6 s download, a 20 s separation); less in the bench's own tests. */
    private val timeScale: Double = 1.0,
) : SeparationService.Work, Closeable {
    private val server: HttpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    private val realSpec = SeparationModel.SPEC
    private val realWork = SeparationService.work

    /** Songs separated (tests). */
    @Volatile var separated = 0
        private set

    init {
        val size = realSpec.sizeBytes
        server.createContext("/htdemucs.onnx") { ex ->
            val from = ex.requestHeaders.getFirst("Range")?.removePrefix("bytes=")?.removeSuffix("-")?.toLongOrNull() ?: 0L
            runCatching {
                ex.sendResponseHeaders(if (from > 0) 206 else 200, size - from)
                ex.responseBody.use { out ->
                    val chunk = ByteArray(CHUNK)
                    var left = size - from
                    while (left > 0) {
                        val n = minOf(left, CHUNK.toLong()).toInt()
                        out.write(chunk, 0, n)
                        left -= n
                        Thread.sleep((DOWNLOAD_MS * timeScale * CHUNK / size).toLong())
                    }
                }
            }
            ex.close()
        }
        server.start()
        SeparationModel.SPEC = VerifiedFile("http://127.0.0.1:${server.address.port}/htdemucs.onnx", realSpec.fileName, zerosSha256(size), size)
        SeparationService.work = this
    }

    override fun close() {
        SeparationService.work = realWork
        SeparationModel.SPEC = realSpec
        server.stop(0)
    }

    override val available = true

    /** The picked file, read as the phone's decoder would (a WAV here): raw floats at its own rate. */
    override fun decode(context: Context, uri: Uri, target: File, maxSeconds: Double, isCancelled: () -> Boolean): AudioDecoder.RawAudio {
        val input = context.contentResolver.openInputStream(uri) ?: throw IOException("fichier introuvable")
        val (channels, rate, pcm) = DataInputStream(input.buffered(1 shl 16)).use { readWav(it) }
        if (pcm.size / channels > maxSeconds * rate) throw AudioDecoder.TooLongException(pcm.size.toDouble() / channels / rate)
        target.parentFile?.mkdirs()
        val bytes = ByteBuffer.allocate(pcm.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        for (v in pcm) bytes.putFloat(v)
        target.writeBytes(bytes.array())
        Thread.sleep((800 * timeScale).toLong())
        if (isCancelled()) throw AudioDecoder.CancelledException()
        return AudioDecoder.RawAudio(target, channels, rate, (pcm.size / channels).toLong())
    }

    override fun separate(model: File, raw: AudioDecoder.RawAudio, outDir: File, progress: Demucs.Progress): Pair<SeparationResult, String> {
        for (step in 1..STEPS) {
            Thread.sleep((SEPARATE_MS * timeScale / STEPS).toLong())
            if (!progress.onProgress(step.toFloat() / STEPS)) return SeparationResult.Cancelled to ""
        }
        outDir.mkdirs()
        val ours = raw.frames == BenchSong.frames(raw.sampleRate).toLong()
        Demucs.STEMS.forEachIndexed { i, stem ->
            val data = when {
                ours -> BenchSong.part(BenchSong.Part.entries[i], Demucs.SAMPLE_RATE)
                stem == "other" -> rawStereo(raw)
                else -> FloatArray(2 * Demucs.SAMPLE_RATE)
            }
            val rate = if (ours || stem != "other") Demucs.SAMPLE_RATE else raw.sampleRate
            FloatWav.write(File(outDir, "$stem.wav"), data, 0, data.size / 2, 2, rate)
        }
        separated++
        return SeparationResult.Ok to ""
    }

    override fun analyse(dir: File): SongInfo {
        Thread.sleep((600 * timeScale).toLong())
        val ours = runCatching { FloatWav.read(File(dir, "vocals.wav")).data.any { it != 0f } }.getOrDefault(false)
        return if (ours) SongInfo(bpm = BenchSong.BPM, firstBeat = 0.0, root = BenchSong.ROOT, minor = true) else SongInfo()
    }

    /** A phone like the owner's (8 GB), apps closed. */
    override fun availableMemory(context: Context) = 4_200_000_000L

    private fun rawStereo(raw: AudioDecoder.RawAudio): FloatArray {
        val b = ByteBuffer.wrap(raw.file.readBytes()).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
        val mono = raw.channels == 1
        return FloatArray(2 * raw.frames.toInt()) { k -> if (mono) b.get(k / 2) else b.get(k) }
    }

    companion object {
        private const val CHUNK = 1 shl 20
        private const val DOWNLOAD_MS = 6_000L
        private const val SEPARATE_MS = 20_000L
        private const val STEPS = 20

        private val shaCache = HashMap<Long, String>()

        /** The model stand-in is [size] zero bytes: its SHA-256, worked out once. */
        @Synchronized
        fun zerosSha256(size: Long): String = shaCache.getOrPut(size) {
            val digest = MessageDigest.getInstance("SHA-256")
            val zeros = ByteArray(CHUNK)
            var left = size
            while (left > 0) {
                val n = minOf(left, CHUNK.toLong()).toInt()
                digest.update(zeros, 0, n)
                left -= n
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }

        /** A plain WAV (16-bit or float, mono or stereo): (channels, rate, interleaved samples). */
        fun readWav(input: DataInputStream): Triple<Int, Int, FloatArray> {
            val head = ByteArray(12).also { input.readFully(it) }
            if (String(head, 0, 4) != "RIFF" || String(head, 8, 4) != "WAVE") throw IOException("pas un WAV")
            var channels = 0
            var rate = 0
            var format = 0
            var bits = 0
            val chunk = ByteArray(8)
            while (true) {
                input.readFully(chunk)
                val id = String(chunk, 0, 4)
                val size = ByteBuffer.wrap(chunk, 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
                val body = ByteArray(size + (size and 1)).also { input.readFully(it) }
                val b = ByteBuffer.wrap(body).order(ByteOrder.LITTLE_ENDIAN)
                when (id) {
                    "fmt " -> {
                        format = b.getShort(0).toInt()
                        channels = b.getShort(2).toInt()
                        rate = b.getInt(4)
                        bits = b.getShort(14).toInt()
                    }
                    "data" -> {
                        if (channels !in 1..2 || rate <= 0) throw IOException("WAV illisible")
                        return when {
                            format == 1 && bits == 16 -> Triple(channels, rate, FloatArray(size / 2) { b.getShort(2 * it) / 32768f })
                            format == 3 && bits == 32 -> Triple(channels, rate, FloatArray(size / 4) { b.getFloat(4 * it) })
                            else -> throw IOException("WAV non pris en charge")
                        }
                    }
                }
            }
        }
    }
}
