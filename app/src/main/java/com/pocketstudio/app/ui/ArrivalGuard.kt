package com.pocketstudio.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag

/** How long what came by itself under the finger takes no touch (ms). */
const val ARRIVAL_GUARD_MS = 1_000L

/**
 * Audit ultra round 2, n° 22 (seat A a, 200 %, back from round 1): what takes the place of what the
 * finger was going to touch, by itself, takes no touch for [ARRIVAL_GUARD_MS]. The catalogue came
 * while « Lire la suite » was being touched: the kit's card took the tutorial's place and the touch
 * started « Le kit Techno… 0 % ». [key] names what [content] shows (its first value is no arrival);
 * when it changes, [asked] says whether the user's own touch changed it (« Plus tard »): then no guard.
 */
@Composable
fun ArrivalGuard(key: Any, asked: () -> Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shown = remember { mutableStateOf(key) }
    val armed = remember { mutableStateOf(false) }
    // Decided in the very composition that shows the new content: no frame where it takes a touch.
    if (shown.value != key) {
        shown.value = key
        armed.value = !asked()
    }
    LaunchedEffect(armed.value, key) {
        if (armed.value) {
            kotlinx.coroutines.delay(ARRIVAL_GUARD_MS)
            armed.value = false
        }
    }
    Box(modifier) {
        content()
        if (armed.value) {
            // Over the content, every touch taken and dropped (from its press to its release).
            Box(
                Modifier.matchParentSize().testTag("arrival_guard").pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                    }
                },
            )
        }
    }
}
