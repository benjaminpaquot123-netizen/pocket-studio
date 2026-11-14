package com.pocketstudio.app.human

import android.os.SystemClock
import com.pocketstudio.app.padlive.ResampleSource
import com.pocketstudio.app.rhythm.RhythmViewModel
import kotlin.math.ceil
import kotlin.math.sin

/**
 * « Enregistrer ce que je joue » on the bench (audit ultra tour 0, seat c: « Pas assez de mémoire pour
 * enregistrer », then nothing): the phone's engine records what Pad Live plays from a bar line to a bar
 * line; Robolectric has none, so the app was refused at once. This stands in for it as the engine does:
 * armed in silence it waits for the music (a pad on, or ▶), records on the bench's clock, and a stop ends
 * on the next bar line; what it gives back is a soft tone of that length. Bench only.
 */
internal class BenchResample(private val vm: RhythmViewModel) : ResampleSource {
    private var next = 1L
    private var handle = 0L
    private var startedAt = -1L
    private var endFrames = -1L
    private var max = 0L

    @Synchronized override fun start(seconds: Int): Long {
        handle = next++
        startedAt = -1L
        endFrames = -1L
        max = seconds.toLong() * ResampleSource.RATE
        return handle
    }

    private fun framesPerBar(): Double = 60.0 / vm.project.value.tempoBpm.coerceAtLeast(1.0) * vm.project.value.beatsPerBar * ResampleSource.RATE

    /** Something sounds: the transport, or a Live pad as the engine says (A.16: the bench's engine plays them). */
    private fun heard(): Boolean = vm.transport.value.playing || vm.padsSoundingNow().isNotEmpty()

    private fun frames(now: Long = SystemClock.uptimeMillis()): Long =
        if (startedAt < 0) 0L else minOf(max, (now - startedAt) * ResampleSource.RATE / 1000)

    @Synchronized override fun status(handle: Long): LongArray? {
        if (handle != this.handle) return null
        if (startedAt < 0 && heard()) startedAt = SystemClock.uptimeMillis()
        val f = frames()
        val done = startedAt >= 0 && (f >= max || endFrames in 0..f)
        return longArrayOf(if (done) 2L else if (startedAt >= 0) 1L else 0L, if (done && endFrames >= 0) endFrames else f)
    }

    @Synchronized override fun stop(handle: Long) {
        if (handle != this.handle || startedAt < 0) return
        val bar = framesPerBar()
        endFrames = minOf(max, (ceil(frames() / bar).coerceAtLeast(1.0) * bar).toLong())
    }

    @Synchronized override fun finish(handle: Long): FloatArray? {
        if (handle != this.handle) return null
        val n = (if (endFrames >= 0) endFrames else frames()).toInt()
        this.handle = 0L
        if (n <= 0) return FloatArray(0)
        return FloatArray(n * 2) { k -> 0.2f * sin(k / 2 * 2.0 * Math.PI * 220.0 / ResampleSource.RATE).toFloat() }
    }
}
