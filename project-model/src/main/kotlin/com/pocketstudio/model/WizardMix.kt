package com.pocketstudio.model

import com.pocketstudio.model.StarterWizard.Part
import com.pocketstudio.model.StarterWizard.PartSound
import com.pocketstudio.model.StarterWizard.Role
import com.pocketstudio.model.StarterWizard.SongForm
import com.pocketstudio.model.StarterWizard.Style
import kotlin.math.log10
import kotlin.math.pow

/**
 * The body of the assistant's songs (audit « corps », phase 29): the mix a new song starts from
 * (each line and part at its place, panned, glued) and the fill that ends each 4-bar phrase.
 * Only for the songs it makes: the kit itself (DrumKit.cpp) and the projects saved before keep their sound.
 */
internal object WizardMix {
    /**
     * Loudness (LUFS) of each built-in kit voice played alone, 4 hits a bar at 128 BPM, through the
     * engine (audit « corps », kitlvl): the kick as loud as a mastered song, the closed hat 40 dB under it.
     */
    val KIT_LUFS = floatArrayOf(
        -5.5f, -11.9f, -17.2f, -14.0f, -15.9f, -45.0f, -30.5f, -9.9f,
        -10.1f, -11.4f, -32.4f, -24.0f, -25.9f, -18.1f, -14.2f, -21.7f,
    )

    /** Where each line of the beat sits in the same figure: the kick the reference, the hats far under it. */
    private val ROLE_LUFS = mapOf(
        Role.Kick to -12f, Role.Snare to -16f, Role.Clap to -16f, Role.Hat to -27f, Role.OpenHat to -27f,
        Role.Perc to -22f, Role.Perc2 to -22f, Role.Tom to -18f, Role.Ride to -28f,
    )

    /** The stereo place of each line (the kick, the snare, the clap and the low end stay in the centre). */
    val PANS = mapOf(Role.Hat to -0.2f, Role.OpenHat to 0.2f, Role.Perc to 0.35f, Role.Perc2 to -0.35f, Role.Ride to 0.25f, Role.Tom to 0.3f)

    /** Lines whose sound is all high frequencies: what their fader cannot add, an 8 kHz shelf adds. */
    private val BRIGHT = setOf(Role.Hat, Role.OpenHat, Role.Ride, Role.Perc, Role.Perc2)

    /** The most a fader adds (1.0 = (1 / 0.8)² ≈ +3.9 dB, engine Mixer). */
    val FADER_MAX_DB = (40 * log10(1 / 0.8)).toFloat()

    /** The fader position of [db] decibels (0.8 = 0 dB, the engine's (v / 0.8)² law). */
    fun fader(db: Float): Float = (0.8f * 10f.pow(db / 40f)).coerceIn(0f, 1f)

    /** The most the 8 kHz shelf of the pad's equaliser adds for a quiet bright line. */
    private const val SHELF_MAX_DB = 9f

    /** Fader of each melodic part (its sound already calibrated, [SoundLibrary.SYNTH_GAIN_DB]), in dB. */
    private fun partDb(part: Part, held: Boolean): Float = when (part) {
        Part.Bass -> 2.5f
        Part.Lead -> -1f
        Part.Pad -> if (held) -4f else -1f
    }

    /** The master of a song the assistant makes: its layers balanced, it leaves the limiter a few dB of work. */
    const val MASTER_DB = -1f

    /** [sound] holds its chords (a pad, not stabs nor keys) in [style]. */
    fun holds(style: Style, sound: PartSound?): Boolean =
        (sound?.family in setOf("Nappes", "pad")) && !isShort(style.melody.chordRhythm)

    /** A rhythm of short hits only (none longer than an eighth). */
    fun isShort(rhythm: String): Boolean = StepLine.parse(rhythm).let { e -> e.isNotEmpty() && e.all { it.length <= 2 } }

