package com.pocketstudio.model

import kotlinx.serialization.Serializable
import kotlin.random.Random

/** One step of the acid bass line. */
@Serializable
data class AcidStep(
    val note: Int = 0,      // 0..12 semitones above the root
    val octave: Int = 0,    // -1..+2
    val accent: Boolean = false,
    val slide: Boolean = false,
    val gate: Boolean = false,
) {
    fun sanitized() = copy(note = note.coerceIn(0, 12), octave = octave.coerceIn(-1, 2))

    /** Same bit layout as the engine's AcidStep::pack(). */
    fun pack(): Int = (note and 0xF) or ((octave.coerceIn(-1, 2) + 1) shl 4) or
        (if (accent) 1 shl 6 else 0) or (if (slide) 1 shl 7 else 0) or (if (gate) 1 shl 8 else 0)
}

/** Sound settings, each 0..1 (the engine maps them to musical ranges). */
@Serializable
data class AcidSound(
    val cutoff: Float = 0.35f,
    val resonance: Float = 0.7f,
    val envMod: Float = 0.5f,
    val decay: Float = 0.3f,
    val accent: Float = 0.6f,
    val tuning: Float = 0.5f,
    val waveform: Float = 0f,
    val drive: Float = 0.3f,
    val level: Float = DEFAULT_LEVEL,
) {
    companion object {
        /** « Acide de base » at about -16 LUFS alone (see [SoundLibrary.ACID]). */
        const val DEFAULT_LEVEL = 0.48f
        /** The level every 303 had before its calibration (phase 30). */
        const val OLD_LEVEL = 0.7f
    }

    /** In engine parameter order (AcidTrack::Param). */
    fun values(): List<Float> = listOf(cutoff, resonance, envMod, decay, accent, tuning, waveform, drive, level)

    fun with(index: Int, v: Float): AcidSound {
        val x = v.coerceIn(0f, 1f)
        return when (index) {
            0 -> copy(cutoff = x); 1 -> copy(resonance = x); 2 -> copy(envMod = x); 3 -> copy(decay = x)
            4 -> copy(accent = x); 5 -> copy(tuning = x); 6 -> copy(waveform = x); 7 -> copy(drive = x)
            8 -> copy(level = x); else -> this
        }
    }

    fun sanitized() = AcidSound().let { d ->
        val defaults = d.values()
        values().foldIndexed(this) { i, acc, v -> acc.with(i, v.finite(defaults[i])) }
    }
}

@Serializable
data class AcidPattern(
    val length: Int = 16,
    val root: Int = 36,  // MIDI note, C2
    val steps: List<AcidStep> = List(MAX_STEPS) { AcidStep() },
    val sound: AcidSound = AcidSound(),
    /** Steps the generator must not touch. */
    val locked: Set<Int> = emptySet(),
    /** Generator: 0 = sparse, 1 = busy. */
    val density: Float = 0.8f,
) {
    fun sanitized(): AcidPattern {
        // Already clean: the pattern itself (an edit on a big project rebuilds only what changed).
        if (isClean()) return this
        val fixed = (steps + List((MAX_STEPS - steps.size).coerceAtLeast(0)) { AcidStep() }).take(MAX_STEPS)
        return copy(
            length = length.coerceIn(1, MAX_STEPS),
            root = root.coerceIn(24, 60),
            steps = fixed.map { it.sanitized() },
            sound = sound.sanitized(),
            locked = locked.filter { it in 0 until MAX_STEPS }.toSet(),
            density = density.finite(0.8f).coerceIn(0f, 1f),
        )
    }

    private fun isClean(): Boolean =
        steps.size == MAX_STEPS && length in 1..MAX_STEPS && root in 24..60 && density.isFinite() && density in 0f..1f &&
            steps.all { it.note in 0..12 && it.octave in -1..2 } && locked.all { it in 0 until MAX_STEPS } && sound.sanitized() == sound

    /** Copies the 16 steps of bar [from] onto bar [to] (both 0-based), locks included. */
    fun copyPage(from: Int, to: Int): AcidPattern {
        val n = STEPS_PER_PAGE
        val steps = steps.toMutableList()
        for (i in 0 until n) if (to * n + i < MAX_STEPS && from * n + i < MAX_STEPS) steps[to * n + i] = steps[from * n + i]
        val movedLocks = locked.filter { it / n == from }.map { it - from * n + to * n }
        return copy(steps = steps, locked = locked.filter { it / n != to }.toSet() + movedLocks).sanitized()
    }

    fun withStep(index: Int, step: AcidStep) = copy(steps = steps.toMutableList().also { it[index] = step.sanitized() })

    companion object {
        const val MAX_STEPS = 128  // 8 bars
        const val STEPS_PER_PAGE = 16
        val LENGTHS_IN_BARS = listOf(1, 2, 3, 4, 8)
    }
}

/**
 * Generates acid lines in the spirit of The Endless Acid Banger by David Whiting
 * (https://github.com/vitling/acid-banger, CC BY 4.0): a note set chosen among a few
 * characteristic offset sets, notes more likely on strong steps, random accents and slides.
 * Locked steps are kept as they are. A [Feel] other than [Feel.CLASSIC] shapes the line for a
 * genre: more or fewer accents and slides, octave jumps (accented, sliding into the high note), a
 * phrase of two bars whose second bar answers the first.
 */
class AcidGenerator(private val random: Random = Random.Default) {

