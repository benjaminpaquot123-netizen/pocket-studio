package com.pocketstudio.model

import com.pocketstudio.model.StarterWizard.Role
import com.pocketstudio.model.StarterWizard.Style
import com.pocketstudio.model.StarterWizard.StyleFx

/**
 * Tour 5 « sons-essai » (docs/sons/PLAN-SONS-MODERNES.md § 6): the same songs, other sounds, to compare. Jump up, tech
 * house and techno take modern drums (the trial packs `essai_<style>`, made by scripts/packs/trial_drums.py, installed on
 * the bench only), sounds of the library no recipe used, and the OTT on their pads and leads. Their lines, notes, form
 * and mix are their recipe's.
 *
 * Hidden and off: only a JVM started with the property [PROPERTY] = 1 (the bench, the tests) turns it on, read at each
 * [StarterWizard.style] call; no screen, setting or file of the app does. Off, [StarterWizard.style] gives the recipe
 * itself (SoundTrialTest: every song the same to the byte).
 */
object SoundTrial {
    const val PROPERTY = "ps.sons.essai"

    /** The styles of the trial. */
    val STYLES = listOf("jump_up", "tech_house", "techno")

    /** The OTT on a trial pad or lead: depth, speed, upward, downward (the master's in bass music: 0.25 deep). */
    val VOICE_OTT = listOf(0.4f, 0.5f, 0.6f, 0.5f)

    fun on(): Boolean = System.getProperty(PROPERTY) == "1"

    /** The trial pack of [styleId]. */
    fun packOf(styleId: String) = "essai_$styleId"

    /** [style] as the trial plays it when [on], else [style] itself. */
    fun styleOf(style: Style): Style = if (on() && style.id in STYLES) variant(style) else style

    private fun variant(style: Style): Style {
        val drums = DRUMS.getValue(style.id)
        val pack = packOf(style.id)
        val sounds = SOUNDS.getValue(style.id)
        return style.copy(
            // Each line the trial pack plays takes it first; a line that named a kit voice would keep the kit
            // (soundCategories): its voice goes. Tech house's shaker keeps its own (the pack's percussions are one kind).
            lines = style.lines.map { if (it.role in drums) it.copy(voice = null) else it },
            kit = drums.associateWith { pack },
            melody = style.melody.copy(
                bass = sounds.bass ?: style.melody.bass,
                pad = sounds.pad ?: style.melody.pad,
                lead = sounds.lead ?: style.melody.lead,
            ),
            effects = style.effects + listOf(StyleFx("pad", insert = "OTT", params = VOICE_OTT), StyleFx("lead", insert = "OTT", params = VOICE_OTT)) +
                SECOND + SECOND_OWN[style.id].orEmpty(),
        )
    }

    /**
     * Trial n° 2 (Benjamin on n° 1: « tu rajoutes juste des cymbales » ; measured: only the drums' highs moved). What a
     * listener notices, with the engine's own effects and nothing the mix does taken away:
     * - the bass: a parallel saturation (driven hard, mostly wet, its loudness held), then the 3-band EQ after it: lows
     *   +5 dB (100 Hz shelf: its sub back), mids +12 dB (1 kHz: its harmonics where a phone plays them), highs -9 dB
     *   (8 kHz shelf: the fizz). The 3-band EQ and not a parametric one: the mix takes a parametric EQ without a low cut
     *   for the bass's own and moves it in front of the exciter (review G1). Two inserts only: a full strip drops the
     *   mix's own (jump up's 120 Hz body). The kick already ducks the music through the mix's bus: no pump added;
     * - leads and pads: more plate reverb and the ping-pong delay sent; no widener of the trial's (the mix widens them
     *   already, and a widener there cost the leads their chorus: review G2).
     */
    private val SECOND = listOf(
        StyleFx("bass", insert = "SATURATION", params = listOf(0.95f, 1f, 0.85f, 0.7f, 1f)),
        StyleFx("bass", insert = "EQ", params = listOf(0.5f + 5f / 24f, 0.5f + 12f / 24f, 0.5f - 9f / 24f)),
        StyleFx("lead", reverb = 0.3f, delay = 0.4f),
        StyleFx("pad", reverb = 0.45f),
    )

    /** Tech house's voice (the darkest of all: -57 dB over 10 kHz) lifted: mids +9 dB (1 kHz), highs +6 dB (8 kHz shelf). */
    private val SECOND_OWN = mapOf(
        "tech_house" to listOf(StyleFx("lead", insert = "EQ", params = listOf(0.5f, 0.5f + 9f / 24f, 0.5f + 6f / 24f))),
    )

    /** The lines the trial pack plays, per style (techno keeps its tuned studio rumble). */
    private val DRUMS = mapOf(
        "jump_up" to setOf(Role.Kick, Role.Snare, Role.Clap, Role.Hat, Role.Perc),
        "tech_house" to setOf(Role.Kick, Role.Clap, Role.Hat, Role.OpenHat, Role.Perc, Role.Ride),
        "techno" to setOf(Role.Kick, Role.OpenHat, Role.Clap, Role.Hat, Role.Perc, Role.Ride),
    )

    private class Sounds(val bass: String? = null, val pad: String? = null, val lead: String? = null)

    /**
     * The library's sounds no recipe played (docs/sons/PLAN-SONS-MODERNES.md § 5): jump up's hoover (1991) gives way to
     * the supersaw and its two-saw pad to the future bass chords. Techno keeps its square pluck: the future pluck measured
     * darker on its pad (tour-5-sons-essai-rapport.md). Null: the recipe's own sound (with the OTT).
     */
    private val SOUNDS = mapOf(
        "jump_up" to Sounds(pad = "Accords future bass", lead = "Supersaw"),
        "tech_house" to Sounds(),
        "techno" to Sounds(),
    )
}