    /**
     * The beat balanced: each built-in kit voice brought to its line's place ([ROLE_LUFS]) by its
     * pad's strip (a bright line short of it also gets the 8 kHz shelf), a kit voice rendered as a
     * sample by its gain; every line panned to its place. A pack's sound keeps its level.
     */
    fun withDrumMix(project: Project, style: Style, sounds: Map<Role, PadSample?>): Project {
        var p = project
        val lanes = p.drums.lanes.toMutableList()
        var channels = p.mixer.channels
        for (line in style.lines) {
            if (line.role !in sounds) continue
            val lane = line.role.lane
            PANS[line.role]?.let { pan -> lanes[lane] = lanes[lane].copy(pan = pan) }
            val target = ROLE_LUFS[line.role] ?: continue
            val sample = sounds[line.role]
            val (voice, base) = when {
                sample == null -> (line.voice ?: lane) to 0f
                sample.name == StarterWizard.RIDE_NAME -> 12 to (20 * log10(sample.gain.coerceAtLeast(0.01f)))
                line.voice != null -> line.voice to 0f
                else -> continue
            }
            var db = target - (KIT_LUFS[voice] + base)
            if (sample != null) {
                // A rendered kit voice: its own gain first (up to x2), the strip for the rest.
                val gain = (sample.gain * 10f.pow(db / 20f)).coerceIn(0f, 2f)
                db -= 20 * log10(gain / sample.gain.coerceAtLeast(0.01f))
                lanes[lane] = lanes[lane].copy(sample = sample.copy(gain = gain))
            }
            if (kotlin.math.abs(db) < 0.5f) continue
            val ch = Mixer.padChannel(lane)
            var c = channels[ch].copy(volume = fader(db.coerceAtMost(FADER_MAX_DB)))
            val rest = db - FADER_MAX_DB
            if (rest > 1f && line.role in BRIGHT) {
                val slot = c.inserts.indexOfFirst { it.type == 0 }
                val shelf = 0.5f + rest.coerceAtMost(SHELF_MAX_DB) / 24f
                if (slot >= 0) c = c.copy(inserts = c.inserts.toMutableList().also { it[slot] = Insert(EffectType.EQ.ordinal, listOf(0.5f, 0.5f, shelf, 0f)) })
            }
            channels = channels.toMutableList().also { it[ch] = c.sanitized() }
        }
        p = p.copy(drums = p.drums.copy(lanes = lanes), mixer = p.mixer.copy(channels = channels))
        return p
    }

    /**
     * [track]'s sound [db] louder (or softer): its calibration ([SynthPatch.GAIN], [PlaitsPatch.gain])
     * or its sample's gain, so that the faders stay at 0 dB for the user.
     */
    private fun Project.louder(track: Int, db: Float): Project {
        val t = track(track)
        return withTrack(when (t.engine) {
            0 -> t.copy(synth = t.synth.with(SynthPatch.GAIN, SynthPatch.gainValue(SynthPatch.gainDb(t.synth.get(SynthPatch.GAIN)) + db)))
            1 -> t.copy(plaits = t.plaits.copy(gain = SynthPatch.gainValue(SynthPatch.gainDb(t.plaits.gain) + db)))
            else -> t.copy(sampled = t.sampled.copy(gain = (t.sampled.gain * 10f.pow(db / 20f)).coerceIn(0f, 2f)))
        })
    }

