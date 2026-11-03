package com.pocketstudio.app.export

import com.pocketstudio.model.Harmony
import com.pocketstudio.model.Scale
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Lot E (« Master pro »): what the exported file says of itself (title, artist, BPM, key, cover),
 * as players and stores read it. Written after the file is complete, never during the render:
 * - WAV: a LIST/INFO chunk (INAM, IART, ICMT) and an « id3 » chunk (ID3v2.4: TIT2, TPE1, TBPM, TKEY,
 *   APIC), after the audio, the RIFF size patched;
 * - FLAC: VORBIS_COMMENT (TITLE, ARTIST, BPM, INITIALKEY) and PICTURE blocks after STREAMINFO;
 * - M4A: moov › udta › meta › ilst (©nam, ©ART, tmpo, ----:com.apple.iTunes:initialkey, covr), the
 *   chunk offsets (stco/co64) moved when the moov comes before the audio.
 * Blocking, worker thread. Throws IOException on a file it cannot read.
 */
data class SongTags(
    val title: String = "",
    val artist: String = "",
    val bpm: Double? = null,
    /** « Am », « F#m », « Eb »: the notation every store reads (ASCII, English letters). */
    val key: String? = null,
    /** A JPEG or PNG cover (null: none). */
    val cover: ByteArray? = null,
) {
    val isEmpty: Boolean get() = title.isBlank() && artist.isBlank() && bpm == null && key == null && cover == null
    val coverMime: String? get() = cover?.let { if (it.size > 3 && it[0] == 0x89.toByte() && it[1] == 'P'.code.toByte()) "image/png" else "image/jpeg" }

    override fun equals(other: Any?): Boolean = other is SongTags && title == other.title && artist == other.artist && bpm == other.bpm &&
        key == other.key && (cover?.contentEquals(other.cover ?: ByteArray(0)) ?: (other.cover == null))
    override fun hashCode(): Int = listOf(title, artist, bpm, key, cover?.contentHashCode()).hashCode()

    companion object {
        private val NAMES = listOf("C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B")

        /** The project's key as stores write it (« Am »), or null when no scale was chosen. */
        fun keyOf(h: Harmony): String? {
            if (h.scale == Scale.Off) return null
            val minor = h.scale.parent != Scale.Major && 3 in h.scale.intervals
            return NAMES[Math.floorMod(h.root, 12)] + if (minor) "m" else ""
        }
    }
}

object Metadata {
    private const val SOFTWARE = "BenStudio"

    /** Tags [file] by its extension (wav, flac, m4a); nothing to say: left as it is. */
    fun tag(file: File, tags: SongTags) {
        if (tags.isEmpty) return
        when (file.extension.lowercase()) {
            "wav" -> tagWav(file, tags)
            "flac" -> tagFlac(file, tags)
            "m4a", "mp4" -> tagM4a(file, tags)
        }
    }

    private fun bpmText(bpm: Double): String = if (bpm == Math.rint(bpm)) bpm.toLong().toString() else String.format(java.util.Locale.ROOT, "%.2f", bpm)

    // --- WAV ----------------------------------------------------------------------------------

    fun tagWav(file: File, tags: SongTags) {
        RandomAccessFile(file, "rw").use { f ->
            val head = ByteArray(12)
            f.readFully(head)
            if (String(head, 0, 4) != "RIFF" || String(head, 8, 4) != "WAVE") throw IOException("not a WAV")
            // Drops a LIST/id3 written before (tagging twice gives one set), keeps everything else.
            var pos = 12L
            val length = f.length()
            var firstTag = -1L  // where a run of LIST/id3 chunks up to the end starts
            while (pos + 8 <= length) {
                f.seek(pos)
                val tag = ByteArray(8)
                f.readFully(tag)
                val id = String(tag, 0, 4)
                val size = le32(tag, 4)
                val isTag = id == "LIST" || id == "id3 "
                if (isTag && firstTag < 0) firstTag = pos
                if (!isTag) firstTag = -1
                pos += 8 + size + (size and 1)
            }
            val end = if (firstTag >= 0) firstTag else length
            f.setLength(end)
            f.seek(end)
            val out = ByteArrayOutputStream()
            if (end % 2 == 1L) out.write(0)
            out.write(chunk("LIST", infoList(tags)))
            out.write(chunk("id3 ", id3(tags)))
            f.write(out.toByteArray())
            val riff = f.length() - 8
            if (riff > 0xFFFFFFFFL) throw IOException("WAV too long")
            f.seek(4)
            f.write(le32Bytes(riff))
        }
    }

