package com.pocketstudio.model

/**
 * What each instrument track is for, read from its name (the app's own names, in any language)
 * and, failing that, from where its notes sit: the part sheet's Break, Fin and Énergie keep the
 * pads and the voices and take the kick and the bass out.
 */
object TrackRoles {
    /** Bass tracks: the assistant's and the user's (« Basse », a « Sub » layer, an 808). */
    private val BASS_NAMES = setOf("Basse", StarterWizard.SUB_TRACK, "808", "Basse 808", TrackKind.Bass.trackName)

    /** Chord and pad tracks: what a Break keeps under the voices. */
    private val PAD_NAMES = (StarterWizard.CHORD_TRACK_NAMES + TrackKind.Chords.trackName).toSet()

    /** Below this MIDI note (Do 3), a line without a telling name is a bass. */
    const val BASS_BELOW = 48

    private fun french(name: String) = DefaultNames.french(name) ?: name

    /** True when [name] says bass (a default name, or a typed one with the word in it). */
    fun isBassName(name: String): Boolean {
        val fr = french(name)
        if (fr in BASS_NAMES) return true
        val low = name.lowercase()
        return low.contains("bass") || low.contains("basse") || low.contains("808") || low == "sub"
    }

    /** True when [name] says chords or a pad. */
    fun isPadName(name: String): Boolean {
        val fr = french(name)
        if (fr in PAD_NAMES) return true
        val low = name.lowercase()
        return low.contains("pad") || low.contains("nappe") || low.contains("chord") || low.contains("accord")
    }

    /** True when [name] is the riser the assistant adds before a Drop. */
    fun isRiserName(name: String): Boolean = french(name) == StarterWizard.RISER_TRACK || french(name) == StarterWizard.OLD_RISER_TRACK
}

/** The median pitch of track [t]'s notes over every scene, or null without notes. */
internal fun Project.medianPitch(t: InstrumentTrack): Int? {
    val notes = allScenes.flatMap { s -> (if (t.id == 0) s.synthNotes else s.trackNotes[t.id])?.notes.orEmpty() }.ifEmpty { t.notes.notes }
    if (notes.isEmpty()) return null
    return notes.map { it.pitch }.sorted()[notes.size / 2]
}

/** The ids of the bass tracks: named so, or a line that sits below Do 3 without a telling name. */
fun Project.bassTrackIds(): List<Int> = instrumentTracks.filter { t ->
    TrackRoles.isBassName(t.name) || (!TrackRoles.isPadName(t.name) && !TrackRoles.isRiserName(t.name) && (medianPitch(t) ?: 60) < TrackRoles.BASS_BELOW)
}.map { it.id }

/** The ids of the pad and chord tracks (named so). */
fun Project.padTrackIds(): List<Int> = instrumentTracks.filter { TrackRoles.isPadName(it.name) }.map { it.id }

/** The assistant's riser track (« Souffle montant »), if the project has one. */
fun Project.riserTrackId(): Int? = instrumentTracks.firstOrNull { TrackRoles.isRiserName(it.name) }?.id

/** The part keys of everything that plays a bass: the 303 and the bass tracks (what a Break takes out). */
fun Project.bassKeys(): Set<String> = setOf(SongPart.ACID) + bassTrackIds().map { SongPart.inst(it) }

/**
 * Every mixer strip that plays a bass (« des basses qui tapent », docs/hits/basses-qui-tapent.md):
 * the bass tracks ([bassTrackIds]: the sub and the second bass included), the sub drop, and the pad
 * that plays the 808 voice. The song bench renders them alone to hear what a phone keeps of the bass.
 */
fun Project.bassChannels(): List<Int> {
    val tracks = bassTrackIds() + instrumentTracks.filter { (DefaultNames.french(it.name) ?: it.name) == StarterWizard.SUB_DROP_TRACK }.map { it.id }
    val pads = padVoices.filterValues { it.kind == PadVoice.BASS_808 }.keys.map { Mixer.padChannel(it) }
    return (tracks.distinct().map { InstrumentTrack.mixerChannel(it) } + pads).distinct().sorted()
}
