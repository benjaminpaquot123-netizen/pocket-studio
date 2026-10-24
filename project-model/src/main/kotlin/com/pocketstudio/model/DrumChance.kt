package com.pocketstudio.model

/**
 * Phase 37: whether a hit with a chance ([Hit.chance]) plays on a pass, drawn exactly as the engine
 * draws it (Engine::chancePlays): from the project's seed ([Project.drumSeed]), the pad and the
 * step's place, in the song (a song step), else in the drums' loop since they started. The same
 * draw every time: what plays live is what the export, the MIDI file and the screen show.
 */
object DrumChance {
    /** The largest seed the engine takes exactly (a float carries it). */
    const val MAX_SEED = 1 shl 24

    /** lowbias32 (Chris Wellons, public domain), the engine's hash. */
    internal fun hash(v: Int): Int {
        var x = v
        x = x xor (x ushr 16)
        x *= 0x7feb352d
        x = x xor (x ushr 15)
        x *= 0x846ca68b.toInt()
        x = x xor (x ushr 16)
        return x
    }

    /** The hit of [chance] % on pad [lane] at place [key] plays with [seed]. */
    fun plays(seed: Int, lane: Int, key: Int, chance: Int): Boolean {
        val skip = 100 - chance
        if (skip <= 0) return true
        if (skip >= 100) return false
        val h = hash(seed xor hash(key * 16 + lane))
        return Integer.remainderUnsigned(h, 100) >= skip
    }

    /** Another seed than [seed] (« Autre tirage »), in range. */
    fun next(seed: Int): Int = Math.floorMod(hash(seed + 1), MAX_SEED).let { if (it == seed) (it + 1) % MAX_SEED else it }
}

/**
 * The pattern step pad [lane] plays at drums step [local] (its own loop, [DrumLane.length], or the
 * pattern's), as the engine counts it.
 */
fun DrumPattern.laneStep(lane: Int, local: Int): Int {
    val n = lanes.getOrNull(lane)?.loopSteps(steps)?.coerceAtLeast(1) ?: steps.coerceAtLeast(1)
    return Math.floorMod(local, n)
}

/** Some pad loops on a length of its own, or some hit has a chance: the pattern is no longer a plain loop. */
val DrumPattern.isPolyOrChance: Boolean get() = lanes.any { l -> l.length > 0 || l.hits.values.any { it.chance < 100 } }
