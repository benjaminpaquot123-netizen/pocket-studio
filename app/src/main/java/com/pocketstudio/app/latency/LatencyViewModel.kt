package com.pocketstudio.app.latency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketstudio.app.Metrics
import com.pocketstudio.engine.LatencyFailure
import com.pocketstudio.engine.LatencyResult
import com.pocketstudio.engine.LatencyStart
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
import java.util.Locale

sealed interface LatencyUiState {
    data object Idle : LatencyUiState
    data class Running(val run: Int, val total: Int) : LatencyUiState
    data class Measured(val roundTripMs: Double, val runs: List<Double>, val diagnostic: String) : LatencyUiState
    data class Failed(val message: String, val diagnostic: String) : LatencyUiState
}

/** Outcome of one run: either the native start failed, or the meter produced a result. */
private data class RunOutcome(val start: LatencyStart, val result: LatencyResult?, val mic: String)

class LatencyViewModel : ViewModel() {
    private val _state = MutableStateFlow<LatencyUiState>(LatencyUiState.Idle)
    val state: StateFlow<LatencyUiState> = _state.asStateFlow()

    private var job: Job? = null

    /** Called when the app leaves the screen: never keep the microphone open in the background. */
    fun cancel() {
        job?.cancel()
        if (_state.value is LatencyUiState.Running) _state.value = LatencyUiState.Idle
    }

    /** Up to [maxAttempts] runs; each failure moves the engine to the next audio configuration. */
    fun measure(wanted: Int = 3, maxAttempts: Int = 6) {
        if (_state.value is LatencyUiState.Running || !NativeEngine.isAvailable) return
        job = viewModelScope.launch {
            val outcomes = mutableListOf<RunOutcome>()
            for (attempt in 1..maxAttempts) {
                _state.value = LatencyUiState.Running(attempt, maxAttempts)
                outcomes += withContext(Dispatchers.Default) { singleRun() }
                if (outcomes.count { it.result?.valid == true } >= wanted) break
                delay(300)
            }
            val values = outcomes.mapNotNull { o -> o.result?.takeIf { it.valid }?.latencyMs }
            val diagnostic = diagnostic(outcomes)
            Metrics.updateLatencyDiagnostic(diagnostic)
            if (values.isEmpty()) {
                _state.value = LatencyUiState.Failed(explain(outcomes.last()), diagnostic)
            } else {
                val median = values.sorted()[values.size / 2]
                Metrics.updateRoundTrip(median)
                _state.value = LatencyUiState.Measured(median, values, diagnostic)
            }
        }
    }

    private suspend fun singleRun(): RunOutcome {
        var finished = false
        try {
            val start = NativeEngine.startLatencyTest()
            if (start != LatencyStart.Ok) return RunOutcome(start, null, NativeEngine.inputDescription())
            var waited = 0
            while (NativeEngine.latencyState() == LatencyState.Running && waited < 4000) {
                delay(50)
                waited += 50
            }
            val result = NativeEngine.latencyResult()
            finished = true
            return RunOutcome(start, result, NativeEngine.inputDescription())
        } finally {
            // Closes the microphone even if the measurement was cancelled.
            if (!finished) withContext(NonCancellable) { NativeEngine.latencyResult() }
        }
    }

    companion object {
        private fun explain(o: RunOutcome): String = when {
            o.start == LatencyStart.MicUnavailable ->
                "Le micro refuse de s'ouvrir. Vérifie que l'accès au micro est autorisé pour Pocket Studio, et qu'aucune autre appli ne l'utilise (appel, dictée)."
            o.start == LatencyStart.NotForeground -> "Mesure interrompue : l'appli est passée en arrière-plan."
            o.start == LatencyStart.OutputFailed -> "Le haut-parleur n'a pas pu démarrer. Réessaie dans quelques secondes."
            o.start == LatencyStart.MicSilent || o.result?.failure == LatencyFailure.Interrupted ->
                "Le téléphone coupe le son dès que le micro démarre, sur tous les réglages essayés. Envoie-moi une capture de cette page."
            o.result?.failure == LatencyFailure.NoInput ->
                "Le micro ne livre aucun son malgré plusieurs réglages essayés. Relance la mesure : l'appli continue d'essayer d'autres réglages."
            o.result?.failure == LatencyFailure.SilentInput ->
                "Le micro n'entend que du silence absolu : l'accès au micro est sans doute coupé dans les réglages rapides (icône Micro)."
            else -> "Signal trop faible pour être repéré. Volume média à fond, téléphone posé sur une table, et réessaie."
        }

        private fun diagnostic(outcomes: List<RunOutcome>): String {
            return outcomes.joinToString(" | ") { o ->
                val r = o.result
                val detail = if (r == null) "départ ${o.start}"
                else String.format(
                    Locale.FRANCE, "%s, niveau %.4f, confiance %.1f, %d éch.",
                    if (r.valid) "OK %.1f ms".format(Locale.FRANCE, r.latencyMs) else r.failure.name,
                    r.inputPeak, r.confidence, r.inputFrames,
                )
                "micro ${o.mic} : $detail"
            }
        }
    }
}
