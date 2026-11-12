package com.pocketstudio.ui

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * One vocabulary of vibrations (docs/DESIGN.md « Retour immédiat »), the same in every screen,
 * always through LocalHapticFeedback (Réglages › Vibrations turns them all off at once).
 * Three kinds only, so each means one thing under the finger.
 */
object Feel {
    /** A pad or a key played: the lightest tick, ready for fast rolls. */
    val Hit = HapticFeedbackType.TextHandleMove

    /** A knob or a slider passing a notch (its default, a step, an end): the same light tick. */
    val Notch = HapticFeedbackType.TextHandleMove

    /** A hold that does something (a long press, a double tap that resets): a firmer bump. */
    val Hold = HapticFeedbackType.LongPress
}

fun HapticFeedback.hit() = performHapticFeedback(Feel.Hit)
fun HapticFeedback.notch() = performHapticFeedback(Feel.Notch)
fun HapticFeedback.hold() = performHapticFeedback(Feel.Hold)

/** Most steps a slider can have and still tick at each one (more would buzz). */
internal const val NOTCHED_STEPS_MAX = 24

/** True when a value going from [before] to [after] passes or lands on [notch] (from elsewhere). */
internal fun passesNotch(before: Float, after: Float, notch: Float): Boolean =
    before != notch && (after == notch || (before - notch) * (after - notch) < 0f)
