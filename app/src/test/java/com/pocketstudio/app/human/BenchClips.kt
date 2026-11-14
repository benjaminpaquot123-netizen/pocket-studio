package com.pocketstudio.app.human

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow

/**
 * Audit « banc crédible » A.16 (F3, G2): the Live pads of the bench's engine, as the phone's ClipLauncher
 * (engine/src/main/cpp/core/ClipLauncher.cpp) plays them, driven only by the commands the app sends ([BenchEngine.post]:
 * 45 SetClipParam, 46 ToggleClip, 150 LaunchClipRow; [BenchEngine.install] of a LiveClip). The bench's pads used to start
 * at once whatever was touched (« sans grille »), their state written by the bench itself through test hooks.
 *
 * What is played: each slot's state (0 off, 1 waiting for its line, 2 playing), on ClipLauncher's own beat clock. That
 * clock runs on its own from the engine's start (transport stopped too), at the tempo; it restarts at 0 when the first pad
 * starts with nothing else on and the transport stopped (`idleClock`); it snaps to the sequencer's beat when the transport
 * reaches its step 0 (ClipLauncher::align: the nearest value with the sequencer's place in a 64-beat cycle; waiting pads
 * take its grid again, loops start over). A touch on a pad off waits for its grid line: its loop length, at most a bar
 * (gridOf); a touch while it waits cancels it; a loop playing stops. R.7 (T5-PADRETRIG): the transport stopped and no other
 * pad on, a hit sounds at once, each touch (still sounding: it starts over); a hit waiting (transport stopped) is never
 * cancelled by a touch again. A row waits for
 * the next bar line all together; a row whose loops all play stops them. A hit plays its sound once (its frames, read
 * faster or slower with its pitch) and goes off. Not played: the sound itself, levels, attack and release (a pad's state
 * only changes on lines and at a hit's end, which is all the app reads: NativeEngine.clipStatesNow).
 *
 * Lazy, as the bench's [BenchEngine.SongClock]: every read or command first plays what happened since the last one, in time
 * order, on the bench's clock (SystemClock, ms). [now], [tempo], [beatsPerBar] are the engine's.
 */
