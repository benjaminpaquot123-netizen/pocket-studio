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