    /**
     * How a genre plays the 303. [accents] and [slides]: the chance of each on a note; [octaveJumps]:
     * the chance that an offbeat note leaps up an octave (the acidcore and acid techno bounce);
     * [phraseBars] 2: a two-bar phrase, the second bar a variation of the first ([variation]: the
     * share of its second half played differently), repeated over a longer line.
     */
    data class Feel(
        val accents: Double = 0.3,
        val slides: Double = 0.1,
        val octaveJumps: Double = 0.0,
        val phraseBars: Int = 1,
        val variation: Double = 0.35,
    ) {
        companion object {
            /** The original generator, unchanged. */
            val CLASSIC = Feel()
            /** Chicago 1987: a looping one-bar line, quite a few slides, accents here and there. */
            val ACID_HOUSE = Feel(accents = 0.25, slides = 0.18, octaveJumps = 0.15)
            /** Acid techno: a busier two-bar line that jumps octaves, sharper accents. */
            val ACID_TECHNO = Feel(accents = 0.3, slides = 0.12, octaveJumps = 0.3, phraseBars = 2)
            /** Acidcore: almost every sixteenth, octave leaps on the offbeats, accented high notes, a two-bar riff. */
            val ACIDCORE = Feel(accents = 0.35, slides = 0.1, octaveJumps = 0.4, phraseBars = 2, variation = 0.4)

            /** The feel of a style of the assistant ([StarterWizard.Style.id]), [CLASSIC] for the others. */
            fun forStyle(id: String): Feel = when (id) {
                "acid_house" -> ACID_HOUSE
                "acid_techno" -> ACID_TECHNO
                "acidcore" -> ACIDCORE
                else -> CLASSIC
            }
        }
    }

    fun generate(pattern: AcidPattern, noteSet: List<Int> = NOTE_SETS.random(random), feel: Feel = Feel.CLASSIC): AcidPattern {
        val steps = pattern.steps.toMutableList()
        // A two-bar phrase needs two bars: a one-bar line grows to two.
        val length = if (feel.phraseBars >= 2) maxOf(pattern.length, 2 * AcidPattern.STEPS_PER_PAGE).coerceAtMost(AcidPattern.MAX_STEPS) else pattern.length
        // The bars written fresh: every bar, or only the first one of a phrase.
        val fresh = if (feel.phraseBars >= 2) AcidPattern.STEPS_PER_PAGE else length
        for (i in 0 until fresh) {
            if (i in pattern.locked) continue
            steps[i] = randomStep(i, pattern.density.toDouble(), noteSet, feel)
        }
        // Always start on the one so the loop has an anchor.
        if (0 !in pattern.locked && !steps[0].gate) steps[0] = AcidStep(gate = true, accent = true)
        if (feel.phraseBars >= 2) {
            val bar = AcidPattern.STEPS_PER_PAGE
            for (i in bar until length) {
                if (i in pattern.locked) continue
                val inBar = i % bar
                val answer = (i / bar) % 2 == 1
                // The answering bar keeps the first half and plays its second half differently.
                steps[i] = if (answer && inBar >= bar / 2 && random.nextDouble() < feel.variation) {
                    randomStep(inBar, pattern.density.toDouble(), noteSet, feel)
                } else {
                    steps[inBar]
                }
            }
            // The phrase ends on a turn: the last step of the answer leaps up and slides back home.
            val last = 2 * bar - 1
            if (last < length && last !in pattern.locked && feel.octaveJumps > 0.0) {
                val s = steps[last]
                steps[last] = AcidStep(note = s.note, octave = (s.octave + 1).coerceAtMost(2), accent = true, slide = true, gate = true)
            }
            for (start in 2 * bar until length step 2 * bar) {
                for (k in 0 until 2 * bar) if (start + k < length && start + k !in pattern.locked) steps[start + k] = steps[k]
            }
        }
        return pattern.copy(steps = steps, length = length)
    }

    /** One step of a fresh bar: a note (or a rest) chosen as the original generator does, then the [feel]'s jumps. */
    private fun randomStep(i: Int, d: Double, noteSet: List<Int>, feel: Feel): AcidStep {
        val base = when {
            i % 4 == 0 -> 0.6
            i % 3 == 0 -> 0.5
            i % 2 == 0 -> 0.3
            else -> 0.1
        }
        // Density 0.8 reproduces the original feel and 0 is nearly empty; above it every step
        // gets closer to a note, until 1.0 (100 %) fills them all.
        val chance = if (d <= 0.8) base * d / 0.8 else base + (1.0 - base) * (d - 0.8) / 0.2
        if (random.nextDouble() >= chance) return AcidStep()
        val offset = noteSet.random(random)
        var step = AcidStep(
            note = offset % 12,
            octave = (offset / 12).coerceAtMost(2),
            accent = random.nextDouble() < feel.accents,
            slide = random.nextDouble() < feel.slides,
            gate = true,
        )
        // An offbeat leaping an octave up, accented: the bounce of acid techno and acidcore.
        if (feel.octaveJumps > 0.0 && i % 2 == 1 && step.octave < 2 && random.nextDouble() < feel.octaveJumps) {
            step = step.copy(octave = step.octave + 1, accent = step.accent || random.nextDouble() < 0.5)
        }
        return step
    }

    companion object {
        /** Offset sets in semitones above the root, from The Endless Acid Banger. */
        val NOTE_SETS: List<List<Int>> = listOf(
            listOf(0, 0, 12, 24, 27),
            listOf(0, 0, 0, 12, 10, 19, 26, 27),
            listOf(0, 1, 7, 10, 12, 13),
            listOf(0),
            listOf(0, 0, 0, 12),
            listOf(0, 0, 12, 14, 15, 19),
            listOf(0, 0, 0, 0, 12, 13, 16, 19, 22, 24, 25),
            listOf(0, 0, 0, 7, 12, 15, 17, 20, 24),
        )
    }
}