    private fun infoList(t: SongTags): ByteArray {
        val b = ByteArrayOutputStream()
        b.write("INFO".toByteArray())
        fun text(id: String, s: String) {
            if (s.isBlank()) return
            val bytes = s.toByteArray(Charsets.UTF_8) + 0
            b.write(chunk(id, bytes))
        }
        text("INAM", t.title)
        text("IART", t.artist)
        text("ICMT", listOfNotNull(t.bpm?.let { "BPM ${bpmText(it)}" }, t.key?.let { "Key $it" }).joinToString(" · "))
        text("ISFT", SOFTWARE)
        return b.toByteArray()
    }

    private fun chunk(id: String, body: ByteArray): ByteArray {
        val b = ByteArrayOutputStream()
        b.write(id.toByteArray(Charsets.US_ASCII))
        b.write(le32Bytes(body.size.toLong()))
        b.write(body)
        if (body.size % 2 == 1) b.write(0)
        return b.toByteArray()
    }

    /** An ID3v2.4 tag (UTF-8 text frames, the cover as APIC « front cover »). */
    fun id3(t: SongTags): ByteArray {
        val frames = ByteArrayOutputStream()
        fun frame(id: String, body: ByteArray) {
            frames.write(id.toByteArray(Charsets.US_ASCII))
            frames.write(syncsafe(body.size))
            frames.write(byteArrayOf(0, 0))
            frames.write(body)
        }
        fun text(id: String, s: String?) {
            if (s.isNullOrBlank()) return
            frame(id, byteArrayOf(3) + s.toByteArray(Charsets.UTF_8))
        }
        text("TIT2", t.title)
        text("TPE1", t.artist)
        text("TBPM", t.bpm?.let { Math.round(it).toString() })
        text("TKEY", t.key)
        text("TSSE", SOFTWARE)
        t.cover?.let { c -> frame("APIC", byteArrayOf(0) + t.coverMime!!.toByteArray(Charsets.US_ASCII) + byteArrayOf(0, 3, 0) + c) }
        val body = frames.toByteArray()
        return "ID3".toByteArray() + byteArrayOf(4, 0, 0) + syncsafe(body.size) + body
    }

    private fun syncsafe(n: Int): ByteArray = byteArrayOf(((n shr 21) and 0x7F).toByte(), ((n shr 14) and 0x7F).toByte(), ((n shr 7) and 0x7F).toByte(), (n and 0x7F).toByte())

    // --- FLAC ---------------------------------------------------------------------------------

    fun tagFlac(file: File, tags: SongTags) {
        val tmp = File(file.parentFile, file.name + ".tag")
        try {
            file.inputStream().buffered(1 shl 16).use { input ->
                tmp.outputStream().buffered(1 shl 16).use { out ->
                    val magic = ByteArray(4)
                    if (input.read(magic) != 4 || String(magic) != "fLaC") throw IOException("not a FLAC")
                    out.write(magic)
                    // The blocks kept (STREAMINFO first, never our own kinds again), then ours, the last one flagged.
                    val kept = ArrayList<Pair<Int, ByteArray>>()
                    while (true) {
                        val h = ByteArray(4)
                        if (input.read(h) != 4) throw IOException("FLAC cut short")
                        val last = h[0].toInt() and 0x80 != 0
                        val type = h[0].toInt() and 0x7F
                        val len = ((h[1].toInt() and 0xFF) shl 16) or ((h[2].toInt() and 0xFF) shl 8) or (h[3].toInt() and 0xFF)
                        val body = ByteArray(len)
                        var got = 0
                        while (got < len) {
                            val n = input.read(body, got, len - got)
                            if (n < 0) throw IOException("FLAC cut short")
                            got += n
                        }
                        if (type != 4 && type != 6 && type != 1) kept += type to body
                        if (last) break
                    }
                    kept += 4 to vorbisComment(tags)
                    tags.cover?.let { kept += 6 to picture(it, tags.coverMime!!) }
                    kept.forEachIndexed { i, (type, body) ->
                        out.write((type or if (i == kept.lastIndex) 0x80 else 0))
                        out.write(byteArrayOf((body.size shr 16).toByte(), (body.size shr 8).toByte(), body.size.toByte()))
                        out.write(body)
                    }
                    input.copyTo(out, 1 shl 16)
                }
            }
            if (!tmp.renameTo(file)) {
                tmp.copyTo(file, overwrite = true)
            }
        } finally {
            tmp.delete()
        }
    }

