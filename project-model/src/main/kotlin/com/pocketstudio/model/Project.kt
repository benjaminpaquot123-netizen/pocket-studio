package com.pocketstudio.model

import com.pocketstudio.model.i18n.tr
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * A song project. Immutable: every edit produces a new value, which makes undo/redo,
 * autosave and handing snapshots to the audio engine trivial.
 *
 * Format rules: every field has a default, unknown keys are ignored. Adding a field never
 * breaks old files; renaming or changing a field's meaning requires bumping [FORMAT_VERSION]
 * and a migration in [ProjectJson].
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Project(
    val formatVersion: Int = FORMAT_VERSION,
    val name: String = "Nouveau projet",
    val tempoBpm: Double = 120.0,
    /** 0 = straight, 0.5 = heavy shuffle (delay of every second 16th, as a fraction of a 16th). */
    val swing: Double = 0.0,
    val drums: DrumPattern = DrumPattern(),
    val mixer: Mixer = Mixer(),
    val acid: AcidPattern = AcidPattern(),
    val synth: SynthPatch = SynthPatch(),
    val synthNotes: NoteClip = NoteClip(),
    val harmony: Harmony = Harmony(),
    /** The synth track's sound: null = the subtractive synth, else a General MIDI instrument. */
    val instrument: InstrumentChoice? = null,
    /** A separated song played along (Remix tab). */
    val remix: Remix = Remix(),
    /** Mixer settings that move with the loop, step by step. */
    val automation: List<AutomationLane> = emptyList(),
    /** Session: up to 24 scenes; [scene] is the one shown and played (mirrored in drums/acid/notes). */
    val scenes: List<Scene> = emptyList(),
    val scene: Int = 0,
    /** The song: scenes in order with repeat counts. */
    val song: List<SongPart> = emptyList(),
    /** The synth track's own sound: 0 = subtractive synth, 1 = Plaits, 2 = [sampled], 3 = [multi], 4 = the 808 (an [instrument] still wins). */
    val synthEngine: Int = 0,
    val plaits: PlaitsPatch = PlaitsPatch(),
    /** The first instrument track's sample played across the keys (see [SampledPatch]). */
    val sampled: SampledPatch = SampledPatch(),
    /** Live pads: sounds switched on and off by a tap, in time. */
    val live: LivePads = LivePads(),
    /** The first instrument track's name and colour (its sound is [synth] / [plaits] / [instrument]). */
    val synthTrackName: String = "Synthé",
    val synthTrackColor: Int = 0,
    /** Instrument tracks added after the first one (see [InstrumentTrack]). */
    val tracks: List<InstrumentTrack> = emptyList(),
    /** Audio tracks: recorded or imported clips (see [AudioTrack]). */
    val audioTracks: List<AudioTrack> = emptyList(),
    /** Time signature [beatsPerBar]/4 (2 to 7). */
    val beatsPerBar: Int = 4,
    /** MIDI learn: controller knobs mapped to settings. */
    val midiMap: List<MidiMapping> = emptyList(),
    /** At the song's end: false = playback stops (the default, older projects too), true = it starts over. */
    val songLoops: Boolean = false,
    /**
     * « ⟳ Partie »: ▶ loops the part being edited (the scene shown) instead of playing the song.
     * Off by default: once there is a song, ▶ plays it on every tab. (Replaces the old
     * « Écouter : cette section / tout le morceau » choice, whose saved value is ignored.)
     */
    val loopPart: Boolean = false,
    /** The live grid's launch quantum in bars (1, 2 or 4): a pad starts on the next one. */
    val liveQuantum: Int = 1,
    /** The Live page's effect settings (grid, release effect, lengths): see [LiveFxSettings]. */
    val liveFx: LiveFxSettings = LiveFxSettings(),
    /**
     * The id of the last Live take written into the song ([PerformanceTake.id]): a take kept on
     * disk while it recorded ([TakeJournal]) is never written into the song a second time.
     */
    val liveTake: String = "",
    /**
     * A song the assistant just made: its live pads are still to be prepared (loops of its tracks,
     * rendered once its sounds are loaded; see [LivePadPlan]). Left out of the file when false:
     * every other project is saved as before.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val livePadsPending: Boolean = false,
    /**
     * The built-in kit's levels ([KIT_V1], [KIT_V2] or [KIT_V3], the engine's DrumKit::setVersion):
     * kit v2 (phase 33) puts each voice near its loudness in a mix; kit v3 (phase 34, S3: the
     * assistant's songs since) has the short studio kick, bright hats and a four-burst clap. The
     * projects saved before keep the kit they had. Left out of the file at v1: those are saved as before.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val kitVersion: Int = KIT_V1,
    /**
     * The pads that play a studio voice of the engine instead of their kit voice (phase 34, S3):
     * the 808, the studio kick, the hard kick, the rumble, with their settings ([PadVoice]). Like a
     * pad's sound, the same in every scene; a pad's own sample still wins over it. Left out of the
     * file when empty: every project saved before has none and sounds as it did.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val padVoices: Map<Int, PadVoice> = emptyMap(),
    /**
     * The instrument tracks' sound ([SOUND_V1], [SOUND_V2] or [SOUND_V3], the engine's SetSoundVersion):
     * since phase 34 (S4) the assistant's songs play their General MIDI instruments and stereo samples
     * in stereo; since the leads chantier (lot 1, [SOUND_V3]) their Plaits FM voices follow the
     * velocity too (louder when struck harder). The projects saved before keep theirs. Left out of
     * the file at v1.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val soundVersion: Int = SOUND_V1,
    /**
     * The mix ([MIX_V1] to [MIX_V4], the engine's SetMixVersion): since phase 34 (S5) the
     * assistant's songs aim their loudness target at their loudest part (the Drop), the master
     * chain taking 3 LU at most there, with a real sidechain and a « Musique » bus; the projects
     * saved before keep theirs (the target on the whole song). Left out of the file at v1.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val mixVersion: Int = MIX_V1,
    /**
     * The assistant's style the song was made with ([StarterWizard.Style.id]), null for a project
     * made by hand: the master's automatic loudness target follows it ([LoudnessTargets]). Left
     * out of the file when null: every other project is saved as before.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val style: String? = null,
    /**
     * The versions of the song kept aside, newest first ([SongVersion]): each Live take keeps the
     * song it replaces there, so no take overwrites the song without a way back. Left out of the
     * file when empty.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val songVersions: List<SongVersion> = emptyList(),
    /**
     * A launch quantum shorter than a bar on the Live page: [LiveSlots.QUANTUM_BEAT] (a beat) or
     * [LiveSlots.QUANTUM_HALF_BAR]; 0 (left out of the file): [liveQuantum] bars.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val liveQuantumFine: Int = 0,
    /**
     * Phase 37: the seed the drums' chances are drawn from ([Hit.chance], [DrumChance]): the same
     * seed, the same passes (an export is the song heard); « Autre tirage » picks another. Left out
     * of the file at 0.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val drumSeed: Int = 0,
    /** Phase 37: the first instrument track's [InstrumentTrack.followChords] (its other fields are here too). */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val synthFollowChords: Boolean? = null,
    /** The first instrument track's multizone instrument (engine 3, phase 38); left out of the file when none. */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val multi: MultiPatch = MultiPatch(),
    /** The first instrument track's 808 settings (engine 4, phase 38); left out of the file at its defaults. */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val voice808: PadVoice = PadVoice(PadVoice.BASS_808),
    /**
     * Whether the 303's steps follow the song's chords ([ChordTrack.acidFollows]): null (left out of
     * the file), the assistant's songs follow and a song made by hand keeps its line.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val acidFollowChords: Boolean? = null,
    /**
     * The Guide's state in this song (docs/PLAN-GUIDE.md §4.1, [com.pocketstudio.model.guide.GuideState]):
     * where it is, what was decided, refused, changed. It lives in the project, so a .zip, an import
     * or a version restored carries it. Left out of the file while empty.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val guide: com.pocketstudio.model.guide.GuideState = com.pocketstudio.model.guide.GuideState.EMPTY,
    /** G1b: the first instrument track's [InstrumentTrack.layers]; left out of the file when it has none. */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val synthLayers: List<TrackLayer> = emptyList(),
) {
    /** Sixteenths in a bar. */
    val stepsPerBar: Int get() = beatsPerBar * 4

    /** Returns a copy with every value inside its allowed range. */
    fun sanitized(): Project = copy(
        // A value that is not a number (NaN) would make every later save fail: back to the default.
        tempoBpm = (if (tempoBpm.isFinite()) tempoBpm else 120.0).coerceIn(MIN_TEMPO, MAX_TEMPO),
        swing = (if (swing.isFinite()) swing else 0.0).coerceIn(0.0, MAX_SWING),
        drums = drums.sanitized(),
        acid = acid.sanitized(),
        synth = synth.sanitized(),
        synthNotes = synthNotes.sanitized(),
        harmony = harmony.sanitized(),
        instrument = instrument?.takeIf { it.bank in 0..128 && it.program in 0..127 },
        mixer = mixer.sanitized(),
        remix = remix.sanitized(),
        automation = AutomationLane.sanitize(automation),
        synthEngine = synthEngine.coerceIn(0, InstrumentTrack.ENGINE_808),
        plaits = plaits.sanitized(),
        sampled = sampled.sanitized(),
        live = live.sanitized(),
        synthTrackName = synthTrackName.take(InstrumentTrack.MAX_NAME).ifBlank { "Synthé" },
        synthTrackColor = synthTrackColor.coerceIn(0, InstrumentTrack.COLORS - 1),
        tracks = tracks.filter { it.id in 1 until InstrumentTrack.MAX }.distinctBy { it.id }.sortedBy { it.id }.map { it.sanitized() },
        beatsPerBar = beatsPerBar.coerceIn(MIN_BEATS, MAX_BEATS),
        midiMap = MidiMapping.sanitize(midiMap),
        audioTracks = audioTracks.filter { it.id in 0 until AudioTrack.MAX }.distinctBy { it.id }.sortedBy { it.id }.map { a ->
            // The frozen sound of a track that is not there any more is an ordinary audio track.
            a.sanitized().let { s -> if (s.frozenFrom != null && s.frozenFrom != 0 && tracks.none { it.id == s.frozenFrom }) s.copy(frozenFrom = null, frozenSignature = 0) else s }
        },
        liveQuantum = liveQuantum.takeIf { it in LiveSlots.QUANTA } ?: 1,
        liveFx = liveFx.sanitized(),
        liveTake = liveTake.take(64),
        kitVersion = kitVersion.coerceIn(KIT_V1, KIT_V3),
        padVoices = PadVoice.sanitize(padVoices),
        soundVersion = soundVersion.coerceIn(SOUND_V1, SOUND_V3),
        mixVersion = mixVersion.coerceIn(MIX_V1, MIX_V4),
        style = style?.take(64)?.ifBlank { null },
        songVersions = songVersions.take(SongVersion.MAX).map { it.sanitized(this) },
        liveQuantumFine = liveQuantumFine.takeIf { it in LiveSlots.FINE_QUANTA } ?: 0,
        drumSeed = drumSeed.coerceIn(0, DrumChance.MAX_SEED),
        multi = multi.sanitized(),
        voice808 = voice808.copy(kind = PadVoice.BASS_808).sanitized(),
        guide = guide.sanitized(),
        synthLayers = Layers.sanitize(synthLayers),
    ).syncScenes().withFittedSampleRoots()

    companion object {
        const val FORMAT_VERSION = 1
        /** [kitVersion]: the kit as it always was, kit v2 (phase 33), kit v3 (the assistant's songs since phase 34). */
        const val KIT_V1 = 1
        const val KIT_V2 = 2
        const val KIT_V3 = 3
        /** [soundVersion]: mono instrument tracks as always, and stereo ones (the assistant's songs since S4). */
        const val SOUND_V1 = 1
        const val SOUND_V2 = 2
        /** The leads chantier, lot 1: as [SOUND_V2], and Plaits' six-operator FM voices louder the harder a note is struck. */
        const val SOUND_V3 = 3
        /** [mixVersion]: the target on the whole song as always, and on its Drop (the assistant's songs since S5). */
        const val MIX_V1 = 1
        const val MIX_V2 = 2
        /**
         * « Des basses qui tapent » (docs/hits/basses-qui-tapent.md, P4): [MIX_V2] with the master's
         * low cut at 25 Hz (the engine's MasterChain::setLowCut). Not given to the assistant's songs:
         * on the bench it made the limiter take more off the Drop (docs/son/banc/apres-basses.md).
         */
        const val MIX_V3 = 3
        /**
         * S7 son (docs/son/banc/S7.md): [MIX_V2] (no master low cut), the assistant's songs since: their
         * pads, chords and leads spread wide (the widener's « Étalement »), bass and sub mono. What
         * sounds different is in the songs' own inserts; the number tells a song made since.
         */
        const val MIX_V4 = 4
        const val MIN_TEMPO = 40.0
        const val MAX_TEMPO = 300.0
        const val MAX_SWING = 0.75
        const val MIN_BEATS = 2
        const val MAX_BEATS = 7
    }
}

