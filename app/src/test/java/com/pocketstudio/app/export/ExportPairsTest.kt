package com.pocketstudio.app.export

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Lot E (« Master pro »): the export matrix's app side and the files' tags.
 * - [encodesAndTagsTheMatrix]: scripts/export-matrix.sh's cases (`-Pbench.out=<matrix folder>` holding
 *   cases.tsv), each rendered by the engine core, encoded and tagged here as the app does it
 *   ([FlacEncoder], [Metadata]); ffmpeg then checks them (scripts/bench/matrix_check.py).
 * - The tags of a WAV, a FLAC and an M4A (its moov before or after the audio) read back by ffprobe, the
 *   audio itself unchanged by the tagging (skipped without ffmpeg).
 */
class ExportPairsTest {
    private val tags = SongTags(title = "Écoute « test »", artist = "BenStudio", bpm = 128.0, key = "Am", cover = PNG)

    @Test
    fun encodesAndTagsTheMatrix() {
        val dir = System.getProperty("ps.bench.out")?.takeIf { it.isNotBlank() }?.let(::File)
        assumeTrue("export matrix: run scripts/export-matrix.sh", dir != null && File(dir, "cases.tsv").exists())
        val lines = File(dir, "cases.tsv").readLines().drop(1).filter { it.isNotBlank() }
        var done = 0
        for (line in lines) {
            val (id, style, format) = line.split('\t')
            val cases = File(dir, "cases")
            val bpm = File(dir, "cmds/$style.pscmd").useLines { seq ->
                seq.firstOrNull { it.startsWith("SONG ") }?.let { Regex("bpm=([0-9.]+)").find(it)?.groupValues?.get(1)?.toDouble() }
            }
            val caseTags = SongTags(title = "Matrice $id", artist = "BenStudio", bpm = bpm, key = "Am", cover = PNG)
            val file = when (format) {
                "flac" -> {
                    val wav = File(cases, "$id.wav")
                    if (!wav.exists()) continue
                    File(cases, "$id.flac").also {
                        FlacEncoder.encode(wav, it)
                        wav.delete()
                    }
                }
                else -> File(cases, "$id.$format")
            }
            if (!file.exists()) continue
            Metadata.tag(file, caseTags)
            done++
        }
        println("matrix: $done files encoded and tagged")
        assertTrue(done > 0)
    }

    private fun ffprobeTags(file: File): Map<String, String> {
        val p = ProcessBuilder("ffprobe", "-v", "error", "-show_entries", "format_tags:stream_tags", "-of", "default=nw=1", file.path)
            .redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText()
        p.waitFor()
        return out.lines().mapNotNull { l -> l.substringAfter("TAG:", "").takeIf { it.contains('=') } }
            .associate { it.substringBefore('=').lowercase() to it.substringAfter('=') }
    }

    private fun decoded(file: File): ByteArray {
        val p = ProcessBuilder("ffmpeg", "-v", "error", "-i", file.path, "-map", "0:a", "-f", "s16le", "-").start()
        val bytes = p.inputStream.readBytes()
        p.waitFor()
        return bytes
    }

    private fun hasFfmpeg(): Boolean = runCatching {
        ProcessBuilder("ffprobe", "-version").start().waitFor() == 0 && ProcessBuilder("ffmpeg", "-version").start().waitFor() == 0
    }.getOrDefault(false)

    @Test
    fun wavTagsAreReadBackAndTheAudioStays() {
        val dir = createTempDir()
        val wav = sineWav(File(dir, "a.wav"), 16)
        val before = wav.readBytes()
        Metadata.tag(wav, tags)
        Metadata.tag(wav, tags.copy(title = "Deux fois"))  // tagged again: one set of tags
        val after = wav.readBytes()
        // The audio and its header untouched (only the RIFF size moved), the tags after it.
        assertArrayEquals(before.copyOfRange(8, before.size), after.copyOfRange(8, before.size))
        assertEquals(after.size - 8, ByteBuffer.wrap(after, 4, 4).order(ByteOrder.LITTLE_ENDIAN).int)
        WavPcm.open(wav).use { assertEquals(48000L, it.framesLeft) }
        assumeTrue("ffmpeg", hasFfmpeg())
        val t = ffprobeTags(wav)
        assertEquals("Deux fois", t["title"])
        assertEquals("BenStudio", t["artist"])
        assertEquals("128", t["tbpm"] ?: t["bpm"])
        assertEquals(decoded(sineWav(File(dir, "b.wav"), 16)).size, decoded(wav).size)
        dir.deleteRecursively()
    }

