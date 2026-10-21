package com.pocketstudio.model

import kotlin.random.Random

/**
 * The assistant's small 303 editor: the style's line in the song's key, a touch on a step to go
 * up the scale (a rest after the top note), a long touch for its accent and slide, a new line from
 * the Acide tab's generator kept on the scale, and four quick sounds.
 */
object WizardAcid {
    /** The quick 303 sounds: their label (French, translated at display) and the library sound each one takes. */
    val SOUNDS: List<Pair<String, String>> = listOf(
        "Classique" to "Acide de base",
        "Saturée" to "Carré saturé",
        "Ronde" to "Basse ronde",
        "Criarde" to "Squelchy",
    )

    /** Lengths the assistant offers, in bars. */
    val BARS = listOf(1, 2)

    /** The sound of quick choice [label], keeping [keep]'s tuning and level (null: unknown label). */
    fun sound(label: String, keep: AcidSound = AcidSound()): AcidSound? =
        SOUNDS.firstOrNull { it.first == label }?.let { (_, name) -> SoundLibrary.ACID.firstOrNull { it.name == name }?.sound }
            ?.copy(tuning = keep.tuning, level = keep.level)

    /** [style]'s 303 line in [mood]'s key, with the style's 303 sound (null: the style has none). */
    fun recipe(style: StarterWizard.Style, mood: StarterWizard.Mood): AcidPattern? = style.melody.acid?.let { a ->
        acidLine(StarterWizard.harmonyOf(style, mood), a.pattern, a.accents, a.slides).let { it.copy(sound = a.soundFrom(it.sound)) }
    }

    /** Whether [pattern] plays a note at all (a line of rests is no line). */
    fun plays(pattern: AcidPattern): Boolean = pattern.steps.take(pattern.length).any { it.gate }

    /** The notes a touch goes through, in semitones above the line's root: the scale over an octave, and the octave. */
    fun offsets(harmony: Harmony): List<Int> = (harmony.scale.intervals.filter { it in 0..11 } + 12).distinct().sorted()

    /** A touch on [step]: the next note up the scale, a rest after the top one, the root after a rest. */
    fun nextNote(step: AcidStep, harmony: Harmony): AcidStep {
        if (!step.gate) return AcidStep(gate = true)
        val current = step.note + 12 * step.octave
        val next = offsets(harmony).firstOrNull { it > current } ?: return AcidStep()
        return step.copy(note = next % 12, octave = next / 12, gate = true)
    }

    /** A long touch on [step]: plain → accent → slide → both → plain (a rest becomes an accented root). */
    fun nextMarks(step: AcidStep): AcidStep = when {
        !step.gate -> AcidStep(gate = true, accent = true)
        !step.accent && !step.slide -> step.copy(accent = true)
        step.accent && !step.slide -> step.copy(accent = false, slide = true)
        !step.accent -> step.copy(accent = true)
        else -> step.copy(accent = false, slide = false)
    }

    /**
     * [pattern] over [bars] bars: a new second bar starts as a copy of the first; one written
     * before (two bars, back to one, then two again) comes back as it was.
     */
    fun withBars(pattern: AcidPattern, bars: Int): AcidPattern {
        val n = AcidPattern.STEPS_PER_PAGE
        val length = bars.coerceIn(1, AcidPattern.LENGTHS_IN_BARS.max()) * n
        val secondEmpty = pattern.steps.subList(n, 2 * n).none { it.gate }
        val grown = if (length > pattern.length && pattern.length == n && secondEmpty) pattern.copyPage(0, 1) else pattern
        return grown.copy(length = length).sanitized()
    }

    /**
     * [pattern]'s notes brought onto [harmony]'s scale (the mood changed after the line was
     * written): each played note goes to the nearest note of the scale, accents, slides and
     * octaves kept; rests stay rests.
     */
    fun inScale(pattern: AcidPattern, harmony: Harmony): AcidPattern = pattern.copy(
        steps = pattern.steps.map { s ->
            if (!s.gate) s else {
                val offset = s.note + 12 * s.octave
                val snapped = harmony.snap(harmony.root + offset) - harmony.root
                val octave = Math.floorDiv(snapped, 12)
                // At the edges of the 303's range, the scale note just inside it.
                if (octave in -1..2) s.copy(note = Math.floorMod(snapped, 12), octave = octave)
                else {
                    val inside = (if (octave < -1) (offset..offset + 6) else (offset downTo offset - 6))
                        .firstOrNull { harmony.inScale(harmony.root + it) } ?: offset
                    s.copy(note = Math.floorMod(inside, 12), octave = Math.floorDiv(inside, 12))
                }
            }
        },
    ).sanitized()

    /**
     * A new line from the Acide tab's generator ([AcidGenerator], at [pattern]'s density and
     * length, with the style's [feel]), its note set brought onto [harmony]'s scale so that it fits the song.
     */
    fun generate(pattern: AcidPattern, harmony: Harmony, random: Random = Random.Default, feel: AcidGenerator.Feel = AcidGenerator.Feel.CLASSIC): AcidPattern {
        val set = AcidGenerator.NOTE_SETS.random(random).map { o -> harmony.snap(harmony.root + o) - harmony.root }
        // « 1 mesure » chosen: the line stays one bar (a two-bar phrase would make it two).
        val asked = if (pattern.length <= AcidPattern.STEPS_PER_PAGE) feel.copy(phraseBars = 1) else feel
        return AcidGenerator(random).generate(pattern.copy(locked = emptySet()), set, asked).sanitized()
    }
}
