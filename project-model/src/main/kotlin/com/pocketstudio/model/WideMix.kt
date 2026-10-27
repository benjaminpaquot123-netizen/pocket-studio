package com.pocketstudio.model

import com.pocketstudio.model.StarterWizard.Part
import com.pocketstudio.model.StarterWizard.Style

/**
 * Wide and moving synths for the assistant's songs (phase 34, chantier S4 of docs/PLAN-SON-PRO.md):
 * the leads and pads spread left and right (stereo unison, mono-compatible) with a chorus and a
 * ping-pong echo, an octave-down double on club leads, a slow filter cycle over 8 bars, the lead's
 * reverb raised at the end of each phrase; the bass music basses with a wide driven mid layer over
 * a mono sub (the reese of drum and bass, the formant « talk » of neurofunk, the square wobble of
 * riddim whose speed changes every 8 bars); a plate (a room in hip-hop) whose return is equalised,
 * and the lows of the whole mix in mono under 120 Hz.
 *
 * Only for the songs the assistant makes ([apply], last step of [StarterWizard.build]): the
 * projects saved before keep their sound (their synths' S4 settings at rest, [Project.soundVersion] 1).
 *
 * The leads chantier, lot 1 ([Project.SOUND_V3], docs/hits/leads-pros.md § 8): no octave-down double
 * where a « Sub » track holds the low end, nor ever under a pluck, a key or a bell (+3 to +6 dB of
 * 150-500 Hz); the filter cycle open on each phrase's first bar, where the hook comes in (it was
 * closed there: the lead at half its brightness on bar 1), drawn as a song curve of the cut-off;
 * the cut-off following the part's notes (key tracking, done once here); unison leads without the
 * chorus that summed them to mono; the leads' reverb a little higher.
 */
object WideMix {
    /** Under this, the whole mix in mono ([Mixer.monoBassHz]). */
    const val MONO_BASS_HZ = 120f

    /** The plate's size (about 2 s) and the returns' equaliser. */
    const val PLATE_SIZE = 0.45f
    const val REVERB_LOW_CUT_HZ = 200f
    const val REVERB_HIGH_CUT_HZ = 9000f
    const val ECHO_LOW_CUT_HZ = 150f
    const val ECHO_HIGH_CUT_HZ = 8000f

    /** The bass music sub's saturation (its harmonics in 60-250 Hz). */
    const val SUB_DRIVE = 0.4f

    /** How wide the pads spread. */
    const val PAD_WIDTH = 0.9f

    /** A bass's stereo above this (its mid layer, its width): under it, mono. */
    const val BASS_MONO_BELOW_HZ = 250f

    /** The bass over a sub layer, high-passed lower in bass music (80 Hz, not 120: no hole between them). */
    const val BASS_MUSIC_SUB_HIGH_PASS = 0.11f

    /** The lead's reverb send on the last bar of each 8-bar phrase (at least), and its usual one. */
    const val PHRASE_END_SEND = 0.4f
    const val LEAD_SEND = 0.15f

    /** Leads chantier: the leads' reverb send (0.15 before), the octave-down double where it stays (0.3 before). */
    const val LEAD_SEND_V3 = 0.2f
    const val OCTAVE_DOWN_V3 = 0.15f

    /** The filter's 8-bar curve open on each phrase's first bar: +-0.5 octave on a lead, +-0.6 on a pad (+-0.4 until son 8: the pads moved 2.1 dB over 8 bars, under the 2.5 aimed at). */
    const val LEAD_PHRASE_OCTAVES = 0.5f
    const val PAD_PHRASE_OCTAVES = 0.6f

    /** Library families with no octave-down double, ever: plucks, keys and bells (the « Pluck » and « Claviers » sounds). */
    private val NO_OCTAVE_CATEGORIES = setOf("Pluck", "Claviers")

    /** Families whose leads take the octave-down double (club music and the « Autres » pop sounds). */
    private val OCTAVE_FAMILIES = setOf("Techno", "House", "Bass music", "Autres")

    /** Styles whose bass wobbles at a speed that changes every 8 bars (1/8 → 1/4 → 1/16). */
    private val WOBBLE_STYLES = setOf("dubstep", "riddim")
    private val REESE_STYLES = setOf("dnb", "liquid_dnb", "jungle")

    // --- The sounds -------------------------------------------------------------------------

    /**
     * A lead: wide, oversampled, its filter breathing over 8 bars, doubled an octave down in club music.
     * [v3] (the leads chantier): no double when the song has a sub track ([hasSub]) or the sound is a
     * pluck, a key or a bell, 0.15 elsewhere; the engine's cycle off ([phraseLane] draws it open on
     * each phrase's first bar instead).
     */
    fun lead(p: SynthPatch, style: Style, v3: Boolean = false, hasSub: Boolean = false, category: String? = SoundLibrary.synthCategoryOf(p)): SynthPatch {
        var s = p.with(SynthPatch.WIDTH, if (p.get(SynthPatch.UNISON) > 0f) 0.8f else 0.6f)
            .with(SynthPatch.OVERSAMPLE, 1f)
            .with(SynthPatch.CYCLE_BARS, SynthPatch.cycleValue(8)).with(SynthPatch.CYCLE_DEPTH, 0.6f)
        if (!v3) {
            if (style.family in OCTAVE_FAMILIES) s = s.with(SynthPatch.OCTAVE_DOWN, 0.3f)
            return s
        }
        s = s.with(SynthPatch.CYCLE_BARS, 0f).with(SynthPatch.CYCLE_DEPTH, 0f)
        if (style.family in OCTAVE_FAMILIES && !hasSub && category !in NO_OCTAVE_CATEGORIES) {
            s = s.with(SynthPatch.OCTAVE_DOWN, OCTAVE_DOWN_V3)
        }
        return s
    }

