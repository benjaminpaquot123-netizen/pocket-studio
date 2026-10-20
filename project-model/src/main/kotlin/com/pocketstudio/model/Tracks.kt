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
    /** 0 = subtractive synth, 1 = Plaits (an [instrument] still wins). */
    val engine: Int = 0,
    val synth: SynthPatch = SynthPatch(),
    val plaits: PlaitsPatch = PlaitsPatch(),
    val instrument: InstrumentChoice? = null,
    /** The current scene's notes (each scene keeps its own, see [Scene.trackNotes]). */
    val notes: NoteClip = NoteClip(),
) {
    fun sanitized(): InstrumentTrack = copy(
        name = name.take(MAX_NAME).ifBlank { "Piste ${id + 1}" },
        color = color.coerceIn(0, COLORS - 1),
        engine = engine.coerceIn(0, 1),
        synth = synth.sanitized(),
        plaits = plaits.sanitized(),
        instrument = instrument?.takeIf { it.bank in 0..128 && it.program in 0..127 },
        notes = notes.sanitized(),
    )

    /** What the track's sound is, in a few words. */
    val soundLabel: String get() = instrument?.name ?: if (engine == 1) "Plaits" else "Synthé"

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
    if (id == 0) InstrumentTrack(0, synthTrackName, synthTrackColor, synthEngine, synth, plaits, instrument, synthNotes)
    else tracks.firstOrNull { it.id == id } ?: track(0)

/** Puts [t] back where it belongs (the project's own fields for the first track). */
fun Project.withTrack(t: InstrumentTrack): Project =
    if (t.id == 0) copy(
        synthTrackName = t.name, synthTrackColor = t.color, synthEngine = t.engine, synth = t.synth,
        plaits = t.plaits, instrument = t.instrument, synthNotes = t.notes,
    )
    else copy(tracks = tracks.map { if (it.id == t.id) t else it })

/**
 * The project seen from track [id]: its sound and notes in the first track's fields, so screens
 * made for one synth track show any of them.
 */
fun Project.viewOfTrack(id: Int): Project {
    if (id == 0) return this
    val t = tracks.firstOrNull { it.id == id } ?: return this
    return copy(synthEngine = t.engine, synth = t.synth, plaits = t.plaits, instrument = t.instrument, synthNotes = t.notes)
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
    return copy(
        tracks = tracks.filterNot { it.id == id },
        scenes = scenes.map { it.copy(trackNotes = it.trackNotes - id) },
        automation = automation.filterNot { it.target.kind != AutoTarget.BUS_PARAM && it.target.ch == InstrumentTrack.mixerChannel(id) },
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
