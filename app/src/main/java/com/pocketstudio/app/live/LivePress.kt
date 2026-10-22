package com.pocketstudio.app.live

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

/**
 * Whether the grid the buttons sit in is scrolling (a fling still running): a finger that lands
 * then only stops it, as on any list, and launches nothing (see [launchOnPress]).
 */
internal val LocalGridScrolling = staticCompositionLocalOf<() -> Boolean> { { false } }

/**
 * A grid button that plays like a controller's: [press] on the finger's down (true: it launched
 * there and then, so a launch lands on the line after the press, not after the let-go); a finger
 * that slides past the touch slop is the grid scrolling, and what its press armed is taken back
 * ([retract]). When [press] did nothing (stopped, the song followed), a plain tap does [tap] on the
 * let-go, as a click. TalkBack's double tap: press, else tap.
 *
 * The scroll containers above still get every move (nothing is consumed here): the grid scrolls
 * from a pad as before. A finger landing while the grid still flings ([LocalGridScrolling]) only
 * stops it: neither press nor tap.
 */
internal fun Modifier.launchOnPress(press: () -> Boolean, retract: () -> Unit, tap: () -> Unit): Modifier = composed {
    // The lambdas change as the grid recomposes (a pad waiting, then playing): the gesture in
    // progress keeps running and calls the latest ones.
    val onPress by rememberUpdatedState(press)
    val onRetract by rememberUpdatedState(retract)
    val onTap by rememberUpdatedState(tap)
    val scrolling = LocalGridScrolling.current
    pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            // Read before the scroll containers see the down (they are outside: their pass comes after).
            if (scrolling()) return@awaitEachGesture  // the rest of the gesture is the list's
            val launched = onPress()
            val slop = viewConfiguration.touchSlop
            while (true) {
                // Final pass: after the scroll containers had their say (a move they took is consumed).
                val event = awaitPointerEvent(PointerEventPass.Final)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) {
                    if (!launched && !change.isConsumed) onTap()
                    break
                }
                if (change.isConsumed || (change.position - down.position).getDistance() > slop) {
                    if (launched) onRetract()
                    break
                }
            }
        }
    }.semantics {
        role = Role.Button
        onClick {
            if (!onPress()) onTap()
            true
        }
    }
}
