package com.pocketstudio.model

/**
 * The Mode simple's « ● Enregistrer ma mélodie » (audit final, 01 M3 and 05 #5): the melody goes on
 * a track of its own, « Ma mélodie » (saved in French, translated at display), never over the
 * assistant's riff nor into the bass of a song without a lead. It sounds as the song's melody track
 * when there is one, else as a new melody track of the style. Once a take is written, it plays
 * through the whole song: every part where the track has nothing of its own plays the part it was
 * recorded in ([spread]).
 */
object SimpleMelody {
    const val NAME = "Ma mélodie"

    /** The project's « Ma mélodie » track (the last one), null when there is none yet. */
    fun trackOf(p: Project): Int? = p.tracks.lastOrNull { it.name == NAME }?.id

    /**
     * [p] with a new, empty « Ma mélodie » track: the sound (and effects) of track [like] when it
     * exists, else a melody sound for [style]. It does not follow the song's chords (its notes are
     * the user's). Null when the eight tracks are taken.
     */
    fun add(p: Project, like: Int?, style: StarterWizard.Style?): Project? {
        val next = (if (like != null && p.instrumentTracks.any { it.id == like }) p.duplicateTrack(like) else NewTracks.add(p, TrackKind.Melody, style))
            ?: return null
        val id = next.tracks.first { t -> p.tracks.none { it.id == t.id } }.id
        val t = next.track(id)
        val steps = t.notes.steps
        return next.withTrack(t.copy(name = NAME, notes = NoteClip(steps = steps), followChords = false))
            .copy(scenes = next.scenes.map { s -> s.copy(trackNotes = s.trackNotes + (id to NoteClip(steps = s.trackNotes[id]?.steps ?: steps))) })
    }

    /**
     * After a take on track [id] in scene [scene]: each song part where the track has no notes to
     * play takes them from [scene] (« ma mélodie » heard through the song, not only where its part
     * comes back). Parts where the track has notes of its own, or mutes it, stay as they are.
     */
    fun spread(p: Project, id: Int, scene: Int): Project {
        if (p.tracks.none { it.id == id } || scene !in p.allScenes.indices) return p
        fun silent(s: Int) = s !in p.allScenes.indices || p.sceneView(s).track(id).notes.notes.isEmpty()
        if (silent(scene)) return p
        val key = SongPart.inst(id)
        val song = p.song.map { part ->
            val plays = part.sceneFor(key)
            when {
                plays == scene || !silent(plays) -> part
                part.scene == scene -> part.copy(play = part.play - key)
                else -> part.copy(play = part.play + (key to scene))
            }
        }
        return if (song == p.song) p else p.copy(song = song)
    }
}
