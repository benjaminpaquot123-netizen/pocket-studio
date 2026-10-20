package com.pocketstudio.model

import kotlinx.serialization.Serializable

/**
 * An instrument track: a piano roll and its sound. Track 0 is the project's first synth track
 * (its sound and notes stay in the project's own fields, so older files load unchanged); tracks
 * 1..7 are added at will and live in [Project.tracks].
 */
@Serializable
data class InstrumentTrack(
    /** 1..[MAX]-1 for an added track: its engine track and mixer channel (see [mixerChannel]). */
    val id: Int,
    val name: String,
    /** Index in the track colour palette. */
    val color: Int = 0,
    /** 0 = subtractive synth, 1 = Plaits, 2 = [sampled] (an [instrument] still wins). */
    val engine: Int = 0,
    val synth: SynthPatch = SynthPatch(),
    val plaits: PlaitsPatch = PlaitsPatch(),
    val instrument: InstrumentChoice? = null,
    /** The current scene's notes (each scene keeps its own, see [Scene.trackNotes]). */
    val notes: NoteClip = NoteClip(),
    val sampled: SampledPatch = SampledPatch(),
) {
    fun sanitized(): InstrumentTrack = copy(
        name = name.take(MAX_NAME).ifBlank { "Piste ${id + 1}" },
        color = color.coerceIn(0, COLORS - 1),
        engine = engine.coerceIn(0, 2),
        synth = synth.sanitized(),
        plaits = plaits.sanitized(),
        instrument = instrument?.takeIf { it.bank in 0..128 && it.program in 0..127 },
        notes = notes.sanitized(),
        sampled = sampled.sanitized(),
    )

    /** What the track's sound is, in a few words. */
    val soundLabel: String get() = instrument?.name ?: when (engine) {
        1 -> "Plaits"
        2 -> sampled.name.ifBlank { "Sample" }
        else -> "Synthé"
    }

    companion object {
        /** Instrument tracks in a project, the first one included (the engine's kTracks). */
        const val MAX = 8
        const val MAX_NAME = 24
        const val COLORS = 8

        /** The mixer channel of track [id] (NativeEngine.trackChannel): 2 for the first, then 5..11. */
        fun mixerChannel(id: Int) = if (id == 0) 2 else 4 + id
    }
}

/** Every instrument track, the first one (id 0, built from the project's own fields) first. */
val Project.instrumentTracks: List<InstrumentTrack> get() = listOf(track(0)) + tracks

/** Track [id]; the first track when there is no such track. */
fun Project.track(id: Int): InstrumentTrack =
    if (id == 0) InstrumentTrack(0, synthTrackName, synthTrackColor, synthEngine, synth, plaits, instrument, synthNotes, sampled)
    else tracks.firstOrNull { it.id == id } ?: track(0)

/** Puts [t] back where it belongs (the project's own fields for the first track). */
fun Project.withTrack(t: InstrumentTrack): Project =
    if (t.id == 0) copy(
        synthTrackName = t.name, synthTrackColor = t.color, synthEngine = t.engine, synth = t.synth,
        plaits = t.plaits, instrument = t.instrument, synthNotes = t.notes, sampled = t.sampled,
    )
    else copy(tracks = tracks.map { if (it.id == t.id) t else it })

/**
 * The project seen from track [id]: its sound and notes in the first track's fields, so screens
 * made for one synth track show any of them.
 */
fun Project.viewOfTrack(id: Int): Project {
    if (id == 0) return this
    val t = tracks.firstOrNull { it.id == id } ?: return this
    return copy(synthEngine = t.engine, synth = t.synth, plaits = t.plaits, instrument = t.instrument, synthNotes = t.notes, sampled = t.sampled)
}

/** A new track (or null when all eight exist), with a free id, a fresh mixer channel and the next colour. */
fun Project.addTrack(name: String? = null, engine: Int = 0): Project? {
    val id = (1 until InstrumentTrack.MAX).firstOrNull { free -> tracks.none { it.id == free } } ?: return null
    val color = (0 until InstrumentTrack.COLORS).firstOrNull { c -> instrumentTracks.none { it.color == c } } ?: (id % InstrumentTrack.COLORS)
    val steps = maxOf(synthNotes.steps, tracks.maxOfOrNull { it.notes.steps } ?: 16)
    val track = InstrumentTrack(id, name ?: "Piste ${instrumentTracks.size + 1}", color, engine, notes = NoteClip(steps = steps))
    val ch = InstrumentTrack.mixerChannel(id)
    return copy(
        tracks = (tracks + track).sortedBy { it.id },
        mixer = mixer.copy(channels = mixer.channels.mapIndexed { i, c -> if (i == ch) Channel() else c }),
    )
}

