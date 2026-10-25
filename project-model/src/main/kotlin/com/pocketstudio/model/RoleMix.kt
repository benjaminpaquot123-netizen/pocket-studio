package com.pocketstudio.model

import com.pocketstudio.model.StarterWizard.Part
import com.pocketstudio.model.StarterWizard.Style
import kotlin.math.ln
import kotlin.math.pow

/**
 * G0 (docs/PLAN-GUIDE.md): no track of the assistant's songs left untreated, each at its place.
 *
 * Every track gets its role's low cut (a flat parametric EQ whose « Coupe-bas » is set, or the
 * high-pass already on a bass over its sub), never above its lowest note: the chords at 150 Hz,
 * the lead at 120, the counter-melody and the vocal at 150, the riser at 150, the basses and the
 * 303 at 30 (the rumble under them). The exceptions: the « Sub » (a sine an octave under the
 * bass: its saturation is in its synth, [WideMix.SUB_DRIVE]) and the « Sub drop » (a sine falling
 * an octave, saturated here so that a phone hears it), and the kick (a pad, its own chain). The
 * tracks that ring in a room (all but the basses and the subs) send to the reverb at least
 * [MIN_SEND]. The pans: the kick, the basses and the subs centred, the lead a little right, the
 * counter-melody a little left (they answer each other), the vocal chops left of the lead, the
 * chords centred but wide (a widener when their sound is not spread already).
 *
 * Only for the songs the assistant makes ([WideMix.apply]); a project saved before keeps its mix.
 */
object RoleMix {
    /** What a track is in the song, for its chain and its place. */
    enum class Role(val lowCutHz: Float?, val pan: Float, val sends: Boolean) {
        Bass(30f, 0f, false),
        Sub(null, 0f, false),
        SubDrop(null, 0f, false),
        Acid(30f, 0f, true),
        Chords(150f, 0f, true),
        Lead(120f, 0.12f, true),
        Counter(150f, -0.15f, true),
        Vocal(150f, 0f, true),
        Riser(150f, 0f, true),
        Other(100f, 0f, true),
    }

    /** The places the assistant gives its tracks (a song's next steps do not take them for the user's own mix). */
    val PANS: Set<Float> get() = Role.entries.map { it.pan }.toSet() + CHOPS_PAN

    /** The reverb send a track that rings in a room gets at least. */
    const val MIN_SEND = 0.1f

    /** The vocal chops (an answer, a stab) sit left of the lead; a shout stays in the centre. */
    const val CHOPS_PAN = -0.12f

    /** A low cut this far under a track's lowest note at most (its fundamental untouched). */
    private const val UNDER_LOWEST = 0.7f

    /** The lowest a low cut goes (the parametric EQ's « Coupe-bas »: 20 × 25^v Hz). */
    private const val FLOOR_HZ = 22f

    /** The chords' widener when their sound is not spread already. */
    val CHORDS_WIDENER = Insert(EffectType.WIDENER.ordinal, listOf(0.65f, 0.35f))

    /** The sub drop's saturation: a little drive, open, at its level (its harmonics are what a phone plays). */
    val SUB_DROP_SATURATION = Insert(EffectType.SATURATION.ordinal, listOf(0.25f, EffectType.TONE_OPEN, 1f, 0.7f, 1f))

    /** The role of instrument track [id] in [p] ([partOfTrack]: the part each of the assistant's tracks plays). */
    fun roleOf(p: Project, id: Int, partOfTrack: Map<Int, Part>): Role {
        val name = p.track(id).name.let { DefaultNames.french(it) ?: it }
        return when {
            name == StarterWizard.SUB_TRACK -> Role.Sub
            name == StarterWizard.SUB_DROP_TRACK -> Role.SubDrop
            name == StarterWizard.BASS_SWITCH_TRACK -> Role.Bass
            name == StarterWizard.COUNTER_TRACK -> Role.Counter
            name == StarterWizard.VOCAL_TRACK -> Role.Vocal
            TrackRoles.isRiserName(name) -> Role.Riser
            else -> when (partOfTrack[id]) {
                Part.Bass -> Role.Bass
                Part.Pad -> Role.Chords
                Part.Lead -> Role.Lead
                null -> Role.Other
            }
        }
    }

    /** The low cut on [c] (Hz): a parametric EQ's « Coupe-bas » or a high-pass filter, null when none. */
    fun lowCutHz(c: Channel): Float? = c.inserts.filter { !it.bypass }.mapNotNull { ins ->
        when (ins.effect) {
            EffectType.PARAM_EQ -> ins.params.getOrNull(3)?.takeIf { it >= 0.01f }?.let { 20f * 25f.pow(it) }
            EffectType.FILTER -> if ((ins.params.getOrElse(2) { 0f } * 3).toInt() >= 2) 40f * 450f.pow(ins.params.getOrElse(0) { 0f }) else null
            else -> null
        }
    }.maxOrNull()

