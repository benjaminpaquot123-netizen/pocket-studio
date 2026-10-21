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
    private const val KEY_ROUND_TRIP_OUTPUT = "round_trip_output_ms"
    private var prefs: SharedPreferences? = null
    /** The output latency when the round trip was measured (speaker, headphones…), if known. */
    private var measuredOutputMs: Double? = null

    private val _streamInfo = MutableStateFlow(StreamInfo())
    val streamInfo: StateFlow<StreamInfo> = _streamInfo.asStateFlow()

    private val _roundTripMs = MutableStateFlow<Double?>(null)
    val roundTripMs: StateFlow<Double?> = _roundTripMs.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences("metrics", Context.MODE_PRIVATE).also { p ->
            if (p.contains(KEY_ROUND_TRIP)) _roundTripMs.value = p.getFloat(KEY_ROUND_TRIP, 0f).toDouble()
            if (p.contains(KEY_ROUND_TRIP_OUTPUT)) measuredOutputMs = p.getFloat(KEY_ROUND_TRIP_OUTPUT, 0f).toDouble()
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
        val output = _streamInfo.value.outputLatencyMs.takeIf { it > 0 }
        measuredOutputMs = output
        val editor = prefs?.edit() ?: return
        editor.putFloat(KEY_ROUND_TRIP, ms.toFloat())
        if (output != null) editor.putFloat(KEY_ROUND_TRIP_OUTPUT, output.toFloat()) else editor.remove(KEY_ROUND_TRIP_OUTPUT)
        editor.apply()
    }

    /**
     * How late the microphone hears what the app plays now, in ms: the measured round trip,
     * moved by how much slower or faster the output is than when it was measured (a Bluetooth
     * headset adds 150 to 250 ms that a measure on the speaker does not have).
     */
    fun recordLatencyMs(): Double = recordLatencyMs(
        _roundTripMs.value, measuredOutputMs, _streamInfo.value.outputLatencyMs,
        bluetooth = com.pocketstudio.app.audio.Headphones.state.value == com.pocketstudio.app.audio.Listening.Bluetooth,
    )

    /**
     * Without a measure, the microphone of a phone or a wired headset hears about as late as the
     * output plays, so the round trip is guessed as twice the output. Not with [bluetooth]: the
     * music goes out through the headset (150 to 250 ms) but the voice still comes in through the
     * phone's own microphone, a few ms away (unprocessed input): twice the output would place the
     * take 150 to 200 ms too early. The guess is then the output plus [BLUETOOTH_INPUT_MS].
     */
    const val BLUETOOTH_INPUT_MS = 20.0

    /** See [recordLatencyMs]; [outputNow] 0 or less: unknown. */
    fun recordLatencyMs(roundTrip: Double?, measuredOutput: Double?, outputNow: Double, bluetooth: Boolean = false): Double {
        val now = outputNow.takeIf { it > 0 }
        val guess = now?.let { if (bluetooth) it + BLUETOOTH_INPUT_MS else it * 2 } ?: 0.0
        return when {
            roundTrip == null -> guess
            now == null -> roundTrip
            measuredOutput != null -> (roundTrip + now - measuredOutput).coerceAtLeast(0.0)
            // Measured before the output was noted: only a much slower output (Bluetooth) is trusted over it.
            now * 2 > roundTrip * 1.5 -> guess
            else -> roundTrip
        }
    }
}
