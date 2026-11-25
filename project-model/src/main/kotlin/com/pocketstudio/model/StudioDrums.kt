package com.pocketstudio.model

import com.pocketstudio.model.StarterWizard.PartSound
import com.pocketstudio.model.StarterWizard.Role
import com.pocketstudio.model.StarterWizard.Style
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow

/**
 * The studio voices the assistant puts on its pads (plan « son pro », chantier S3; the engine's
 * StudioVoices.h): the 808 that plays the bass line in trap, drill, phonk and jersey club (tuned
 * to the key, each hit on the chord's note, sliding 60 to 200 ms where the line slides), the short
 * studio kick tuned to the key (45 to 65 Hz) in house and techno, its hard variant in hard techno,
 * gabber and acidcore, the rumble under the techno kick.
 */
object StudioDrums {
    /**
     * Styles whose bass is an 808 (docs/hits/hiphop.md: « la 808 n'est pas une 808 »); reggaeton's
     * dembow since « des basses qui tapent » (docs/hits/basses-qui-tapent.md § 9).
     */
    val BASS_808 = setOf("trap", "drill", "phonk", "jersey_club", "reggaeton")

    /** Styles whose kick is the hard (gabber) one: soft-clipped, a deeper sweep. */
    val HARD_KICK = setOf("hard_techno", "gabber", "acidcore", "industrial_techno")

    /** Soft kicks without a click (docs/hits/house.md: amapiano, deep, afro). */
    private val SOFT_KICK = setOf("deep_house", "afro_house", "amapiano")

    /** The 808's pad: the second kick's (Role.Bass808). */
    val LANE_808: Int get() = Role.Bass808.lane

    /**
     * The 808 plays [style]'s bass when the bass's sound is the style's own synth sound (its « Basse
     * sub »); a sound chosen in a pack, or another of the library, keeps its track.
     */
    fun bassOn808(style: Style, sound: PartSound?): Boolean =
        style.id in BASS_808 && sound != null && sound.name == style.melody.bass && sound.sampled == null && sound.plaits == null

    fun hz(midi: Int): Double = 440.0 * 2.0.pow((midi - 69) / 12.0)

    /** The kick's note: the key's root between 43 and 66 Hz, else its fifth (C#, D, D#, E). */
    fun kickHz(root: Int): Float {
        fun inBand(pc: Int) = (1..4).map { hz(12 * it + Math.floorMod(pc, 12)) }.firstOrNull { it in 43.0..66.0 }
        return (inBand(root) ?: inBand(root + 7) ?: 52.0).toFloat()
    }

    /** The 808's tuning: the key's root at the bottom of the bass (24 + root: 32.7 to 61.7 Hz). */
    fun tune808(root: Int): Int = 24 + Math.floorMod(root, 12)

    /** The 808's slide (ms) where [style]'s line slides, 0 for a style whose 808 never glides. */
    fun glideMs(style: Style): Int = when {
        !style.melody.bassGlide -> 0
        style.id == "drill" -> 180
        style.id == "phonk" -> 110
        else -> 150
    }

