package com.pocketstudio.model

import com.pocketstudio.model.i18n.tr
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.roundToInt

/** A note of a MIDI file: [start] and [length] in sixteenths × [NoteClip.TICKS_PER_STEP] ticks. */
data class MidiNote(val channel: Int, val pitch: Int, val start: Int, val length: Int, val velocity: Int)

/**
 * Standard MIDI files (type 1) to and from projects. One quarter note = 96 ticks, the piano roll's
 * own resolution, so nothing is rounded on the way out.
 */
object MidiFile {
    const val PPQ = 4 * NoteClip.TICKS_PER_STEP
    const val DRUM_CHANNEL = 9

    /** General MIDI drum note of each pad, in DrumKit order (Kick, Kick 2, Caisse…). */
    val PAD_NOTES = listOf(36, 35, 38, 40, 39, 42, 46, 41, 45, 48, 37, 56, 49, 70, 63, 75)

    // --- Export -----------------------------------------------------------------------------

    /**
     * The project as MIDI: the whole song (scenes chained) when it has one and [song] is set, else
     * the current scene once. Tracks: the first instrument track (channel 1), Basse acide (2), the
     * added instrument tracks (3 to 9), Batterie (10).
     */
    fun export(project: Project, song: Boolean = project.song.isNotEmpty()): ByteArray {
        val scenes = project.allScenes
        val parts = if (song && project.song.isNotEmpty()) project.song.flatMap { p -> List(p.repeats) { p.scene } }
        else listOf(project.scene.coerceIn(0, scenes.lastIndex))
        val synth = ArrayList<MidiNote>()
        val acid = ArrayList<MidiNote>()
        val drums = ArrayList<MidiNote>()
        val added = project.tracks.associate { it.id to ArrayList<MidiNote>() }
        var at = 0
        var partsDone = 0
        for (index in parts) {
            val scene = scenes.getOrNull(index) ?: run { partsDone++; null } ?: continue
            val sceneSteps = scene.stepsFor(project.stepsPerBar)
            val sceneTicks = sceneSteps * NoteClip.TICKS_PER_STEP
            // A song part leaves out its muted tracks (the timeline).
            val part = if (song && project.song.isNotEmpty()) project.song[partIndexOf(project, partsDone)] else null
            fun plays(key: String) = part?.plays(key) ?: true
            if (plays(SongPart.inst(0))) synth += loopNotes(scene.synthNotes, sceneTicks, at)
            if (plays(SongPart.ACID)) acid += acidNotes(scene.acid, project.acid.root, sceneSteps, at)
            if (plays(SongPart.DRUMS)) drums += drumNotes(scene.drums, sceneSteps, at)
            project.tracks.forEach { t ->
                if (plays(SongPart.inst(t.id))) scene.trackNotes[t.id]?.let { added.getValue(t.id) += loopNotes(it, sceneTicks, at) }
            }
            partsDone++
            at += sceneTicks
        }
        val tempo = (60_000_000.0 / project.tempoBpm).roundToInt()
        val conductor = Track().apply {
            name(project.name)
            meta(0, 0x51, byteArrayOf((tempo shr 16).toByte(), (tempo shr 8).toByte(), tempo.toByte()))
            meta(0, 0x58, byteArrayOf(project.beatsPerBar.toByte(), 2, 24, 8))  // n/4
        }
        val program = project.instrument?.takeIf { it.bank < 128 }?.program ?: 81  // GM "Lead 2 (sawtooth)"
        val tracks = listOf(
            conductor,
            noteTrack(project.synthTrackName, 0, program, synth),
            noteTrack(tr("Basse acide"), 1, 38, acid),  // GM "Synth Bass 1"
        ) + project.tracks.map { t ->
            // Channels 3..9 (index 2..8): never the drums' channel 10.
            noteTrack(t.name, 1 + t.id, t.instrument?.takeIf { it.bank < 128 }?.program ?: 81, added.getValue(t.id))
        } + noteTrack(tr("Batterie"), DRUM_CHANNEL, null, drums)
        val out = ByteArrayOutputStream()
        out.write("MThd".toByteArray())
        out.int32(6)
        out.int16(1)
        out.int16(tracks.size)
        out.int16(PPQ)
        tracks.forEach { t ->
            val body = t.finish()
            out.write("MTrk".toByteArray())
            out.int32(body.size)
            out.write(body)
        }
        return out.toByteArray()
    }