    /**
     * The melodic tracks at their place (their sound's level, the faders left at 0 dB): the bass
     * up front, the lead and the chords behind it, held chords spread wide (a widener and some
     * reverb, unless the style treats them), the lead with a little echo; the counter-melody under them.
     */
    fun withPartMix(project: Project, style: Style, parts: Map<Part, PartSound>, partOfTrack: Map<Int, Part>, counter: Int?): Project {
        var p = project
        for ((track, part) in partOfTrack) p = p.louder(track, partDb(part, part == Part.Pad && holds(style, parts[part])))
        if (counter != null) p = p.louder(counter, COUNTER_DB)
        var channels = p.mixer.channels
        fun edit(track: Int, f: (Channel) -> Channel) {
            val ch = InstrumentTrack.mixerChannel(track)
            channels = channels.toMutableList().also { it[ch] = f(it[ch]).sanitized() }
        }
        for ((track, part) in partOfTrack) {
            val held = part == Part.Pad && holds(style, parts[part])
            edit(track) { c ->
                var out = c
                val treated = out.inserts.any { it.type != 0 }
                if (held && !treated) {
                    val slot = out.inserts.indexOfFirst { it.type == 0 }
                    if (slot >= 0) out = out.copy(inserts = out.inserts.toMutableList().also { it[slot] = Insert(EffectType.WIDENER.ordinal, listOf(0.75f, 0.35f)) })
                    out = out.copy(sends = listOf(maxOf(out.sends[0], 0.2f), out.sends[1]))
                }
                if (part == Part.Lead && out.sends.all { it == 0f }) out = out.copy(sends = listOf(0.1f, 0.15f))
                out
            }
        }
        if (counter != null) edit(counter) { it.copy(sends = listOf(0.15f, 0.2f)) }
        return p.copy(mixer = p.mixer.copy(channels = channels))
    }

    /** The counter-melody's fader: under the chords and the lead, it answers them. */
    const val COUNTER_DB = -5f

    /** Styles that bring their own master or no pump on the bass (they squash or duck it themselves). */
    private val OWN_PUMP = setOf("gabber", "acidcore", "industrial_techno", "hard_techno")

    /**
     * What glues the song when the style does not: the beat through a light compressor and some
     * saturation, the master through a gentle compressor and saturation (a light OTT first in bass
     * music), and in house and techno the bass ducked by the kick (the pump).
     */
    fun withGlue(project: Project, style: Style, partOfTrack: Map<Int, Part>, hasDrums: Boolean): Project {
        var channels = project.mixer.channels
        var master = project.mixer.master
        fun free(list: List<Insert>, add: List<Insert>): List<Insert> {
            var out = list
            for (ins in add) {
                val slot = out.indexOfFirst { it.type == 0 }
                if (slot >= 0) out = out.toMutableList().also { it[slot] = ins }
            }
            return out
        }
        if (hasDrums && channels[0].inserts.all { it.type == 0 }) {
            channels = channels.toMutableList().also {
                it[0] = it[0].copy(inserts = free(it[0].inserts, listOf(
                    Insert(EffectType.COMPRESSOR.ordinal, listOf(0.6f, 0.25f, 0.45f, 0.3f)),
                    Insert(EffectType.SATURATION.ordinal, listOf(0.15f, 0.85f, 1f, 0.7f)),
                    // A touch of air (+2 dB over 8 kHz): the kit's hats are far under its kick.
                    Insert(EffectType.EQ.ordinal, listOf(0.5f, 0.5f, 0.5f + 2f / 24f, 0f)),
                ))).sanitized()
            }
        }
        // The style's own master (an OTT) keeps it, the glue goes after it; a master already squashed and saturated (acidcore) stays.
        if (master.none { it.type == EffectType.COMPRESSOR.ordinal || it.type == EffectType.SATURATION.ordinal }) {
            val ott = if (style.family == "Bass music" && master.none { it.type == EffectType.OTT.ordinal }) {
                listOf(Insert(EffectType.OTT.ordinal, listOf(0.25f, 0.5f, 0.5f, 0.5f)))
            } else emptyList()
            master = free(master, ott + listOf(
                Insert(EffectType.COMPRESSOR.ordinal, listOf(0.6f, 0.2f, 0.7f, 0.45f)),
                Insert(EffectType.SATURATION.ordinal, listOf(0.2f, 0.85f, 1f, 0.8f)),
            ))
        }
        if (style.family in setOf("House", "Techno") && style.id !in OWN_PUMP) {
            val bass = partOfTrack.entries.firstOrNull { it.value == Part.Bass }?.key?.let { InstrumentTrack.mixerChannel(it) }
            if (bass != null && channels[bass].inserts.none { it.type == EffectType.PUMP.ordinal }) {
                channels = channels.toMutableList().also {
                    it[bass] = it[bass].copy(inserts = free(it[bass].inserts, listOf(Insert(EffectType.PUMP.ordinal, listOf(0.45f, 0.35f, 0f))))).sanitized()
                }
            }
        }
        return project.copy(mixer = project.mixer.copy(channels = channels, master = master, masterDb = MASTER_DB))
    }

