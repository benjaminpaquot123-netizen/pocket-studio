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
            effects = style.effects + listOf(StyleFx("pad", insert = "OTT", params = VOICE_OTT), StyleFx("lead", insert = "OTT", params = VOICE_OTT)),
        )
    }

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
