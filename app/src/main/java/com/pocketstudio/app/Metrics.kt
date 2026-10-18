package com.pocketstudio.app

import android.content.Context
import android.content.SharedPreferences
import com.pocketstudio.engine.StreamInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Latest audio measurements, shared by the screens and attached to every report. */
object Metrics {
    private const val KEY_ROUND_TRIP = "round_trip_ms"
    private var prefs: SharedPreferences? = null

    private val _streamInfo = MutableStateFlow(StreamInfo())
    val streamInfo: StateFlow<StreamInfo> = _streamInfo.asStateFlow()

    private val _roundTripMs = MutableStateFlow<Double?>(null)
    val roundTripMs: StateFlow<Double?> = _roundTripMs.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences("metrics", Context.MODE_PRIVATE).also { p ->
            if (p.contains(KEY_ROUND_TRIP)) _roundTripMs.value = p.getFloat(KEY_ROUND_TRIP, 0f).toDouble()
        }
    }

    fun updateStream(info: StreamInfo) {
        _streamInfo.value = info
    }

    private val _latencyDiagnostic = MutableStateFlow<String?>(null)
    /** Technical details of the last latency measurement, attached to reports. */
    val latencyDiagnostic: StateFlow<String?> = _latencyDiagnostic.asStateFlow()

    fun updateLatencyDiagnostic(text: String) {
        _latencyDiagnostic.value = text
    }

    fun updateRoundTrip(ms: Double) {
        _roundTripMs.value = ms
        prefs?.edit()?.putFloat(KEY_ROUND_TRIP, ms.toFloat())?.apply()
    }
}