    /**
     * A pad: wide, breathing over 8 bars (oversampled when it has few copies: the CPU of 8 voices stays
     * low). [v3]: the engine's cycle off, [phraseLane] draws it open on each phrase's first bar.
     */
    fun pad(p: SynthPatch, v3: Boolean = false): SynthPatch {
        var s = p.with(SynthPatch.WIDTH, PAD_WIDTH).with(SynthPatch.CYCLE_BARS, SynthPatch.cycleValue(8)).with(SynthPatch.CYCLE_DEPTH, 0.2f)
        if (v3) s = s.with(SynthPatch.CYCLE_BARS, 0f).with(SynthPatch.CYCLE_DEPTH, 0f)
        if (p.get(SynthPatch.UNISON) <= 0.5f) s = s.with(SynthPatch.OVERSAMPLE, 1f)
        return s
    }

    /**
     * A bass music bass: mono under 250 Hz, a wide driven mid layer over it, and its own wave an
     * octave down (its fundamental under the 80 Hz cut, its harmonics filling 60-250 Hz between the
     * sub and the growl: the band that was 8 to 11 dB short).
     */
    fun bassMusic(p: SynthPatch): SynthPatch =
        p.with(SynthPatch.MONO_BELOW, SynthPatch.monoBelowValue(BASS_MONO_BELOW_HZ)).with(SynthPatch.OCTAVE_DOWN, 0.5f)
            .with(SynthPatch.WIDTH, if (p.get(SynthPatch.UNISON) > 0f) 0.5f else 0.35f)
            .with(SynthPatch.MID_LAYER, 0.5f).with(SynthPatch.DRIVE, 0.25f).with(SynthPatch.OVERSAMPLE, 1f)

    /** The drum and bass reese: two saws ±27 cents (Saunderson's), slow phasing, wide above 250 Hz over a mono sub. */
    fun reese(p: SynthPatch): SynthPatch =
        bassMusic(p).with(SynthPatch.UNISON, maxOf(p.get(SynthPatch.UNISON), 0.33f)).with(SynthPatch.UNISON_DETUNE, 0.51f)
            .with(SynthPatch.WIDTH, 0.55f).with(SynthPatch.MID_LAYER, 0.4f).with(SynthPatch.DRIVE, 0.3f)

    /** The neurofunk bass that talks: a vowel filter moved by the sweep, driven. */
    fun neuro(p: SynthPatch): SynthPatch {
        var s = bassMusic(p).with(SynthPatch.FORMANT, 0.6f).with(SynthPatch.VOWEL, 0.25f).with(SynthPatch.DRIVE, 0.4f)
        if (s.get(SynthPatch.SWEEP) < 0.5f) s = s.with(SynthPatch.SWEEP, 0.5f)
        if (s.get(SynthPatch.LFO2_SYNC) == 0f) s = s.with(SynthPatch.LFO2_SYNC, SynthPatch.syncValue(SynthPatch.SYNC_EIGHTH))
        return s
    }

    /**
     * Final audit (report 10, « le Drop ne tombe pas au téléphone »): the Drop's bass speaks where a phone
     * plays (over 300 Hz): its mid layer fuller and driven harder, the wobble's filter never shut as far
     * (its cut-off this much higher). (mid layer, its drive 0..1, cut-off added) by style.
     */
    private val PHONE_MIDS: Map<String, Triple<Float, Float, Float>> = mapOf(
        "dubstep" to Triple(0.9f, 0.7f, 0.08f), "riddim" to Triple(0.8f, 0.65f, 0.05f), "neurofunk" to Triple(0.8f, 0.65f, 0.04f),
        "jump_up" to Triple(0.75f, 0.6f, 0f), "dnb" to Triple(0.6f, 0.6f, 0f),
    )

    /** [p] with [style]'s [PHONE_MIDS] (as it is for a style without). */
    fun phoneMids(p: SynthPatch, style: String): SynthPatch {
        val (layer, drive, cutoff) = PHONE_MIDS[style] ?: return p
        return p.with(SynthPatch.MID_LAYER, maxOf(p.get(SynthPatch.MID_LAYER), layer)).with(SynthPatch.MID_LAYER_DRIVE, drive)
            .with(SynthPatch.CUTOFF, (p.get(SynthPatch.CUTOFF) + cutoff).coerceAtMost(1f))
    }

    /** The riddim wobble: a square sweep (a gate), resonant. */
    fun riddim(p: SynthPatch): SynthPatch =
        bassMusic(p).with(SynthPatch.LFO2_SHAPE, SynthPatch.SHAPE_SQUARE)
            .with(SynthPatch.RESONANCE, maxOf(p.get(SynthPatch.RESONANCE), 0.55f))

    // --- The song ---------------------------------------------------------------------------