    /**
     * [style]'s 808 in [harmony]'s key: long and round in trap, longer in drill, dirtier in phonk,
     * short in jersey club, a dembow's in reggaeton. Since « des basses qui tapent »
     * (docs/hits/basses-qui-tapent.md § 5.3, P2) the 808's version 1, the one heard on a phone: it
     * drops from +18 to +24 semitones over 20 to 35 ms (the « boum »), its drive squares it off (0.7;
     * phonk 0.9 with its second clip, reggaeton 0.5), « tone » is its 4th to 8th harmonics. A project
     * saved before keeps its 808 (no version: version 0).
     */
    fun voice808(style: Style, harmony: Harmony): PadVoice {
        data class V(val decay: Float, val drive: Float, val click: Float, val tone: Float, val drop: Float, val dropMs: Float)
        val v = when (style.id) {
            "drill" -> V(1.5f, 0.7f, 0.4f, 0.5f, 24f, 35f)
            "phonk" -> V(1.1f, 0.9f, 0.5f, 0.55f, 24f, 25f)
            "jersey_club" -> V(0.6f, 0.6f, 0.5f, 0.5f, 18f, 20f)
            "reggaeton" -> V(0.9f, 0.5f, 0.45f, 0.5f, 18f, 30f)
            else -> V(1.3f, 0.7f, 0.5f, 0.5f, 24f, 25f)
        }
        return PadVoice(
            PadVoice.BASS_808,
            mapOf(
                PadVoice.TUNE to tune808(harmony.root).toFloat(), PadVoice.DECAY to v.decay, PadVoice.DRIVE to v.drive,
                PadVoice.CLICK to v.click, PadVoice.TONE to v.tone, PadVoice.GLIDE to glideMs(style) / 1000f,
                PadVoice.VERSION to 1f, PadVoice.PITCH_DROP to v.drop, PadVoice.DROP_TIME to v.dropMs / 1000f,
            ),
        ).sanitized()
    }

    /**
     * A style family's kick (Benjamin, 2026-11-25, « le kick tape pas assez », version B): the note it
     * aims at ([hz], then tuned to the key's root or fifth nearest to it), the sweep's start over the
     * note ([ratio]: 4 to 6, acidcore 2.8, lofi 3) and its time constant ([sweepMs], 25 to 45 ms: the
     * kick lands on its note 70 to 145 ms after the hit), the saturation ([drive]: clean house,
     * trance and drum & bass, dirty disco, big room and hard), the length ([decay], s, -40 dB) and
     * the click. The engine reads them as PITCH_DROP (semitones) and DROP_TIME (s).
     */
    data class KickFamily(
        val hz: Float, val ratio: Float, val sweepMs: Float, val drive: Float, val decay: Float,
        val click: Float = 0.5f, val hard: Boolean = false,
    ) {
        val dropSt: Float get() = (12.0 * ln(ratio.toDouble()) / ln(2.0)).toFloat()
    }

    /** The table, by style id (Benjamin's « table style → kick », 2026-11-25); [KICK_DEFAULT] elsewhere. */
    val KICK_FAMILIES: Map<String, KickFamily> = buildMap {
        // House: round, a little saturation, a fast sweep.
        for (id in listOf("house", "deep_house", "afro_house", "amapiano")) {
            put(id, KickFamily(48f, 4f, 27f, 0.25f, 0.3f, click = if (id in SOFT_KICK) 0.25f else 0.5f))
        }
        put("tech_house", KickFamily(47f, 5f, 35f, 0.4f, 0.3f))
        put("acid_house", KickFamily(50f, 4f, 30f, 0.1f, 0.25f))
        put("disco", KickFamily(56f, 4f, 25f, 0.9f, 0.18f))
        put("jersey_club", KickFamily(38f, 5f, 25f, 0.6f, 0.12f))
        for (id in listOf("uk_garage", "speed_garage")) put(id, KickFamily(51f, 4f, 30f, 0.3f, 0.25f, click = 0.35f))
        // Techno: version B, 49 Hz over a bass on another note.
        for (id in listOf("techno", "detroit", "retro_techno", "acid_techno")) put(id, KickFamily(49f, 4.5f, 35f, 0.6f, 0.25f))
        put("minimal", KickFamily(49f, 4f, 25f, 0.3f, 0.16f))
        for (id in listOf("hard_techno", "industrial_techno")) put(id, KickFamily(49f, 5f, 40f, 0.95f, 0.3f, click = 0.4f, hard = true))
        put("gabber", KickFamily(48f, 6f, 45f, 1f, 0.29f, click = 0.4f, hard = true))
        put("acidcore", KickFamily(69f, 2.8f, 35f, 0.9f, 0.22f, click = 0.4f, hard = true))
        put("trance", KickFamily(50f, 5f, 45f, 0.15f, 0.28f))
        // Drum & bass: short and clean ([dnbKick]'s length and click).
        for (id in listOf("dnb", "jump_up", "jungle", "neurofunk")) {
            put(id, KickFamily(46f, 4.5f, 28f, if (id == "neurofunk") 0.3f else 0.2f, DNB_KICK_DECAY, DNB_KICK_CLICK))
        }
        put("liquid_dnb", KickFamily(50f, 4f, 30f, 0.15f, 0.2f))
        for (id in listOf("dubstep", "riddim", "grime")) put(id, KickFamily(46f, 4.5f, 32f, 0.4f, 0.25f))
        // The 808 styles: a short kick, the 808 holds the low end.
        for (id in listOf("trap", "drill", "phonk")) put(id, KickFamily(50f, 4.5f, 25f, 0.5f, KICK_808_DECAY))
        for (id in listOf("reggaeton", "afrobeats")) put(id, KickFamily(49f, 4.5f, 35f, 0.5f, 0.35f))
        for (id in listOf("lofi", "boom_bap")) put(id, KickFamily(79f, 3f, 25f, 0.7f, 0.15f, click = 0.4f))
        for (id in listOf("synthwave", "pop")) put(id, KickFamily(60f, 4f, 25f, 0.4f, 0.22f))
    }

