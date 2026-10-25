package com.pocketstudio.model

import com.pocketstudio.model.i18n.tr
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * G1b, « une piste = une pile de couches » (docs/PLAN-GUIDE.md § 3.4, 4.1): an instrument track
 * stacks up to [Layers.MAX] more sounds over its own ([InstrumentTrack.layers]). Each one plays the
 * track's notes (the same piano roll, the same keys), kept to its note range and transposed, with
 * its own level, place, effects and sends; the engine sums it into the track's strip, before the
 * track's effects (or after them: [TrackLayer.throughTrackFx]). The track's own sound is the first
 * of its four « couches »; a track without layers is saved as before.
 */

/** What a layer sounds like: the same kinds as a track's own sound ([InstrumentTrack.engine]). */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class LayerSound(
    /** [InstrumentTrack.ENGINE_SYNTH] .. [InstrumentTrack.ENGINE_808] (an [instrument] still wins). */
    val engine: Int = InstrumentTrack.ENGINE_SYNTH,
    val synth: SynthPatch = SynthPatch(),
    @EncodeDefault(EncodeDefault.Mode.NEVER) val plaits: PlaitsPatch = PlaitsPatch(),
    @EncodeDefault(EncodeDefault.Mode.NEVER) val instrument: InstrumentChoice? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val sampled: SampledPatch = SampledPatch(),
    @EncodeDefault(EncodeDefault.Mode.NEVER) val multi: MultiPatch = MultiPatch(),
    @EncodeDefault(EncodeDefault.Mode.NEVER) val voice808: PadVoice = PadVoice(PadVoice.BASS_808),
) {
    fun sanitized(): LayerSound = copy(
        engine = engine.coerceIn(0, InstrumentTrack.ENGINE_808),
        synth = synth.sanitized(),
        plaits = plaits.sanitized(),
        instrument = instrument?.takeIf { it.bank in 0..128 && it.program in 0..127 },
        sampled = sampled.sanitized(),
        multi = multi.sanitized(),
        voice808 = voice808.copy(kind = PadVoice.BASS_808).sanitized(),
    )

    /** The sound as a track of its own (the engine's sound commands take it that way): id [id]. */
    fun asTrack(id: Int, name: String = ""): InstrumentTrack =
        InstrumentTrack(id, name, engine = engine, synth = synth, plaits = plaits, instrument = instrument, sampled = sampled, multi = multi, voice808 = voice808)

    /** What it is, in a few words. */
    val label: String get() = asTrack(0).soundLabel

    companion object {
        /** Track [t]'s own sound. */
        fun of(t: InstrumentTrack) = LayerSound(t.engine, t.synth, t.plaits, t.instrument, t.sampled, t.multi, t.voice808)
        fun synth(patch: SynthPatch) = LayerSound(InstrumentTrack.ENGINE_SYNTH, synth = patch)
        fun plaits(patch: PlaitsPatch) = LayerSound(InstrumentTrack.ENGINE_PLAITS, plaits = patch)
        fun sample(patch: SampledPatch) = LayerSound(InstrumentTrack.ENGINE_SAMPLED, sampled = patch)
        fun generalMidi(choice: InstrumentChoice) = LayerSound(InstrumentTrack.ENGINE_SYNTH, instrument = choice)
        fun multi(patch: MultiPatch) = LayerSound(InstrumentTrack.ENGINE_MULTI, multi = patch)
        fun bass808(voice: PadVoice = PadVoice(PadVoice.BASS_808)) = LayerSound(InstrumentTrack.ENGINE_808, voice808 = voice)
    }
}

