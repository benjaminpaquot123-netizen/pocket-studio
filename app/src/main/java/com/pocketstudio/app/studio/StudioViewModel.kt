package com.pocketstudio.app.studio

import androidx.lifecycle.ViewModel
import com.pocketstudio.engine.NativeEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class StudioUiState(
    val playing: Boolean = false,
    val frequencyHz: Float = 440f,
    val volume: Float = 0.5f,
)

class StudioViewModel : ViewModel() {
    private val _state = MutableStateFlow(StudioUiState())
    val state: StateFlow<StudioUiState> = _state.asStateFlow()

    init {
        pushAll()
    }

    fun togglePlaying() {
        _state.update { it.copy(playing = !it.playing) }
        if (NativeEngine.isAvailable) NativeEngine.setPlaying(_state.value.playing)
    }

    fun setFrequency(hz: Float) {
        _state.update { it.copy(frequencyHz = hz) }
        if (NativeEngine.isAvailable) NativeEngine.setFrequency(hz)
    }

    fun setVolume(volume: Float) {
        _state.update { it.copy(volume = volume) }
        if (NativeEngine.isAvailable) NativeEngine.setGain(volume)
    }

    /** Re-sends the UI state, e.g. after the stream was reopened. */
    fun pushAll() {
        if (!NativeEngine.isAvailable) return
        val s = _state.value
        NativeEngine.setFrequency(s.frequencyHz)
        NativeEngine.setGain(s.volume)
        NativeEngine.setPlaying(s.playing)
    }

    override fun onCleared() {
        if (NativeEngine.isAvailable) NativeEngine.setPlaying(false)
    }
}