    /** Which song part the [n]th scene played belongs to (parts are expanded by their repeats). */
    private fun partIndexOf(project: Project, n: Int): Int {
        var left = n
        project.song.forEachIndexed { i, p ->
            if (left < p.repeats) return i
            left -= p.repeats
        }
        return project.song.lastIndex
    }

    /** The clip's notes over [sceneTicks], the clip repeating like it does when the scene plays. */
    private fun loopNotes(clip: NoteClip, sceneTicks: Int, offset: Int): List<MidiNote> {
        val out = ArrayList<MidiNote>()
        var loop = 0
        while (loop < sceneTicks) {
            clip.notes.forEach { n ->
                val start = loop + n.start
                if (start < sceneTicks) out += MidiNote(0, n.pitch, offset + start, minOf(n.length, sceneTicks - start), velocity(n.velocity))
            }
            loop += clip.ticks
        }
        return out
    }

    private fun acidNotes(acid: AcidPattern, root: Int, sceneSteps: Int, offset: Int): List<MidiNote> {
        val out = ArrayList<MidiNote>()
        val step = NoteClip.TICKS_PER_STEP
        var s = 0
        while (s < sceneSteps) {
            val a = acid.steps[s % acid.length]
            if (a.gate) {
                // A slide overlaps the next note a little (legato: synths glide there); else half a step.
                val pitch = (root + a.note + 12 * a.octave).coerceIn(0, 127)
                out += MidiNote(1, pitch, offset + s * step, if (a.slide) step + step / 4 else step / 2, if (a.accent) 120 else 90)
            }
            s++
        }
        return out
    }

    private fun drumNotes(drums: DrumPattern, sceneSteps: Int, offset: Int): List<MidiNote> {
        val out = ArrayList<MidiNote>()
        for (s in 0 until sceneSteps) {
            drums.lanes.forEachIndexed { lane, l ->
                l.hits[s % drums.steps]?.let { hit ->
                    out += MidiNote(DRUM_CHANNEL, PAD_NOTES[lane], offset + s * NoteClip.TICKS_PER_STEP, NoteClip.TICKS_PER_STEP / 2, velocity(hit.velocity))
                }
            }
        }
        return out
    }

    private fun velocity(v: Float) = (v * 127).roundToInt().coerceIn(1, 127)

    private fun noteTrack(name: String, channel: Int, program: Int?, notes: List<MidiNote>): Track = Track().apply {
        name(name)
        if (program != null) event(0, byteArrayOf((0xC0 or channel).toByte(), program.toByte()))
        // Note-offs before note-ons at the same tick, so repeated notes are never cut short.
        val events = notes.flatMap { n ->
            listOf(
                Triple(n.start, 1, byteArrayOf((0x90 or channel).toByte(), n.pitch.toByte(), n.velocity.toByte())),
                Triple(n.start + n.length, 0, byteArrayOf((0x80 or channel).toByte(), n.pitch.toByte(), 0)),
            )
        }.sortedWith(compareBy({ it.first }, { it.second }))
        events.forEach { (tick, _, bytes) -> event(tick, bytes) }
    }

    private class Track {
        private val bytes = ByteArrayOutputStream()
        private var lastTick = 0

        fun event(tick: Int, data: ByteArray) {
            varLen(maxOf(0, tick - lastTick))
            lastTick = maxOf(lastTick, tick)
            bytes.write(data)
        }

        fun meta(tick: Int, type: Int, data: ByteArray) {
            event(tick, byteArrayOf(0xFF.toByte(), type.toByte()))
            varLen(data.size)
            bytes.write(data)
        }

        fun name(text: String) = meta(0, 0x03, text.toByteArray(Charsets.UTF_8).take(127).toByteArray())