    @Test
    fun flacTagsAreReadBackAndTheAudioStays() {
        val dir = createTempDir()
        val wav = sineWav(File(dir, "a.wav"), 24)
        val flac = File(dir, "a.flac")
        FlacEncoder.encode(wav, flac)
        Metadata.tag(flac, tags)
        Metadata.tag(flac, tags)
        assumeTrue("ffmpeg", hasFfmpeg())
        val t = ffprobeTags(flac)
        assertEquals(tags.title, t["title"])
        assertEquals("BenStudio", t["artist"])
        assertEquals("128", t["bpm"])
        assertEquals("Am", t["initialkey"])
        assertArrayEquals(decoded(wav), decoded(flac))
        dir.deleteRecursively()
    }

    @Test
    fun m4aTagsMoveTheChunkOffsetsWithTheMoov() {
        assumeTrue("ffmpeg", hasFfmpeg())
        val dir = createTempDir()
        val wav = sineWav(File(dir, "a.wav"), 16)
        for (fast in listOf(true, false)) {
            val m4a = File(dir, "a-$fast.m4a")
            val args = mutableListOf("ffmpeg", "-v", "error", "-y", "-i", wav.path, "-c:a", "aac", "-b:a", "128k")
            if (fast) args += listOf("-movflags", "+faststart")  // the moov before the audio: its offsets must move
            args += m4a.path
            assertEquals(0, ProcessBuilder(args).start().waitFor())
            val before = decoded(m4a)
            Metadata.tag(m4a, tags)
            Metadata.tag(m4a, tags.copy(title = "Encore"))
            val t = ffprobeTags(m4a)
            assertEquals("moov first: $fast", "Encore", t["title"])
            assertEquals("BenStudio", t["artist"])
            assertEquals("128", t["tmpo"] ?: t["bpm"])
            assertArrayEquals("moov first: $fast", before, decoded(m4a))
        }
        dir.deleteRecursively()
    }

    @Test
    fun keyIsWrittenAsStoresReadIt() {
        assertEquals("Am", SongTags.keyOf(com.pocketstudio.model.Harmony(root = 9, scale = com.pocketstudio.model.Scale.Minor)))
        assertEquals("F#", SongTags.keyOf(com.pocketstudio.model.Harmony(root = 6, scale = com.pocketstudio.model.Scale.Major)))
        assertEquals("Ebm", SongTags.keyOf(com.pocketstudio.model.Harmony(root = 3, scale = com.pocketstudio.model.Scale.MinorPentatonic)))
        assertEquals(null, SongTags.keyOf(com.pocketstudio.model.Harmony()))
    }

    private fun createTempDir(): File = kotlin.io.path.createTempDirectory("ps-tags").toFile()

    /** One second of a 440 Hz sine, stereo, 48 kHz, in [bits]. */
    private fun sineWav(file: File, bits: Int): File {
        val frames = 48000
        val bytes = bits / 8
        val b = ByteBuffer.allocate(44 + frames * 2 * bytes).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray()).putInt(36 + frames * 2 * bytes).put("WAVEfmt ".toByteArray()).putInt(16).putShort(1).putShort(2)
            .putInt(48000).putInt(48000 * 2 * bytes).putShort((2 * bytes).toShort()).putShort(bits.toShort()).put("data".toByteArray()).putInt(frames * 2 * bytes)
        repeat(frames) { i ->
            val v = 0.3 * sin(2 * PI * 440.0 * i / 48000)
            repeat(2) {
                if (bits == 16) b.putShort((v * 32767).roundToInt().toShort())
                else {
                    val q = (v * 8388607).roundToInt()
                    b.put(q.toByte()).put((q shr 8).toByte()).put((q shr 16).toByte())
                }
            }
        }
        file.writeBytes(b.array())
        return file
    }

    companion object {
        /** A 1x1 PNG: a cover. */
        val PNG: ByteArray = java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==")
    }
}
