package com.pocketstudio.app.remix

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What the separation service is doing, for the Remix screen and the notification. */
sealed interface SeparationState {
    data object Idle : SeparationState
    data class DownloadingModel(val progress: Float) : SeparationState
    data class Decoding(val name: String) : SeparationState
    data class Separating(val name: String, val progress: Float) : SeparationState
    data class Done(val set: StemSet) : SeparationState
    data class Failed(val message: String) : SeparationState

    val busy get() = this is DownloadingModel || this is Decoding || this is Separating
}

/** Process-wide state shared by [SeparationService] (writer) and the UI (reader). */
object SeparationJob {
    private val _state = MutableStateFlow<SeparationState>(SeparationState.Idle)
    val state: StateFlow<SeparationState> = _state.asStateFlow()

    internal fun set(state: SeparationState) {
        _state.value = state
    }

    /** Clears a finished or failed result once the screen has shown it. */
    fun dismiss() {
        _state.compareAndSet(_state.value.takeIf { !it.busy } ?: return, SeparationState.Idle)
    }
}