internal class BenchClips(
    private val now: () -> Long,
    private val tempo: () -> Double,
    private val beatsPerBar: () -> Int,
) {
    private class Slot {
        /** The sound installed (its handle, 0: empty) and its length in frames at 48 kHz. */
        var sample = 0L
        var frames = 0L
        var beats = 4
        var oneShot = false
        var speed = 1.0
        var state = OFF
        /** Waiting: the beat it starts on; playing: the beat its pass began on. */
        var startBeat = 0.0
        /** A hit's sound: when it ends (ms on the bench's clock), -1 none sounding. */
        var voiceEnd = -1.0
    }

    private val slots = Array(SLOTS) { Slot() }

    /** ClipLauncher's beat_, at [at] (ms). The clock starts with the engine, at 0. */
    private var beat = 0.0
    private var at = now().toDouble()

    /** When the transport reaches (or reached) its step 0 and the clock snaps to it, -1 none waiting. */
    private var syncAt = -1.0

    private fun gridOf(s: Slot): Double = if (s.beats < bpb()) s.beats.toDouble() else bpb().toDouble()
    private fun bpb() = beatsPerBar().coerceIn(2, 7)
    private fun perMs() = tempo() / 60_000.0

    /** The transport started: its step 0 comes at [zeroAt] (ms; after the count-in); -1: stopped. */
    @Synchronized fun transportZeroAt(zeroAt: Long) {
        advance()
        syncAt = if (zeroAt < 0) -1.0 else zeroAt.toDouble()
    }

    /** Plays what is due up to the bench's now, in time order. */
    @Synchronized fun advance() {
        val end = now().toDouble()
        while (true) {
            val rate = perMs()
            // The next instant something changes: the clock's snap, a pad reaching its line, a hit's sound ending.
            var next = Double.MAX_VALUE
            if (syncAt >= 0) next = maxOf(syncAt, at)
            for (s in slots) {
                if (s.sample == 0L) continue
                if (s.state == WAITING) next = minOf(next, at + maxOf(0.0, s.startBeat - beat) / rate)
                else if (s.state == PLAYING && s.oneShot && s.voiceEnd >= 0) next = minOf(next, maxOf(s.voiceEnd, at))
            }
            if (next > end) {
                beat += (end - at) * rate
                at = end
                return
            }
            beat += (next - at) * rate
            at = next
            // Everything due at this instant, in ClipLauncher::render's order: the clock's snap first, then each slot.
            if (syncAt >= 0 && syncAt <= at + EPS_MS) {
                syncAt = -1.0
                align()
            }
            for ((k, s) in slots.withIndex()) {
                if (s.sample == 0L) continue
                if (s.state == WAITING && beat + EPS_BEAT >= s.startBeat) {
                    s.state = PLAYING
                    s.voiceEnd = if (s.oneShot) at + ceil(s.frames / s.speed) * 1000.0 / RATE else -1.0
                    if (s.oneShot && hits.size < SLOTS) hits.addLast(k)
                } else if (s.state == PLAYING && s.oneShot && s.voiceEnd >= 0 && s.voiceEnd <= at + EPS_MS) {
                    s.state = OFF
                    s.voiceEnd = -1.0
                }
            }
        }
    }

    /** ClipLauncher::align with the sequencer at its beat 0: the nearest clock value on a 64-beat cycle line. */
    private fun align() {
        val was = Math.round(beat).toDouble()
        val cycle = (if (bpb() == 4) MAX_BEATS else bpb() * 16).toDouble()
        var target = was - was.mod(cycle)
        if (target - was > cycle / 2) target -= cycle else if (was - target > cycle / 2) target += cycle
        beat = target
        if (target == was) return
        for (s in slots) {
            if (s.sample == 0L) continue
            if (s.state == WAITING) {
                val grid = gridOf(s)
                s.startBeat = ceil(beat / grid - 1e-9) * grid
            } else if (s.state == PLAYING && !s.oneShot) {
                s.startBeat = floor(beat / s.beats) * s.beats
            }
        }
    }

    /** A LiveClip installed ([sample] 0: none; [frames]: its length), ClipLauncher::install. */
    @Synchronized fun install(slot: Int, sample: Long, frames: Long) {
        advance()
        val s = slots.getOrNull(slot) ?: return
        if (sample == s.sample) return
        // The pass sounding fades out (fadeAway): a hit playing has no voice left, it goes off on the next block.
        if (s.state == PLAYING && s.oneShot) s.voiceEnd = at
        s.sample = sample
        s.frames = frames
        if (sample == 0L) {
            s.state = OFF
            s.voiceEnd = -1.0
        }
    }

    /** CMD_CLIP_PARAM: ClipLauncher::setParam (beats, one-shot, pitch; the others do not change a state). */
    @Synchronized fun setParam(slot: Int, param: Int, value: Float) {
        advance()
        val s = slots.getOrNull(slot) ?: return
        if (!value.isFinite()) return
        when (param) {
            0 -> s.beats = Math.round(value).coerceIn(1, MAX_BEATS)
            1 -> s.oneShot = value > 0.5f
            3 -> s.speed = 2.0.pow(value.coerceIn(-24f, 24f) / 12.0)
        }
    }

    /** CMD_TOGGLE_CLIP with a pad < 0 (« Tout couper », ■): ClipLauncher::stopAll. */
    @Synchronized fun stopAll() {
        advance()
        for (s in slots) stop(s)
    }

    /** CMD_TOGGLE_CLIP: ClipLauncher::toggle ([idleClock]: the transport stopped). */
    @Synchronized fun toggle(slot: Int, idleClock: Boolean) {
        advance()
        val s = slots.getOrNull(slot) ?: return
        if (s.sample == 0L) return
        val alone = idleClock && slots.none { it !== s && it.state != OFF }
        when {
            s.oneShot && alone -> {
                // R.7: at once (render starts it on its next frame); nothing on: the clock starts over with it.
                if (s.state == OFF) beat = 0.0
                s.startBeat = beat
                s.state = WAITING
            }
            s.state == PLAYING && !s.oneShot -> stop(s)
            s.state == WAITING && s.oneShot && idleClock -> Unit  // R.7: never cancelled by a touch again
            s.state == WAITING -> s.state = OFF
            else -> {
                // A one-shot with other pads on or the transport playing: hit again on the grid.
                if (idleClock && slots.none { it.state != OFF }) beat = 0.0
                val grid = gridOf(s)
                s.startBeat = ceil(beat / grid - 1e-9) * grid
                s.state = WAITING
            }
        }
    }

    /** CMD_LAUNCH_CLIP_ROW: ClipLauncher::launchRow, the pads [first] until [first] + [count]. */
    @Synchronized fun launchRow(first: Int, count: Int, idleClock: Boolean) {
        advance()
        val from = first.coerceIn(0, SLOTS)
        val to = (first + maxOf(count, 0)).coerceIn(from, SLOTS)
        val row = (from until to).map { slots[it] }.filter { it.sample != 0L }
        if (row.isEmpty()) return
        val loops = row.filter { !it.oneShot }
        if (loops.isNotEmpty() && loops.all { it.state != OFF }) {
            loops.forEach { stop(it) }
            return
        }
        if (idleClock && slots.none { it.state != OFF }) beat = 0.0
        val bar = bpb().toDouble()
        val start = ceil(beat / bar - 1e-9) * bar
        for (s in row) {
            if (s.state == PLAYING && !s.oneShot) continue
            s.startBeat = start
            s.state = WAITING
        }
    }

    /** ClipLauncher::stopSlot: off (a hit stopped fades out with its release: nothing to read any more). */
    private fun stop(s: Slot) {
        s.state = OFF
        s.voiceEnd = -1.0
    }

    /** Each pad's state as NativeEngine.clipStates gives it (0 off, 1 waiting, 2 playing). */
    @Synchronized fun states(out: IntArray) {
        advance()
        for (k in out.indices) out[k] = slots.getOrNull(k)?.state ?: OFF
    }

    /** The hits that started sounding since the last call (pads, in order): a bench's ear hears a hit after it is over. */
    private val hits = ArrayDeque<Int>()

    @Synchronized fun hitsPlayed(): List<Int> {
        advance()
        return hits.toList().also { hits.clear() }
    }

    /** The clock's beat now (tests). */
    @Synchronized fun beatNow(): Double {
        advance()
        return beat
    }

    companion object {
        const val SLOTS = 64
        const val MAX_BEATS = 64
        const val OFF = 0
        const val WAITING = 1
        const val PLAYING = 2
        private const val RATE = 48_000.0
        /** The tolerances of ClipLauncher::render (a line reached a hair before its exact frame count, never late). */
        private const val EPS_BEAT = 1e-6
        private const val EPS_MS = 1e-6
    }
}