    /** A style missing from [KICK_FAMILIES]: the engine's own studio kick (x4.5 over 35 ms). */
    val KICK_DEFAULT = KickFamily(52f, 4.5f, 35f, 0.6f, 0.25f)

    fun kickFamily(style: Style): KickFamily = KICK_FAMILIES[style.id] ?: KICK_DEFAULT

    /** The key's root or fifth nearest to [target] Hz (any octave): the kick in the key, near its family's note. */
    fun kickNear(root: Int, target: Float): Float =
        listOf(root, root + 7).flatMap { pc -> (0..5).map { hz(12 * it + Math.floorMod(pc, 12)) } }
            .minBy { abs(ln(it / target)) }.toFloat()

    /**
     * [style]'s kick voice in [harmony]'s key: its family's ([KICK_FAMILIES]). The 808 styles keep
     * the 808's key (final audit, report 12: [kickHz], the root else the fifth, 43 to 66 Hz).
     */
    fun kickVoice(style: Style, harmony: Harmony): PadVoice? {
        val f = kickFamily(style)
        val tune = if (style.id in BASS_808) kickHz(harmony.root) else kickNear(harmony.root, f.hz)
        return PadVoice(
            if (f.hard || style.id in HARD_KICK) PadVoice.KICK_HARD else PadVoice.KICK_STUDIO,
            mapOf(
                PadVoice.TUNE to tune, PadVoice.DECAY to f.decay, PadVoice.DRIVE to f.drive, PadVoice.CLICK to f.click,
                PadVoice.PITCH_DROP to f.dropSt, PadVoice.DROP_TIME to f.sweepMs / 1000f,
            ),
        ).sanitized()
    }

    /** The studio kick's decay over an 808 (s; its own 0.25 a little shorter: the 808 is the low end that rings). */
    const val KICK_808_DECAY = 0.2f

    /**
     * Final audit (report 12): the 808's pad ducked under the kick (a pump on its strip, keyed by the kick's
     * sound since S5): depth, release (about 70 ms). The Musique bus takes no pad; this is its low end's place.
     */
    val PUMP_808: Insert get() = Insert(EffectType.PUMP.ordinal, listOf(0.5f, 0.15f, 0f)).sanitized()

    /** The rumble under [style]'s kick: tuned with it, rolling for about a beat, a little dirty. */
    fun rumbleVoice(style: Style, harmony: Harmony): PadVoice = PadVoice(
        PadVoice.RUMBLE,
        mapOf(
            PadVoice.TUNE to kickHz(harmony.root), PadVoice.DECAY to (60f / style.bpm.toFloat() * 1.2f).coerceIn(0.3f, 0.9f),
            PadVoice.DRIVE to 0.4f, PadVoice.TONE to 0.35f,
        ),
    ).sanitized()