    private fun vorbisComment(t: SongTags): ByteArray {
        val b = ByteArrayOutputStream()
        val vendor = SOFTWARE.toByteArray()
        b.write(le32BytesInt(vendor.size))
        b.write(vendor)
        val fields = listOfNotNull(
            t.title.takeIf { it.isNotBlank() }?.let { "TITLE=$it" },
            t.artist.takeIf { it.isNotBlank() }?.let { "ARTIST=$it" },
            t.bpm?.let { "BPM=${bpmText(it)}" },
            t.key?.let { "INITIALKEY=$it" },
        ).map { it.toByteArray(Charsets.UTF_8) }
        b.write(le32BytesInt(fields.size))
        fields.forEach {
            b.write(le32BytesInt(it.size))
            b.write(it)
        }
        return b.toByteArray()
    }

    private fun picture(data: ByteArray, mime: String): ByteArray {
        val b = ByteBuffer.allocate(32 + mime.length + data.size).order(ByteOrder.BIG_ENDIAN)
        b.putInt(3)  // front cover
        b.putInt(mime.length)
        b.put(mime.toByteArray(Charsets.US_ASCII))
        b.putInt(0)  // no description
        b.putInt(0); b.putInt(0); b.putInt(0); b.putInt(0)  // size and colours unknown
        b.putInt(data.size)
        b.put(data)
        return b.array()
    }

    // --- M4A ----------------------------------------------------------------------------------

    fun tagM4a(file: File, tags: SongTags) {
        val bytes = file.readBytes()
        val top = boxes(bytes, 0, bytes.size)
        val moov = top.firstOrNull { it.type == "moov" } ?: throw IOException("no moov")
        val mdat = top.firstOrNull { it.type == "mdat" }
        // The moov without any udta of its own, then ours.
        val inner = boxes(bytes, moov.bodyStart, moov.end).filter { it.type != "udta" }
        val body = ByteArrayOutputStream()
        inner.forEach { body.write(bytes, it.start, it.end - it.start) }
        body.write(box("udta", box("meta", byteArrayOf(0, 0, 0, 0) + hdlr() + box("ilst", ilst(tags)))))
        val newMoov = box("moov", body.toByteArray())
        // The audio after the moov moves by the size change: its chunk offsets with it.
        val delta = newMoov.size - (moov.end - moov.start)
        if (mdat != null && mdat.start > moov.start && delta != 0) shiftOffsets(newMoov, 0, newMoov.size, delta.toLong())
        val out = ByteArrayOutputStream(bytes.size + delta.coerceAtLeast(0))
        out.write(bytes, 0, moov.start)
        out.write(newMoov)
        out.write(bytes, moov.end, bytes.size - moov.end)
        val tmp = File(file.parentFile, file.name + ".tag")
        try {
            tmp.writeBytes(out.toByteArray())
            if (!tmp.renameTo(file)) tmp.copyTo(file, overwrite = true)
        } finally {
            tmp.delete()
        }
    }

    private class Box(val type: String, val start: Int, val bodyStart: Int, val end: Int)

    private fun boxes(b: ByteArray, from: Int, to: Int): List<Box> {
        val out = ArrayList<Box>()
        var p = from
        while (p + 8 <= to) {
            var size = be32(b, p)
            val type = String(b, p + 4, 4, Charsets.ISO_8859_1)
            var header = 8
            if (size == 1L) {
                size = ByteBuffer.wrap(b, p + 8, 8).long
                header = 16
            } else if (size == 0L) {
                size = (to - p).toLong()
            }
            if (size < header || p + size > to) throw IOException("bad box $type")
            out += Box(type, p, p + header, (p + size).toInt())
            p += size.toInt()
        }
        return out
    }