/** One layer of a track: its sound and how it plays and mixes. */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class TrackLayer(
    val sound: LayerSound = LayerSound(),
    /** Its name (saved in French when the app gives it, like a track's); empty: its role's, or its sound's. */
    val label: String = "",
    /** What it brings ([LayerRole]: "sub", "body", "attack", "air"; "" none said): the Guide reads it. */
    val role: String = "",
    /** Its level after its effects, in dB ([Layers.MIN_GAIN_DB] = silent .. [Layers.MAX_GAIN_DB]). */
    val gainDb: Float = 0f,
    /** Its place in the stereo, -1 .. 1. */
    val pan: Float = 0f,
    /** The track's notes it plays: [low] .. [high] (MIDI), before its transposition. */
    val low: Int = 0,
    val high: Int = 127,
    /** Semitones ([Layers.MAX_TRANSPOSE] either way) and cents (-100 .. 100). */
    val transpose: Int = 0,
    val fine: Float = 0f,
    /** Its own effects, [Layers.MAX_INSERTS] at most (no echo, reverb nor tape stop: the sends are for space). */
    val inserts: List<Insert> = emptyList(),
    /** Its sends to the reverb and the echo (0..1), after its level, following the track's fader. */
    val sends: List<Float> = listOf(0f, 0f),
    val mute: Boolean = false,
    /**
     * True: it goes through the track's effects with the track's own sound (one sound). False: it
     * joins the track right after its effects (a clean sub under a driven bass keeps its own effects
     * only); the track's quick controls, fader, pan and sends still act on it, as on the whole track.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val throughTrackFx: Boolean = true,
) {
    fun sanitized(): TrackLayer {
        val lo = low.coerceIn(0, 127)
        return copy(
            sound = sound.sanitized(),
            label = label.take(InstrumentTrack.MAX_NAME),
            role = role.take(16),
            gainDb = gainDb.finiteOr(0f).coerceIn(Layers.MIN_GAIN_DB, Layers.MAX_GAIN_DB),
            pan = pan.finiteOr(0f).coerceIn(-1f, 1f),
            low = lo,
            high = high.coerceIn(lo, 127),
            transpose = transpose.coerceIn(-Layers.MAX_TRANSPOSE, Layers.MAX_TRANSPOSE),
            fine = fine.finiteOr(0f).coerceIn(-100f, 100f),
            // Its effects without the long ones (their memory is the project's pool: Mixer.longFxCount)
            // nor scenes of their own (a layer plays wherever its track plays).
            inserts = inserts.map { it.sanitized() }.filter { it.effect != EffectType.NONE && EffectType.longLimit(it.effect) == Int.MAX_VALUE }
                .map { it.copy(scenes = null) }.take(Layers.MAX_INSERTS),
            sends = (sends + listOf(0f, 0f)).take(Buses.COUNT).map { it.finiteOr(0f).coerceIn(0f, 1f) },
        )
    }

    /** Its name as shown: its own, its role's, or its sound's. */
    val title: String get() = label.ifBlank { LayerRole.title(role) ?: sound.label }

    /** The linear gain the engine takes (SetLayer LayerGain): 0 at [Layers.MIN_GAIN_DB]. */
    val engineGain: Float get() = Layers.linear(gainDb)

    /** Whether note [note] of the track plays on this layer. */
    fun plays(note: Int): Boolean = note in low..high

    private fun Float.finiteOr(fallback: Float) = if (isFinite()) this else fallback
}

/** What a layer brings (docs/PLAN-GUIDE.md § 3.4: one sound per role and band, never two). */
object LayerRole {
    const val SUB = "sub"
    const val BODY = "body"
    const val ATTACK = "attack"
    const val AIR = "air"
    val ALL = listOf(SUB, BODY, ATTACK, AIR)

    /** The role's name as shown, or null for none. */
    fun title(role: String): String? = when (role) {
        SUB -> tr("Sub")
        BODY -> tr("Corps")
        ATTACK -> tr("Attaque")
        AIR -> tr("Air")
        else -> null
    }

    /** A new layer of [sound] for [role], with the role's usual settings. */
    fun layer(sound: LayerSound, role: String): TrackLayer = when (role) {
        // Clean and centred, past the track's effects (its drive and its high-pass leave it alone).
        SUB -> TrackLayer(sound, role = SUB, throughTrackFx = false)
        // Above the sub: an octave up, a little apart.
        AIR -> TrackLayer(sound, role = AIR, transpose = 12, gainDb = -6f)
        ATTACK -> TrackLayer(sound, role = ATTACK, gainDb = -3f)
        else -> TrackLayer(sound, role = role.takeIf { it in ALL } ?: "")
    }.sanitized()
}

object Layers {
    /** Layers over a track's own sound (the engine's InstrumentTrack::kLayers): four sounds in all. */
    const val MAX = 3
    const val MAX_INSERTS = 4
    const val MIN_GAIN_DB = -40f
    /** The engine takes up to 4 (+12 dB). */
    const val MAX_GAIN_DB = 12f
    const val MAX_TRANSPOSE = 48

    /** The linear gain of [db] (0 at [MIN_GAIN_DB] and below). */
    fun linear(db: Float): Float = if (db <= MIN_GAIN_DB) 0f else 10f.pow(db.coerceAtMost(MAX_GAIN_DB) / 20f)