    /** The voice [style] puts on [role]'s pad in [harmony]'s key, or null (its kit voice). */
    fun voiceFor(style: Style, role: Role, harmony: Harmony): PadVoice? = when (role) {
        Role.Kick -> kickVoice(style, harmony)
        Role.Rumble -> rumbleVoice(style, harmony)
        else -> null
    }

    /**
     * The 808's hits from the bass line [clip] over [steps] drum steps: one hit where each note
     * starts, on the note's pitch from the voice's [tune] (folded into ±2 octaves), sliding for
     * [glideMs] where the note starts under the one before (the line's '/'); [level] scales the
     * strengths (the Drop's +1.5 dB is the bass's lift). A note off the sixteenths (« Motifs de
     * basse »'s triplets) keeps its place: its step's micro-timing ([Hit.offset], at [bpm]) moves the
     * hit there, up to [Hit.MAX_OFFSET_MS].
     */
    fun hits808(clip: NoteClip, steps: Int, tune: Int, glideMs: Int, level: Float = 1f, bpm: Float = 0f): Map<Int, Hit> {
        val t = NoteClip.TICKS_PER_STEP
        val out = LinkedHashMap<Int, Hit>()
        var prev: Note? = null
        for (n in clip.notes.sortedBy { it.start }) {
            val step = Math.round(n.start.toFloat() / t)
            if (step !in 0 until steps || step in out) continue
            var pitch = n.pitch - tune
            while (pitch > Hit.MAX_PITCH) pitch -= 12
            while (pitch < -Hit.MAX_PITCH) pitch += 12
            val slides = glideMs > 0 && prev != null && n.start < prev.end && abs(n.pitch - prev.pitch) > 0
            // Ticks off the step, in ms (a step is a sixteenth: a quarter of a beat).
            val off = n.start - step * t
            val offsetMs = if (bpm > 0f && off != 0) off.toFloat() / (4 * t) * 60000f / bpm else 0f
            out[step] = Hit(
                (n.velocity / NOTE_VELOCITY * level).coerceIn(DrumPattern.MIN_VELOCITY, 1f), pitch = pitch,
                glide = if (slides) glideMs else 0, offset = offsetMs.coerceIn(-Hit.MAX_OFFSET_MS, Hit.MAX_OFFSET_MS),
            ).sanitized()
            prev = n
        }
        return out
    }

    /** A bass note's usual strength: it plays the 808 at full. */
    private const val NOTE_VELOCITY = 0.85f

    /** The 808's strength outside the Drop (the Refrain): the bass's lift of [StarterWizard.BASS_LIFT_DB] there. */
    val OUTSIDE_DROP: Float get() = 10f.pow(-StarterWizard.BASS_LIFT_DB / 20f)

    /**
     * The 808's pad fader, in dB (its voice at -12 LUFS played alone, 4 hits a bar at 128 BPM, like
     * the kit's second kick it replaces; a held bass note sounds longer: a little under the kick).
     * The bench (docs/son/banc/apres-S3-generateur.md) found it at the old bass's loudness in trap,
     * drill and phonk; jersey club's short bass was 4.5 LU softer: its 808 doubles the kick, lower.
     */
    fun level808Db(style: Style): Float = if (style.id == "jersey_club") -5f else -1f

    /** Lot 2: the kits built in that put [style]'s 808 on its pad, by style id (« Kit ▾ » offers them, nothing to download). */
    val KITS_808 = listOf("trap", "drill")

    /**
     * Lot 2 (audit 35, amateur 2): « Kit ▾ » › Trap or Drill without a pack: the studio kit on the
     * 16 pads (their own sounds off), [styleId]'s 808 in the project's key on its pad (the second
     * kick's) at its level; the other studio voices (a rumble) leave. Null: no such style.
     */
    fun with808Kit(project: Project, styleId: String): Project? {
        val style = StarterWizard.style(styleId) ?: return null
        val harmony = project.harmony.takeIf { it.scale != Scale.Off } ?: Harmony(style.melody.root)
        val ch = Mixer.padChannel(LANE_808)
        val channels = project.mixer.channels.toMutableList()
        if (ch in channels.indices) channels[ch] = channels[ch].copy(volume = WizardMix.fader(level808Db(style))).sanitized()
        return project.copy(
            kitVersion = Project.KIT_V3,
            drums = project.drums.copy(lanes = project.drums.lanes.map { it.copy(sample = null) }),
            padVoices = mapOf(LANE_808 to voice808(style, harmony)),
            mixer = project.mixer.copy(channels = channels),
        )
    }

