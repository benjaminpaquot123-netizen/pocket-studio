package com.pocketstudio.model

/**
 * Audit étage 0 and §3: the test songs, all made here from the app's own assistant (nothing from
 * Benjamin's projects or from a song: the repository is public). Each is written to its test
 * resource by [SyntheticSongsTest] (`PS_REGENERATE=1`), and the committed file must stay equal.
 */
object SyntheticSongs {
    /**
     * The sounds of the app's own library (Plaits, rendered by the engine) that the test folders
     * carry as files: drum pads, audio clips and live pads point to them by name.
     */
    val SAMPLES = listOf(
        "28456cfe-cf2" to "Kick 13", "af4f056e-c38" to "Caisse claire 7", "1ac2fbe3-3f6" to "Clap 7",
        "7a33e3a2-a6d" to "Charley 4", "accd5484-e87" to "Percu 8", "05bf504a-b2f" to "Nappe 3 · Future bass",
    )

    fun wizard(id: String): Project {
        val style = StarterWizard.style(id)!!
        return StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? }, style.melody.mood,
            StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() })
    }

    /**
     * A beginner's jump up draft: the assistant's song, then what a newcomer typically leaves (the
     * Guide's diagnosis must find each one): the three Drops are the same Drop, Drops 2 and 3 with
     * their bass played by « Basse 2 » (only the sound changes); the pad plays all through each Drop; nothing
     * panned, every fader at 0 dB; the kick's pad hard left; Sub, Sub drop and Voix without any effect;
     * no low cut on Nappe, Lead and Voix; three fader curves drawn by hand.
     */
    fun jumpUpDraft(): Project {
        var p = wizard("jump_up").copy(name = "Brouillon jump up")
        val dropC = 2
        val dropH = 7
        val dropI = 8
        val scenes = p.scenes.toMutableList()
        // « Basse 2 » doubles the bass in the assistant's Drop: here it is silent there, then takes the bass over in I.
        val bass2 = 4
        val c = scenes[dropC].let { it.copy(trackNotes = it.trackNotes + (bass2 to NoteClip(steps = it.synthNotes.steps))) }
        scenes[dropC] = c
        // Scenes H and I become copies of C, the bass played by « Basse 2 » in I.
        scenes[dropH] = c.copy(name = scenes[dropH].name, label = scenes[dropH].label)
        scenes[dropI] = c.copy(name = scenes[dropI].name, label = scenes[dropI].label, synthNotes = NoteClip(steps = c.synthNotes.steps),
            trackNotes = c.trackNotes + (bass2 to c.synthNotes))
        p = p.copy(scenes = scenes)
        // Drop 1 plays C (or its copy H), Drops 2 and 3 play I all through.
        val drops = DropVariation.drops(p).drop(1)
        p = p.copy(song = p.song.mapIndexed { i, part ->
            val bar = p.songStepOf(i) / p.stepsPerBar
            if (part.scene in setOf(dropC, dropH, dropI) && drops.any { bar >= it.start && bar < it.start + it.bars }) part.copy(scene = dropI) else part
        })
        // Every part plays every track (no Drop « 1A » on the bass alone), no curve drawn, and the pad plays all through.
        p = p.copy(song = p.song.map { it.copy(muted = emptySet()) }, automation = emptyList())
        // The voice answers the lead in the Drops: its first bar, an octave down.
        val lead = 2
        val voice = 6
        p = p.copy(scenes = p.scenes.map { s ->
            val l = s.trackNotes[lead]
            if (!s.label.startsWith("Drop") || l == null) s
            else s.copy(trackNotes = s.trackNotes + (voice to l.copy(notes = l.notes.filter { it.start < 16 * NoteClip.TICKS_PER_STEP }.map { it.copy(pitch = it.pitch - 12) })))
        })
        val pad = 1
        p = p.copy(scenes = p.scenes.map { s ->
            val notes = s.trackNotes[pad] ?: return@map s
            if (s.label.startsWith("Drop")) s.copy(trackNotes = s.trackNotes + (pad to held(notes, s.stepsFor(16))))
            else s
        })
        // The mix as it comes when nothing was touched, but the kick's pad hard left (a lane shift).
        val bare = setOf(3, 7, 6).map(InstrumentTrack::mixerChannel).toSet()
        val noCut = setOf(1, 2, 6).map(InstrumentTrack::mixerChannel).toSet()
        p = p.copy(
            mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, ch ->
                ch.copy(
                    volume = 0.8f, pan = 0f,
                    inserts = when (i) {
                        in bare -> ch.inserts.map { Insert() }
                        in noCut -> ch.inserts.map { if (lowCut(it)) Insert() else it }
                        else -> ch.inserts
                    },
                )
            }),
            drums = p.drums.copy(lanes = p.drums.lanes.mapIndexed { i, l ->
                // Five pads play a sound of the library saved in the project's folder, as « Remplacer le son » does.
                val sample = DRAFT_PADS[i]?.let { k -> SAMPLES[k].let { PadSample("${it.first}.wav", it.second) } } ?: l.sample
                l.copy(pan = if (i == 0) -1f else 0f, sample = sample)
            }),
            tracks = p.tracks.map { t ->
                if (t.id == pad) t.copy(engine = 2, sampled = t.sampled.copy(file = "${SAMPLES[5].first}.wav", name = SAMPLES[5].second)) else t
            },
        )
        // Three song curves drawn by hand, a fader each: the bass hands over to « Basse 2 » on the
        // second Drop, the lead waits for the second half of the first Drop.
        val starts = DropVariation.drops(p).map { it.start }
        fun curve(ch: Int, on: (Int) -> Boolean) = AutomationLane(AutoTarget(AutoTarget.VOLUME, ch), mode = AutomationLane.SONG,
            songValues = List(p.songBars) { bar -> if (on(bar)) 0.8f else 0f })
        p = p.copy(automation = listOf(
            curve(InstrumentTrack.mixerChannel(0)) { it < starts[1] },
            curve(InstrumentTrack.mixerChannel(bass2)) { it >= starts[1] },
            curve(InstrumentTrack.mixerChannel(lead)) { it !in starts[0] until starts[0] + 16 },
        ))
        // The current scene is mirrored in the project's own fields: shown again from the scenes edited.
        return p.showing(p.scenes, p.scene).sanitized()
    }

    /** Drum pad → index in [SAMPLES] in the draft (kick, snare, clap, hats, percussion). */
    private val DRAFT_PADS = mapOf(0 to 0, 2 to 1, 4 to 2, 5 to 3, 10 to 4)

    /** Audit §3: the empty reference project, as « Nouveau projet » makes it. */
    fun empty(): Project = Project(name = "Référence vide").sanitized()

    /** Audit §3: a small project, the assistant's house song cut to its first 16 bars or less. */
    fun small(): Project {
        var p = wizard("house").copy(name = "Référence petit")
        while (p.songBars > 16 && p.song.size > 1) p = p.removeSongPart(p.song.size - 1)
        return p.sanitized()
    }

    /**
     * Audit §3: the huge reference project: 100 bars, 16 tracks (drums, the 303, the 8 instrument
     * tracks, 6 audio tracks), 16 live pads and 16 drum pads, each with a sound. [seconds] gives each
     * sample's length (read from the files). The separated song is not in it: a song stays out of the
     * repository (a local copy only, AUDIT §3).
     */
    fun huge(seconds: (String) -> Double): Project {
        var p = wizard("jump_up").copy(name = "Référence énorme")
        while (p.instrumentTracks.size < InstrumentTrack.MAX) p = p.addTrack() ?: break
        val small = SAMPLES.take(5)
        for (i in 0 until AudioTrack.MAX - 2) {
            p = p.addAudioTrack() ?: break
            val (file, name) = small[i % small.size]
            val id = p.audioTracks.last().id
            p = p.withAudioTrack(p.audioTracks.last().copy(clip = AudioClip("$file.wav", name, seconds = seconds("$file.wav"), loopSteps = 16 * (1 + i % 2), gain = 0.5f)))
            check(p.audioTracks.any { it.id == id && it.clip != null })
        }
        p = p.copy(
            live = LivePads(size = LivePads.MAX, clips = List(LivePads.MAX) { i ->
                val (file, name) = small[i % small.size]
                LiveClip("$file.wav", "$name ${i + 1}", beats = 4, oneShot = i % 4 == 0, gain = 0.8f)
            }),
            drums = p.drums.copy(lanes = p.drums.lanes.mapIndexed { i, l -> l.copy(sample = small[i % small.size].let { PadSample("${it.first}.wav", it.second) }) }),
        )
        // 100 bars: parts taken off the end, the last one cut to fit.
        while (p.songBars > 100 && p.song.size > 1) p = p.removeSongPart(p.song.size - 1)
        while (p.songBars < 100) p = p.duplicateSongPart(p.song.size - 1)
        if (p.songBars > 100) {
            val last = p.song.last()
            val bars = p.partSteps(last) / p.stepsPerBar - (p.songBars - 100)
            p = p.copy(song = p.song.dropLast(1) + last.copy(bars = bars))
        }
        return p.sanitized()
    }

    /** The pad's chords held over each whole bar: the pitches it plays in each bar, from the bar's start to its end. */
    private fun held(clip: NoteClip, steps: Int): NoteClip {
        val bar = 16 * NoteClip.TICKS_PER_STEP
        val bars = steps / 16
        val byBar = (0 until bars).map { b -> clip.notes.filter { it.start / bar == b }.map { it.pitch }.distinct() }
        val fallback = byBar.firstOrNull { it.isNotEmpty() } ?: clip.notes.map { it.pitch }.distinct()
        var last = fallback
        val notes = (0 until bars).flatMap { b ->
            val pitches = byBar[b].ifEmpty { last }.also { last = it }
            pitches.map { Note(start = b * bar, length = bar, pitch = it, velocity = 0.7f) }
        }
        return clip.copy(steps = steps, notes = notes)
    }

    private fun lowCut(i: Insert): Boolean =
        (i.effect == EffectType.FILTER && i.params.getOrElse(2) { 0f } >= 2f / 3f) || (i.effect == EffectType.PARAM_EQ && i.params.getOrElse(3) { 0f } >= 0.01f)
}