    /** Its decibels, the other way ([MIN_GAIN_DB] for silence). */
    fun db(linear: Float): Float = if (linear <= 0f) MIN_GAIN_DB else (20f * kotlin.math.log10(linear)).coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)

    /** A number with its sign, for a transposition (« +12 », « −5 », « 0 »). */
    fun signedSemitones(v: Int): String = when {
        v > 0 -> "+$v"
        v < 0 -> "−${-v}"
        else -> "0"
    }

    /** Cents as a whole number with its sign. */
    fun signedCents(v: Float): String = signedSemitones(v.roundToInt())

    /** At most [MAX] layers, each in range. */
    fun sanitize(layers: List<TrackLayer>): List<TrackLayer> = layers.take(MAX).map { it.sanitized() }

    /** The engine's sound index of layer [layer] of track [track] (Engine.h layerSound): after the tracks' own. */
    fun soundIndex(track: Int, layer: Int): Int = InstrumentTrack.MAX + track * MAX + layer

    /** The engine's mixer strip of layer [layer] of track [track] (Mixer::layerChannel): after the project's strips. */
    fun channel(track: Int, layer: Int): Int = FIRST_CHANNEL + track * MAX + layer
    const val FIRST_CHANNEL = Mixer.CHANNELS

    /**
     * The most notes a layer's synth plays at once (the engine's kLayerVoices, the CPU budget): a
     * chord of more steals its oldest note on the layer (the track's own sound plays 8). The
     * editor warns when a track's notes stack more than this.
     */
    const val MAX_VOICES = 6

    /**
     * Which of a track's sounds are heard (the engine's SetLayerListen): bit 0 its own sound, bit
     * 1 + k its layer k. [ALL]: as mixed.
     */
    const val ALL = 0xF
    /** Only its own sound ([only] = [OWN]) or only layer [only] (0 .. [MAX] - 1). */
    const val OWN = -1
    fun only(only: Int): Int = if (only == OWN) 1 else 1 shl (only + 1)
    /** Everything but layer [layer] (« sans cette couche »; [OWN]: without the track's own sound). */
    fun without(layer: Int): Int = ALL and only(layer).inv()
}

/** Track [track]'s layers (none for a track that does not exist). */
fun Project.layersOf(track: Int): List<TrackLayer> = if (track == 0 || tracks.any { it.id == track }) track(track).layers else emptyList()

/** Every track's layers, by track id (tracks without any left out). */
val Project.allLayers: Map<Int, List<TrackLayer>> get() = instrumentTracks.filter { it.layers.isNotEmpty() }.associate { it.id to it.layers }

/** Every sound the project plays on its instrument tracks: their own and their layers'. */
val Project.allTrackSounds: List<LayerSound> get() = instrumentTracks.flatMap { t -> listOf(LayerSound.of(t)) + t.layers.map { it.sound } }

/**
 * A new layer of [sound] on track [track] for [role] ([LayerRole]), at the end of its stack; null
 * when the track has [Layers.MAX] already or does not exist. One edit (one undo step in the app).
 */
fun Project.addLayer(track: Int, sound: LayerSound, role: String = "", label: String = ""): Project? {
    if (track != 0 && tracks.none { it.id == track }) return null
    val t = track(track)
    if (t.layers.size >= Layers.MAX) return null
    return withTrack(t.copy(layers = t.layers + LayerRole.layer(sound, role).copy(label = label.take(InstrumentTrack.MAX_NAME))))
}

/** Layer [index] of track [track] removed (the ones after it move up); unchanged when there is none. */
fun Project.removeLayer(track: Int, index: Int): Project {
    val t = track(track)
    if (index !in t.layers.indices || (track != 0 && tracks.none { it.id == track })) return this
    return withTrack(t.copy(layers = t.layers.filterIndexed { i, _ -> i != index }))
}

/** Layer [index] of track [track] changed by [transform] (kept in range); unchanged when there is none. */
fun Project.editLayer(track: Int, index: Int, transform: (TrackLayer) -> TrackLayer): Project {
    val t = track(track)
    if (index !in t.layers.indices || (track != 0 && tracks.none { it.id == track })) return this
    return withTrack(t.copy(layers = t.layers.mapIndexed { i, l -> if (i == index) transform(l).sanitized() else l }))
}

/** The settings of layer [index] of track [track] that are given (the others as they are). */
fun Project.setLayer(
    track: Int, index: Int, gainDb: Float? = null, pan: Float? = null, low: Int? = null, high: Int? = null,
    transpose: Int? = null, fine: Float? = null, mute: Boolean? = null, throughTrackFx: Boolean? = null,
): Project = editLayer(track, index) { l ->
    l.copy(
        gainDb = gainDb ?: l.gainDb, pan = pan ?: l.pan, low = low ?: l.low, high = maxOf(high ?: l.high, low ?: l.low),
        transpose = transpose ?: l.transpose, fine = fine ?: l.fine, mute = mute ?: l.mute, throughTrackFx = throughTrackFx ?: l.throughTrackFx,
    )
}