    /**
     * The drum & bass kick (tutorial lot 3, owner: « ça doit sonner jump up dès le chapitre 2 »): the
     * studio kick tuned to the key, shorter (0.18 s) and with more click than the kit's own (0.25 s):
     * measured through the engine (TutoDefaultSoundBench), it is down 40 dB 185 ms after its peak
     * (the kit's 290 ms) and keeps its low end (64 %), where the hard kick loses it (39 %).
     */
    const val DNB_KICK_DECAY = 0.18f
    const val DNB_KICK_CLICK = 0.7f

    fun dnbKick(root: Int): PadVoice = PadVoice(
        PadVoice.KICK_STUDIO,
        mapOf(
            PadVoice.TUNE to kickHz(root), PadVoice.DECAY to DNB_KICK_DECAY, PadVoice.CLICK to DNB_KICK_CLICK,
            PadVoice.DRIVE to 0.2f, PadVoice.PITCH_DROP to KickFamily(46f, 4.5f, 28f, 0.2f, DNB_KICK_DECAY).dropSt, PadVoice.DROP_TIME to 0.028f,
        ),
    ).sanitized()

    /** [voice] is the drum & bass kick ([dnbKick], in any key): « Kit ▾ » then says « Drum & bass ». */
    fun isDnbKick(voice: PadVoice?): Boolean =
        voice != null && voice.kind == PadVoice.KICK_STUDIO && voice.param(PadVoice.DECAY) == DNB_KICK_DECAY && voice.param(PadVoice.CLICK) == DNB_KICK_CLICK

    /**
     * « Kit ▾ » › Drum & bass, built in (nothing to download), and the jump up tutorial's kit from its
     * start: the studio kit (kit v3) on the 16 pads, their own sounds off, the drum & bass kick
     * ([dnbKick], in the project's key, else F) on the kick's pad, and the « Punch » preset on the
     * kick's and the snare's strips when they have no effect of their own (a tight snare that cracks).
     */
    fun withDnbKit(project: Project): Project {
        val root = project.harmony.takeIf { it.scale != Scale.Off }?.root ?: StarterWizard.style("jump_up")?.melody?.root ?: 5
        val punch = FxPresets.byId("punch")
        val strips = setOf(Mixer.padChannel(Role.Kick.lane), Mixer.padChannel(Role.Snare.lane))
        return project.copy(
            kitVersion = Project.KIT_V3,
            drums = project.drums.copy(lanes = project.drums.lanes.map { it.copy(sample = null) }),
            padVoices = project.padVoices.filterValues { !it.isKick && it.kind != PadVoice.BASS_808 } + (Role.Kick.lane to dnbKick(root)),
            mixer = project.mixer.copy(channels = project.mixer.channels.mapIndexed { i, c ->
                if (i in strips && punch != null && c.inserts.all { it.effect == EffectType.NONE }) c.withFx(punch).sanitized() else c
            }),
        )
    }

    /** The rumble's pad fader, in dB: under the kick, it rolls between the hits (its voice alone at -17.5 LUFS). */
    const val RUMBLE_DB = -2f

    /**
     * The rumble's strip when its pad plays the rumble voice (the voice is the kick's tail already:
     * no reverb), ducked by the kick and in mono.
     */
    val RUMBLE_STRIP: List<Insert> get() = listOf(
        Insert(EffectType.PUMP.ordinal, listOf(0.9f, 0.35f, 0f)),
        Insert(EffectType.WIDENER.ordinal, listOf(0f, 0f)),
    ).map { it.sanitized() }
}