        private fun varLen(v: Int) {
            var buffer = v and 0x7F
            var rest = v shr 7
            while (rest > 0) {
                buffer = (buffer shl 8) or 0x80 or (rest and 0x7F)
                rest = rest shr 7
            }
            while (true) {
                bytes.write(buffer and 0xFF)
                if (buffer and 0x80 != 0) buffer = buffer shr 8 else break
            }
        }

        fun finish(): ByteArray {
            meta(lastTick, 0x2F, ByteArray(0))  // end of track
            return bytes.toByteArray()
        }
    }

    private fun ByteArrayOutputStream.int32(v: Int) { write(v shr 24); write(v shr 16); write(v shr 8); write(v) }
    private fun ByteArrayOutputStream.int16(v: Int) { write(v shr 8); write(v) }

    // --- Import -----------------------------------------------------------------------------

    /** What a MIDI file holds: its notes (ticks rescaled to ours) and its first tempo, if any. */
    data class Content(val notes: List<MidiNote>, val tempoBpm: Double?)

    /** Reads a type 0 or 1 file. Throws IOException when it is not a MIDI file. */
    fun read(data: ByteArray): Content {
        val r = Reader(data)
        if (r.text(4) != "MThd") throw IOException(tr("pas un fichier MIDI"))
        val headerLength = r.int32()
        r.int16()  // format
        val tracks = r.int16()
        val division = r.int16()
        r.skip(headerLength - 6)
        if (division <= 0 || division and 0x8000 != 0) throw IOException(tr("fichier MIDI en SMPTE non pris en charge"))
        val notes = ArrayList<MidiNote>()
        var tempo: Double? = null
        repeat(tracks) {
            if (r.remaining < 8) return@repeat
            val id = r.text(4)
            val length = r.int32()
            if (length < 0 || length > r.remaining) throw IOException(tr("fichier MIDI tronqué"))
            if (id != "MTrk") { r.skip(length); return@repeat }
            val end = r.pos + length
            var tick = 0L
            var status = 0
            val open = HashMap<Int, Pair<Long, Int>>()  // channel*128+pitch -> (start, velocity)
            while (r.pos < end) {
                tick += r.varLen()
                var b = r.u8()
                if (b == 0xFF) {
                    val type = r.u8()
                    val len = r.varLen()
                    if (type == 0x51 && len == 3 && tempo == null) {
                        val us = (r.u8() shl 16) or (r.u8() shl 8) or r.u8()
                        if (us > 0) tempo = 60_000_000.0 / us
                    } else r.skip(len)
                    continue
                }
                if (b == 0xF0 || b == 0xF7) { r.skip(r.varLen()); continue }
                if (b < 0x80) { r.pos--; b = status } else status = b
                val channel = b and 0x0F
                when (b and 0xF0) {
                    0x90, 0x80 -> {
                        val pitch = r.u8()
                        val vel = r.u8()
                        val key = channel * 128 + pitch
                        val off = b and 0xF0 == 0x80 || vel == 0
                        open.remove(key)?.let { (start, v) ->
                            notes += MidiNote(channel, pitch, scale(start, division), maxOf(1, scale(tick, division) - scale(start, division)), v)
                        }
                        if (!off) open[key] = tick to vel
                    }
                    0xA0, 0xB0, 0xE0 -> r.skip(2)
                    0xC0, 0xD0 -> r.skip(1)
                    else -> throw IOException(tr("événement MIDI inconnu"))
                }
            }
            r.pos = end
        }
        return Content(notes.sortedWith(compareBy({ it.start }, { it.pitch })), tempo)
    }

    private fun scale(tick: Long, division: Int) = (tick * PPQ / division).coerceAtMost(Int.MAX_VALUE / 2L).toInt()