/** Layer [index] of track [track] moved to [to] in its stack. */
fun Project.moveLayer(track: Int, index: Int, to: Int): Project {
    val t = track(track)
    if (index !in t.layers.indices || to !in t.layers.indices || index == to) return this
    val list = t.layers.toMutableList()
    list.add(to, list.removeAt(index))
    return withTrack(t.copy(layers = list))
}

/**
 * Bass music (WideMix): the sub track ([StarterWizard.SUB_TRACK], the bass's notes an octave down)
 * folded into the bass track [bass] as its « Sub » layer: its sound, its level (its strip's fader
 * as the layer's gain), its effects (the pump), transposed an octave down and past the bass's own
 * effects (its drive and its high-pass leave the sub clean, as on its own strip). The sub track
 * goes (its frozen sound too). Null when [sub] is not a sub of [bass] (other notes than the bass's
 * an octave down in some scene), its strip has what a layer cannot hold (a long effect, a mute or
 * a solo), or the bass's layers are full: the two tracks then stay as they are.
 */
fun Project.subAsLayer(bass: Int, sub: Int): Project? {
    if (subLayerProblem(bass, sub) != null) return null
    val b = track(bass)
    val s = track(sub)
    val ch = mixer.channels[InstrumentTrack.mixerChannel(sub)]
    val bassFader = mixer.channels.getOrNull(InstrumentTrack.mixerChannel(bass))?.volume ?: 0.8f
    // Its fader (0.8 = unity, squared law) against the bass's (which the layer goes through after
    // this); with the same fader curve on both, the curve sets both alike: unity.
    val sameCurve = automation.any { it.target.kind == AutoTarget.VOLUME && it.target.ch == InstrumentTrack.mixerChannel(sub) }
    val gain = if (sameCurve) 1f else (ch.volume / 0.8f).let { it * it } / (bassFader / 0.8f).let { (it * it).coerceAtLeast(1e-4f) }
    val layer = TrackLayer(
        sound = LayerSound.of(s), label = StarterWizard.SUB_TRACK, role = LayerRole.SUB, gainDb = Layers.db(gain),
        pan = ch.pan, transpose = -12, inserts = ch.inserts.filter { it.effect != EffectType.NONE }, sends = ch.sends, throughTrackFx = false,
    ).sanitized()
    // Sanitized: the song parts let go of the sub's keys (its mutes, its scenes).
    return withTrack(b.copy(layers = b.layers + layer)).removeTrack(sub).sanitized()
}

/** What a layer follows of its track's strip: its fader, its place, its quick controls. */
private val FOLLOWED_KINDS = setOf(AutoTarget.VOLUME, AutoTarget.PAN, AutoTarget.MACRO)

