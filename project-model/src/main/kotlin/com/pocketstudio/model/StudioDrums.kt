package com.pocketstudio.model

import com.pocketstudio.model.StarterWizard.PartSound
import com.pocketstudio.model.StarterWizard.Role
import com.pocketstudio.model.StarterWizard.Style
import kotlin.math.abs
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

    /** Families whose kick is the studio kick tuned to the key. */
    private val TUNED_FAMILIES = setOf("House", "Techno")

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

    /** [style]'s kick voice in [harmony]'s key, or null: the kit's own (kit v3: the studio kick at its own tuning). */
    fun kickVoice(style: Style, harmony: Harmony): PadVoice? {
        val tune = kickHz(harmony.root)
        return when {
            style.id in HARD_KICK -> PadVoice(
                PadVoice.KICK_HARD,
                mapOf(PadVoice.TUNE to tune, PadVoice.DRIVE to if (style.id == "gabber" || style.id == "acidcore") 0.9f else 0.7f),
            )
            style.family in TUNED_FAMILIES -> PadVoice(
                PadVoice.KICK_STUDIO,
                if (style.id in SOFT_KICK) mapOf(PadVoice.TUNE to tune, PadVoice.CLICK to 0.25f) else mapOf(PadVoice.TUNE to tune),
            )
            else -> null
        }?.sanitized()
    }

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