    /**
     * Puts imported notes into the project's current scene: channel 10 onto the drum pads (up to
     * 4 bars, nearest sixteenth), every other channel into the piano roll (up to 8 bars). The
     * tempo follows the file when it has one.
     */
    fun importInto(project: Project, content: Content): Project {
        val step = NoteClip.TICKS_PER_STEP
        val melodic = content.notes.filter { it.channel != DRUM_CHANNEL }
        val drumHits = content.notes.filter { it.channel == DRUM_CHANNEL }
        var p = project
        if (melodic.isNotEmpty()) {
            val first = melodic.minOf { it.start } / (16 * step) * (16 * step)  // keep the bar position
            // One channel per instrument track: the first channel on the first track, the others on
            // new tracks (as long as there is room), all starting on the same bar.
            melodic.groupBy { it.channel }.toSortedMap().entries.forEachIndexed { i, (channel, group) ->
                val lastEnd = group.maxOf { it.start + it.length } - first
                val steps = NoteClip.LENGTHS_IN_STEPS.firstOrNull { it * step >= lastEnd } ?: NoteClip.MAX_STEPS
                val notes = group.map { Note(it.start - first, it.length, it.pitch, it.velocity / 127f) }
                    .filter { it.start < steps * step }
                if (i == 0) {
                    p = p.copy(synthNotes = p.synthNotes.copy(steps = steps, notes = notes).sanitized())
                } else {
                    val next = p.addTrack(tr("Canal %d", channel + 1)) ?: return@forEachIndexed
                    val t = next.tracks.first { n -> p.tracks.none { it.id == n.id } }
                    p = next.withTrack(t.copy(notes = NoteClip(steps = steps, notes = notes).sanitized()))
                }
            }
        }
        if (drumHits.isNotEmpty()) {
            val first = drumHits.minOf { it.start } / (16 * step) * (16 * step)
            val lastStep = (drumHits.maxOf { it.start } - first) / step
            val steps = ((lastStep / 16 + 1) * 16).coerceIn(DrumPattern.MIN_STEPS, DrumPattern.MAX_STEPS)
            var drums = p.drums.copy(steps = steps, lanes = p.drums.lanes.map { it.copy(hits = emptyMap()) })
            drumHits.forEach { n ->
                val lane = padFor(n.pitch) ?: return@forEach
                val s = ((n.start - first).toDouble() / step).roundToInt()
                if (s in 0 until steps) {
                    val l = drums.lanes[lane]
                    drums = drums.copy(lanes = drums.lanes.toMutableList().also { it[lane] = l.copy(hits = l.hits + (s to Hit(n.velocity / 127f))) })
                }
            }
            p = p.copy(drums = drums)
        }
        content.tempoBpm?.let { p = p.copy(tempoBpm = it) }
        return p.sanitized()
    }

    /** The pad playing a GM drum note: its own one, else the closest family (kick, snare, hats…). */
    fun padFor(note: Int): Int? {
        PAD_NOTES.indexOf(note).takeIf { it >= 0 }?.let { return it }
        return when (note) {
            34 -> 1
            44 -> 5                        // pedal hi-hat
            43 -> 7                        // low floor tom
            47 -> 8
            50 -> 9
            51, 52, 55, 57, 59 -> 12       // other cymbals
            53 -> 11                       // ride bell
            54, 69, 82 -> 13               // tambourine, cabasa, shaker
            in 60..68 -> 14                // bongos, congas, timbales, agogo
            in 71..81 -> 15                // whistles, guiro, claves, blocks, cuica, triangle
            else -> null
        }
    }

    private class Reader(val data: ByteArray) {
        var pos = 0
        val remaining get() = data.size - pos
        fun u8(): Int { if (pos >= data.size) throw IOException(tr("fichier MIDI tronqué")); return data[pos++].toInt() and 0xFF }
        fun int16() = (u8() shl 8) or u8()
        fun int32() = (u8() shl 24) or (u8() shl 16) or (u8() shl 8) or u8()
        fun text(n: Int) = String(ByteArray(n) { u8().toByte() }, Charsets.US_ASCII)
        fun skip(n: Int) { if (n < 0 || pos + n > data.size) throw IOException(tr("fichier MIDI tronqué")); pos += n }
        fun varLen(): Int {
            var v = 0
            repeat(4) {
                val b = u8()
                v = (v shl 7) or (b and 0x7F)
                if (b and 0x80 == 0) return v
            }
            throw IOException(tr("fichier MIDI abîmé"))
        }
    }
}
