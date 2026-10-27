package com.pocketstudio.model

/**
 * Audit final (lot notes): the tracks the assistant made to double a bass, [StarterWizard.SUB_TRACK]
 * (its notes an octave down, [StarterWizard.subNotes]) and [StarterWizard.BASS_SWITCH_TRACK] (the
 * same notes, another sound), follow the bass's notes when they are edited. A follower is one that
 * still doubles the bass in every scene before the edit: once changed by hand, it keeps its own notes.
 */
object LinkedNotes {
    /** A track that follows [bass]: [sub] plays its notes an octave down, else the same notes. */
    data class Follower(val id: Int, val sub: Boolean)

    /** The tracks of [p] that double [bass] in every scene (none when [bass] is itself such a track). */
    fun followers(p: Project, bass: Int): List<Follower> {
        val tracks = p.instrumentTracks
        if (tracks.none { it.id == bass } || role(tracks.first { it.id == bass }.name) != null) return emptyList()
        // Nearly every project has none: nothing compared then (this runs on each edit of notes).
        if (tracks.none { it.id != bass && role(it.name) != null }) return emptyList()
        val places = places(p)
        val bassNotes = places.map { sorted(clipOf(p, it, bass)?.notes.orEmpty()) }
        if (bassNotes.all { it.isEmpty() }) return emptyList()
        return tracks.mapNotNull { t ->
            val sub = role(t.name) ?: return@mapNotNull null
            if (t.id == bass || Freeze.isFrozen(p, t.id)) return@mapNotNull null
            val same = places.indices.all { i ->
                val own = sorted(clipOf(p, places[i], t.id)?.notes.orEmpty())
                own == if (sub) StarterWizard.subNotes(bassNotes[i]) else bassNotes[i]
            }
            if (same) Follower(t.id, sub) else null
        }
    }

    /**
     * [after] ([before] with [bass]'s notes edited) with the [followers] of [bass] in [before] given the
     * bass's new notes wherever they changed. [after] itself when nothing follows or nothing changed.
     */
    fun sync(before: Project, after: Project, bass: Int): Project {
        if (after.instrumentTracks.none { it.id == bass }) return after
        val follow = followers(before, bass).filter { f -> after.instrumentTracks.any { it.id == f.id } }
        if (follow.isEmpty()) return after
        // The current scene (the tracks' own fields), then the other scenes when the list is the same.
        var out = after
        val oldCurrent = before.track(bass).notes
        val newCurrent = after.track(bass).notes
        if (oldCurrent != newCurrent || before.scene != after.scene) {
            for (f in follow) out = out.withTrack(out.track(f.id).let { t -> t.copy(notes = followed(newCurrent, t.notes, f.sub)) })
        }
        if (before.scenes.size == after.scenes.size && before.scene == after.scene) {
            val scenes = out.scenes.mapIndexed { i, s ->
                if (i == after.scene) return@mapIndexed s
                val old = clipOf(before.scenes[i], bass)
                val new = clipOf(s, bass)
                if (old == new || new == null) s
                else follow.fold(s) { acc, f -> withClip(acc, f.id, followed(new, clipOf(acc, f.id) ?: NoteClip(), f.sub)) }
            }
            if (scenes != out.scenes) out = out.copy(scenes = scenes)
        }
        return out
    }

    /** [bass]'s clip as [follower] plays it: an octave down for a sub; the follower keeps its own performance. */
    private fun followed(bass: NoteClip, follower: NoteClip, sub: Boolean): NoteClip =
        (if (sub) bass.copy(notes = StarterWizard.subNotes(bass.notes), hidden = StarterWizard.subNotes(bass.hidden)) else bass)
            .copy(performance = follower.performance).sanitized()

    /** True for a sub, false for a second bass, null for any other track (saved names in French, or shown in another language). */
    private fun role(name: String): Boolean? = when (DefaultNames.french(name) ?: name) {
        StarterWizard.SUB_TRACK -> true
        StarterWizard.BASS_SWITCH_TRACK -> false
        else -> null
    }

    /** Null: the current scene (the tracks' fields); else that scene. */
    private fun places(p: Project): List<Int?> = listOf<Int?>(null) + p.scenes.indices.filter { it != p.scene }

    private fun clipOf(p: Project, place: Int?, id: Int): NoteClip? =
        if (place == null) p.track(id).notes else clipOf(p.scenes[place], id)

    private fun clipOf(s: Scene, id: Int): NoteClip? = if (id == 0) s.synthNotes else s.trackNotes[id]

    private fun withClip(s: Scene, id: Int, clip: NoteClip): Scene =
        if (id == 0) s.copy(synthNotes = clip) else s.copy(trackNotes = s.trackNotes + (id to clip))

    private fun sorted(notes: List<Note>): List<Note> = notes.sortedWith(compareBy({ it.start }, { it.pitch }))
}
