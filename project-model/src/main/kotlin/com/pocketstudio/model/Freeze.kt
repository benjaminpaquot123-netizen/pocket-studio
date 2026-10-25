package com.pocketstudio.model

/**
 * Phase 37, « Geler la piste »: an instrument track rendered once to audio, offline, over the whole
 * song, and played from an audio track ([AudioTrack.frozenFrom]) instead of its synth: its notes
 * stay silent in the song parts (the CPU its sound took is saved) and are kept, untouched, so
 * « Dégeler » brings the track back as it was. A part played on its own (outside the song) still
 * plays the notes: the frozen sound only plays with the song.
 */
object Freeze {
    /**
     * The longest song a track can be frozen over: 7 minutes (techno and bass music run 5 to 6). Its
     * sound is mono past [STEREO_SECONDS], like an audio track's: 7 minutes is 81 MB of memory.
     */
    const val MAX_SECONDS = 420

    /** A frozen sound up to this long stays in stereo (2 minutes: 46 MB). */
    const val STEREO_SECONDS = 120

    /** A song of [seconds] can be frozen. */
    fun fits(seconds: Double): Boolean = seconds <= MAX_SECONDS

    /** The frozen sound of a song of [seconds] is kept (and rendered) in mono. */
    fun mono(seconds: Double): Boolean = seconds > STEREO_SECONDS

    /** The audio track holding track [id]'s frozen sound, or null when it is not frozen. */
    fun audioOf(p: Project, id: Int): AudioTrack? =
        p.audioTracks.firstOrNull { it.frozenFrom == id && it.clip != null }?.takeIf { p.instrumentTracks.any { t -> t.id == id } }

    fun isFrozen(p: Project, id: Int): Boolean = audioOf(p, id) != null

    /** The ids of the frozen instrument tracks. */
    fun frozenIds(p: Project): Set<Int> = p.audioTracks.mapNotNull { t -> t.frozenFrom?.takeIf { t.clip != null && p.instrumentTracks.any { it.id == t.frozenFrom } } }.toSet()

    /** The part keys silenced in a song part because their sound plays frozen. */
    fun mutedKeys(p: Project): Set<String> = frozenIds(p).map { SongPart.inst(it) }.toSet()

    /**
     * What track [id]'s frozen sound depends on: the song, the tempo and metre, its notes in every
     * scene, its sound and its strip. A frozen track whose signature changed no longer matches.
     */
    fun signature(p: Project, id: Int): Int {
        val t = p.track(id)
        val notes = p.allScenes.map { s -> if (id == 0) s.synthNotes else s.trackNotes[id] }
        // From the text of the values, not their hashCode(): an enum's (the arpeggio's mode) changes
        // with every launch of the app, a frozen track would never be up to date again.
        // G1b: its layers too, only when it has some (a track frozen before them stays up to date). The
        // synth's settings appended since (« des basses qui tapent ») count only off their defaults.
        return (listOf(p.song, p.tempoBpm, p.swing, p.beatsPerBar, notes, SynthPatch.beforeBassParams(t.synth), t.plaits, t.instrument, t.sampled, t.engine,
            t.multi, t.voice808,
            p.mixer.channels.getOrNull(InstrumentTrack.mixerChannel(id)), p.automation.filter { it.target.ch == InstrumentTrack.mixerChannel(id) }) +
            // (their names and roles change nothing heard)
            listOfNotNull(t.layers.takeIf { it.isNotEmpty() }?.map { it.copy(label = "", role = "") })).toString().hashCode()
    }

    /** Track [id] is frozen but the song (or the track) changed since: « regèle-la ». */
    fun isStale(p: Project, id: Int): Boolean = audioOf(p, id)?.let { it.frozenSignature != signature(p, id) } ?: false

    /**
     * Track [id] frozen into a new audio track named [name], its sound [file] ([seconds] long) a song
     * clip from the song's start; null when every audio track is taken or there is no song.
     */
    fun frozen(p: Project, id: Int, file: String, seconds: Double, name: String): Project? {
        if (p.song.isEmpty() || isFrozen(p, id) || p.instrumentTracks.none { it.id == id }) return null
        val grown = p.addAudioTrack(name.take(InstrumentTrack.MAX_NAME)) ?: return null
        val audio = grown.audioTracks.first { t -> p.audioTracks.none { it.id == t.id } }
        val track = p.track(id)
        return grown.withAudioTrack(
            audio.copy(
                color = track.color,
                clip = AudioClip(file, name.take(40), seconds, start = 0, song = true, bpm = p.tempoBpm),
                frozenFrom = id,
                frozenSignature = signature(p, id),
            ),
        )
    }

    /** « Dégeler »: the audio track of track [id]'s frozen sound goes; its notes play again. */
    fun unfrozen(p: Project, id: Int): Project {
        val audio = audioOf(p, id) ?: return p
        return p.removeAudioTrack(audio.id)
    }
}

/**
 * Phase 37: [lane] (a song lane with a point per beat) back to a point per bar: each bar keeps its
 * first point drawn (the finer points between are dropped; Annuler brings them back).
 */
fun perBar(lane: AutomationLane, beatsPerBar: Int): AutomationLane {
    if (!lane.songPerBeat) return lane
    val beats = beatsPerBar.coerceAtLeast(1)
    val bars = lane.songValues.chunked(beats).map { bar -> bar.firstOrNull { it != null } }.take(AutomationLane.MAX_SONG_BARS)
    return lane.copy(songPerBeat = false, songValues = bars.dropLastWhile { it == null }, songJumps = lane.songJumps.map { it / beats }.distinct().filter { it < bars.size })
}

/**
 * Phase 37: where each part of the song starts, in bars, with its name (« A · Intro »), for the
 * automation curves drawn along the song.
 */
fun Project.partMarks(): List<Pair<Int, String>> {
    var bar = 0
    return song.map { part ->
        val mark = bar to sceneTitle(part.scene)
        bar += (partSteps(part) + stepsPerBar - 1) / stepsPerBar
        mark
    }
}
