package com.pocketstudio.app.rhythm

import com.pocketstudio.app.audio.FloatWav
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.report.AppLog
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.engine.NativeSample
import com.pocketstudio.model.PadSample
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.UUID
import kotlin.math.abs

/** What the screen shows for a sample: its length and a min/max outline for the waveform. */
class SampleInfo(val seconds: Double, val peaks: FloatArray)

/**
 * The project's pad sounds: files in `<project>/samples/` (32-bit float WAV at their original
 * rate) and their native copies, resampled to 48 kHz, that the engine plays.
 * Load and save run on a background dispatcher; [native] and the map are read on the main thread.
 */
class PadSamples(private val dir: File) {
    private val loaded = HashMap<String, NativeSample>()
    private val _info = MutableStateFlow<Map<String, SampleInfo>>(emptyMap())
    val info: StateFlow<Map<String, SampleInfo>> = _info.asStateFlow()

    fun native(file: String): NativeSample? = loaded[file]

    /** Blocking (background): stores [pcm] as a new file and prepares it for the engine. */
    fun add(pcm: Pcm, name: String): Pair<PadSample, Prepared> {
        dir.mkdirs()
        val file = "${UUID.randomUUID().toString().take(12)}.wav"
        FloatWav.write(File(dir, file), pcm)
        return PadSample(file, name.take(40)) to prepare(file, pcm)
    }

    /** Blocking (background): reads the files the project uses; others are leftovers and removed. */
    fun loadAll(files: Set<String>, cleanUp: Boolean = true): List<Prepared> {
        if (cleanUp) dir.listFiles()?.filter { it.name !in files }?.forEach { it.delete() }
        return files.mapNotNull { file ->
            runCatching { prepare(file, FloatWav.read(File(dir, file))) }
                .onFailure { AppLog.add("Son illisible $file : ${it.message}") }
                .getOrNull()
        }
    }

    class Prepared(val file: String, val native: NativeSample, val info: SampleInfo)

    private fun prepare(file: String, pcm: Pcm): Prepared {
        val native = NativeSample.create(pcm.data, pcm.channels, pcm.sampleRate) ?: error("son inutilisable")
        return Prepared(file, native, SampleInfo(pcm.seconds, peaks(pcm)))
    }

    /** Main thread: makes a prepared sample available to the pads. */
    fun register(p: Prepared) {
        loaded.put(p.file, p.native)?.close()
        _info.value = _info.value + (p.file to p.info)
    }

    // Tempo-fitted copies, by stretchKey. Only the ones in use are kept.
    private val stretched = HashMap<String, NativeSample>()

    private fun stretchKey(s: PadSample, bpm: Double) = "${s.file}|${s.start}|${s.end}|${s.fittedFrames(bpm)}|${s.pitch}"

    /** What [computeStretch] still has to make for the current pads (main thread). */
    fun missingStretches(samples: List<PadSample?>, bpm: Double): List<PadSample> =
        samples.filterNotNull().filter { it.fitBeats > 0 && loaded[it.file] != null && stretchKey(it, bpm) !in stretched }
            .distinctBy { stretchKey(it, bpm) }

    /** Main thread: the source a stretch job will read (looked up here, never from the worker). */
    fun sourceOf(s: PadSample): NativeSample? = loaded[s.file]

    fun keyOf(s: PadSample, bpm: Double) = stretchKey(s, bpm)

    /** Blocking (worker thread): the fitted copy of [s] made from [source], or null if too short. */
    fun computeStretch(source: NativeSample, s: PadSample, bpm: Double): NativeSample? =
        source.stretched(s.start, s.end, s.fittedFrames(bpm), s.pitch)

    /** Main thread. */
    fun registerStretch(key: String, sample: NativeSample?) {
        if (sample == null) return
        stretched.put(key, sample)?.close()
    }

    // Last fitted copy each pad got: it keeps playing while a new tempo's copy is computed.
    private val lastFitted = HashMap<Int, Pair<String, NativeSample>>()

    /** Main thread: gives every pad its sample (or its drum sound); fitted pads get their copy once ready. */
    fun pushPads(samples: List<PadSample?>, bpm: Double) {
        if (!NativeEngine.isAvailable) return
        samples.forEachIndexed { pad, s ->
            val fresh = s?.takeIf { it.fitBeats > 0 }?.let { stretchKey(it, bpm) }?.let { k -> stretched[k]?.let { k to it } }
            if (fresh != null) lastFitted[pad] = fresh
            if (s == null || s.fitBeats == 0) lastFitted.remove(pad)
            val fitted = fresh?.second ?: s?.takeIf { it.fitBeats > 0 }?.let { lastFitted[pad]?.second }
            if (s != null) NativeEngine.setPadParams(pad, s.engineParams(stretched = fitted != null))
            NativeEngine.setPadSample(pad, fitted ?: s?.let { loaded[it.file] })
        }
        // The engine holds its own references: copies no pad uses any more can go.
        val inUse = lastFitted.values.map { it.first }.toSet()
        stretched.keys.filter { it !in inUse }.forEach { stretched.remove(it)?.close() }
    }

    fun close() {
        loaded.values.forEach { it.close() }
        loaded.clear()
        stretched.values.forEach { it.close() }
        stretched.clear()
    }

    companion object {
        const val PEAK_BUCKETS = 2048

        /** Min and max of each bucket, interleaved; channels mixed. */
        fun peaks(pcm: Pcm, buckets: Int = PEAK_BUCKETS): FloatArray {
            val out = FloatArray(buckets * 2)
            val frames = pcm.frames
            if (frames == 0) return out
            for (b in 0 until buckets) {
                val from = (b.toLong() * frames / buckets).toInt()
                val to = maxOf(from + 1, ((b + 1).toLong() * frames / buckets).toInt()).coerceAtMost(frames)
                var lo = 0f
                var hi = 0f
                for (f in from until to) {
                    var v = 0f
                    for (c in 0 until pcm.channels) v += pcm.data[f * pcm.channels + c]
                    v /= pcm.channels
                    if (v < lo) lo = v
                    if (v > hi) hi = v
                }
                out[b * 2] = lo
                out[b * 2 + 1] = hi
            }
            return out
        }

        fun nameFrom(displayName: String?): String =
            displayName?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }?.take(40) ?: "Son importé"

        fun isSilent(pcm: Pcm) = pcm.data.all { abs(it) < 1e-4f }
    }
}
