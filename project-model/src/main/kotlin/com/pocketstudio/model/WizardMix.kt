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
     * Loudness (LUFS) of each voice of kit v2 ([Project.KIT_V2], the assistant's songs) played
     * alone, 4 hits a bar at 128 BPM, through the engine without its limiter (phase 33): each near
     * its role's place in [ROLE_LUFS], the rim (a click) under it. Kit v1 had the kick at -5.5 and
     * the closed hat at -45: the mix lifted the hats with full faders and a 9 dB shelf.
     */
    val KIT_LUFS_V2 = floatArrayOf(
        -11.5f, -11.9f, -17.0f, -16.0f, -16.0f, -27.0f, -27.0f, -18.0f,
        -18.0f, -18.0f, -30.9f, -22.0f, -28.0f, -22.0f, -22.0f, -22.0f,
    )

    /** Where each line of the beat sits in the same figure: the kick the reference, the hats far under it. */
    private val ROLE_LUFS = mapOf(
        Role.Kick to -12f, Role.Snare to -16f, Role.Clap to -16f, Role.Hat to -27f, Role.OpenHat to -27f,
        Role.Perc to -22f, Role.Perc2 to -22f, Role.Tom to -18f, Role.Ride to -28f,
    )

    /** The stereo place of each line (the kick, the snare, the clap and the low end stay in the centre). */
    val PANS = mapOf(Role.Hat to -0.2f, Role.OpenHat to 0.2f, Role.Perc to 0.35f, Role.Perc2 to -0.35f, Role.Ride to 0.25f, Role.Tom to 0.3f)

    /** The most a fader adds (1.0 = (1 / 0.8)² ≈ +3.9 dB, engine Mixer). */
    val FADER_MAX_DB = (40 * log10(1 / 0.8)).toFloat()

    /** The fader position of [db] decibels (0.8 = 0 dB, the engine's (v / 0.8)² law). */
    fun fader(db: Float): Float = (0.8f * 10f.pow(db / 40f)).coerceIn(0f, 1f)

    /** Fader of each melodic part (its sound already calibrated, [SoundLibrary.SYNTH_GAIN_DB]), in dB. */
    private fun partDb(part: Part, held: Boolean): Float = when (part) {
        Part.Bass -> 2.5f
        Part.Lead -> -1f
        Part.Pad -> if (held) HELD_PAD_DB else -1f
    }

    /** A held pad's place in the part mix (dB). */
    const val HELD_PAD_DB = -4f

    /** The master of a song the assistant makes: its layers balanced, it leaves the limiter a few dB of work. */
    const val MASTER_DB = -1f

    /** [sound] holds its chords (a pad, not stabs nor keys) in [style]. */
    fun holds(style: Style, sound: PartSound?): Boolean =
        (sound?.family in setOf("Nappes", "pad")) && !isShort(style.melody.chordRhythm)

    /** A rhythm of short hits only (none longer than an eighth). */
    fun isShort(rhythm: String): Boolean = StepLine.parse(rhythm).let { e -> e.isNotEmpty() && e.all { it.length <= 2 } }

    /**
     * The beat balanced: each built-in kit voice (kit v2) brought to its line's place ([ROLE_LUFS])
     * by its pad's fader (a few dB: the kit is near it already), a kit voice rendered as a sample
     * by its gain; every line panned to its place. A pack's sound keeps its level.
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
            var db = target - (KIT_LUFS_V2[voice] + base)
            if (sample != null) {
                // A rendered kit voice: its own gain first (up to x2), the strip for the rest.
                val gain = (sample.gain * 10f.pow(db / 20f)).coerceIn(0f, 2f)
                db -= 20 * log10(gain / sample.gain.coerceAtLeast(0.01f))
                lanes[lane] = lanes[lane].copy(sample = sample.copy(gain = gain))
            }
            if (kotlin.math.abs(db) < 0.5f) continue
            val ch = Mixer.padChannel(lane)
            val c = channels[ch].copy(volume = fader(db.coerceAtMost(FADER_MAX_DB)))
            channels = channels.toMutableList().also { it[ch] = c.sanitized() }
        }
        p = p.copy(drums = p.drums.copy(lanes = lanes), mixer = p.mixer.copy(channels = channels))
        return p
    }

    /** [track] (playing [part]) at the level the assistant gives that part: a track added after the song is made sits in its mix too. */
    fun withPartLevel(project: Project, track: Int, part: Part, held: Boolean): Project = project.louder(track, partDb(part, held))

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
     * The drum bus's glue: a light compressor whose detector skips the kick's low end (~90 Hz),
     * then a touch of saturation. Under the threshold both leave the sound as it is (0 dB, flat to
     * 18 kHz: the engine test glue_chains_flat_under_threshold holds the same numbers). Before phase
     * 33 the compressor added a fixed +4.2 dB, the saturation hid a 12 kHz low-pass, an EQ added
     * +2 dB of air for the quiet hats of kit v1.
     */
    val BUS_GLUE = listOf(
        Insert(EffectType.COMPRESSOR.ordinal, listOf(0.6f, 0.25f, 0.45f, 0.3f, 0f, 0f, 0f, 0.5f, 1f)),
        Insert(EffectType.SATURATION.ordinal, listOf(0.15f, EffectType.TONE_OPEN, 1f, 0.7f, 1f)),
    )

    /** The master's glue, the same way (before phase 33: +3.7 dB, and -3.1 dB at 18 kHz). */
    val MASTER_GLUE = listOf(
        Insert(EffectType.COMPRESSOR.ordinal, listOf(0.6f, 0.2f, 0.7f, 0.45f, 0f, 0f, 0f, 0.5f, 1f)),
        Insert(EffectType.SATURATION.ordinal, listOf(0.2f, EffectType.TONE_OPEN, 1f, 0.7f, 1f)),
    )

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
                it[0] = it[0].copy(inserts = free(it[0].inserts, BUS_GLUE)).sanitized()
            }
        }
        // The style's own master (an OTT) keeps it, the glue goes after it; a master already squashed and saturated (acidcore) stays.
        if (master.none { it.type == EffectType.COMPRESSOR.ordinal || it.type == EffectType.SATURATION.ordinal }) {
            val ott = if (style.family == "Bass music" && master.none { it.type == EffectType.OTT.ordinal }) {
                listOf(Insert(EffectType.OTT.ordinal, listOf(0.25f, 0.5f, 0.5f, 0.5f)))
            } else emptyList()
            master = free(master, ott + MASTER_GLUE)
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

    // --- S5: an honest mix and master (docs/PLAN-SON-PRO.md, chantier S5) ----------------------

    /**
     * The drum bus's glue since S5: RMS, 2:1, attack 25 ms, release 100 ms: it holds the beat
     * together without touching the hits' attack (the one stage on a kick's transient is its own
     * Punch, when the style gives it one). Before: 2 ms, peaks (pro2.md § 3: −6,5 dB at 2 ms).
     */
    val BUS_GLUE_V2 = listOf(
        Insert(EffectType.COMPRESSOR.ordinal, listOf(0.6f, 0.231f, 0.826f, 0.436f, 0f, 0f, 1f, 0.5f, 1f)),
        BUS_GLUE[1],
    )

    /** The master's glue since S5: RMS, 1.8:1, attack 30 ms, release 150 ms (about 2 dB at most), then its saturation. */
    val MASTER_GLUE_V2 = listOf(
        Insert(EffectType.COMPRESSOR.ordinal, listOf(0.6f, 0.2f, 0.853f, 0.546f, 0f, 0f, 1f, 0.5f, 1f)),
        MASTER_GLUE[1],
    )

    /** The Punch's attack in an assistant's song since S5: 10 ms (it was 2: the click was taken off with the body). */
    const val PUNCH_ATTACK = 0.689f

    /** A style's pump on its bass (removed: the bus ducks once) becomes this share of the bus's low depth. */
    const val BASS_PUMP_TO_LOW = 0.75f

    /** The bass's soft clipper (before its OTT): 6 dB over its level, half soft. */
    val BASS_CLIPPER = Insert(EffectType.CLIPPER.ordinal, listOf(0.25f, 0.5f, 1f, 0f))

    /**
     * The drum bus's soft clipper, after its glue: the kick's and the snare's tips rounded off a
     * margin over the beat's level, so the master's limiter has less to take (clipping the drums,
     * as club producers do). Its margin by family (dB over the bus's level): the hardest music the
     * tightest; none in the dynamic hip-hop.
     */
    fun drumClipMarginDb(style: Style): Float? = when {
        style.id in NO_CLIP -> null
        style.id in HARD -> 4f
        style.family in setOf("Techno", "Bass music") -> 4.5f
        else -> 7f
    }

    /** The hardest styles: their drums and basses clipped the most (their genre's sound). */
    private val HARD = setOf("hard_techno", "gabber", "industrial_techno", "acidcore")

    /** The bass's clipper margin (dB over its level): tighter in club and bass music. */
    fun bassClipMarginDb(style: Style): Float = if (style.family in setOf("Techno", "Bass music")) 4.5f else 6f

    /** The chords' and the lead's clipper margins (dB over their level), and the club stabs' level. */
    const val PAD_CLIP_DB = 6f
    const val LEAD_CLIP_DB = 8f
    const val STAB_DB = -2f

    /** Bass music: the sub a little lower and the bass's body lifted (60-250 Hz, the band that is short). */
    const val BASS_MUSIC_SUB_DB = -2f
    /**
     * Final audit (report 10): a style's Sub further down: the neurofunk's bass talks in the mids, its
     * Drop had the lowest of the family (0.5-2 kHz at −12.3 dB against the whole, the median −9.1).
     */
    private val SUB_TRIM_DB = mapOf("neurofunk" to -3f)
    val BASS_BODY = Insert(EffectType.PARAM_EQ.ordinal, listOf(0.2594f, 0.5833f, 0.2416f, 0f))  // +3 dB at 120 Hz, Q 0.7

    /** Styles whose basses keep their whole crest (no clipper): the dynamic hip-hop. */
    private val NO_CLIP = setOf("lofi", "boom_bap")

    /** S5's wider drum image (the hats, percussions, ride and toms further out; the kick, snare and clap in the centre). */
    val PANS_V2 = mapOf(Role.Hat to -0.3f, Role.OpenHat to 0.3f, Role.Perc to 0.5f, Role.Perc2 to -0.5f, Role.Ride to 0.4f, Role.Tom to 0.45f)

    /**
     * The « Musique » bus's duck under the kick, by family: (depth, low depth, release). Club
     * music pumps (house and techno), the lows (the bass, the sub) clear the kick's place deeper
     * everywhere; hip-hop and the rest only make room.
     */
    fun musicDuck(style: Style, padPump: Float): Triple<Float, Float, Float> = when {
        style.id in OWN_PUMP -> Triple(0f, 0.5f, 0.3f)
        style.family in setOf("House", "Techno") -> Triple(padPump.coerceIn(0.2f, 0.45f), 0.6f, 0.35f)
        // Final audit (report 10): the basses out of the kick's way, short (0.55 and 93 ms before: the bass
        // alone dipped less than 3 dB under the kick, read against itself not ducked).
        style.family == "Bass music" -> Triple(0.12f, BASS_MUSIC_LOW_DUCK, BASS_MUSIC_DUCK_RELEASE)
        style.family == "Hip-hop" -> Triple(0f, 0.3f, 0.25f)
        else -> Triple(padPump.coerceAtMost(0.35f), 0.4f, 0.3f)
    }

    /** Bass music's low duck under the kick (the « Musique » bus's low depth) and its release (0.19: about 80 ms). */
    const val BASS_MUSIC_LOW_DUCK = 0.8f
    const val BASS_MUSIC_DUCK_RELEASE = 0.19f

    /**
     * S5, in [StarterWizard.build] just before [WideMix.apply] (then [withAudibleBass], last): the assistant's song gets an
     * honest mix ([Project.MIX_V2], since S7 [Project.MIX_V4]: its loudness target aims at the Drop, the master chain takes
     * 3 LU at most there). One stage on the kick's transient (the drum bus and the master glue
     * slow and in RMS, the Punch at 10 ms); a soft clipper on the bass before its OTT; a real
     * sidechain: the kick's sound (its pad) is the key, the melodic tracks and the returns go
     * through the « Musique » bus that ducks once under it (their own kick pumps are taken off;
     * a pad's pump, the rumble's, listens to the kick's sound); the drums' image a little wider.
     * The projects saved before keep their mix.
     */
    fun honest(project: Project, style: Style, partOfTrack: Map<Int, Part>): Project {
        var p = project.copy(mixVersion = Project.MIX_V4)
        var channels = p.mixer.channels.toMutableList()
        fun replaced(list: List<Insert>, from: Insert, to: Insert) = list.map { if (it.type == from.type && it.params == from.sanitized().params) to else it }
        channels[0] = channels[0].copy(inserts = replaced(channels[0].inserts, BUS_GLUE[0].sanitized(), BUS_GLUE_V2[0])).sanitized()
        val master = replaced(p.mixer.master, MASTER_GLUE[0].sanitized(), MASTER_GLUE_V2[0]).map { it.sanitized() }
        // The Punch (on a pad, on the drums): 10 ms.
        val punch = FxPresets.byId("punch")?.inserts?.firstOrNull()?.sanitized()
        if (punch != null) {
            for (ch in listOf(0) + (Mixer.FIRST_PAD until Mixer.FIRST_PAD + Mixer.PADS)) {
                channels[ch] = channels[ch].copy(inserts = channels[ch].inserts.map {
                    if (it.type == punch.type && it.params == punch.params) it.copy(params = it.params.toMutableList().also { v -> v[2] = PUNCH_ATTACK }) else it
                }).sanitized()
            }
        }
        // The basses' soft clipper, before their OTT (or in their first free slot).
        val basses = partOfTrack.filterValues { it == Part.Bass }.keys +
            p.instrumentTracks.filter { it.name == StarterWizard.BASS_SWITCH_TRACK }.map { it.id }
        if (style.id !in NO_CLIP) {
            for (id in basses.toSet()) {
                val ch = InstrumentTrack.mixerChannel(id)
                if (ch !in channels.indices || p.instrumentTracks.none { it.id == id }) continue
                val clip = BASS_CLIPPER.copy(params = listOf(((bassClipMarginDb(style) - 3f) / 12f).coerceIn(0f, 1f)) + BASS_CLIPPER.params.drop(1))
                channels[ch] = channels[ch].copy(inserts = withBefore(channels[ch].inserts, clip, EffectType.OTT.ordinal)).sanitized()
            }
        }
        // The chords and the lead: their attacks rounded off (a stab's 4 notes at once peak 20 dB over
        // its level: they, not the kick, set what the limiter takes); club stabs 2 dB further back.
        if (style.id !in NO_CLIP) {
            for ((id, part) in partOfTrack) {
                if (part == Part.Bass) continue
                val ch = InstrumentTrack.mixerChannel(id)
                if (ch !in channels.indices || p.instrumentTracks.none { it.id == id }) continue
                val margin = if (part == Part.Pad) PAD_CLIP_DB else LEAD_CLIP_DB
                val free = channels[ch].inserts.indexOfFirst { it.type == 0 }
                if (free >= 0) channels[ch] = channels[ch].copy(inserts = channels[ch].inserts.toMutableList().also {
                    it[free] = Insert(EffectType.CLIPPER.ordinal, listOf(((margin - 3f) / 12f).coerceIn(0f, 1f), 0.5f, 1f, 0f))
                }).sanitized()
                if (part == Part.Pad && isShort(style.melody.chordRhythm) && style.family in setOf("House", "Techno", "Bass music")) p = p.louder(id, STAB_DB)
            }
        }
        // The riser's noise (its climb and its impact on the Drop's first beat): its tips rounded too.
        p.instrumentTracks.filter { it.name == StarterWizard.RISER_TRACK }.forEach { t ->
            val ch = InstrumentTrack.mixerChannel(t.id)
            val free = channels[ch].inserts.indexOfFirst { it.type == 0 }
            if (free >= 0) channels[ch] = channels[ch].copy(inserts = channels[ch].inserts.toMutableList().also {
                it[free] = Insert(EffectType.CLIPPER.ordinal, listOf(((PAD_CLIP_DB - 3f) / 12f).coerceIn(0f, 1f), 0.5f, 1f, 0f))
            }).sanitized()
        }
        // The drum bus clipped a little after its glue.
        drumClipMarginDb(style)?.let { margin ->
            val clip = Insert(EffectType.CLIPPER.ordinal, listOf(((margin - 3f) / 12f).coerceIn(0f, 1f), 0.5f, 1f, 0f))
            val free = channels[0].inserts.indexOfFirst { it.type == 0 }
            if (free >= 0 && channels[0].inserts.any { it.type != 0 }) {
                channels[0] = channels[0].copy(inserts = channels[0].inserts.toMutableList().also { it[free] = clip }).sanitized()
            }
        }
        // Bass music's lows: the sub down, the bass's body up.
        if (style.family == "Bass music") {
            for (id in basses.toSet()) {
                val ch = InstrumentTrack.mixerChannel(id)
                if (ch !in channels.indices || p.instrumentTracks.none { it.id == id }) continue
                val free = channels[ch].inserts.indexOfFirst { it.type == 0 }
                if (free >= 0) channels[ch] = channels[ch].copy(inserts = channels[ch].inserts.toMutableList().also { it[free] = BASS_BODY }).sanitized()
            }
            p.instrumentTracks.filter { it.name == StarterWizard.SUB_TRACK }.forEach { p = p.louder(it.id, BASS_MUSIC_SUB_DB + (SUB_TRIM_DB[style.id] ?: 0f)) }
        }
        // The sidechain: the kick's pad is the key; the Musique bus ducks once.
        val kick = Role.Kick.lane
        val hasKick = (listOf(p.drums) + p.scenes.map { it.drums }).any { it.lanes.getOrNull(kick)?.hits?.isNotEmpty() == true }
        var musicBus: MusicBus? = null
        if (hasKick) {
            val members = (listOf(1) + p.instrumentTracks.filter { it.name != StarterWizard.SUB_DROP_TRACK }.map { InstrumentTrack.mixerChannel(it.id) })
                .filter { it in channels.indices && it != Mixer.MASTER }.toSet()
            var padPump = 0f
            var bassPump = 0f
            for (ch in members) {
                val c = channels[ch]
                val pumps = c.inserts.filter { it.type == EffectType.PUMP.ordinal && it.params.getOrElse(2) { 0f } < 0.5f }
                if (pumps.isEmpty()) continue
                val bass = p.instrumentTracks.any { InstrumentTrack.mixerChannel(it.id) == ch && (it.id in basses || it.name == StarterWizard.SUB_TRACK) }
                val deepest = pumps.maxOf { it.params.getOrElse(0) { 0f } }
                if (bass) bassPump = maxOf(bassPump, deepest) else padPump = maxOf(padPump, deepest)
                channels[ch] = c.copy(inserts = c.inserts.map { if (it in pumps) Insert() else it }).sanitized()
            }
            for (ch in Mixer.FIRST_PAD until Mixer.FIRST_PAD + Mixer.PADS) {
                channels[ch] = channels[ch].copy(inserts = channels[ch].inserts.map {
                    if (it.type == EffectType.PUMP.ordinal && it.params.getOrElse(2) { 0f } < 0.5f) {
                        it.copy(params = it.params.toMutableList().also { v -> while (v.size <= EffectType.PUMP_AUDIO_KEY) v += 0f; v[EffectType.PUMP_AUDIO_KEY] = 1f })
                    } else it
                }).sanitized()
            }
            val (depth, low, release) = musicDuck(style, padPump)
            // A style's own pump on its bass (the « pump » preset: 0.8) asks the lows to go deeper.
            val lows = maxOf(low, depth, bassPump * BASS_PUMP_TO_LOW).coerceAtMost(0.8f)
            musicBus = MusicBus(members = members, depth = depth, lowDepth = lows, release = release, returns = true)
        }
        // The drums' image: the lines still at the assistant's place go a little further out.
        val lanes = p.drums.lanes.toMutableList()
        for ((role, pan) in PANS_V2) {
            val lane = role.lane
            // A line the style does not play (the last Drop's extra layer, G0) takes its place too.
            val extra = lanes.getOrNull(lane)?.pan == 0f && style.lines.none { it.role == role }
            if (lane in lanes.indices && (lanes[lane].pan == PANS[role] || extra)) lanes[lane] = lanes[lane].copy(pan = pan)
        }
        p = withRisingMontees(p, drumsPeak = if (style.form == SongForm.Build || style.id in PHONE_FLAT_ROLL) 0.8f else DRUMS_PEAK)
        p = withSectionLevels(
            p, build = style.form == SongForm.Build, monteeTop = monteeTopDb(style), monteeFloor = monteeFloorDb(style),
            groove = grooveDb(style), intro = introDb(style), breakTrim = BREAK_SPREAD_TRIM_DB + (BREAK_LAYER_TRIM_DB[style.id] ?: 0f),
        )
        p = p.copy(
            drums = p.drums.copy(lanes = lanes),
            scenes = p.scenes.map { sc ->
                sc.copy(drums = sc.drums.copy(lanes = sc.drums.lanes.mapIndexed { i, l -> if (i < lanes.size) l.copy(pan = lanes[i].pan) else l }))
            },
            mixer = p.mixer.copy(
                channels = channels, master = master,
                sidechainKey = if (hasKick) Mixer.padChannel(kick) else -1, musicBus = musicBus,
            ),
        )
        return p
    }

    // --- « Des basses qui tapent » (docs/hits/basses-qui-tapent.md, P1 and P4) ---------------

    /**
     * The bass exciter's amount by style (« Basse audible », [EffectType.BASS_EXCITER]): hip-hop's
     * 808s the most (0.5: they are almost a sine), club music and pop 0.35, bass music 0.25 (its mid
     * layers already speak), the soft hip-hop 0.2; the analysis's own (§ 9) where it gives one.
     */
    private val EXCITER_BY_STYLE = mapOf(
        "lofi" to 0.2f, "boom_bap" to 0.2f, "deep_house" to 0.3f, "disco" to 0.3f, "amapiano" to 0.4f, "liquid_dnb" to 0.25f,
        "grime" to 0.3f, "reggaeton" to 0.4f, "afrobeats" to 0.35f, "synthwave" to 0.3f,
    )

    fun exciterAmount(style: Style): Float = (EXCITER_BY_STYLE[style.id] ?: when (style.family) {
        "Hip-hop" -> 0.5f
        "Bass music" -> 0.25f
        else -> 0.35f
    }) + if (style.id in PHONE_SHORT) PHONE_BUMP else 0f

    /**
     * S6 son (M11): the styles whose Drop still lost more than 4 LU through a phone's speaker on the
     * S6 bench (lo-fi 8.1, boom bap 6.8, afro house 6.5…): their exciter [PHONE_BUMP] higher, for the
     * new songs (a little more of the 300-800 Hz harmonics a phone plays).
     */
    private val PHONE_SHORT = setOf(
        "acid_techno", "afro_house", "afrobeats", "amapiano", "boom_bap", "deep_house", "disco", "drill", "house", "jersey_club", "lofi",
        "minimal", "phonk", "pop", "retro_techno", "speed_garage", "synthwave", "techno", "trap",
    )
    const val PHONE_BUMP = 0.1f

    /** The exciter on a bass: [amount], its band under 110 Hz, its colour mostly even, its top at 600 Hz. */
    fun bassExciter(amount: Float) = Insert(EffectType.BASS_EXCITER.ordinal, listOf(amount, 0.5f, 0.7f, 0.55f))

    /**
     * The 808 pad's low cut (the tracks' are [RoleMix]'s): 30 Hz, never over 0.7 x its lowest note (C1's
     * 32.7 Hz keeps its fundamental: 23 Hz there), as RoleMix cuts a bass.
     */
    fun pad808LowCutHz(lowestHz: Float): Float = minOf(30f, 0.7f * lowestHz)

    /** A flat parametric EQ whose « Coupe-bas » is at [hz]. */
    fun lowCutEq(hz: Float) = Insert(EffectType.PARAM_EQ.ordinal, listOf(0.5f, 0.5f, 0.4f, RoleMix.lowCutValue(hz)))

    /**
     * « Des basses qui tapent », in [StarterWizard.build] last (after [WideMix.apply] and its
     * [RoleMix]): what plays the bass (the bass tracks, the second bass, the Sub, the Sub drop, the 808's
     * pad, a pack's sample as much as a synth) gets the bass exciter ([exciterAmount]: the phone hears
     * its harmonics, the ear the note), before the strip's high-pass, clipper, OTT or pump (the clipper
     * rounds what the exciter adds), its low cut moved in front of it. One owner per role for the low
     * end: RoleMix's low cuts (a bass's adaptive one, under its lowest note; none on the Sub, a sine
     * with no sub of its own; the Sub drop's saturation), the 808 pad's here ([pad808LowCutHz]). In a
     * free slot only, moved by swaps so that the song's curves follow their effects. The song keeps
     * [Project.MIX_V2]: the master's own low cut ([Project.MIX_V3]) cost the Drop 0.6 to 1.3 LU on the
     * bench. The projects saved before keep their mix.
     */
    fun withAudibleBass(project: Project, style: Style, partOfTrack: Map<Int, Part>): Project {
        var p = project
        val amount = exciterAmount(style)
        val tracks = partOfTrack.filterValues { it == Part.Bass }.keys + p.instrumentTracks.filter {
            it.name in setOf(StarterWizard.BASS_SWITCH_TRACK, StarterWizard.SUB_TRACK, StarterWizard.SUB_DROP_TRACK)
        }.map { it.id }
        val pads808 = p.padVoices.filterValues { it.kind == PadVoice.BASS_808 }
        val strips = tracks.filter { id -> p.instrumentTracks.any { it.id == id } }.map { InstrumentTrack.mixerChannel(it) } +
            pads808.keys.map { Mixer.padChannel(it) }
        val before = setOf(EffectType.FILTER.ordinal, EffectType.CLIPPER.ordinal, EffectType.OTT.ordinal, EffectType.PUMP.ordinal)
        for (ch in strips.toSet()) {
            if (ch !in p.mixer.channels.indices) continue
            if (p.mixer.channels[ch].inserts.any { it.type == EffectType.BASS_EXCITER.ordinal }) continue
            // The 808's pad: its own low cut (RoleMix treats the tracks only).
            val lane = pads808.keys.firstOrNull { Mixer.padChannel(it) == ch }
            if (lane != null && RoleMix.lowCutHz(p.mixer.channels[ch]) == null) {
                val tune = pads808.getValue(lane).param(PadVoice.TUNE) ?: 36f
                val hits = (listOf(p.drums) + p.allScenes.map { it.drums }).flatMap { it.lanes.getOrNull(lane)?.hits?.values.orEmpty() }
                val lowest = tune + (hits.minOfOrNull { it.pitch } ?: 0)
                p = p.withInsertedBefore(ch, lowCutEq(pad808LowCutHz(StudioDrums.hz(lowest.toInt()).toFloat())).sanitized(), before)
            }
            p = p.withInsertedBefore(ch, bassExciter(amount).sanitized(), before)
            // Its low cut (RoleMix's EQ, put after the clipper) moved in front of it: no infra feeds its harmonics.
            val now = p.mixer.channels[ch].inserts
            val exciter = now.indexOfFirst { it.type == EffectType.BASS_EXCITER.ordinal }
            val cut = now.indexOfFirst { it.type == EffectType.PARAM_EQ.ordinal && !it.bypass && it.params.getOrElse(3) { 0f } >= 0.01f }
            if (exciter >= 0 && cut > exciter) for (k in cut downTo exciter + 1) p = p.withInsertsSwapped(ch, k, k - 1)
        }
        return p
    }

    /**
     * [ins] on strip [ch] just before its first effect of a type in [types] (or in its first free slot):
     * put in the free slot, then moved up one slot at a time with [withInsertsSwapped], so that the
     * song's curves and MIDI knobs on the effects it passes follow them (a curve on the bass's filter
     * must not end up on the exciter, or on the high-pass over its Sub). Unchanged without a free slot.
     */
    private fun Project.withInsertedBefore(ch: Int, ins: Insert, types: Set<Int>): Project {
        val list = mixer.channels.getOrNull(ch)?.inserts ?: return this
        val free = list.indexOfFirst { it.type == 0 }
        if (free < 0) return this
        var p = copy(mixer = mixer.copy(channels = mixer.channels.toMutableList().also { it[ch] = it[ch].copy(inserts = list.toMutableList().also { l -> l[free] = ins }).sanitized() }))
        val at = list.indexOfFirst { it.type in types }
        if (at in 0 until free) for (k in free downTo at + 1) p = p.withInsertsSwapped(ch, k, k - 1)
        return p
    }

    // --- S5: a Montée that really rises -----------------------------------------------------

    /**
     * The riser's fader outside its rise (as before) and at the top of the rise: +1 dB only (S5b: at
     * +3.9 dB its noise, which the limiter leaves whole, made the Montée's end louder than the Drop
     * after the master; the rise is its filter opening).
     */
    const val RISER_REST = 0.8f
    const val RISER_PEAK = 0.85f
    /**
     * The drums' fader at the top of a Montée (+1 dB over its last 4 bars: the roll comes up, its
     * density and its velocity do the rest), back at 0 dB on the Drop. S5b: +3 dB before.
     */
    const val DRUMS_PEAK = 0.85f
    /**
     * Final audit (report 10): the Montées whose roll does not come up on the drums' fader (a Build's, and
     * these): on a phone the roll, all over 300 Hz, was what the Drop had to land over.
     */
    private val PHONE_FLAT_ROLL = setOf("jungle")

    /** Bars of the rise: the Montée's last 4. */
    const val RISE_BARS = 4
    /** Curves WideMix still adds after this (lead reverb, wobble, vowel): room kept for them. */
    private const val WIDE_LANES = 3

    /**
     * The bars (first, end) of each run of song parts in a Montée ([SongShape.MONTEE], its numbered
     * variants; the drummer's one-bar fills and crashes in it count with it).
     */
    fun monteeRuns(p: Project): List<Pair<Int, Int>> {
        val numbered = Regex("""^(.+) \d+$""")
        val runs = ArrayList<Pair<Int, Int>>()
        var bar = 0
        var open: Int? = null
        for (part in p.song) {
            val bars = p.partSteps(part) / p.stepsPerBar.coerceAtLeast(1)
            val label = p.allScenes.getOrNull(part.scene)?.label.orEmpty()
            val stem = numbered.matchEntire(label)?.groupValues?.get(1) ?: label
            val drumBar = label == PartIntents.FILL_LABEL || label == DrumArrangement.CRASH_LABEL
            if (stem == SongShape.MONTEE) {
                if (open == null) open = bar
            } else if (!drumBar && open != null) {
                runs += open to bar
                open = null
            }
            bar += bars
        }
        if (open != null) runs += open to bar
        return runs
    }

    /**
     * Each Montée rises over its last [RISE_BARS] bars while its kick and bass stop: the riser (its
     * fader at [RISER_REST] elsewhere: unchanged there) climbs to [RISER_PEAK] (+1 dB), the drums
     * (the roll) by 1 dB; both fall back on the Drop's first beat. Song
     * curves (per beat, song only), as many as fit beside WideMix's.
     */
    fun withRisingMontees(project: Project, drumsPeak: Float = DRUMS_PEAK): Project {
        val runs = monteeRuns(project)
        if (runs.isEmpty()) return project
        val p = project
        val b = p.beatsPerBar.coerceAtLeast(1)
        val riser = p.instrumentTracks.firstOrNull { it.name == StarterWizard.RISER_TRACK }
        val lanes = ArrayList<AutomationLane>()
        fun rising(target: AutoTarget, rest: Float, peak: Float): AutomationLane {
            var lane = AutomationLane(target, mode = AutomationLane.SONG, songOnly = true, songPerBeat = true).withSongPoint(0, rest)
            for ((first, end) in runs) {
                val from = maxOf(first, end - RISE_BARS) * b
                val top = end * b - 2  // the beat before the Montée's silent last beat
                if (top <= from) continue
                lane = lane.withSongPoint(from, rest).withSongPoint(top, peak).withSongPoint(top + 1, peak)
                    .withSongPoint(end * b, rest).withSongJump(end * b, true)
            }
            return lane
        }
        if (riser != null) {
            lanes += rising(AutoTarget(AutoTarget.VOLUME, InstrumentTrack.mixerChannel(riser.id)), RISER_REST, RISER_PEAK)
        }
        lanes += rising(AutoTarget(AutoTarget.VOLUME, 0), 0.8f, drumsPeak)
        val room = AutomationLane.MAX_LANES - WIDE_LANES - p.automation.count { l -> lanes.none { it.target == l.target } }
        if (room <= 0) return project
        val kept = p.automation.filterNot { l -> lanes.any { it.target == l.target } }
        val added = lanes.take(room)
        return p.copy(automation = kept + added)
    }

    // --- S5: the sections at their level ------------------------------------------------------

    /**
     * Each section's level on the song's master fader (before the master chain; the loudness target,
     * aimed at the Drop, does not change it), in dB: the Groove and the Couplet under the Drop, the
     * Break and the Pont further under, a Montée (a Pré-refrain) from the level of what is before it
     * up to [MONTEE_TOP_DB] over its last 4 bars, never the Drop's own. Real songs leave their Drop
     * that much above the rest.
     */
    const val GROOVE_DB = -2f
    const val BREAK_DB = -3f

    /**
     * S7 son ([Project.MIX_V4]): the Break (or Pont) this much lower again. The pads and leads spread
     * wide (WideMix.withSpread) read 0.4 to 0.6 LU louder, and a Break is mostly them: without it,
     * Drop − Break fell under +4 LU (dubstep 3.75, acid techno 3.85) on the bench.
     */
    const val BREAK_SPREAD_TRIM_DB = 0.6f

    /**
     * Son 8 (the leads chantier's lots B to D): a Break further down where its pads came out louder (the
     * dubstep pads spread wider, the liquid keys' « air » layer): Drop − Break had fallen to +3.96 and
     * +3.95 LU on the bench.
     */
    private val BREAK_LAYER_TRIM_DB: Map<String, Float> = mapOf("dubstep" to 0.4f, "liquid_dnb" to 0.3f)

    /**
     * S5b: the top of a Montée (a Pré-refrain) on the master fader, and the floor a club Montée eases
     * down to before it climbs over its last 4 bars. A Montée's end has no kick nor bass for the
     * limiter to hold: at 0 dB it came out louder than the Drop it leads into (Benjamin's A/B test,
     * the bench's « Drop − fin de Montée »). Its tension is its roll, its riser's filter, not its level.
     */
    const val MONTEE_TOP_DB = -2f
    /** 2.5 dB under the top (S5b: 1.5 before), so that a club Montée climbs again over its last 4 bars, its kick and its bass out. */
    const val MONTEE_FROM_DB = -4.5f

    /**
     * S5b: the Montée of bass music (a Build song) has no kick nor bass at all, and its Drop loses up
     * to 3 LU in the limiter: it sits lower, at [BUILD_FROM_DB] from its start (about the Intro's
     * level after the master), up to [BUILD_TOP_DB] over its last 4 bars.
     */
    const val BUILD_TOP_DB = -3.5f
    const val BUILD_FROM_DB = -5.5f

    /**
     * S5b: each style's Montée a little higher or lower than its form's (dB on its top and its floor),
     * measured on the bench so that its Drop lands +1.5 to +3 LU over the Montée's end after the master
     * chain. What sets it is how much the limiter holds the Drop (a wobble or a reese bass: up to
     * 4 LU more than the Montée, which it leaves whole) and what still plays at the Montée's end
     * (Detroit's and garage's chords and lead, the acid styles' 303). Lower: the Drop would land under;
     * higher: a Montée that ends 4 to 8 LU under its Drop (the sparse house claps) falls flat.
     */
    private val MONTEE_TRIM_DB = mapOf(
        "acid_house" to -0.5f, "acid_techno" to 0f, "acidcore" to -3f, "afro_house" to 2f, "amapiano" to 2f, "deep_house" to 0.5f,
        "detroit" to -3f, "disco" to -1f, "hard_techno" to -1.5f, "industrial_techno" to 1f, "jungle" to -4f, "minimal" to 0f,
        "retro_techno" to -3.5f, "speed_garage" to -3.5f,
        "dnb" to -1f, "dubstep" to -2f, "gabber" to -1f, "jump_up" to -1f, "liquid_dnb" to -0.5f, "neurofunk" to -2.5f, "riddim" to -4f,
    )

    /** The top of [style]'s Montée on the master fader (dB): its form's, its own trim on it, never over the Drop's. */
    fun monteeTopDb(style: Style): Float =
        ((if (style.form == SongForm.Build) BUILD_TOP_DB + BUILD_PHONE_TRIM_DB else MONTEE_TOP_DB) + (MONTEE_TRIM_DB[style.id] ?: 0f)).coerceIn(-9f, 0f)

    /**
     * Final audit (report 10): a Build's Montée (bass music, its roll and its noise all over 300 Hz) this much
     * lower again, top and floor: on a phone, which plays nothing under 120 Hz, the Drop landed under its end.
     */
    const val BUILD_PHONE_TRIM_DB = -2f

    /**
     * S5b: how much deeper than its form's a style's Montée starts its climb (dB): the acid styles,
     * whose 303 plays on to the silent beat (it carries their build), and trance, whose chords do.
     */
    private val MONTEE_DIP_DB = mapOf("acid_house" to 1.5f, "acid_techno" to 1.5f, "trance" to 0.5f)

    /** The floor [style]'s Montée climbs from over its last 4 bars (dB on the master fader). */
    fun monteeFloorDb(style: Style): Float =
        monteeTopDb(style) + (if (style.form == SongForm.Build) BUILD_FROM_DB - BUILD_TOP_DB else MONTEE_FROM_DB - MONTEE_TOP_DB) - (MONTEE_DIP_DB[style.id] ?: 0f)

    /**
     * S6 son (M9): how much further under the Drop (or the Refrain) a style's Groove (or Couplet) sits,
     * its Intro with it (dB): the Drop only 0.5 to 1.5 LU over its Groove after the master chain
     * (median +1.8, 15 styles of 37 at +2), because the chain holds the Drop and leaves the Groove whole.
     * Measured on the S6 bench so that the Drop lands about +2.5 LU over its Groove, exported. S7
     * (the spread, [Project.MIX_V4]): synthwave 1.0 → 1.5 (its wide Couplet read louder: +2.1 → +1.75 LU);
     * son 8: synthwave 1.8 (its leads' touch: +2.14 → +1.95 LU).
     */
    private val GROOVE_TRIM_DB: Map<String, Float> = mapOf(
        "acid_house" to 2.0f, "acid_techno" to 2.5f, "acidcore" to 2.5f, "afro_house" to 0.5f, "afrobeats" to 1.0f,
        "boom_bap" to 0.5f, "deep_house" to 0.5f, "detroit" to 1.5f, "disco" to 0.5f, "drill" to 0.5f, "grime" to 0.5f,
        "house" to 2.5f, "industrial_techno" to 0.5f, "jersey_club" to 1.5f, "jungle" to 1.0f, "minimal" to 1.0f,
        "phonk" to 1.0f, "pop" to 0.5f, "reggaeton" to 1.0f, "retro_techno" to 1.0f, "synthwave" to 1.8f, "tech_house" to 2.0f,
        "techno" to 1.0f, "trance" to 0.5f, "trap" to 1.0f, "uk_garage" to 1.5f,
    )

    /** [style]'s Groove (and Couplet) on the master fader (dB). */
    fun grooveDb(style: Style): Float = GROOVE_DB - (GROOVE_TRIM_DB[style.id] ?: 0f)

    /**
     * [style]'s Intro on the master fader (dB): 0, or in club music as much lower as its Groove is (S6:
     * an Intro as loud as the Groove, kept so); a rap or pop Intro is already well under its Refrain.
     */
    fun introDb(style: Style): Float = when {
        // Final audit (lot son, report 11): a techno Intro at the Groove's own level. 2 dB over it, the
        // Intro's end came out louder than the Groove's first bars (techno −11.1 against −12.4 LUFS,
        // industrial −8.2 against −10.0; acidcore's Intro 0.3 LU under its Drop).
        style.family == "Techno" -> grooveDb(style)
        style.family in INTRO_WITH_GROOVE -> -(GROOVE_TRIM_DB[style.id] ?: 0f)
        else -> 0f
    }

    private val INTRO_WITH_GROOVE = setOf("Techno", "House")

    private fun sectionDb(stem: String, groove: Float = GROOVE_DB, intro: Float = 0f, breakTrim: Float = 0f): Float? = when (stem) {
        "Groove", "Couplet" -> groove
        // Never over the Groove's level (a Groove brought lower takes its Break down with it).
        "Break", "Pont" -> minOf(BREAK_DB, groove - 1f) - breakTrim
        "Intro" -> intro.takeIf { it != 0f }
        else -> null
    }

    /** A master fader position of [db] (the strips' law, 0.8 = 0 dB; 1 is 0 dB too, the master being capped at unity). */
    private fun masterFader(db: Float): Float = if (db >= 0f) 1f else fader(db)

    /**
     * The song's master volume curve with each section at its level ([GROOVE_DB], [BREAK_DB], the
     * Montées rising), the silent beats already on it (S2: the beat before each Drop) kept. One point
     * per beat. Unchanged without a song, or without room for the curve.
     */
    fun withSectionLevels(
        project: Project,
        build: Boolean = false,
        monteeTop: Float = if (build) BUILD_TOP_DB else MONTEE_TOP_DB,
        monteeFloor: Float = monteeTop + if (build) BUILD_FROM_DB - BUILD_TOP_DB else MONTEE_FROM_DB - MONTEE_TOP_DB,
        groove: Float = GROOVE_DB,
        intro: Float = 0f,
        breakTrim: Float = 0f,
    ): Project {
        fun sectionDb(stem: String) = sectionDb(stem, groove, intro, breakTrim)
        val p = project
        if (p.song.isEmpty()) return p
        val b = p.beatsPerBar.coerceAtLeast(1)
        val numbered = Regex("""^(.+) \d+$""")
        // Each part's owner section (a fill or a crash bar belongs to the section it is in) and its beats.
        data class Span(val stem: String, val start: Int, val end: Int)
        val spans = ArrayList<Span>()
        var beat = 0
        for (part in p.song) {
            val beats = p.partSteps(part) / 4
            val label = p.allScenes.getOrNull(part.scene)?.label.orEmpty()
            val drumBar = label == PartIntents.FILL_LABEL || label == DrumArrangement.CRASH_LABEL
            val stem = if (drumBar && spans.isNotEmpty()) spans.last().stem else numbered.matchEntire(label)?.groupValues?.get(1) ?: label
            val last = spans.lastOrNull()
            if (last != null && last.stem == stem) spans[spans.size - 1] = last.copy(end = beat + beats) else spans += Span(stem, beat, beat + beats)
            beat += beats
        }
        val total = beat
        if (total <= 1 || total > AutomationLane.MAX_SONG_BEATS) return p
        val rises = setOf(SongShape.MONTEE, SongShape.PRE_CHORUS)
        if (spans.none { sectionDb(it.stem) != null || it.stem in rises }) return p
        val db = FloatArray(total)
        val ramp = BooleanArray(total)
        for ((i, sp) in spans.withIndex()) {
            val level = sectionDb(sp.stem)
            if (sp.stem in rises) {
                // Under the Drop all along: a Montée eases down from the level of what came before (a Groove,
                // a Break) to its floor through its first bars, then climbs over its last 4 bars to its top
                // (the kick and the bass leave there: its roll and its riser rise); a Build song's Montée (its
                // own section, no kick nor bass) sits at its floor from its start; a Pré-refrain holds the
                // Couplet's level up to its top.
                val before = spans.getOrNull(i - 1)?.let { sectionDb(it.stem) } ?: 0f
                val (floor, peak) = when {
                    sp.stem != SongShape.MONTEE -> minOf(before, MONTEE_TOP_DB) to MONTEE_TOP_DB
                    build -> monteeFloor to monteeTop
                    else -> minOf(before, monteeFloor) to monteeTop
                }
                val start = if (build || sp.stem != SongShape.MONTEE) floor else minOf(before, peak)
                val from = maxOf(sp.start, sp.end - RISE_BARS * b)
                val top = sp.end - 2
                for (k in sp.start until sp.end) {
                    db[k] = when {
                        k < from -> start + (floor - start) * (k - sp.start).toFloat() / (from - sp.start).coerceAtLeast(1)
                        k >= top -> peak
                        else -> floor + (peak - floor) * (k - from).toFloat() / (top - from).coerceAtLeast(1)
                    }
                    ramp[k] = k > sp.start
                }
            } else {
                for (k in sp.start until sp.end) db[k] = level ?: 0f
            }
        }
        val target = AutoTarget.masterVolume()
        val old = p.automation.firstOrNull { it.target == target }
        val silent = old?.songValues?.withIndex()?.filter { it.value == 0f }?.map { it.index }?.toSet().orEmpty()
        var lane = AutomationLane(target, mode = AutomationLane.SONG, songOnly = true, songPerBeat = true)
        for (k in 0 until total) {
            lane = lane.withSongPoint(k, if (k in silent) 0f else masterFader(db[k]))
            // A step between two sections: a jump on the beat (no slide over the beat before it).
            val next = k + 1
            if (k in silent || (next < total && next !in silent && db[next] != db[k] && !ramp[next])) lane = lane.withSongJump(k, true)
        }
        val kept = p.automation.filterNot { it.target == target }
        if (kept.size + 1 > AutomationLane.MAX_LANES - (if (old == null) WIDE_LANES else 0)) return p
        return p.copy(automation = kept + lane)
    }

    /** [list] with [ins] put just before its first effect of type [before] (the rest moved one slot on), or in its first free slot. */
    private fun withBefore(list: List<Insert>, ins: Insert, before: Int): List<Insert> {
        if (list.none { it.type == 0 }) return list
        val at = list.indexOfFirst { it.type == before }
        if (at < 0) {
            val free = list.indexOfFirst { it.type == 0 }
            return list.toMutableList().also { it[free] = ins }
        }
        val out = list.toMutableList()
        out.add(at, ins)
        out.removeAt(out.indexOfLast { it.type == 0 })
        return out.take(list.size)
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

    /**
     * The Groove of club music holds its bass back: the bass's low-pass opens through the Groove
     * (dark at its start, open at the Drop: the Montée slides it there), so that the Drop lands
     * fuller (Black Sea, the techno research: 0.4 to 1 from the Groove to the Drop), unless the
     * style sweeps its bass itself.
     */
    fun grooveSweeps(style: Style, plan: StarterWizard.ScenePlan, partOfTrack: Map<Int, Part>): List<StarterWizard.Sweep> {
        if (plan.form.sung || plan.form == SongForm.Build || Part.Bass !in partOfTrack.values) return emptyList()
        if (1 !in plan.parts[Part.Bass].orEmpty() || style.sweeps.any { it.target == Part.Bass.id }) return emptyList()
        return listOf(StarterWizard.Sweep(Part.Bass.id, mapOf(1 to (GROOVE_BASS_FROM to GROOVE_BASS_TO)), resonance = 0.15f))
    }

    /** The Groove's bass low-pass, from its start to its end (Filtre knob: about 400 Hz to 2.5 kHz). */
    const val GROOVE_BASS_FROM = 0.4f
    const val GROOVE_BASS_TO = 0.7f

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
        val roll = if (form.sung) null else listOf(Role.Snare, Role.Clap).firstOrNull { has(it) }
        if (roll != null) set(roll, mapOf(last to Hit(0.45f), last + 1 to Hit(0.55f), last + 2 to Hit(0.7f, repeats = 2), last + 3 to Hit(0.85f, repeats = 2)))
        else if (has(Role.Hat)) set(Role.Hat, mapOf(last to Hit(0.5f), last + 1 to Hit(0.5f), last + 2 to Hit(0.6f, repeats = 2), last + 3 to Hit(0.7f, repeats = 3)))
        // The kick stops for the last beat (when it plays elsewhere in the phrase).
        if (has(Role.Kick) && out[Role.Kick.lane].hits.keys.any { it < last }) set(Role.Kick, emptyMap())
        if (has(Role.OpenHat)) out[Role.OpenHat.lane] = out[Role.OpenHat.lane].copy(hits = out[Role.OpenHat.lane].hits + (last + 2 to Hit(0.6f)))
        return out
    }
}