    /**
     * [project] (made by [StarterWizard.build] for [style], [partOfTrack] the part each of its
     * tracks plays) with its synths wide and moving, its effects and its curves.
     */
    fun apply(project: Project, style: Style, partOfTrack: Map<Int, Part>): Project {
        var p = project.copy(soundVersion = Project.SOUND_V3)
        val hasSub = p.instrumentTracks.any { (DefaultNames.french(it.name) ?: it.name) == StarterWizard.SUB_TRACK }
        p = p.copy(mixer = withBuses(p.mixer, style).copy(monoBassHz = MONO_BASS_HZ))
        val bassMusic = style.family == "Bass music"
        val leads = partOfTrack.filterValues { it == Part.Lead }.keys +
            p.instrumentTracks.filter { DefaultNames.french(it.name) == StarterWizard.COUNTER_TRACK || it.name == StarterWizard.COUNTER_TRACK }.map { it.id }
        val basses = partOfTrack.filterValues { it == Part.Bass }.keys +
            p.instrumentTracks.filter { it.name == StarterWizard.BASS_SWITCH_TRACK }.map { it.id }
        val pads = partOfTrack.filterValues { it == Part.Pad }.keys + p.instrumentTracks.filter { it.name == StarterWizard.PAD_LAYER_TRACK }.map { it.id }
        for (t in p.instrumentTracks) {
            val synth = t.engine == 0 && t.instrument == null
            val category = if (synth) SoundLibrary.synthCategoryOf(t.synth) else null
            // The cut-off following the part's notes (key tracking, once, here): a pluck two octaves up as bright as at C4.
            // (Lots B to D: a library sound whose cut-off follows each note in the engine, KEY_TRACK, is left as it is.)
            val tracked = if (synth && (t.id in leads || t.id in pads) && t.synth.get(SynthPatch.KEY_TRACK) == 0f) {
                ProVoices.keyTracked(t.synth, p.medianPitch(t), ProVoices.keyTracking(category))
            } else t.synth
            val edited = when {
                t.id in leads -> if (synth) t.copy(synth = lead(tracked, style, v3 = true, hasSub = hasSub, category = category)) else t
                t.id in pads -> if (synth) t.copy(synth = pad(tracked, v3 = true)) else t
                // A lead's sound playing a bass (the grime square) keeps the square sub under it, and (son 8)
                // the low end it had: none of the leads' touch (a cut-off following the note around C4).
                t.id in basses && synth && category in BASS_WITHOUT_SUB -> {
                    val plain = SoundLibrary.synthNameOf(t.synth)?.let { ProVoices.untouched(t.synth, it) } ?: t.synth
                    t.copy(synth = if (plain.get(SynthPatch.SUB) == 0f) plain.with(SynthPatch.SUB, SynthPatch.DEFAULTS[SynthPatch.SUB]) else plain)
                }
                t.id in basses && bassMusic && synth -> t.copy(synth = phoneMids(when {
                    style.id == "neurofunk" -> neuro(t.synth)
                    style.id == "riddim" -> riddim(t.synth)
                    style.id in REESE_STYLES && isReese(t.synth) -> reese(t.synth)
                    else -> bassMusic(t.synth)
                }, style.id))
                else -> t
            }
            if (edited != t) p = p.withTrack(edited)
        }
        // The leads' chorus and ping-pong echo, and a little more of the reverb.
        for (id in leads) p = withLeadEffects(p, id, v3 = true)
        // S6 son (M10): a wider Drop (side/centre −18 dB median, the target −15 to −9): the 303's
        // echoes and the chords' go out left and right, the lines themselves stay in the centre.
        p = withWideEchoes(p, style, pads)
        // A Plaits pad or keys (the FM pianos of the leads chantier) is mono: the electric piano's
        // stereo tremolo (an auto-pan on eighths, 30 %) and a light chorus spread it, as the synth's
        // two oscillators spread left and right did.
        for (id in pads) if (p.track(id).engine == 1) p = withChorus(p, id)
        // Room for the voices above the bass: a lead or counter-melody under C5 cut under 250 Hz with
        // a little presence at 3 kHz (its fundamentals are what put 150-500 Hz 5 to 10 dB over
        // 1-4 kHz), a pad under C4 cut under 160 Hz; never over the track's lowest note (as G0's cuts).
        for (id in leads) if ((p.medianPitch(p.track(id)) ?: 72) < 72) p = withInsert(p, id, voiceEq(p, id, LEAD_EQ, 250f))
        for (id in pads) if ((p.medianPitch(p.track(id)) ?: 60) < 60) p = withInsert(p, id, voiceEq(p, id, PAD_EQ, 160f))
        // Brighter, they read louder (the loudness is weighted towards 2-4 kHz): the leads 1 dB and the
        // pads 1.5 dB lower keep the Break and the Groove under the Drop as before (measured: without
        // it, Drop - Break fell from +5.8 to +3.5 LU in liquid, +6.2 to +3.8 in jungle, +4.4 to +3.5 in dnb).
        for (id in leads) p = withLevel(p, id, LEAD_TRIM_DB)
        for (id in pads) p = withLevel(p, id, PAD_TRIM_DB)
        // Bass music: the bass over the sub cut at 80 Hz, not 120; the sub saturated a little (still
        // mono, no width): its third harmonic, 130-260 Hz, is what a phone hears of it.
        if (bassMusic) {
            for (id in basses) p = withLowerSubCut(p, id)
            p.instrumentTracks.filter { it.name == StarterWizard.SUB_TRACK && it.engine == 0 }.forEach { sub ->
                p = p.withTrack(sub.copy(synth = sub.synth.with(SynthPatch.DRIVE, SUB_DRIVE)))
            }
        }
        // G0: each track's low cut, sends and place (after the chorus and the echo took their slots).
        p = RoleMix.apply(p, style, partOfTrack)
        // S7 son (M10): the pads, chords, leads and 303 spread wide (the widener's « Étalement »).
        p = withSpread(p, style, leads, pads)
        // The curves: the lead's reverb at each phrase's end, the wobble's speed every 8 bars, the vowel.
        val sections = StarterWizard.songSections(p)
        if (sections.isEmpty()) return p
        val lanes = ArrayList<AutomationLane>()
        partOfTrack.entries.firstOrNull { it.value == Part.Lead }?.key?.let { lead -> phraseEndLane(p, lead, sections)?.let(lanes::add) }
        val bass = partOfTrack.entries.firstOrNull { it.value == Part.Bass }?.key
        if (bass != null && bassMusic && p.track(bass).engine == 0) {
            if (style.id in WOBBLE_STYLES && p.track(bass).synth.get(SynthPatch.LFO2_SYNC) > 0f) lanes += wobbleLane(bass, sections, style.id == "riddim")
            if (style.id == "neurofunk") lanes += vowelLane(bass, sections)
        }
        p = withLanes(p, lanes)
        // The filter's 8-bar curve, open where each phrase starts: the lead, the counter-melody, the
        // pads, as long as the engine has lanes left; a voice without one keeps the engine's cycle,
        // shallower (closed on the phrase's first bar, but by half an octave at most).
        val curved = leads.map { it to LEAD_PHRASE_OCTAVES } + pads.map { it to PAD_PHRASE_OCTAVES }
        for ((id, octaves) in curved) {
            val t = p.track(id)
            if (t.engine != 0 || t.instrument != null) continue
            val lane = if (p.automation.size < AutomationLane.MAX_LANES) phraseLane(p, id, sections, octaves) else null
            p = if (lane != null) withLanes(p, listOf(lane))
            else p.withTrack(t.copy(synth = t.synth.with(SynthPatch.CYCLE_BARS, SynthPatch.cycleValue(8)).with(SynthPatch.CYCLE_DEPTH, octaves / 2f)
                // Lot B: the engine's cycle half a turn on, open on each 8 bars' first one as the curve is.
                .with(SynthPatch.CYCLE_OFFSET, 0.5f)))
        }
        // Lot D: the voices' layers (a supersaw's bite an octave up, a pluck's pad, a piano's air).
        p = withVoiceLayers(p, partOfTrack.entries.firstOrNull { it.value == Part.Lead }?.key, pads)
        return p
    }

