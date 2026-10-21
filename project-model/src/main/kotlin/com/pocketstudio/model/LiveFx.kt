package com.pocketstudio.model

import kotlinx.serialization.Serializable

/**
 * A repeat (« Répéter », the roll pads) or chopper division, with the value the engine reads for
 * it (PerformanceFx::divisionSteps: bins of 1/8, so the older values 0.125, 0.375, 0.5, 0.875
 * still mean 1/4, 1/8, 1/16 and 1/32). [steps]: sixteenths.
 */
enum class LiveDivision(val label: String, val engineValue: Float, val steps: Double) {
    QUARTER("1/4", 0.125f, 4.0),
    EIGHTH_TRIPLET("1/8T", 0.3125f, 4.0 / 3.0),
    EIGHTH("1/8", 0.4375f, 2.0),
    SIXTEENTH("1/16", 0.5625f, 1.0),
    SIXTEENTH_TRIPLET("1/16T", 0.8125f, 2.0 / 3.0),
    THIRTY_SECOND("1/32", 0.9375f, 0.5),
    ;

    companion object {
        /** The roll pads, slowest first. */
        val ROLLS = listOf(QUARTER, EIGHTH, SIXTEENTH, THIRTY_SECOND, EIGHTH_TRIPLET, SIXTEENTH_TRIPLET)

        /** The division an engine value means (a song curve's point, an older project's). */
        fun ofValue(v: Float): LiveDivision = when ((v.coerceIn(0f, 1f) * 8).toInt().coerceIn(0, 7)) {
            0, 1 -> QUARTER
            2 -> EIGHTH_TRIPLET
            3 -> EIGHTH
            4, 5 -> SIXTEENTH
            6 -> SIXTEENTH_TRIPLET
            else -> THIRTY_SECOND
        }
    }
}

/**
 * The Live page's effect settings, saved with the project: a take played back (or exported)
 * sounds as it was played. Every field has a default (older projects load with them).
 *
 * [grid]: held effects start on the next sixteenth ([GRID_SIXTEENTH]), beat ([GRID_BEAT]) or at
 * once ([GRID_OFF]); [release]: how a held repeat, chopper, reverse or half time ends when let go
 * ([RELEASE_CUT], [RELEASE_ECHO], [RELEASE_BRAKE], [RELEASE_BACKSPIN]: then the music comes back on
 * the next bar line); [brake] and [backspin]: their length ([BRAKE_BEATS], [BACKSPIN_BEATS]
 * index); [restart]: let go, the brake starts again like a tape ([RESTART_BEATS] index, 0 = no,
 * it just comes back); [repeat] and [chop]: the divisions of « Répéter » and « Hachoir »;
 * [halfTimeBar]: half time by the bar (else by the beat); [reverse]: the slice played backwards
 * ([REVERSE_STEPS] index); [riserBars]: the riser's and downlifter's length (1, 2, 4 or 8 bars).
 */
@Serializable
data class LiveFxSettings(
    val grid: Int = GRID_SIXTEENTH,
    val release: Int = RELEASE_CUT,
    val brake: Int = 2,
    val restart: Int = 0,
    val backspin: Int = 1,
    val repeat: LiveDivision = LiveDivision.EIGHTH,
    val chop: LiveDivision = LiveDivision.SIXTEENTH,
    val halfTimeBar: Boolean = true,
    val reverse: Int = 2,
    val riserBars: Int = 4,
) {
    fun sanitized(): LiveFxSettings = copy(
        grid = grid.coerceIn(GRID_OFF, GRID_BEAT),
        release = release.coerceIn(RELEASE_CUT, RELEASE_BACKSPIN),
        brake = brake.coerceIn(0, BRAKE_BEATS.lastIndex),
        restart = restart.coerceIn(0, RESTART_BEATS.lastIndex),
        backspin = backspin.coerceIn(0, BACKSPIN_BEATS.lastIndex),
        reverse = reverse.coerceIn(0, REVERSE_STEPS.lastIndex),
        riserBars = riserBars.takeIf { it in RISER_BARS } ?: 4,
    )

    /** Sixteenths a held effect waits for at most (0: none). */
    val gridSteps: Int get() = when (grid) {
        GRID_SIXTEENTH -> 1
        GRID_BEAT -> 4
        else -> 0
    }

    /** The settings as the engine's PerformanceFx parameters (sent with the project, export included). */
    fun engineParams(): List<Pair<Int, Float>> {
        fun of(i: Int, n: Int) = (i + 0.5f) / n
        return listOf(
            ENGINE_GRID to of(grid, 3),
            ENGINE_RELEASE to of(release, 4),
            ENGINE_BRAKE_LENGTH to of(brake, BRAKE_BEATS.size),
            ENGINE_BRAKE_RESTART to of(restart, RESTART_BEATS.size),
            ENGINE_BACKSPIN_LENGTH to of(backspin, BACKSPIN_BEATS.size),
            ENGINE_CHOP_DIVISION to chop.engineValue,
            ENGINE_HALF_TIME_LENGTH to if (halfTimeBar) 1f else 0f,
            ENGINE_REVERSE_LENGTH to of(reverse, REVERSE_STEPS.size),
            ENGINE_RISER_LENGTH to of(RISER_BARS.indexOf(riserBars).coerceAtLeast(0), RISER_BARS.size),
        )
    }

    companion object {
        const val GRID_OFF = 0
        const val GRID_SIXTEENTH = 1
        const val GRID_BEAT = 2
        const val RELEASE_CUT = 0
        const val RELEASE_ECHO = 1
        const val RELEASE_BRAKE = 2
        const val RELEASE_BACKSPIN = 3
        val BRAKE_BEATS = listOf(0.25, 0.5, 1.0, 2.0, 4.0, 8.0)
        val RESTART_BEATS = listOf(0.0, 0.25, 0.5, 1.0)
        val BACKSPIN_BEATS = listOf(0.5, 1.0, 2.0, 4.0)
        /** The reverse's slice in sixteenths: 1/16, 1/8, 1/4. */
        val REVERSE_STEPS = listOf(1, 2, 4)
        val RISER_BARS = listOf(1, 2, 4, 8)

        // The engine's PerformanceFx numbers of the settings and of the one-shot commands.
        const val ENGINE_BRAKE_LENGTH = 18
        const val ENGINE_BRAKE_RESTART = 19
        const val ENGINE_BACKSPIN_LENGTH = 20
        const val ENGINE_CHOP_DIVISION = 21
        const val ENGINE_HALF_TIME_LENGTH = 22
        const val ENGINE_RELEASE = 23
        const val ENGINE_GRID = 24
        const val ENGINE_RISER_LENGTH = 25
        const val ENGINE_REVERSE_LENGTH = 26
        /** « Drop »: every live effect back at rest on the next bar line (a rise of the value). */
        const val ENGINE_DROP = AutoTarget.LIVE_DROP
        /** « Tout remettre »: every live effect back at rest at once (a rise of the value). */
        const val ENGINE_RESET_ALL = AutoTarget.LIVE_RESET_ALL
    }
}

/**
 * The transport position (sixteenths) a gesture made at [position] takes effect on in the engine:
 * the next line of [gridSteps] (the position itself when it is on one, or with no grid).
 */
fun nextGridLine(position: Double, gridSteps: Int): Double {
    if (gridSteps <= 0 || position < 0) return position
    val lines = kotlin.math.ceil(position / gridSteps - 1e-9)
    return lines * gridSteps
}