    private val CONTAINERS = setOf("moov", "trak", "mdia", "minf", "stbl", "edts", "dinf", "mvex")

    private fun shiftOffsets(b: ByteArray, from: Int, to: Int, delta: Long) {
        for (x in boxes(b, from, to)) {
            when (x.type) {
                in CONTAINERS -> shiftOffsets(b, x.bodyStart, x.end, delta)
                "stco" -> {
                    val n = be32(b, x.bodyStart + 4).toInt()
                    for (i in 0 until n) {
                        val at = x.bodyStart + 8 + 4 * i
                        putBe32(b, at, be32(b, at) + delta)
                    }
                }
                "co64" -> {
                    val n = be32(b, x.bodyStart + 4).toInt()
                    for (i in 0 until n) {
                        val at = x.bodyStart + 8 + 8 * i
                        val bb = ByteBuffer.wrap(b, at, 8)
                        val v = bb.long
                        ByteBuffer.wrap(b, at, 8).putLong(v + delta)
                    }
                }
            }
        }
    }

    private fun hdlr(): ByteArray = box("hdlr", ByteArray(8) + "mdirappl".toByteArray() + ByteArray(9))

    private fun ilst(t: SongTags): ByteArray {
        val b = ByteArrayOutputStream()
        fun data(kind: Int, payload: ByteArray) = box("data", be32Bytes(kind.toLong()) + ByteArray(4) + payload)
        fun item(type: String, kind: Int, payload: ByteArray) = b.write(box(type, data(kind, payload)))
        val copy = "©"
        if (t.title.isNotBlank()) item("${copy}nam", 1, t.title.toByteArray(Charsets.UTF_8))
        if (t.artist.isNotBlank()) item("${copy}ART", 1, t.artist.toByteArray(Charsets.UTF_8))
        fun freeform(name: String, value: String) = b.write(
            box("----", box("mean", ByteArray(4) + "com.apple.iTunes".toByteArray()) + box("name", ByteArray(4) + name.toByteArray()) +
                data(1, value.toByteArray(Charsets.UTF_8))),
        )
        t.bpm?.let {
            item("tmpo", 21, byteArrayOf((Math.round(it) shr 8).toByte(), Math.round(it).toByte()))
            freeform("BPM", bpmText(it))
        }
        t.key?.let { freeform("initialkey", it) }
        item("${copy}too", 1, SOFTWARE.toByteArray())
        t.cover?.let { item("covr", if (t.coverMime == "image/png") 14 else 13, it) }
        return b.toByteArray()
    }

    private fun box(type: String, body: ByteArray): ByteArray = be32Bytes((8 + body.size).toLong()) + type.toByteArray(Charsets.ISO_8859_1) + body

    private fun be32(b: ByteArray, at: Int): Long =
        ((b[at].toLong() and 0xFF) shl 24) or ((b[at + 1].toLong() and 0xFF) shl 16) or ((b[at + 2].toLong() and 0xFF) shl 8) or (b[at + 3].toLong() and 0xFF)
    private fun putBe32(b: ByteArray, at: Int, v: Long) {
        if (v !in 0..0xFFFFFFFFL) throw IOException("offset out of range")
        b[at] = (v shr 24).toByte(); b[at + 1] = (v shr 16).toByte(); b[at + 2] = (v shr 8).toByte(); b[at + 3] = v.toByte()
    }
    private fun be32Bytes(v: Long) = byteArrayOf((v shr 24).toByte(), (v shr 16).toByte(), (v shr 8).toByte(), v.toByte())
    private fun le32(b: ByteArray, at: Int): Long =
        (b[at].toLong() and 0xFF) or ((b[at + 1].toLong() and 0xFF) shl 8) or ((b[at + 2].toLong() and 0xFF) shl 16) or ((b[at + 3].toLong() and 0xFF) shl 24)
    private fun le32Bytes(v: Long) = byteArrayOf(v.toByte(), (v shr 8).toByte(), (v shr 16).toByte(), (v shr 24).toByte())
    private fun le32BytesInt(v: Int) = le32Bytes(v.toLong())
}