    // --- The leads chantier, lot D: layers (docs/hits/leads-pros.md § 4.1, 4.6, 4.8) -----------

    /** The supersaw's bite: a square an octave up, 12 dB under it (§ 4.1). */
    const val BITE_DB = -12f
    /** A pluck's pad: the same notes held softly, 14 dB under (§ 4.8). */
    const val PLUCK_PAD_DB = -14f
    /** A piano's air: the « Nappe » an octave up (its partials at 2-6 kHz), 18 dB under (§ 4.6: « une nappe d'air »). */
    const val AIR_DB = -18f

    /**
     * [lead] (the song's lead) and [pads] with their lot D layer, each on a track that has a free layer:
     * a Supersaw lead gets [BITE_DB] of « Lead carré » an octave up (its envelope); a Pluck lead
     * [PLUCK_PAD_DB] of « Nappe »; a Plaits FM piano or keys on the pads [AIR_DB] of « Nappe » an octave up.
     * One layer a track (the CPU of a layer's six voices), none on a sampled instrument.
     */
    internal fun withVoiceLayers(project: Project, lead: Int?, pads: Collection<Int>): Project {
        var p = project
        fun named(name: String) = SoundLibrary.SYNTH.first { it.name == name }.sound
        fun add(id: Int, layer: TrackLayer) {
            val t = p.track(id)
            if (t.instrument != null || t.layers.size >= Layers.MAX) return
            p = p.withTrack(t.copy(layers = Layers.sanitize(t.layers + layer)))
        }
        if (lead != null && p.instrumentTracks.any { it.id == lead }) {
            val t = p.track(lead)
            when (if (t.engine == 0 && t.instrument == null) SoundLibrary.synthNameOf(t.synth) else null) {
                "Supersaw" -> {
                    // The square takes the lead's envelope: it bites with each note, lets go with it.
                    val env = listOf(SynthPatch.A_ATTACK, SynthPatch.A_DECAY, SynthPatch.A_SUSTAIN, SynthPatch.A_RELEASE)
                    val square = env.fold(named("Lead carré").with(SynthPatch.CLICK, 0f)) { s, i -> s.with(i, t.synth.get(i)) }
                    add(lead, TrackLayer(LayerSound.synth(square), label = "Lead carré", role = LayerRole.ATTACK, transpose = 12, gainDb = BITE_DB))
                }
                "Pluck", "Pluck future" -> add(lead, TrackLayer(LayerSound.synth(named("Nappe")), label = "Nappe", role = LayerRole.BODY, gainDb = PLUCK_PAD_DB))
                else -> {}
            }
        }
        for (id in pads) {
            val t = p.instrumentTracks.firstOrNull { it.id == id } ?: continue
            val fmKeys = t.engine == 1 && t.instrument == null && t.plaits.model in 2..4
            if (fmKeys) add(id, TrackLayer(LayerSound.synth(named("Nappe")), label = "Air", role = LayerRole.AIR, transpose = 12, gainDb = AIR_DB))
        }
        return p
    }