@Serializable
data class DrumPattern(
    val steps: Int = 16,
    val lanes: List<DrumLane> = List(PAD_COUNT) { DrumLane() },
) {
    fun sanitized(): DrumPattern {
        val s = steps.coerceIn(MIN_STEPS, MAX_STEPS) / 4 * 4
        // Already clean: the pattern itself (an edit on a big project rebuilds only what changed).
        if (s == steps && lanes.size == PAD_COUNT && lanes.all { it.isClean() }) return this
        val fixedLanes = (lanes + List((PAD_COUNT - lanes.size).coerceAtLeast(0)) { DrumLane() }).take(PAD_COUNT)
        return copy(
            steps = s,
            lanes = fixedLanes.map { lane ->
                lane.copy(
                    // Hits past the loop's end stay, unheard: a time signature tried and undone gives them back.
                    hits = lane.hits.filterKeys { it in 0 until MAX_STEPS }.mapValues { (_, h) -> h.sanitized() },
                    sample = lane.sample?.takeIf { it.isSafe }?.sanitized(),
                    pan = lane.pan.finite(0f).coerceIn(-1f, 1f),
                    length = lane.length.coerceIn(0, MAX_STEPS),
                )
            },
        )
    }

    /**
     * The loop at [newSteps] (whole bars, 16 to 64): a longer loop repeats the current bars so the
     * groove keeps going; a shorter one drops the hits past its end (undo brings them back).
     */
    fun withSteps(newSteps: Int): DrumPattern {
        val n = (newSteps.coerceIn(MIN_STEPS, MAX_STEPS) / 4) * 4
        if (n == steps) return this
        return copy(
            steps = n,
            lanes = lanes.map { lane ->
                val hits = if (n > steps) (0 until n).mapNotNull { st -> lane.hits[st % steps]?.let { st to it } }.toMap()
                else lane.hits.filterKeys { it < n }
                lane.copy(hits = hits)
            },
        )
    }

    private fun withHits(lane: Int, hits: Map<Int, Hit>): DrumPattern =
        copy(lanes = lanes.toMutableList().also { it[lane] = it[lane].copy(hits = hits) })

    /** A hit every [every] steps over the whole loop (replacing the lane's hits). */
    fun fill(lane: Int, every: Int, velocity: Float = 1f): DrumPattern =
        withHits(lane, (0 until steps step every.coerceAtLeast(1)).associateWith { Hit(velocity) })

    /** The lane's hits moved by [by] steps, wrapping around the loop (those past its end stay put). */
    fun rotate(lane: Int, by: Int): DrumPattern =
        withHits(lane, lanes[lane].hits.mapKeys { (step, _) -> if (step < steps) Math.floorMod(step + by, steps) else step })

    /** Next roll for a hit (the « Roulements » tool): 1 → 2 → 3 → 4 → 1. No hit: nothing changes. */
    fun cycleRepeats(lane: Int, step: Int): DrumPattern {
        val hit = lanes[lane].hits[step] ?: return this
        return withHits(lane, lanes[lane].hits + (step to hit.copy(repeats = hit.repeats % CYCLE_REPEATS + 1)))
    }

    /**
     * One hit's roll: [repeats] hits in its step (1..[MAX_REPEATS]) and the way its strength goes
     * along them ([Hit.ramp]: 0 even, 1 rising, -1 falling). No hit there: unchanged.
     */
    fun withRoll(lane: Int, step: Int, repeats: Int, ramp: Int = 0): DrumPattern {
        val hit = lanes[lane].hits[step] ?: return this
        return withHits(lane, lanes[lane].hits + (step to hit.copy(repeats = repeats.coerceIn(1, MAX_REPEATS), ramp = ramp.coerceIn(-1, 1))))
    }

    /**
     * A burst of 32nd notes over the beat of [step] (its four 16ths, each doubled, [ramp] as in
     * [withRoll]): the steps without a hit get one; the hit's own strength is kept on the others.
     */
    fun burst(lane: Int, step: Int, ramp: Int = 1): DrumPattern {
        if (step !in 0 until steps) return this
        val first = step / 4 * 4
        val velocity = lanes[lane].hits[step]?.velocity ?: 1f
        val added = (first until minOf(first + 4, steps)).associateWith { st ->
            (lanes[lane].hits[st] ?: Hit(velocity)).copy(repeats = 2, ramp = ramp.coerceIn(-1, 1))
        }
        return withHits(lane, lanes[lane].hits + added)
    }

    /** The cell at [step] with a hit ([on]) or without: painting with a finger sets, never toggles. */
    fun setHit(lane: Int, step: Int, on: Boolean, velocity: Float = 1f): DrumPattern {
        if (step !in 0 until steps) return this
        val current = lanes[lane]
        val has = step in current.hits
        if (has == on) return this
        return withHits(lane, if (on) current.hits + (step to Hit(velocity)) else current.hits - step)
    }

    /**
     * Phase 37: pad [lane]'s own loop of [length] steps (a polyrhythm: 3 against 4…), 0 or the
     * pattern's length = the pattern's loop. Its hits past that length stay, unheard.
     */
    fun withLaneLength(lane: Int, length: Int): DrumPattern {
        if (lane !in lanes.indices) return this
        val n = length.coerceIn(0, MAX_STEPS).let { if (it == steps) 0 else it }
        return copy(lanes = lanes.toMutableList().also { it[lane] = it[lane].copy(length = n) })
    }

    /** Phase 37: one hit's chance to play on each pass, 0..100 % (no hit there: unchanged). */
    fun withHitChance(lane: Int, step: Int, chance: Int): DrumPattern {
        val hit = lanes.getOrNull(lane)?.hits?.get(step) ?: return this
        return withHits(lane, lanes[lane].hits + (step to hit.copy(chance = chance.coerceIn(Hit.MIN_CHANCE, 100))))
    }

    /**
     * The pad's place in the stereo field, -1 (left) .. 1 (right), for every scene (a pad's pan is the
     * song's). Near the middle it snaps to the centre, as the console's pan does (G0: a kick dragged
     * off-centre by a finger is hard to bring back exactly).
     */
    fun withPadPan(lane: Int, pan: Float): DrumPattern {
        val p = pan.coerceIn(-1f, 1f).let { if (kotlin.math.abs(it) < PAN_SNAP) 0f else it }
        return copy(lanes = lanes.toMutableList().also { it[lane] = it[lane].copy(pan = p) })
    }

    /** One hit's own pan and pitch (no hit: nothing changes). */
    fun withHitDetail(lane: Int, step: Int, pan: Float, pitch: Int): DrumPattern {
        val hit = lanes[lane].hits[step] ?: return this
        return withHits(lane, lanes[lane].hits + (step to hit.copy(pan = pan, pitch = pitch).sanitized()))
    }

    /** One hit's strength, 0.05..1 (« Force »): its roll, pan and pitch stay. No hit there: unchanged. */
    fun withHitVelocity(lane: Int, step: Int, velocity: Float): DrumPattern {
        val hit = lanes[lane].hits[step] ?: return this
        val v = if (velocity.isFinite()) velocity.coerceIn(MIN_VELOCITY, 1f) else hit.velocity
        return withHits(lane, lanes[lane].hits + (step to hit.copy(velocity = v)))
    }

    fun withSample(lane: Int, sample: PadSample?): DrumPattern =
        copy(lanes = lanes.toMutableList().also { it[lane] = it[lane].copy(sample = sample) })

    fun toggle(lane: Int, step: Int, velocity: Float = 1f): DrumPattern {
        val current = lanes[lane]
        val hits = if (step in current.hits) current.hits - step else current.hits + (step to Hit(velocity))
        return copy(lanes = lanes.toMutableList().also { it[lane] = current.copy(hits = hits) })
    }

    companion object {
        const val PAD_COUNT = 16
        /** A pad's pan this near the middle is the centre (the console's slider snaps the same). */
        const val PAN_SNAP = 0.04f
        /** Hits a step can hold (×2 to ×8); the engine plays up to 4 until its roll command grows (phase 34, S3). */
        const val MAX_REPEATS = 8
        /** What the « Roulements » tool cycles through (×2, ×3, ×4; the long press offers the rest). */
        const val CYCLE_REPEATS = 4
        /** The rolls the long press on a hit offers. */
        val ROLLS = listOf(1, 2, 3, 4, 6, 8)
        /** The chances the long press on a hit offers (phase 37). */
        val CHANCES = listOf(100, 75, 50, 25)
        /** The softest hit « Force » sets (velocity 0 would remove it in the engine). */
        const val MIN_VELOCITY = 0.05f
        const val MIN_STEPS = 8
        const val MAX_STEPS = 64
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class DrumLane(
    /** step index -> hit. A map keeps sparse patterns small on disk. */
    val hits: Map<Int, Hit> = emptyMap(),
    /** A sound of the user's that replaces the synthesized drum (null = the kit sound). */
    val sample: PadSample? = null,
    /** -1 (left) .. 1 (right); like the sound, the same in every scene. */
    val pan: Float = 0f,
    /**
     * Phase 37: the steps this pad loops over on its own (a polyrhythm), 0 = the pattern's length.
     * A scene's own, like its hits. Left out of the file at 0: older projects read the same.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val length: Int = 0,
) {
    /** The steps this lane plays before it starts over in a pattern of [patternSteps]. */
    fun loopSteps(patternSteps: Int): Int = if (length > 0) length else patternSteps

    /** What [DrumPattern.sanitized] would leave as it is. */
    internal fun isClean(): Boolean =
        pan.isFinite() && pan in -1f..1f && length in 0..DrumPattern.MAX_STEPS &&
            (sample == null || (sample.isSafe && sample.sanitized() == sample)) &&
            hits.all { (step, h) -> step in 0 until DrumPattern.MAX_STEPS && h.isClean() }
}

/**
 * A pad's own sound. [file] lives in the project folder under `samples/`; [name] is what the
 * user sees. The rest shapes playback (units match the engine's Sampler::Param).
 */
@Serializable
data class PadSample(
    val file: String,
    val name: String,
    val start: Float = 0f,
    val end: Float = 1f,
    val pitch: Float = 0f,
    val reverse: Boolean = false,
    val loop: Boolean = false,
    val attack: Float = 0f,
    val decay: Float = 0f,
    val sustain: Float = 1f,
    val release: Float = 0.003f,
    val choke: Int = 0,
    val gain: Float = 1f,
    /** 0 = plays at its own speed; otherwise lasts this many beats at the project tempo (pitch kept). */
    val fitBeats: Int = 0,
) {
    /** Only a plain file name may come from disk: never a path. */
    val isSafe: Boolean get() = file.isNotEmpty() && file.length <= 80 && file.matches(SAFE_NAME) && !file.startsWith(".")

    fun sanitized(): PadSample {
        val s = start.finite(0f).coerceIn(0f, 0.999f)
        val e = end.finite(1f).coerceIn(0.001f, 1f)
        return copy(
            name = name.take(40),
            start = minOf(s, e - 0.001f).coerceAtLeast(0f),
            end = maxOf(e, s + 0.001f).coerceAtMost(1f),
            pitch = pitch.finite(0f).coerceIn(-24f, 24f),
            attack = attack.finite(0f).coerceIn(0f, 2f),
            decay = decay.finite(0f).coerceIn(0f, 2f),
            sustain = sustain.finite(1f).coerceIn(0f, 1f),
            release = release.finite(0.003f).coerceIn(0f, 2f),
            choke = choke.coerceIn(0, CHOKE_GROUPS),
            gain = gain.finite(1f).coerceIn(0f, 2f),
            fitBeats = fitBeats.takeIf { it in FIT_BEATS } ?: 0,
        )
    }

    /**
     * Values in engine order (Sampler::Param). [stretched]: the engine plays a copy already cut
     * to the region, fitted to the tempo and transposed, so region and pitch are neutral.
     */
    fun engineParams(stretched: Boolean = false): FloatArray = floatArrayOf(
        if (stretched) 0f else start, if (stretched) 1f else end, if (stretched) 0f else pitch,
        if (reverse) 1f else 0f, if (loop) 1f else 0f,
        attack, decay, sustain, release, choke.toFloat(), gain,
    )

    /** Frames at 48 kHz this pad lasts when fitted to [bpm]. */
    fun fittedFrames(bpm: Double): Long = Math.round(fitBeats * 60.0 / bpm * 48_000)

    companion object {
        private val SAFE_NAME = Regex("[A-Za-z0-9._-]+")
        const val CHOKE_GROUPS = 4
        val FIT_BEATS = listOf(0, 1, 2, 4, 8, 16, 32)

        /** The number of beats (from [FIT_BEATS]) closest to [seconds] at [bpm]. */
        fun suggestBeats(seconds: Double, bpm: Double): Int {
            val beats = seconds * bpm / 60.0
            return FIT_BEATS.drop(1).minBy { kotlin.math.abs(kotlin.math.ln(it / beats)) }
        }
    }
}

internal fun Float.finite(default: Float) = if (isFinite()) this else default

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Hit(
    val velocity: Float = 1f,
    /** Hits in the step: 1 = plain, 2 to [DrumPattern.MAX_REPEATS] = a roll (the step is split evenly). */
    val repeats: Int = 1,
    /** Added to the pad's pan for this hit, -1..1. */
    val pan: Float = 0f,
    /** Semitones for this hit, -24..24 (the pad's own sounds; the kit's synthesized drums ignore it). */
    val pitch: Int = 0,
    /**
     * A roll's strength along its hits: 0 as rolls always played (each hit 0.85 x the one before,
     * a slight fall), 1 rising (a snare build: from half the velocity), -1 falling (a bounce: to
     * half). Left out of the file at 0: hits saved before read the same. The engine plays it through
     * the ramp bits of its drum-hit command (SetDrumHit bits 24..25).
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val ramp: Int = 0,
    /**
     * Micro-timing in milliseconds, -[MAX_OFFSET_MS] (early) .. [MAX_OFFSET_MS] (late): a clap a
     * few ms ahead, hats behind, a laid-back snare (phase 34, S3). Left out of the file at 0: hits
     * saved before play on the grid as they did. The engine's SetDrumStepOffset, after the hit.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val offset: Float = 0f,
    /**
     * The 808's slide into this hit's pitch, in milliseconds (0 = a new note, up to [MAX_GLIDE_MS]):
     * only a pad playing the 808 voice ([PadVoice.BASS_808]) hears it. Left out of the file at 0.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val glide: Int = 0,
    /**
     * Phase 37: the chance it plays on each pass, [MIN_CHANCE]..100 %, drawn the same way every time
     * ([DrumChance], [Project.drumSeed]). Left out of the file at 100: hits saved before always play.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val chance: Int = 100,
) {
    /** No pan nor pitch of its own (the engine's step detail command is not needed). */
    val isPlain: Boolean get() = pan == 0f && pitch == 0

    /** What [sanitized] would leave as it is. */
    internal fun isClean(): Boolean = velocity.isFinite() && repeats in 1..DrumPattern.MAX_REPEATS &&
        pan.isFinite() && pan in -1f..1f && pitch in -MAX_PITCH..MAX_PITCH && ramp in -1..1 &&
        offset.isFinite() && offset in -MAX_OFFSET_MS..MAX_OFFSET_MS && glide in 0..MAX_GLIDE_MS &&
        chance in MIN_CHANCE..100

    fun sanitized() = copy(
        // Not clamped (any strength already saved stays); only a NaN, which no save could write, goes.
        velocity = velocity.finite(1f),
        repeats = repeats.coerceIn(1, DrumPattern.MAX_REPEATS),
        pan = pan.finite(0f).coerceIn(-1f, 1f),
        pitch = pitch.coerceIn(-MAX_PITCH, MAX_PITCH),
        ramp = ramp.coerceIn(-1, 1),
        offset = offset.finite(0f).coerceIn(-MAX_OFFSET_MS, MAX_OFFSET_MS),
        glide = glide.coerceIn(0, MAX_GLIDE_MS),
        chance = chance.coerceIn(MIN_CHANCE, 100),
    )

    companion object {
        const val MAX_PITCH = 24
        /** The engine's micro-timing range (SetDrumStepOffset), each way. */
        const val MAX_OFFSET_MS = 50f
        /** The engine's longest 808 slide (SetDrumStepGlide). */
        const val MAX_GLIDE_MS = 500
        /** The least chance a hit keeps (0 % would be no hit: remove it instead). */
        const val MIN_CHANCE = 5
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Mixer(
    val channels: List<Channel> = List(CHANNELS) { Channel() },
    /** Inserts on the master, after the send returns and before the limiter. */
    val master: List<Insert> = List(INSERTS) { Insert() },
    /** Parameters of the send buses (see [Buses]). */
    val buses: List<List<Float>> = Buses.DEFAULTS,
    /** The master's gain in dB, before the limiter ([MASTER_MIN_DB]..[MASTER_MAX_DB]). */
    val masterDb: Float = 0f,
    /**
     * The master's loudness target (« Cible »). Projects saved before phase 33 have none ([LoudnessTarget.OFF]:
     * their master as it was); the assistant's songs start on [LoudnessTarget.AUTO]. Left out of the file when off.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val target: LoudnessTarget = LoudnessTarget.OFF,
    /**
     * Phase 34 (S4): the whole mix's lows in mono under this frequency (Hz; the engine folds the
     * side under it), 0 = off. The assistant's songs since S4 use 120 Hz; left out of the file at 0.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val monoBassHz: Float = 0f,
    /**
     * Phase 34 (S5): the sidechain's key, the sound the pumps on « Son du kick » and the Musique
     * bus duck under: a mixer channel ([padChannel] for a drum pad: the kick's), -1 none. Left out
     * of the file at -1.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val sidechainKey: Int = -1,
    /** Phase 34 (S5): the « Musique » bus (null: none, every strip straight into the mix, as before). */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val musicBus: MusicBus? = null,
) {
    /** [masterDb] as a linear gain, for the engine. */
    val masterGain: Float get() = Math.pow(10.0, masterDb / 20.0).toFloat()

    fun sanitized(): Mixer = copy(
        masterDb = masterDb.takeIf { it.isFinite() }?.coerceIn(MASTER_MIN_DB, MASTER_MAX_DB) ?: 0f,
        channels = (channels + List((CHANNELS - channels.size).coerceAtLeast(0)) { Channel() })
            .take(CHANNELS)
            .map { it.sanitized() },
        master = sanitizeInserts(master),
        buses = Buses.sanitized(buses),
        monoBassHz = monoBassHz.takeIf { it.isFinite() && it >= 20f }?.coerceAtMost(400f) ?: 0f,
        sidechainKey = sidechainKey.takeIf { it in 0 until CHANNELS && it != MASTER } ?: -1,
        musicBus = musicBus?.sanitized(),
    )

    companion object {
        /**
         * 0..15 the tracks (4 is the master's index), 16..31 the drum pads' own strips, 32..35 the
         * audio tracks 5..8 (after the pads: the numbers saved before stay).
         */
        const val CHANNELS = 36
        const val FIRST_PAD = 16
        const val PADS = 16
        /** The strip of audio track 5 (the 5th to 8th come after the pads'). */
        const val FIRST_AUDIO2 = FIRST_PAD + PADS

        /** Channel [ch] is a drum pad's own strip. */
        fun isPad(ch: Int): Boolean = ch in FIRST_PAD until FIRST_PAD + PADS

        /** Every strip that is not a pad's (the master's index 4 included: callers leave it out). */
        val TRACK_STRIPS: List<Int> = (0 until FIRST_PAD) + (FIRST_AUDIO2 until CHANNELS)
        const val INSERTS = 8
        /** The master's index where a channel number is expected (automation, engine). */
        const val MASTER = 4
        const val MASTER_MIN_DB = -24f
        const val MASTER_MAX_DB = 6f
        /**
         * A new project's master gain: the default kit, acid line and synth then leave the limiter
         * under 3 dB of work (about 8 dB at 0 dB, which pumped). Projects saved before keep theirs.
         */
        const val NEW_PROJECT_DB = -6f
        /** The Mixer's one-tap hint when the limiter works hard: the master this much lower. */
        const val HINT_STEP_DB = 6f

        /** The strip of drum pad [lane] (its own effects, before the drums' strip). */
        fun padChannel(lane: Int) = FIRST_PAD + lane

        fun sanitizeInserts(inserts: List<Insert>): List<Insert> =
            (inserts + List((INSERTS - inserts.size).coerceAtLeast(0)) { Insert() }).take(INSERTS).map { it.sanitized() }
    }
}

/**
 * Phase 34 (S5): the « Musique » group bus. The [members] strips (tracks, not pads) and, with
 * [returns], the reverb and echo returns are summed apart and ducked once under the sidechain's
 * key ([Mixer.sidechainKey]): by [depth] (0..1 of their level at a full key), their lows (under
 * 150 Hz) by [lowDepth]; back up over [release] (0..1 = 50..600 ms). Then they join the mix.
 */
@Serializable
data class MusicBus(
    val members: Set<Int> = emptySet(),
    val depth: Float = 0f,
    val lowDepth: Float = 0f,
    val release: Float = 0.35f,
    val returns: Boolean = false,
) {
    fun sanitized(): MusicBus = copy(
        members = members.filter { it in 0 until Mixer.CHANNELS && !Mixer.isPad(it) && it != Mixer.MASTER }.sorted().toSet(),
        depth = depth.finite(0f).coerceIn(0f, 1f),
        lowDepth = lowDepth.finite(0f).coerceIn(0f, 1f),
        release = release.finite(0.35f).coerceIn(0f, 1f),
    )
}

/**
 * [this] as a new project starts (blank, demo, starter, wizard): its master at
 * [Mixer.NEW_PROJECT_DB] unless it chose a gain of its own. Never applied to a project loaded.
 */
fun Project.asNewProject(): Project =
    if (mixer.masterDb == 0f) copy(mixer = mixer.copy(masterDb = Mixer.NEW_PROJECT_DB)) else this

/** Something on the strip changes the sound (a pad's strip is only used then). */
fun Channel.hasOwnFx(): Boolean =
    inserts.any { it.type != 0 } || macros != Macros.DEFAULTS || sends.any { it > 0f } || volume != 0.8f || pan != 0f

/** A pad plays through its own strip: it changes the sound ([hasOwnFx]), it is muted (« Couper ce pad ») or soloed. */
fun Channel.routesPad(): Boolean = hasOwnFx() || mute || solo

@Serializable
data class Channel(
    val volume: Float = 0.8f,
    val pan: Float = 0f,
    val mute: Boolean = false,
    val solo: Boolean = false,
    val inserts: List<Insert> = List(Mixer.INSERTS) { Insert() },
    /** Post-fader sends to the buses (0 = reverb, 1 = delay), 0..1. */
    val sends: List<Float> = List(Buses.COUNT) { 0f },
    /** The quick controls ([Macros]: colour, dirt, motion, space), after the inserts. */
    val macros: List<Float> = Macros.DEFAULTS,
) {
    fun sanitized(): Channel = copy(
        volume = volume.finite(0.8f).coerceIn(0f, 1f),
        pan = pan.finite(0f).coerceIn(-1f, 1f),
        inserts = Mixer.sanitizeInserts(inserts),
        sends = (sends + List((Buses.COUNT - sends.size).coerceAtLeast(0)) { 0f }).take(Buses.COUNT)
            .map { v -> v.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f },
        macros = Macros.sanitized(macros),
    )
}
