package com.pocketstudio.app.rhythm

import com.pocketstudio.engine.NativeSample
import com.pocketstudio.model.AutoTempo

/**
 * Step 4e (Benjamin's list n° 16): the loops that follow the project's tempo (a library loop on the
 * Live grid, a live pad loop) get a copy stretched to it, its pitch kept, made on a worker thread
 * (never in the audio callback). While a new tempo's copy is being made, each slot keeps the copy
 * it had; the new one goes in flagged as a refit (the engine carries the loop on at its place).
 * Main thread, except [compute].
 */
class TempoFits {
    /**
     * A slot ("a3": audio track 3, "l5": live pad 5) that plays [file] lasting [frames];
     * [asStored]: at its own tempo, the sound as stored (no copy).
     */
    data class Want(val slot: String, val file: String, val frames: Long, val asStored: Boolean) {
        val key: String get() = "$file|$frames"
    }

    /** What a slot plays: [sample] (null: the sound as stored), [refit] when it is the same loop at another length. */
    data class Pick(val sample: NativeSample?, val refit: Boolean)

    private class Given(val key: String?, val file: String)

    private val copies = HashMap<String, NativeSample>()
    // What each slot was last given: it keeps playing while the next copy is made.
    private val given = HashMap<String, Given>()
    // Copies that could not be made: not tried again on every push.
    private val failed = HashSet<String>()

    /** The copies still to make for [wants]. */
    fun missing(wants: List<Want>): List<Want> =
        wants.filter { !it.asStored && it.key !in copies && it.key !in failed }.distinctBy { it.key }

    /** Blocking (worker thread): [source] lasting [frames], or null (too short, too long, let go meanwhile). */
    fun compute(source: NativeSample, frames: Long): NativeSample? =
        runCatching { source.stretched(0f, 1f, frames, 0f) }.getOrNull()

    fun register(key: String, sample: NativeSample?) {
        if (sample == null) {
            failed += key
            return
        }
        copies.put(key, sample)?.close()
    }

    /** The copy for [want] if made (an export: nothing remembered). */
    fun ready(want: Want?): NativeSample? = want?.takeIf { !it.asStored }?.let { copies[it.key] }

    /**
     * What [slot] plays for [want] (null: not a loop that follows the tempo): the copy at the tempo
     * once made, else the copy of the same sound it had, else the sound as stored.
     */
    fun pick(slot: String, want: Want?): Pick {
        val had = given[slot]
        if (want == null) {
            given.remove(slot)
            return Pick(null, false)
        }
        val sameLoop = had != null && had.file == want.file
        if (want.asStored) {
            given[slot] = Given(null, want.file)
            return Pick(null, sameLoop && had?.key != null)
        }
        val fresh = copies[want.key]
        if (fresh != null) {
            given[slot] = Given(want.key, want.file)
            return Pick(fresh, sameLoop && had?.key != want.key)
        }
        val kept = had?.takeIf { sameLoop }?.key?.let { copies[it] }
        if (kept == null) given[slot] = Given(null, want.file)
        return Pick(kept, false)
    }

    /** Lets go of the copies neither played nor wanted (the engine holds its own references). */
    fun prune(wants: List<Want>) {
        val keep = given.values.mapNotNull { it.key }.toSet() + wants.map { it.key }
        copies.keys.filter { it !in keep }.forEach { copies.remove(it)?.close() }
    }

    fun close() {
        copies.values.forEach { it.close() }
        copies.clear()
        given.clear()
    }

    companion object {
        /** [slot] playing [file] ([storedFrames] long, made at [bpm]) at [tempo]; null when it keeps its speed. */
        fun want(slot: String, file: String, storedFrames: Long, bpm: Double, tempo: Double): Want? {
            if (bpm <= 0.0 || storedFrames <= 0L || !AutoTempo.canFit(bpm, tempo)) return null
            return if (AutoTempo.needsFit(bpm, tempo)) Want(slot, file, AutoTempo.fittedFrames(storedFrames, bpm, tempo), false)
            else Want(slot, file, storedFrames, true)
        }
    }
}
