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
    val level: Float = 0.7f,
) {
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

    fun sanitized() = AcidSound().let { _ ->
        values().foldIndexed(this) { i, acc, v -> acc.with(i, v) }
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
        val fixed = (steps + List((MAX_STEPS - steps.size).coerceAtLeast(0)) { AcidStep() }).take(MAX_STEPS)
        return copy(
            length = length.coerceIn(1, MAX_STEPS),
            root = root.coerceIn(24, 60),
            steps = fixed.map { it.sanitized() },
            sound = sound.sanitized(),
            locked = locked.filter { it in 0 until MAX_STEPS }.toSet(),
            density = density.coerceIn(0f, 1f),
        )
    }

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
 * Locked steps are kept as they are.
 */
class AcidGenerator(private val random: Random = Random.Default) {

    fun generate(pattern: AcidPattern, noteSet: List<Int> = NOTE_SETS.random(random)): AcidPattern {
        val steps = pattern.steps.toMutableList()
        for (i in 0 until pattern.length) {
            if (i in pattern.locked) continue
            val base = when {
                i % 4 == 0 -> 0.6
                i % 3 == 0 -> 0.5
                i % 2 == 0 -> 0.3
                else -> 0.1
            }
            // density 0.8 reproduces the original feel; 1.0 is busier, 0 nearly empty.
            val chance = (base * pattern.density / 0.8).coerceAtMost(0.95)
            steps[i] = if (random.nextDouble() < chance) {
                val offset = noteSet.random(random)
                AcidStep(
                    note = offset % 12,
                    octave = (offset / 12).coerceAtMost(2),
                    accent = random.nextDouble() < 0.3,
                    slide = random.nextDouble() < 0.1,
                    gate = true,
                )
            } else {
                AcidStep()
            }
        }
        // Always start on the one so the loop has an anchor.
        if (0 !in pattern.locked && !steps[0].gate) steps[0] = AcidStep(gate = true, accent = true)
        return pattern.copy(steps = steps)
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