    /** Library families whose sounds have no sub of their own since the leads chantier (a bass playing one gets it back). */
    private val BASS_WITHOUT_SUB = setOf("Leads", "Nappes", "Pluck", "Claviers")

    /** A reese: two oscillators (or a few copies) detuned against each other, saws. */
    fun isReese(p: SynthPatch): Boolean = p.get(SynthPatch.OSC1_WAVE) == 0f && (p.get(SynthPatch.OSC2_DETUNE) >= 0.6f || p.get(SynthPatch.UNISON) > 0f)

    /** The reverb a plate (a room in hip-hop), both returns equalised (no mud under 150-200 Hz, no hiss). */
    fun withBuses(mixer: Mixer, style: Style): Mixer {
        val buses = Buses.sanitized(mixer.buses).map { it.toMutableList() }
        val room = style.family == "Hip-hop"
        buses[0][Buses.REVERB_TYPE] = if (room) Buses.ROOM else Buses.PLATE
        if (!room && buses[0][0] == Buses.DEFAULTS[0][0]) buses[0][0] = PLATE_SIZE
        buses[0][Buses.LOW_CUT] = Buses.lowCutValue(REVERB_LOW_CUT_HZ)
        buses[0][Buses.HIGH_CUT] = Buses.highCutValue(REVERB_HIGH_CUT_HZ)
        buses[1][Buses.LOW_CUT] = Buses.lowCutValue(ECHO_LOW_CUT_HZ)
        buses[1][Buses.HIGH_CUT] = Buses.highCutValue(ECHO_HIGH_CUT_HZ)
        return mixer.copy(buses = buses)
    }

    /** A light chorus, then a dotted-eighth ping-pong echo, in the lead's free slots (unless it has its own). */
    val LEAD_CHORUS = Insert(EffectType.CHORUS.ordinal, listOf(0.3f, 0.4f, 0.4f, 0.4f, 1f))
    val LEAD_PING_PONG = Insert(EffectType.PING_PONG.ordinal, listOf(0.42f, 0.35f, 0.5f, 0.25f))

    private fun withLeadEffects(project: Project, track: Int, v3: Boolean = false): Project {
        val ch = InstrumentTrack.mixerChannel(track)
        var c = project.mixer.channels[ch]
        val t = project.track(track)
        // A unison lead is wide already: the chorus would fold its copies to mono first (it sums its input).
        val unison = v3 && t.engine == 0 && t.instrument == null && t.synth.get(SynthPatch.UNISON) > 0f
        val send = if (v3) LEAD_SEND_V3 else LEAD_SEND
        fun has(type: EffectType) = c.inserts.any { it.effect == type }
        fun add(ins: Insert) {
            val slot = c.inserts.indexOfFirst { it.type == 0 }
            if (slot >= 0) c = c.copy(inserts = c.inserts.toMutableList().also { it[slot] = ins })
        }
        if (!unison && !has(EffectType.CHORUS) && !has(EffectType.FLANGER) && !has(EffectType.WIDENER)) add(LEAD_CHORUS)
        val echoes = has(EffectType.ECHO) || has(EffectType.PING_PONG)
        if (!echoes && project.mixer.longFxCount(EffectType.PING_PONG) < EffectType.longLimit(EffectType.PING_PONG)) add(LEAD_PING_PONG)
        c = c.copy(sends = c.sends.mapIndexed { bus, v -> if (bus == 0) maxOf(v, send) else v })
        return project.copy(mixer = project.mixer.copy(channels = project.mixer.channels.toMutableList().also { it[ch] = c.sanitized() }))
    }

    // --- S6 son (M10): a wider Drop -------------------------------------------------------------

    /**
     * The 303's ping-pong: dotted eighths, few repeats, dark (its fizz stays in the centre), low in
     * the mix: the acid line in the middle, its echoes bouncing out (acid styles measured at −30 to
     * −36 dB of side against centre on their Drop).
     */
    val ACID_PING_PONG = Insert(EffectType.PING_PONG.ordinal, listOf(0.42f, 0.3f, 0.35f, 0.2f))

    /** The chords' or pad's ping-pong (house measured at −22 dB, trap −21, lo-fi −26): dotted eighths, a little brighter. */
    val CHORDS_PING_PONG = Insert(EffectType.PING_PONG.ordinal, listOf(0.42f, 0.3f, 0.45f, 0.28f))

    /**
     * Bass music keeps its pads dry of it: wide already (−14 to −18 dB), and its Breaks are mostly
     * pads (with it, dubstep's Drop − Break fell from +4.3 to +3.9 LU on the bench).
     */
    private val NO_CHORDS_ECHO_FAMILIES = setOf("Bass music")