    /**
     * The Break breathes: the chords' low-pass shut at its start, opening through it into the Drop
     * (a song curve; a section played in a loop hears them open), unless the style sweeps them itself.
     */
    fun breakSweeps(style: Style, plan: StarterWizard.ScenePlan, partOfTrack: Map<Int, Part>): List<StarterWizard.Sweep> {
        if (!plan.hasBreak || plan.form == SongForm.Rave || Part.Pad !in partOfTrack.values) return emptyList()
        if (3 !in plan.parts[Part.Pad].orEmpty() || style.sweeps.any { it.target == Part.Pad.id }) return emptyList()
        return listOf(StarterWizard.Sweep(Part.Pad.id, mapOf(3 to (0.35f to 0.85f)), resonance = 0.3f))
    }

    /** Steps of a 4-bar phrase: the beat of a section with a melody is written out over it, its last beat a fill. */
    const val PHRASE_STEPS = 64

    /**
     * [lanes] (one scene's beat of [steps] steps) written out over [PHRASE_STEPS], the last beat a
     * fill: the snare (or the clap) rolls crescendo, the kick stops, the open hat breathes on the
     * last eighth; a beat without snare nor clap (an intro, a song's verse) rolls its hats instead.
     * Only lines already playing in the scene get the fill.
     */
    fun phrase(lanes: List<DrumLane>, steps: Int, form: SongForm): List<DrumLane> {
        if (steps <= 0 || PHRASE_STEPS % steps != 0) return lanes
        val out = lanes.map { l ->
            l.copy(hits = (0 until PHRASE_STEPS step steps).flatMap { start -> l.hits.filterKeys { it < steps }.map { (i, h) -> start + i to h } }.toMap())
        }.toMutableList()
        val last = PHRASE_STEPS - 4
        fun has(role: Role) = out[role.lane].hits.isNotEmpty()
        fun set(role: Role, hits: Map<Int, Hit>) {
            out[role.lane] = out[role.lane].copy(hits = out[role.lane].hits.filterKeys { it < last } + hits)
        }
        // Rap and pop roll the hats; club music the snare (or the clap).
        val roll = if (form == SongForm.Song) null else listOf(Role.Snare, Role.Clap).firstOrNull { has(it) }
        if (roll != null) set(roll, mapOf(last to Hit(0.45f), last + 1 to Hit(0.55f), last + 2 to Hit(0.7f, repeats = 2), last + 3 to Hit(0.85f, repeats = 2)))
        else if (has(Role.Hat)) set(Role.Hat, mapOf(last to Hit(0.5f), last + 1 to Hit(0.5f), last + 2 to Hit(0.6f, repeats = 2), last + 3 to Hit(0.7f, repeats = 3)))
        // The kick stops for the last beat (when it plays elsewhere in the phrase).
        if (has(Role.Kick) && out[Role.Kick.lane].hits.keys.any { it < last }) set(Role.Kick, emptyMap())
        if (has(Role.OpenHat)) out[Role.OpenHat.lane] = out[Role.OpenHat.lane].copy(hits = out[Role.OpenHat.lane].hits + (last + 2 to Hit(0.6f)))
        return out
    }
}
