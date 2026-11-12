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
