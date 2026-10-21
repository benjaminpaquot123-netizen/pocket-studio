package com.pocketstudio.model

/**
 * What a new instrument track is for, chosen under "+ Piste": each kind gets its name (saved in
 * French, translated at display) and a sound suited to it, the style's own when the project's
 * style is known, as the assistant's parts get theirs.
 */
enum class TrackKind(val id: String, val trackName: String, val title: String, val description: String, val family: String) {
    Melody("melody", "Mélodie", "Mélodie (lead)", "Un son aigu pour une petite mélodie", "Leads"),
    Chords("chords", "Accords", "Accords (nappe)", "Des accords tenus pour l'ambiance", "Nappes"),
    Bass("bass", "Basse", "Basse", "Les notes graves sous la batterie", "Basses"),
    Arp("arp", "Arpège", "Arpège", "Tiens un accord : ses notes une à une", "Pluck"),
    Pluck("pluck", "Pluck", "Pluck / percussif", "Un son court qui claque", "Pluck"),
}

object NewTracks {
    /** A melody written when the style plays an arpeggio rather than a riff: up from the octave, back to the root. */
    const val FALLBACK_RIFF = "O..5..3.5..3..R."

    /** The style a project without one borrows its melody's shape from. */
    private const val FALLBACK_STYLE = "pop"

    /** The assistant's part a kind stands for, for its sound and its notes. */
    private fun part(kind: TrackKind) = when (kind) {
        TrackKind.Bass -> StarterWizard.Part.Bass
        TrackKind.Chords -> StarterWizard.Part.Pad
        else -> StarterWizard.Part.Lead
    }

    /** The synth sounds of a library family, as the assistant's parts take them. */
    private fun family(category: String) =
        SoundLibrary.SYNTH.filter { it.category == category }.map { StarterWizard.PartSound(it.name, synth = it.sound) }

    /**
     * The sound of a new [kind] track in [style] (null: none known): the sound the assistant gives
     * that part in the style, else the first of its family. Only sounds that need no file (synth
     * or Plaits): a pack sound would have to be copied into the project first.
     */
    fun sound(kind: TrackKind, style: StarterWizard.Style?): StarterWizard.PartSound {
        val styleSounds = style?.let { StarterWizard.soundsFor(part(kind), it) }.orEmpty().filter { it.sampled == null }
        return when (kind) {
            TrackKind.Arp -> styleSounds.firstOrNull()?.takeIf { style?.melody?.leadPattern?.isBlank() == true }
                ?: family("Pluck").firstOrNull()
            TrackKind.Pluck -> styleSounds.firstOrNull()?.takeIf { s -> SoundLibrary.SYNTH.any { it.name == s.name && it.category == "Pluck" } }
                ?: family("Pluck").firstOrNull()
            TrackKind.Melody -> styleSounds.firstOrNull() ?: family("Leads").firstOrNull()
            TrackKind.Chords -> styleSounds.firstOrNull() ?: family("Nappes").firstOrNull()
            TrackKind.Bass -> styleSounds.firstOrNull() ?: family("Basses").firstOrNull()
        } ?: StarterWizard.PartSound("Synthé")
    }

    /**
     * Four bars of melody in [project]'s key (its scale, else [style]'s usual key), on the
     * assistant's chords for that feeling: the style's riff, or [FALLBACK_RIFF] when the style
     * arpeggiates. Returns the notes and the key they are in.
     */
    fun melody(project: Project, style: StarterWizard.Style?): Pair<NoteClip, Harmony> {
        val s = style ?: StarterWizard.style(FALLBACK_STYLE) ?: StarterWizard.STYLES.first()
        val mood = when (project.harmony.scale) {
            Scale.Major -> StarterWizard.Mood.Happy
            Scale.Minor -> StarterWizard.Mood.Dark
            Scale.Dorian -> StarterWizard.Mood.Future
            else -> s.melody.mood
        }
        val harmony = if (project.harmony.scale != Scale.Off) project.harmony else StarterWizard.harmonyOf(s, mood).copy(lock = false)
        val riffStyle = if (s.melody.leadPattern.isBlank()) s.copy(melody = s.melody.copy(leadPattern = FALLBACK_RIFF)) else s
        return StarterWizard.startingNotes(StarterWizard.Part.Lead, riffStyle, mood, harmony) to harmony
    }

    /**
     * [project] with a new [kind] track (null when all eight exist): its name, its sound, an
     * arpeggiator for [TrackKind.Arp]; with [write], four bars of melody ([melody]) in the scene
     * shown, and a key given to a project that had none.
     */
    fun add(project: Project, kind: TrackKind, style: StarterWizard.Style?, write: Boolean = false): Project? {
        val sound = sound(kind, style)
        val added = project.addTrack(kind.trackName) ?: return null
        val track = added.tracks.first { t -> project.tracks.none { it.id == t.id } }
        val written = if (write) melody(project, style) else null
        var notes = written?.first ?: track.notes
        if (kind == TrackKind.Arp) notes = notes.copy(performance = notes.performance.copy(arp = ArpMode.UpDown, arpRate = style?.melody?.arpRate ?: NoteClip.TICKS_PER_STEP))
        // The style's line slides where it says '/' (its bass, the riff a Melody track writes).
        val glides = (kind == TrackKind.Bass && style?.melody?.bassGlide == true) ||
            (kind == TrackKind.Melody && write && style?.melody?.leadGlide == true)
        val made = track.copy(
            notes = notes,
            synth = if (glides) sound.synth.with(SynthPatch.GLIDE, StarterWizard.BASS_GLIDE) else sound.synth,
            engine = if (sound.plaits != null) 1 else 0,
            plaits = sound.plaits ?: track.plaits,
        ).sanitized()
        var next = added.withTrack(made)
        if (written != null && project.harmony.scale == Scale.Off) next = next.copy(harmony = written.second)
        return next
    }
}
