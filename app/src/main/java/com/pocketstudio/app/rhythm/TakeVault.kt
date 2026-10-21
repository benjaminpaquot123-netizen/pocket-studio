package com.pocketstudio.app.rhythm

import com.pocketstudio.app.audio.FloatWav
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.report.AppLog
import com.pocketstudio.model.PendingTake
import com.pocketstudio.model.PendingTakes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.UUID
import kotlin.math.abs

/**
 * Finishes microphone takes outside any screen's lifetime: the sound is taken from the engine,
 * written into the project's `samples/` and noted as waiting ([PendingTakes]) even when the app
 * is closed in the middle (swiped away, the screen's ViewModel cleared). The screen then puts it
 * on its track; when the screen is gone, the next opening of the project does.
 *
 * [finishTake]: the engine's `recordFinish` (a fake in tests). Runs on its own scope, never a
 * ViewModel's: cancelling the screen never drops a take.
 */
class TakeVault(
    private val finishTake: (Long) -> FloatArray?,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    /** What to do with a take once the engine hands it over. */
    class Request(
        val handle: Long,
        /** The project's folder (its `samples/` and `takes/`). */
        val projectFolder: File,
        val track: Int,
        val name: String,
        val bpm: Double,
        val song: Boolean,
        val start: Int,
        /** Frames cut at the start: the measured latency, then what was recorded before [start]. */
        val latencyFrames: Int,
        val cutFrames: Int,
        /** Stopped during the count-in: nothing to keep, and no error. */
        val waiting: Boolean,
        val sampleRate: Int = 48000,
        /** The track's clip file when the take began (see [PendingTake.replaces]). */
        val replaces: String? = null,
        /** A scene take: the scene it was recorded on (see [PendingTake.scene]). */
        val scene: Int? = null,
    )

    sealed interface Outcome {
        /** [pcm] is read back from the written file when first asked (off the main thread): minutes of sound are never held twice. */
        class Saved(val take: PendingTake, load: () -> Pcm) : Outcome {
            val pcm: Pcm by lazy(load)
        }
        /** Stopped during the count-in. */
        data object Nothing : Outcome
        data object TooShort : Outcome
        data object Silent : Outcome
        /** The engine did not hand it over. */
        data object Lost : Outcome
        class Failed(val error: Throwable) : Outcome
    }

    private val _busy = MutableStateFlow(0)
    /** Takes recording or being written: the app must not go before they are on disk. */
    val busy: StateFlow<Int> = _busy.asStateFlow()

    private val open = HashSet<Long>()

    /** A take started recording (the service waits for it before it lets the app go). */
    fun opened(handle: Long) = synchronized(open) {
        if (handle != 0L && open.add(handle)) _busy.value += 1
    }

    private fun close(handle: Long) = synchronized(open) {
        if (open.remove(handle)) _busy.value -= 1
    }

    /** Ends take [Request.handle] and stores it (see the class comment). */
    fun finish(r: Request): Deferred<Outcome> {
        synchronized(open) { _busy.value += 1 }
        close(r.handle)
        return scope.async {
            try {
                store(r)
            } catch (e: Throwable) {
                AppLog.add("Prise : écriture impossible (${e.message})")
                Outcome.Failed(e)
            } finally {
                synchronized(open) { _busy.value -= 1 }
            }
        }
    }

    /** Leaves a take without keeping it: the microphone closes and its memory goes. */
    fun discard(handle: Long) {
        if (handle == 0L) return
        scope.launch {
            try {
                finishTake(handle)
            } finally {
                close(handle)
            }
        }
    }

    private fun store(r: Request): Outcome {
        val audio = finishTake(r.handle) ?: return Outcome.Lost
        val keep = keptRange(audio.size, r.latencyFrames, r.cutFrames, r.sampleRate) ?: return if (r.waiting && audio.size < r.sampleRate / 10) Outcome.Nothing else Outcome.TooShort
        if (isSilent(audio, keep)) return Outcome.Silent
        val samples = File(r.projectFolder, "samples").apply { mkdirs() }
        val file = "${UUID.randomUUID().toString().take(12)}.wav"
        val wav = File(samples, file)
        // Straight from the engine's copy: no second copy of a long take in memory.
        FloatWav.write(wav, audio, keep.first, keep.last + 1, 1, r.sampleRate)
        val seconds = (keep.last + 1 - keep.first).toDouble() / r.sampleRate
        val take = PendingTake(r.track, file, r.name.take(40), seconds, r.bpm, r.song, r.start, r.replaces, scene = r.scene)
        PendingTakes.write(r.projectFolder, take)
        AppLog.add("Prise écrite : ${"%.1f".format(seconds)} s")
        return Outcome.Saved(take) { FloatWav.read(wav) }
    }

    /** Suspends until no take is recording or being written, at most [timeoutMs]. */
    suspend fun awaitIdle(timeoutMs: Long): Boolean = withTimeoutOrNull(timeoutMs) { busy.first { it <= 0 } } != null

    /** Blocking [awaitIdle], for a project being opened. */
    fun awaitIdleBlocking(timeoutMs: Long): Boolean = busy.value <= 0 || runBlocking { awaitIdle(timeoutMs) }

    companion object {
        /** The app's vault (the engine's recordings). */
        val app: TakeVault by lazy { TakeVault({ com.pocketstudio.engine.NativeEngine.recordFinish(it) }) }

        /**
         * The frames of a recording of [size] kept: after the [latencyFrames] (at most half of
         * it) and the [cutFrames]; null when less than a tenth of a second would be left.
         */
        fun keptRange(size: Int, latencyFrames: Int, cutFrames: Int, sampleRate: Int = 48000): IntRange? {
            if (size < sampleRate / 10) return null
            val from = latencyFrames.coerceIn(0, size / 2).toLong() + cutFrames.coerceAtLeast(0)
            if (size - from < sampleRate / 10) return null
            return from.toInt() until size
        }

        /** No sound above -80 dB in [range] of [audio] (a microphone that stayed shut). */
        fun isSilent(audio: FloatArray, range: IntRange): Boolean {
            for (i in range.first..range.last) if (abs(audio[i]) >= 1e-4f) return false
            return true
        }
    }
}
