package com.pocketstudio.app.rhythm

import com.pocketstudio.model.AudioTrack
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.Project
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.partView

/**
 * The files of a « Piste par piste » export: (label, mixer channel) of each strip that sounds in
 * it. A strip muted in the mixer, or with nothing to play in the exported scene (or in any part
 * of the song when [song]), gives no file: an empty WAV only gets in the way. Live pads are only
 * launched by hand, so they never play in an export.
 */
internal fun stemExports(p: Project, song: Boolean): List<Pair<String, Int>> {
    val tracks = stripSounds(p, song).filter { (stem, sounds) -> sounds && p.mixer.channels.getOrNull(stem.second)?.mute != true }.map { it.first }
    // The Live page's own sounds (riser, downlifter, the build-up's noise) come from no track: each
    // track's file leaves them out, and they get a file of their own, so the files add up to the mix.
    return if (tracks.isNotEmpty() && hasLiveNoises(p, song)) tracks + (LIVE_FX_STEM_LABEL to LIVE_FX_STEM) else tracks
}

/**
 * Each track's strip as (file label, mixer channel), and whether it has something to play in the
 * scene (or in any part of the song when [song]), whatever the mixer does with it. Also read by
 * « Prêt à partager ? »: a track muted in the mixer although it has notes.
 */
internal fun stripSounds(p: Project, song: Boolean): List<Pair<Pair<String, Int>, Boolean>> {
    val inSong = song && p.song.isNotEmpty()
    val views = if (inSong) p.song.map { p.partView(it) } else listOf(p)
    // Phase 37: a frozen track is silent in the song (its sound is its audio track's file).
    val frozen = if (inSong) com.pocketstudio.model.Freeze.frozenIds(p) else emptySet()
    fun slug(name: String) = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').take(16)
    return listOf(
        "batterie" to 0 to views.any { v -> v.drums.lanes.any { lane -> lane.hits.keys.any { it < v.drums.steps } } },
        "acide" to 1 to views.any { v -> v.acid.steps.take(v.acid.length).any { it.gate } },
        "synthe" to 2 to (0 !in frozen && views.any { v -> v.synthNotes.notes.isNotEmpty() }),
        "remix" to 3 to (p.remix.setId != null),
    ) + p.tracks.map { t ->
        // One file per added instrument track, named after it.
        "piste${t.id + 1}-${slug(t.name)}" to InstrumentTrack.mixerChannel(t.id) to
            (t.id !in frozen && views.any { v -> v.tracks.firstOrNull { it.id == t.id }?.notes?.notes?.isNotEmpty() == true })
    } + p.audioTracks.map { t ->
        "audio${t.id + 1}-${slug(t.name)}" to AudioTrack.mixerChannel(t.id) to
            (t.clip != null && (!song || p.song.isEmpty() || p.song.any { it.plays(SongPart.audio(t.id)) }))
    }
}

/** The « Effets live » file's channel in [stemExports]: no strip, the Live page's own sounds alone. */
internal const val LIVE_FX_STEM = -1
internal const val LIVE_FX_STEM_LABEL = "effets-live"

/** The Live params that make a sound of their own (not a change of the mix). */
private val LIVE_NOISES = setOf(AutoTarget.LIVE_RISER, AutoTarget.LIVE_DOWNLIFTER, AutoTarget.LIVE_BUILD_UP)

/** Whether a lane of [p] plays the riser, the downlifter or the build-up in the export ([song]: the whole song). */
internal fun hasLiveNoises(p: Project, song: Boolean): Boolean = p.automation.any { lane ->
    lane.target.kind == AutoTarget.LIVE && lane.target.param in LIVE_NOISES && (
        lane.values.any { (it ?: 0f) > 0f } || lane.sceneValues.values.any { v -> v.any { (it ?: 0f) > 0f } } ||
            ((song || !lane.songOnly) && lane.songValues.any { (it ?: 0f) > 0f })
        )
}

