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

    /** Families whose leads take the octave-down double (club music and the « Autres » pop sounds). */
    private val OCTAVE_FAMILIES = setOf("Techno", "House", "Bass music", "Autres")

    /** Styles whose bass wobbles at a speed that changes every 8 bars (1/8 → 1/4 → 1/16). */
    private val WOBBLE_STYLES = setOf("dubstep", "riddim")
    private val REESE_STYLES = setOf("dnb", "liquid_dnb", "jungle")

    // --- The sounds -------------------------------------------------------------------------

    /** A lead: wide, oversampled, its filter breathing over 8 bars, doubled an octave down in club music. */
    fun lead(p: SynthPatch, style: Style): SynthPatch {
        var s = p.with(SynthPatch.WIDTH, if (p.get(SynthPatch.UNISON) > 0f) 0.8f else 0.6f)
            .with(SynthPatch.OVERSAMPLE, 1f)
            .with(SynthPatch.CYCLE_BARS, SynthPatch.cycleValue(8)).with(SynthPatch.CYCLE_DEPTH, 0.6f)
        if (style.family in OCTAVE_FAMILIES) s = s.with(SynthPatch.OCTAVE_DOWN, 0.3f)
        return s
    }

    /** A pad: wide, breathing over 8 bars (oversampled when it has few copies: the CPU of 8 voices stays low). */
    fun pad(p: SynthPatch): SynthPatch {
        var s = p.with(SynthPatch.WIDTH, PAD_WIDTH).with(SynthPatch.CYCLE_BARS, SynthPatch.cycleValue(8)).with(SynthPatch.CYCLE_DEPTH, 0.2f)
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
        var p = project.copy(soundVersion = Project.SOUND_V2)
        p = p.copy(mixer = withBuses(p.mixer, style).copy(monoBassHz = MONO_BASS_HZ))
        val bassMusic = style.family == "Bass music"
        val leads = partOfTrack.filterValues { it == Part.Lead }.keys +
            p.instrumentTracks.filter { DefaultNames.french(it.name) == StarterWizard.COUNTER_TRACK || it.name == StarterWizard.COUNTER_TRACK }.map { it.id }
        val basses = partOfTrack.filterValues { it == Part.Bass }.keys +
            p.instrumentTracks.filter { it.name == StarterWizard.BASS_SWITCH_TRACK }.map { it.id }
        for (t in p.instrumentTracks) {
            val synth = t.engine == 0 && t.instrument == null
            val edited = when {
                t.id in leads -> if (synth) t.copy(synth = lead(t.synth, style)) else t
                partOfTrack[t.id] == Part.Pad -> if (synth) t.copy(synth = pad(t.synth)) else t
                t.id in basses && bassMusic && synth -> t.copy(synth = when {
                    style.id == "neurofunk" -> neuro(t.synth)
                    style.id == "riddim" -> riddim(t.synth)
                    style.id in REESE_STYLES && isReese(t.synth) -> reese(t.synth)
                    else -> bassMusic(t.synth)
                })
                else -> t
            }
            if (edited != t) p = p.withTrack(edited)
        }
        // The leads' chorus and ping-pong echo, and a little more of the reverb.
        for (id in leads) p = withLeadEffects(p, id)
        // Bass music: the bass over the sub cut at 80 Hz, not 120; the sub saturated a little (still
        // mono, no width): its third harmonic, 130-260 Hz, is what a phone hears of it.
        if (bassMusic) {
            for (id in basses) p = withLowerSubCut(p, id)
            p.instrumentTracks.filter { it.name == StarterWizard.SUB_TRACK && it.engine == 0 }.forEach { sub ->
                p = p.withTrack(sub.copy(synth = sub.synth.with(SynthPatch.DRIVE, SUB_DRIVE)))
            }
        }
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
        return withLanes(p, lanes)
    }

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
    val LEAD_CHORUS = Insert(EffectType.CHORUS.ordinal, listOf(0.3f, 0.4f, 0.4f, 0.4f))
    val LEAD_PING_PONG = Insert(EffectType.PING_PONG.ordinal, listOf(0.42f, 0.35f, 0.5f, 0.25f))

    private fun withLeadEffects(project: Project, track: Int): Project {
        val ch = InstrumentTrack.mixerChannel(track)
        var c = project.mixer.channels[ch]
        fun has(type: EffectType) = c.inserts.any { it.effect == type }
        fun add(ins: Insert) {
            val slot = c.inserts.indexOfFirst { it.type == 0 }
            if (slot >= 0) c = c.copy(inserts = c.inserts.toMutableList().also { it[slot] = ins })
        }
        if (!has(EffectType.CHORUS) && !has(EffectType.FLANGER) && !has(EffectType.WIDENER)) add(LEAD_CHORUS)
        val echoes = has(EffectType.ECHO) || has(EffectType.PING_PONG)
        if (!echoes && project.mixer.longFxCount(EffectType.PING_PONG) < EffectType.longLimit(EffectType.PING_PONG)) add(LEAD_PING_PONG)
        c = c.copy(sends = c.sends.mapIndexed { bus, v -> if (bus == 0) maxOf(v, LEAD_SEND) else v })
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