/** Why [subAsLayer] keeps track [sub] apart from the bass [bass] (a short English reason), or null when it can fold it. */
fun Project.subLayerProblem(bass: Int, sub: Int): String? {
    if (bass == sub || sub == 0 || tracks.none { it.id == sub } || (bass != 0 && tracks.none { it.id == bass })) return "no such pair of tracks"
    val b = track(bass)
    val s = track(sub)
    if (b.layers.size >= Layers.MAX) return "the bass's layers are full"
    if (s.layers.isNotEmpty()) return "the sub has layers"
    // The sub plays the bass's notes an octave down, in every scene.
    fun sameOctaveDown(bassClip: NoteClip?, subClip: NoteClip?): Boolean {
        val want = bassClip?.notes.orEmpty().map { it.copy(pitch = it.pitch - 12) }
        return want == subClip?.notes.orEmpty() && want.all { it.pitch >= 0 }
    }
    if (!sameOctaveDown(b.notes, s.notes)) return "other notes"
    if (allScenes.any { sc -> !sameOctaveDown(if (bass == 0) sc.synthNotes else sc.trackNotes[bass], sc.trackNotes[sub]) }) return "other notes in a scene"
    val ch = mixer.channels.getOrNull(InstrumentTrack.mixerChannel(sub)) ?: return "no strip"
    val heard = ch.inserts.filter { it.effect != EffectType.NONE }
    if (ch.mute || ch.solo) return "the sub is muted or soloed"
    if (heard.any { EffectType.longLimit(it.effect) != Int.MAX_VALUE || it.scenes != null }) return "a long effect or one for some scenes on the sub"
    if (heard.size > Layers.MAX_INSERTS) return "too many effects on the sub"
    // A layer joining its track « after its effects » joins it after its inserts: the bass's quick
    // controls, pan and sends then shape the sub too. None away from rest on either strip (the sub's
    // own quick controls have no place on a layer).
    val bassStrip = mixer.channels.getOrNull(InstrumentTrack.mixerChannel(bass)) ?: return "no strip"
    if (bassStrip.sends.any { it > 0f }) return "the bass sends to the reverb or the echo"
    if (bassStrip.pan != 0f) return "the bass is panned"
    if (Macros.sanitized(bassStrip.macros) != Macros.DEFAULTS) return "the bass's quick controls are moved"
    if (Macros.sanitized(ch.macros) != Macros.DEFAULTS) return "the sub's quick controls are moved"
    // Song curves on the sub's strip have no place on a layer, and the bass's fader, pan and quick
    // controls (which the layer follows) must not move where the sub's did not.
    val bassCh = InstrumentTrack.mixerChannel(bass)
    val subCh = InstrumentTrack.mixerChannel(sub)
    // (The assistant draws the same fader curve on both: then the layer, which follows the bass's
    // fader, moves exactly as the sub did.)
    val subLanes = automation.filter { it.target.kind != AutoTarget.BUS_PARAM && it.target.ch == subCh }
    val bassLanes = automation.filter { it.target.ch == bassCh && it.target.kind in FOLLOWED_KINDS }
    fun onBass(l: AutomationLane) = l.copy(target = l.target.copy(ch = bassCh))
    if (subLanes.any { l -> l.target.kind != AutoTarget.VOLUME || onBass(l) !in bassLanes }) return "curves on the sub"
    if (bassLanes.any { l -> subLanes.none { onBass(it) == l } }) return "the bass's level moves"
    // Both play in the same parts of the song, the same scenes from the same bar; neither is frozen.
    val bk = SongPart.inst(bass)
    val sk = SongPart.inst(sub)
    if (song.any { it.plays(bk) != it.plays(sk) || it.sceneFor(bk) != it.sceneFor(sk) || it.fromBars(bk) != it.fromBars(sk) }) return "not in the same parts"
    if (Freeze.isFrozen(this, bass) || Freeze.isFrozen(this, sub)) return "frozen"
    if (mixer.channels.getOrNull(bassCh)?.let { it.mute || it.solo } == true) return "the bass is muted or soloed"
    // The sub's strip is no key of the sidechain, and both are on the same side of the « Musique » bus.
    if (mixer.sidechainKey == subCh) return "the sub is the sidechain's key"
    if (mixer.musicBus?.let { (bassCh in it.members) != (subCh in it.members) } == true) return "one in the Musique bus"
    return null
}


/**
 * Bass music's sub and mid as one bass track of two layers (§ 3.4: « les basses de bass music
 * naissent en deux couches »): each sub track the assistant made ([StarterWizard.SUB_TRACK]) goes
 * into the bass it doubles as that bass's « Sub » layer ([subAsLayer]); a sub that cannot (the bass
 * fades into a second bass along the song, the bass's layers full…) stays a track of its own, as
 * before. The assistant keeps making two tracks for now (DECISIONS.md, G1b): the Guide and the
 * track sheet (G3) call this when they show layers.
 */
object SubLayers {
    /** [project] with every sub that can be folded into its bass folded (the others as they are). */
    fun apply(project: Project): Project {
        var p = project
        for (sub in project.instrumentTracks.filter { DefaultNames.french(it.name) == StarterWizard.SUB_TRACK || it.name == StarterWizard.SUB_TRACK }) {
            val bass = bassOf(p, sub.id) ?: continue
            p = p.subAsLayer(bass, sub.id) ?: continue
        }
        return p
    }

    /** The track [sub] doubles an octave down (its first such track), or null. */
    fun bassOf(p: Project, sub: Int): Int? {
        fun clip(sc: Scene?, id: Int): List<Note> = if (sc == null) p.track(id).notes.notes else (if (id == 0) sc.synthNotes else sc.trackNotes[id])?.notes.orEmpty()
        val places = listOf<Scene?>(null) + p.allScenes
        if (places.all { clip(it, sub).isEmpty() }) return null
        return p.instrumentTracks.firstOrNull { t ->
            t.id != sub && places.all { sc -> clip(sc, t.id).map { it.copy(pitch = (it.pitch - 12).coerceAtLeast(0)) } == clip(sc, sub) }
        }?.id
    }
}