    /** The EQ's « Coupe-bas » value for [hz]. */
    fun lowCutValue(hz: Float): Float = (ln(hz.coerceIn(FLOOR_HZ, 500f) / 20f) / ln(25f)).coerceIn(0.01f, 1f)

    /** [wanted] (Hz), or lower so as never to cut track [id]'s lowest note (the leads' voice EQ). */
    fun cutUnderLowest(p: Project, id: Int, wanted: Float): Float =
        lowestPitch(p, id)?.let { minOf(wanted, hzOf(it) * UNDER_LOWEST) } ?: wanted

    /**
     * The lowest pitch track [id] plays anywhere in [p] (its synth's octave-down copy counted), or
     * null when it plays nothing. Known limit: the synth's sub oscillator ([SynthPatch.SUB]) and a
     * second oscillator tuned down ([SynthPatch.OSC2_OCTAVE]) are not counted, so such a layer an
     * octave under the notes is cut a little (the basses, cut at 30 Hz, are not concerned).
     */
    private fun lowestPitch(p: Project, id: Int): Int? {
        val clips = p.allScenes.mapNotNull { s -> if (id == 0) s.synthNotes else s.trackNotes[id] } + p.track(id).notes
        val low = clips.flatMap { it.notes }.minOfOrNull { it.pitch } ?: return null
        val t = p.track(id)
        val down = t.engine == 0 && t.instrument == null && t.synth.get(SynthPatch.OCTAVE_DOWN) > 0f
        return if (down) low - 12 else low
    }

    private fun hzOf(pitch: Int): Float = 440f * 2f.pow((pitch - 69) / 12f)

    /** [p] (made by [StarterWizard.build] for [style]) with each track's low cut, sends and pan. */
    fun apply(project: Project, style: Style, partOfTrack: Map<Int, Part>): Project {
        var p = project
        val channels = p.mixer.channels.toMutableList()
        fun treat(ch: Int, role: Role, lowest: Int?, pan: Float = role.pan) {
            if (ch !in channels.indices) return
            var c = channels[ch]
            val inserts = c.inserts.toMutableList()
            fun free() = inserts.indexOfFirst { it.type == 0 }
            role.lowCutHz?.let { wanted ->
                if (lowCutHz(c) != null) return@let
                val hz = lowest?.let { minOf(wanted, hzOf(it) * UNDER_LOWEST) } ?: wanted
                val v = lowCutValue(hz)
                // A flat EQ of its own already there (the bass's body): its low cut set; else one more, flat.
                val eq = inserts.indexOfFirst { it.effect == EffectType.PARAM_EQ && !it.bypass && it.params.getOrElse(3) { 0f } < 0.01f }
                if (eq >= 0) inserts[eq] = inserts[eq].copy(params = inserts[eq].params.toMutableList().also { while (it.size < 4) it += 0f; it[3] = v })
                else if (free() >= 0) inserts[free()] = Insert(EffectType.PARAM_EQ.ordinal, listOf(0.5f, 0.5f, 0.4f, v))
            }
            if (role == Role.SubDrop && inserts.none { it.type != 0 } && free() >= 0) inserts[free()] = SUB_DROP_SATURATION
            if (role == Role.Chords && inserts.none { it.effect == EffectType.WIDENER } && free() >= 0) {
                val t = p.instrumentTracks.firstOrNull { InstrumentTrack.mixerChannel(it.id) == ch }
                val spread = t != null && t.engine == 0 && t.instrument == null && t.synth.get(SynthPatch.WIDTH) > 0f
                if (!spread) inserts[free()] = CHORDS_WIDENER
            }
            c = c.copy(inserts = inserts)
            if (role.sends && c.sends.all { it <= 0f }) c = c.copy(sends = c.sends.mapIndexed { bus, v -> if (bus == 0) MIN_SEND else v })
            if (c.pan == 0f) c = c.copy(pan = pan)
            channels[ch] = c.sanitized()
        }
        for (t in p.instrumentTracks) {
            val role = roleOf(p, t.id, partOfTrack)
            val pan = if (role == Role.Vocal && style.vocal in setOf(StarterWizard.Vocal.Answer, StarterWizard.Vocal.Stab)) CHOPS_PAN else role.pan
            // The riser is noise: its note's pitch says nothing of its lows. A track that plays nothing
            // (a drums-only song's first track) keeps its strip as it is: a bass played there later is not thinned.
            val lowest = lowestPitch(p, t.id)
            if (lowest == null && role != Role.Riser) continue
            treat(InstrumentTrack.mixerChannel(t.id), role, if (role == Role.Riser) null else lowest, pan)
        }
        // The 303, when it plays: its cut under its lowest step too.
        val acids = listOf(p.acid) + p.allScenes.map { it.acid }
        val acidLowest = acids.flatMap { a -> a.steps.take(a.length.coerceAtLeast(0)).filter { it.gate }.map { a.root + it.note + 12 * it.octave } }.minOrNull()
        if (acidLowest != null) treat(AutoTarget.ACID_CHANNEL, Role.Acid, acidLowest)
        p = p.copy(mixer = p.mixer.copy(channels = channels))
        return p
    }
}
