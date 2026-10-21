package com.pocketstudio.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Tells when a finger is on the control and when it lets go ([onHold] true, then false), without
 * taking the touch from it (its own drag or tap still works): what a touch take-over needs (a
 * knob held during the song wins over its automation curve). A gesture cut short (the control
 * leaving the screen) still ends with false.
 */
fun Modifier.holding(onHold: ((Boolean) -> Unit)?): Modifier = if (onHold == null) this else composed {
    val hold by rememberUpdatedState(onHold)
    pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            hold(true)
            try {
                do {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                } while (event.changes.any { it.pressed })
            } finally {
                hold(false)
            }
        }
    }
}
