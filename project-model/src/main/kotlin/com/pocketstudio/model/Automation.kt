package com.pocketstudio.model

import kotlinx.serialization.Serializable

/**
 * What an automation lane moves: [kind] in [AutoTarget] order (same numbers as the engine),
 * the channel [ch] (4 = the master, for insert parameters), the insert [slot] or the bus,
 * and the effect [param].
 */
@Serializable
data class AutoTarget(val kind: Int, val ch: Int = 0, val slot: Int = 0, val param: Int = 0) {
    fun pack(): Int = (kind shl 16) or (ch shl 12) or (slot shl 8) or param

    companion object {
        const val VOLUME = 0
        const val PAN = 1
        const val SEND = 2
        const val INSERT_PARAM = 3
        const val BUS_PARAM = 4
    }
}

/** One automated setting: a value 0..1 (or nothing) on each step of the drum pattern's loop. */
@Serializable
data class AutomationLane(val target: AutoTarget, val values: List<Float?> = List(STEPS) { null }) {
    fun sanitized(): AutomationLane = copy(
        values = (values + List((STEPS - values.size).coerceAtLeast(0)) { null }).take(STEPS)
            .map { v -> v?.takeIf { it.isFinite() }?.coerceIn(0f, 1f) },
    )

    fun withPoint(step: Int, value: Float?): AutomationLane =
        if (step !in 0 until STEPS) this else copy(values = values.mapIndexed { i, v -> if (i == step) value?.coerceIn(0f, 1f) else v })

    companion object {
        const val STEPS = 64
        const val MAX_LANES = 8

        fun sanitize(lanes: List<AutomationLane>): List<AutomationLane> =
            lanes.filter { it.target.kind in 0..4 }.distinctBy { it.target }.take(MAX_LANES).map { it.sanitized() }
    }
}
