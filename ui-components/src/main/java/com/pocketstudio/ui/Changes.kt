package com.pocketstudio.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * What [read] gives, as a state that changes only when that value changes: a composable that reads it is composed
 * again then, and never because what [read] looks at moved (the engine's readings, 20 to 30 a second while the
 * song plays) without changing it. Tour 5 « jank »: the cells and pads of Sons looked at the playhead through
 * derivedStateOf, and every one of them was composed again at each step all the same.
 * The first value is read at once (the first frame is drawn with it); later ones follow a frame after.
 */
@Composable
fun <T> rememberChangesOf(vararg keys: Any?, read: () -> T): State<T> {
    val latest by rememberUpdatedState(read)
    val state = remember(*keys) { mutableStateOf(Snapshot.withoutReadObservation { read() }) }
    LaunchedEffect(state) { snapshotFlow { latest() }.collect { state.value = it } }
    return state
}

/**
 * [rememberChangesOf] for a number shown as text: it changes at most once every [periodMillis] (the latest value
 * then, never an old one). Tour 5 « mixwake »: the Mix's loudness and limiter numbers changed at every reading of the
 * engine (20 a second), each change a new text measured and the screen around it laid out again; a number that
 * moves 20 times a second cannot be read anyway. The first value is read at once (the first frame shows it).
 */
@Composable
fun <T> rememberChangesEvery(periodMillis: Long, vararg keys: Any?, read: () -> T): State<T> {
    val latest by rememberUpdatedState(read)
    val state = remember(*keys) { mutableStateOf(Snapshot.withoutReadObservation { read() }) }
    LaunchedEffect(state, periodMillis) {
        // snapshotFlow keeps only the latest change while this waits: the value shown after the pause is the newest.
        snapshotFlow { latest() }.collect {
            state.value = it
            kotlinx.coroutines.delay(periodMillis)
        }
    }
    return state
}

/**
 * [rememberChangesEvery] for a warning or a level that must not be missed: instead of the latest reading, what it shows
 * is the WORST one read during the period ([worse] picks it from the worst so far and the new one), the worst starting
 * again from the last reading after each change. A clip of one reading (33 ms) falling between two samples is shown
 * all the same; it still changes at most once every [periodMillis]. Tour 5 « mixwake », review finding 1.
 */
@Composable
fun <T> rememberWorstEvery(periodMillis: Long, vararg keys: Any?, worse: (T, T) -> T, read: () -> T): State<T> {
    val latest by rememberUpdatedState(read)
    val worseNow by rememberUpdatedState(worse)
    val state = remember(*keys) { mutableStateOf(Snapshot.withoutReadObservation { read() }) }
    LaunchedEffect(state, periodMillis) {
        var has = false
        var held: Any? = null
        var last: Any? = null
        launch {
            snapshotFlow { latest() }.collect {
                @Suppress("UNCHECKED_CAST")
                held = if (has) worseNow(held as T, it) else it
                last = it
                has = true
            }
        }
        // Same thread as the collector: nothing here races with it.
        while (true) {
            delay(periodMillis)
            if (has) {
                @Suppress("UNCHECKED_CAST")
                state.value = held as T
                // The next period's worst starts from the reading the screen now stands on, so a steady value is not held.
                held = last
            }
        }
    }
    return state
}