    /**
     * [ACID_PING_PONG] on the 303 when it plays in the song, [CHORDS_PING_PONG] on the pads and
     * chords (not in bass music): each in a free slot of its strip, unless it has an echo already, while the engine's
     * echo pool has room. The bass and the sub keep none (the mix's lows are mono under 120 Hz too).
     */
    internal fun withWideEchoes(project: Project, style: Style, pads: Collection<Int>): Project {
        var p = project
        fun add(ch: Int, ins: Insert) {
            val c = p.mixer.channels[ch]
            if (c.inserts.any { it.effect == EffectType.ECHO || it.effect == EffectType.PING_PONG }) return
            if (p.mixer.longFxCount(EffectType.PING_PONG) >= EffectType.longLimit(EffectType.PING_PONG)) return
            val slot = c.inserts.indexOfFirst { it.type == 0 }.takeIf { it >= 0 } ?: return
            val edited = c.copy(inserts = c.inserts.toMutableList().also { it[slot] = ins }).sanitized()
            p = p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.toMutableList().also { it[ch] = edited }))
        }
        if (p.allScenes.any { sc -> sc.acid.steps.take(sc.acid.length.coerceAtLeast(0)).any { it.gate } }) add(ACID_CHANNEL, ACID_PING_PONG)
        if (style.family !in NO_CHORDS_ECHO_FAMILIES) for (id in pads) add(InstrumentTrack.mixerChannel(id), CHORDS_PING_PONG)
        return p
    }

    // --- S7 son (M10): the Drop wide ----------------------------------------------------------

    /**
     * The widener's « Étalement » (its param 2, a stereo chorus made from the sound above 250 Hz,
     * mono-compatible) by role: the pads and chords the widest, the leads and the 303 less (they
     * stay in the middle, their copies go out). Bass music's pads are wide already (their Breaks
     * are mostly pads). The basses and the sub never get it.
     */
    const val PAD_SPREAD = 1f
    const val LEAD_SPREAD = 0.75f
    const val ACID_SPREAD = 0.7f
    const val BASS_MUSIC_PAD_SPREAD = 0.7f

    /** The clap's and the snare's (their pads' strips): their body in the centre, their crack a little out. */
    const val CLAP_SPREAD = 0.4f

    /**
     * Son 8 (the S7 leftovers): the styles whose Drop stayed under the side/centre target (S7 bench: deep
     * house −15.5, dubstep −16.0, neurofunk −16.8, phonk −16.9 dB, their Drop mostly kick, bass and 808,
     * kept in the centre): their leads, pads and clap or snare spread wider ([NARROW_LEAD_SPREAD],
     * [PAD_SPREAD] even in bass music, [NARROW_CLAP_SPREAD]).
     */
    private val NARROW_DROP_STYLES = setOf("deep_house", "dubstep", "neurofunk", "phonk")
    const val NARROW_LEAD_SPREAD = 1f
    const val NARROW_CLAP_SPREAD = 0.8f

    /**
     * The styles wide already (S6 bench: side/centre of the Drop −9.8 to −13.6 dB, the target −15 to
     * −9): their spread scaled down, or none (trance and hard techno, in range without it; at full
     * spread they went to −6.6 and −7.4 dB, and lost 1.2 to 1.5 LU in mono). Detroit (−18.6) went to
     * −7.1 at full spread (its pads and 303 are its Drop).
     * Son 8: reggaeton none (0.55 before; with its pluck lead's pad layer it went to −8.1, at 0.3 to −8.8).
     */
    private val SPREAD_SCALE = mapOf("trance" to 0f, "hard_techno" to 0f, "reggaeton" to 0f, "jump_up" to 0.5f, "minimal" to 0.7f, "detroit" to 0.5f)

    fun spreadScale(style: Style): Float = SPREAD_SCALE[style.id] ?: 1f

    /** [WIDENER][EffectType.WIDENER] as it is (width 0.5: x1), only its spread. */
    fun spreadWidener(spread: Float) = Insert(EffectType.WIDENER.ordinal, listOf(0.5f, 0f, spread, 0f))

    /**
     * Each lead, pad and the 303 (when it plays) with [spreadWidener]: a widener on the strip already
     * (the chords', RoleMix's) gets the spread, else one in a free slot. Mix version 4: the projects
     * saved before keep theirs ([Project.MIX_V4]).
     */
    internal fun withSpread(project: Project, style: Style, leads: Collection<Int>, pads: Collection<Int>): Project {
        var p = project
        fun spread(ch: Int, amount: Float) {
            if (ch !in p.mixer.channels.indices || amount <= 0f) return
            val c = p.mixer.channels[ch]
            val inserts = c.inserts.toMutableList()
            val at = inserts.indexOfFirst { it.effect == EffectType.WIDENER && !it.bypass }
            if (at >= 0) {
                inserts[at] = inserts[at].copy(params = inserts[at].params.toMutableList().also { while (it.size < 3) it += 0f; it[2] = amount })
            } else {
                val slot = inserts.indexOfFirst { it.type == 0 }.takeIf { it >= 0 } ?: return
                inserts[slot] = spreadWidener(amount)
            }
            p = p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.toMutableList().also { it[ch] = c.copy(inserts = inserts).sanitized() }))
        }
        val scale = spreadScale(style)
        val narrow = style.id in NARROW_DROP_STYLES
        val padSpread = scale * if (style.family == "Bass music" && !narrow) BASS_MUSIC_PAD_SPREAD else PAD_SPREAD
        for (id in pads) if (p.instrumentTracks.any { it.id == id }) spread(InstrumentTrack.mixerChannel(id), padSpread)
        for (id in leads) if (p.instrumentTracks.any { it.id == id }) spread(InstrumentTrack.mixerChannel(id), scale * if (narrow) NARROW_LEAD_SPREAD else LEAD_SPREAD)
        if (p.allScenes.any { sc -> sc.acid.steps.take(sc.acid.length.coerceAtLeast(0)).any { it.gate } }) spread(ACID_CHANNEL, scale * ACID_SPREAD)
        for (role in listOf(StarterWizard.Role.Clap, StarterWizard.Role.Snare)) {
            if (p.allScenes.any { sc -> sc.drums.lanes.getOrNull(role.lane)?.hits?.isNotEmpty() == true }) spread(Mixer.padChannel(role.lane), scale * if (narrow) NARROW_CLAP_SPREAD else CLAP_SPREAD)
        }
        return p
    }

    /** The 303's mixer strip. */
    private const val ACID_CHANNEL = 1

    /** The leads chantier: the leads' and the pads' level change (dB) for their brighter sound. */
    const val LEAD_TRIM_DB = -1f
    const val PAD_TRIM_DB = -1.5f

    /** [track]'s sound [db] louder (its calibration, [SynthPatch.GAIN] or [PlaitsPatch.gain]); a sample or an instrument as it is. */
    private fun withLevel(project: Project, track: Int, db: Float): Project {
        val t = project.track(track)
        if (t.instrument != null) return project
        return project.withTrack(when (t.engine) {
            0 -> t.copy(synth = t.synth.with(SynthPatch.GAIN, SynthPatch.gainValue(SynthPatch.gainDb(t.synth.get(SynthPatch.GAIN)) + db)))
            1 -> t.copy(plaits = t.plaits.copy(gain = SynthPatch.gainValue(SynthPatch.gainDb(t.plaits.gain) + db)))
            else -> return project
        })
    }

    /** The leads chantier's voice equalisers (PARAM_EQ: 3 kHz bell +2 dB Q 0.7 and a low cut at 250 Hz; a pad's low cut at 160 Hz). */
    val LEAD_EQ = Insert(EffectType.PARAM_EQ.ordinal, listOf(0.725f, 0.556f, 0.241f, 0.785f))
    val PAD_EQ = Insert(EffectType.PARAM_EQ.ordinal, listOf(0.725f, 0.5f, 0.241f, 0.64f))

    /** [ins] in a free slot of [track]'s strip, unless an effect of its type is there already. */
    /** [eq] with its low cut at [hz], or under [track]'s lowest note. */
    private fun voiceEq(p: Project, track: Int, eq: Insert, hz: Float): Insert {
        val cut = RoleMix.cutUnderLowest(p, track, hz)
        return if (cut >= hz) eq else eq.copy(params = eq.params.toMutableList().also { it[3] = RoleMix.lowCutValue(cut) })
    }

    private fun withInsert(project: Project, track: Int, ins: Insert): Project {
        val ch = InstrumentTrack.mixerChannel(track)
        val c = project.mixer.channels[ch]
        if (c.inserts.any { it.type == ins.type }) return project
        val slot = c.inserts.indexOfFirst { it.type == 0 }.takeIf { it >= 0 } ?: return project
        val edited = c.copy(inserts = c.inserts.toMutableList().also { it[slot] = ins }).sanitized()
        return project.copy(mixer = project.mixer.copy(channels = project.mixer.channels.toMutableList().also { it[ch] = edited }))
    }

    /** The keys' stereo tremolo: an auto-pan on eighths, 30 % deep, soft. */
    val KEYS_AUTO_PAN = Insert(EffectType.AUTO_PAN.ordinal, listOf(0.72f, 0.3f, 0f))

    /** [KEYS_AUTO_PAN] then [LEAD_CHORUS] in free slots of [track]'s strip, unless it moves or widens already. */
    private fun withChorus(project: Project, track: Int): Project {
        val ch = InstrumentTrack.mixerChannel(track)
        var c = project.mixer.channels[ch]
        if (c.inserts.any { it.effect in setOf(EffectType.CHORUS, EffectType.FLANGER, EffectType.WIDENER, EffectType.AUTO_PAN) }) return project
        for (ins in listOf(KEYS_AUTO_PAN, LEAD_CHORUS)) {
            val slot = c.inserts.indexOfFirst { it.type == 0 }.takeIf { it >= 0 } ?: break
            c = c.copy(inserts = c.inserts.toMutableList().also { it[slot] = ins })
        }
        return project.copy(mixer = project.mixer.copy(channels = project.mixer.channels.toMutableList().also { it[ch] = c.sanitized() }))
    }

    private fun withLowerSubCut(project: Project, track: Int): Project {
        val ch = InstrumentTrack.mixerChannel(track)
        val c = project.mixer.channels[ch]
        val inserts = c.inserts.map { ins ->
            if (ins.effect == EffectType.FILTER && ins.params.firstOrNull() == StarterWizard.SUB_HIGH_PASS) {
                ins.copy(params = listOf(BASS_MUSIC_SUB_HIGH_PASS) + ins.params.drop(1))
            } else ins
        }
        if (inserts == c.inserts) return project
        return project.copy(mixer = project.mixer.copy(channels = project.mixer.channels.toMutableList().also { it[ch] = c.copy(inserts = inserts).sanitized() }))
    }

    /** Song bars, from the sections. */
    private fun bars(sections: List<StarterWizard.Section>): Int = sections.last().let { it.start + it.bars }.coerceAtMost(AutomationLane.MAX_SONG_BARS)

    /**
     * The lead's reverb send: its own level, raised for the last bar of each 8-bar phrase (or of a
     * shorter section), jumping up and down on the bar lines: the phrase's last note rings out.
     */
    fun phraseEndLane(project: Project, lead: Int, sections: List<StarterWizard.Section>): AutomationLane? {
        val ch = InstrumentTrack.mixerChannel(lead)
        val base = project.mixer.channels[ch].sends[0]
        val high = maxOf(PHRASE_END_SEND, base * 2.5f).coerceAtMost(1f)
        val n = bars(sections)
        val values = MutableList(n) { base }
        for (sec in sections) {
            var b = sec.start
            while (b < sec.start + sec.bars) {
                val end = minOf(b + 8, sec.start + sec.bars) - 1
                if (end in values.indices) values[end] = high
                b += 8
            }
        }
        if (values.none { it == high }) return null
        val jumps = values.indices.filter { it > 0 && values[it] != values[it - 1] }
        return AutomationLane(AutoTarget(AutoTarget.SEND, ch, 0), mode = AutomationLane.SONG, songValues = values, songJumps = jumps)
    }

    /**
     * The leads chantier: [track]'s synth cut-off over the song, its own value swinging [octaves] up
     * and down over each 8-bar phrase (of each section, as [phraseEndLane] counts them), at its most
     * open on the phrase's first bar, where the hook comes in, at its darkest on the fifth (a cosine,
     * a point per bar, sliding between them). Null without a section, or when a lane already moves it.
     */
    fun phraseLane(project: Project, track: Int, sections: List<StarterWizard.Section>, octaves: Float): AutomationLane? {
        val target = AutoTarget.synth(track, SynthPatch.CUTOFF)
        if (project.automation.any { it.target == target }) return null
        val base = project.track(track).synth.get(SynthPatch.CUTOFF)
        val n = bars(sections)
        if (n <= 0) return null
        val values = MutableList(n) { base }
        for (sec in sections) {
            for (b in sec.start until minOf(n, sec.start + sec.bars)) {
                val inPhrase = (b - sec.start) % 8
                val swing = octaves * kotlin.math.cos(2.0 * Math.PI * inPhrase / 8.0).toFloat()
                values[b] = ProVoices.cutoffMoved(base, swing).coerceIn(0f, 1f)
            }
        }
        return AutomationLane(target, mode = AutomationLane.SONG, songValues = values)
    }

    /**
     * The wobble's speed: 1/8, then 1/4, then 1/16 (and round again) for each 8-bar block of each
     * section; riddim ends each block on the 1/8 triplet. Each point holds (a speed, not a slide).
     */
    fun wobbleLane(bass: Int, sections: List<StarterWizard.Section>, triplets: Boolean): AutomationLane {
        val order = listOf(SynthPatch.SYNC_EIGHTH, SynthPatch.SYNC_QUARTER, SynthPatch.SYNC_SIXTEENTH)
        val n = bars(sections)
        val values = MutableList(n) { SynthPatch.syncValue(SynthPatch.SYNC_EIGHTH) }
        for (sec in sections) {
            for (b in sec.start until minOf(n, sec.start + sec.bars)) {
                val block = (b - sec.start) / 8
                val lastOfBlock = (b - sec.start) % 8 == 7
                values[b] = SynthPatch.syncValue(if (triplets && lastOfBlock) SynthPatch.SYNC_EIGHTH_TRIPLET else order[block % order.size])
            }
        }
        return AutomationLane(AutoTarget.synth(bass, SynthPatch.LFO2_SYNC), mode = AutomationLane.SONG, songValues = values, hold = true)
    }

    /** Neurofunk's vowel: a different one for each 8-bar block, sliding between them (the bass changes what it says). */
    fun vowelLane(bass: Int, sections: List<StarterWizard.Section>): AutomationLane {
        val vowels = listOf(0.2f, 0.55f, 0.35f, 0.75f)
        val n = bars(sections)
        val values = MutableList(n) { vowels[0] }
        for (sec in sections) {
            for (b in sec.start until minOf(n, sec.start + sec.bars)) values[b] = vowels[((b - sec.start) / 8) % vowels.size]
        }
        return AutomationLane(AutoTarget.synth(bass, SynthPatch.VOWEL), mode = AutomationLane.SONG, songValues = values)
    }

    /** [lanes] added (each replacing a lane of the same target), as many as fit. */
    private fun withLanes(project: Project, lanes: List<AutomationLane>): Project {
        if (lanes.isEmpty()) return project
        val kept = project.automation.filterNot { l -> lanes.any { it.target == l.target } }
        return project.copy(automation = (kept + lanes).take(AutomationLane.MAX_LANES))
    }
}

/**
 * The lead's track: the first instrument track that is neither a bass (the sub and the second bass
 * included), nor the chords, the riser or the counter-melody. Null when there is none (the song
 * bench renders it alone to measure how its sound moves).
 */
fun Project.leadTrackId(): Int? {
    val basses = bassTrackIds().toSet()
    val pads = padTrackIds().toSet()
    val riser = riserTrackId()
    return instrumentTracks.firstOrNull { t ->
        val name = DefaultNames.french(t.name) ?: t.name
        t.id !in basses && t.id !in pads && t.id != riser && name != StarterWizard.COUNTER_TRACK && name != StarterWizard.SUB_TRACK &&
            name != StarterWizard.BASS_SWITCH_TRACK && (t.notes.notes.isNotEmpty() || allScenes.any { s -> s.trackNotes[t.id]?.notes?.isNotEmpty() == true })
    }?.id
}
