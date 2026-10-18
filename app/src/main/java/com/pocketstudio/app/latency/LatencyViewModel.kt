package com.pocketstudio.app.latency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketstudio.app.Metrics
import com.pocketstudio.engine.LatencyState
import com.pocketstudio.engine.NativeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface LatencyUiState {
    data object Idle : LatencyUiState
    data class Running(val run: Int, val total: Int) : LatencyUiState
    data class Measured(val roundTripMs: Double, val runs: List<Double>) : LatencyUiState
    data class Failed(val message: String) : LatencyUiState
}

class LatencyViewModel : ViewModel() {
    private val _state = MutableStateFlow<LatencyUiState>(LatencyUiState.Idle)
    val state: StateFlow<LatencyUiState> = _state.asStateFlow()

    private var job: Job? = null

    /** Called when the app leaves the screen: never keep the microphone open in the background. */
    fun cancel() {
        job?.cancel()
        if (_state.value is LatencyUiState.Running) _state.value = LatencyUiState.Idle
    }

    fun measure(runs: Int = 3) {
        if (_state.value is LatencyUiState.Running || !NativeEngine.isAvailable) return
        job = viewModelScope.launch {
            val values = mutableListOf<Double>()
            for (run in 1..runs) {
                _state.value = LatencyUiState.Running(run, runs)
                val ms = withContext(Dispatchers.Default) { singleRun() }
                if (ms != null) values += ms
                delay(200)
            }
            if (values.isEmpty()) {
                _state.value = LatencyUiState.Failed(
                    "Signal non détecté. Retire le casque, monte le volume au maximum et réessaie dans le calme.",
                )
            } else {
                val median = values.sorted()[values.size / 2]
                Metrics.updateRoundTrip(median)
                _state.value = LatencyUiState.Measured(median, values)
            }
        }
    }

    private suspend fun singleRun(): Double? {
        var finished = false
        try {
            if (!NativeEngine.startLatencyTest()) return null
            var waited = 0
            while (NativeEngine.latencyState() == LatencyState.Running && waited < 4000) {
                delay(50)
                waited += 50
            }
            val result = NativeEngine.latencyResult()
            finished = true
            return if (result.valid) result.latencyMs else null
        } finally {
            // Closes the microphone even if the measurement was cancelled.
            if (!finished) withContext(NonCancellable) { NativeEngine.latencyResult() }
        }
    }
}