/** Removes added track [id] from the project and from every scene (the first track stays). */
fun Project.removeTrack(id: Int): Project {
    if (id == 0) return this
    val ch = InstrumentTrack.mixerChannel(id)
    return copy(
        tracks = tracks.filterNot { it.id == id },
        scenes = scenes.map { it.copy(trackNotes = it.trackNotes - id) },
        automation = automation.filterNot { it.target.kind != AutoTarget.BUS_PARAM && it.target.ch == ch },
        // Its strip back to neutral: a solo left on it would silence every other one.
        mixer = mixer.copy(channels = mixer.channels.mapIndexed { i, c -> if (i == ch) Channel() else c }),
    )
}

/** A copy of track [id] with its notes and mixer settings, as a new track (null when full). */
fun Project.duplicateTrack(id: Int): Project? {
    val source = track(id)
    val added = addTrack() ?: return null
    val newId = added.tracks.map { it.id }.first { n -> tracks.none { it.id == n } }
    val copy = source.copy(id = newId, name = "${source.name.take(InstrumentTrack.MAX_NAME - 2)} 2", color = added.track(newId).color)
    val from = InstrumentTrack.mixerChannel(id)
    val to = InstrumentTrack.mixerChannel(newId)
    return added.withTrack(copy).copy(
        mixer = added.mixer.copy(channels = added.mixer.channels.mapIndexed { i, c -> if (i == to) added.mixer.channels[from] else c }),
        scenes = added.scenes.map { s -> s.copy(trackNotes = s.trackNotes + (newId to (s.trackNotes[id] ?: if (id == 0) s.synthNotes else NoteClip()))) },
    )
}

/**
 * A sound played across the keys (track engine 2): a file in the project's samples folder, the
 * note it was recorded at and how it plays. A pack's 808 with [glide] makes trap slides.
 */
@Serializable
data class SampledPatch(
    /** File in the project's samples folder; null = none chosen yet (the track plays its synth). */
    val file: String? = null,
    val name: String = "",
    /** MIDI note the recording sounds at (found by pitch detection when it is chosen). */
    val root: Float = 60f,
    val start: Float = 0f,
    val end: Float = 1f,
    val attack: Float = 0f,
    val decay: Float = 0f,
    val sustain: Float = 1f,
    val release: Float = 0.05f,
    /** Seconds to slide between held notes; 0 = several notes at once (chords). */
    val glide: Float = 0f,
    /** The sound repeats while the key is held (pads, drones); else it plays once. */
    val loop: Boolean = false,
    val gain: Float = 1f,
) {
    fun sanitized(): SampledPatch = copy(
        file = file?.takeIf { PadSample(it, "").isSafe },
        name = name.take(40),
        root = root.finiteOr(60f).coerceIn(0f, 127f),
        start = start.finiteOr(0f).coerceIn(0f, 0.999f),
        end = end.finiteOr(1f).coerceIn(0.001f, 1f),
        attack = attack.finiteOr(0f).coerceIn(0f, 2f),
        decay = decay.finiteOr(0f).coerceIn(0f, 2f),
        sustain = sustain.finiteOr(1f).coerceIn(0f, 1f),
        release = release.finiteOr(0.05f).coerceIn(0f, 4f),
        glide = glide.finiteOr(0f).coerceIn(0f, 1f),
        gain = gain.finiteOr(1f).coerceIn(0f, 2f),
    )

    private fun Float.finiteOr(default: Float) = if (isFinite()) this else default

    /** What the engine gets, in SampleInstrument::Param order (root, start, end, attack, decay, sustain, release, glide, loop, gain). */
    fun engineValues(): List<Float> = listOf(root, start, end, attack, decay, sustain, release, glide, if (loop) 1f else 0f, gain)

    companion object {
        /** Glide for an 808 slide, in seconds. */
        const val SLIDE = 0.08f

        /**
         * The root for a sound found to sound at [detected], put in the octave of the notes it will
         * play: the same note name (the song stays in key), but a 808 found at La 0 under chords
         * around Do 4 would otherwise be read 16 times too fast and last a tenth of a second.
         */
        fun fitRoot(detected: Float?, notes: List<Note>): Float {
            val pitches = notes.map { it.pitch }.sorted()
            val centre = if (pitches.isEmpty()) 60 else pitches[pitches.size / 2]
            val root = detected ?: return centre.toFloat()
            val octaves = Math.round((centre - root) / 12f)
            return (root + 12f * octaves).coerceIn(0f, 127f)
        }

        /** True when [notes] never overlap (a line that can glide, not chords). */
        fun isMonophonic(notes: List<Note>): Boolean =
            notes.sortedBy { it.start }.zipWithNext().none { (a, b) -> b.start < a.end }
    }
}