/**
 * Lot E (« Master pro »): separate tracks all of one length, from the song's frame 0. Each stem's
 * automatic tail ends where its own sound dies away; the shorter ones get digital silence up to the
 * longest (the RIFF and data sizes patched). Blocking, worker thread.
 */
internal object WavLength {
    private class Layout(val dataAt: Long, val dataBytes: Long, val frameBytes: Int, val bits: Int, val float: Boolean)

    private fun layout(f: java.io.RandomAccessFile): Layout {
        val head = ByteArray(12)
        f.seek(0)
        f.readFully(head)
        if (String(head, 0, 4) != "RIFF" || String(head, 8, 4) != "WAVE") throw java.io.IOException("not a WAV")
        var pos = 12L
        var frameBytes = 0
        var bits = 0
        var float = false
        val tag = ByteArray(8)
        while (pos + 8 <= f.length()) {
            f.seek(pos)
            f.readFully(tag)
            val id = String(tag, 0, 4)
            val size = java.nio.ByteBuffer.wrap(tag, 4, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xFFFFFFFFL
            if (id == "fmt ") {
                val fmt = ByteArray(16)
                f.readFully(fmt)
                val b = java.nio.ByteBuffer.wrap(fmt).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                float = b.short.toInt() == 3
                val channels = b.short.toInt()
                b.int; b.int; b.short
                bits = b.short.toInt()
                frameBytes = channels * bits / 8
            } else if (id == "data") {
                return Layout(pos + 8, size, frameBytes, bits, float)
            }
            pos += 8 + size + (size and 1)
        }
        throw java.io.IOException("no data")
    }

    fun frames(wav: java.io.File): Long = java.io.RandomAccessFile(wav, "r").use { f -> layout(f).let { it.dataBytes / it.frameBytes } }

    /** [wav] (a WAV the engine wrote, its data last) padded with silence to [frames] frames. */
    fun padTo(wav: java.io.File, frames: Long) {
        java.io.RandomAccessFile(wav, "rw").use { f ->
            val l = layout(f)
            val want = frames * l.frameBytes
            if (want <= l.dataBytes) return
            if (l.dataAt + l.dataBytes != f.length()) throw java.io.IOException("data not last")
            f.seek(f.length())
            val zeros = ByteArray(1 shl 16)
            var left = want - l.dataBytes
            while (left > 0) {
                val n = minOf(left, zeros.size.toLong()).toInt()
                f.write(zeros, 0, n)
                left -= n
            }
            val le = { v: Long -> byteArrayOf(v.toByte(), (v shr 8).toByte(), (v shr 16).toByte(), (v shr 24).toByte()) }
            f.seek(l.dataAt - 4)
            f.write(le(want))
            f.seek(4)
            f.write(le(f.length() - 8))
            // A float WAV's « fact » chunk counts frames too.
            if (l.float) {
                var pos = 12L
                val tag = ByteArray(8)
                while (pos + 8 < l.dataAt) {
                    f.seek(pos)
                    f.readFully(tag)
                    val size = java.nio.ByteBuffer.wrap(tag, 4, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xFFFFFFFFL
                    if (String(tag, 0, 4) == "fact") {
                        f.seek(pos + 8)
                        f.write(le(frames))
                    }
                    pos += 8 + size + (size and 1)
                }
            }
        }
    }

    /** The sample peak of [wav] in dBFS (the M4A guard's first gain; null when silent or unreadable). */
    fun peakDb(wav: java.io.File): Double? = runCatching {
        com.pocketstudio.app.export.WavPcm.open(wav).use { pcm ->
            val buf = FloatArray(4096 * pcm.channels)
            var peak = 0f
            while (true) {
                val n = pcm.readFloat(buf, 4096)
                if (n == 0) break
                for (i in 0 until n * pcm.channels) peak = maxOf(peak, kotlin.math.abs(buf[i]))
            }
            if (peak > 0f) 20 * kotlin.math.log10(peak.toDouble()) else null
        }
    }.getOrNull()
}
