package com.pocketstudio.app.instruments

import android.content.Context
import android.net.Uri
import com.pocketstudio.app.audio.AudioDecoder
import com.pocketstudio.app.library.PackStore
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.engine.NativeSample
import com.pocketstudio.model.MultiInstrument
import com.pocketstudio.model.PackManifest
import com.pocketstudio.model.SoundPacks
import java.io.File
import java.io.IOException

/**
 * A downloaded multizone instrument in the engine's memory (phase 38): its zones as 16-bit
 * samples, shared by every track that plays it and by the exports (each keeps its own reference
 * in the engine, so [close] is safe at any time: the memory goes once nothing plays it).
 */
class MultiBank(val pack: String, val version: Int, val instrument: MultiInstrument, val zones: List<NativeSample>) : AutoCloseable {
    override fun close() = zones.forEach { it.close() }

    /**
     * Sends the instrument to track [track] (the live engine, or the export being captured): its
     * zones, their settings, then the instrument's own, [gain] on top of its calibration.
     */
    fun push(track: Int, gain: Float = 1f) {
        val params = instrument.engineParams()
        // Emptied first (Count 0, the zones of an instrument before let go): a note in between never
        // plays half an instrument, and a smaller one leaves no zone of the last behind.
        clear(track)
        instrument.zones.forEachIndexed { i, z ->
            NativeEngine.setTrackZone(track, i, zones[i])
            z.engineValues().forEachIndexed { p, v -> NativeEngine.setTrackZoneParam(track, i, p, v) }
        }
        params.forEachIndexed { p, v ->
            if (p == 0) return@forEachIndexed
            NativeEngine.setTrackMultiParam(track, p, if (p == GAIN) (v * gain).coerceIn(0f, 4f) else v)
        }
        NativeEngine.setTrackMultiParam(track, 0, params[0])
    }

    companion object {
        /** MultiSampler::Gain. */
        private const val GAIN = 4

        /**
         * The engine's memory the instruments may take together: two pianos and the orchestra. Past
         * it a track plays its synth sound (« trop d'instruments à la fois »).
         */
        const val BUDGET_BYTES = 200_000_000L

        /** MultiSampler::ClearZones. */
        private const val CLEAR_ZONES = 6

        /** Track [track] has no multizone instrument (its synth plays): its zones are let go. */
        fun clear(track: Int) {
            NativeEngine.setTrackMultiParam(track, CLEAR_ZONES, 1f)
        }

        /** The instrument of installed pack [pack] (null: not on the phone, or no instrument). Blocking. */
        fun manifest(context: Context, pack: String): PackManifest? {
            if (!PackManifest.safeName(pack)) return null
            val f = File(File(PackStore.dir(context), pack), "pack.json")
            return runCatching { SoundPacks.parseManifest(f.readText()) }.getOrNull()?.takeIf { it.id == pack && it.instrument != null }
        }

        /**
         * Decodes every zone of [pack] into the engine (a few seconds for a piano: a worker thread).
         * Throws when a zone cannot be read: never half an instrument.
         */
        fun load(context: Context, pack: String): MultiBank {
            val m = manifest(context, pack) ?: throw IOException("instrument absent : $pack")
            val inst = m.instrument!!
            val dir = File(PackStore.dir(context), pack)
            val zones = ArrayList<NativeSample>(inst.zones.size)
            try {
                for (z in inst.zones) {
                    val pcm = AudioDecoder.decode(context, Uri.fromFile(File(dir, z.file)))
                    zones += NativeSample.createCompact(pcm.data, pcm.channels, pcm.sampleRate) ?: throw IOException("zone illisible : ${z.file}")
                }
            } catch (e: Throwable) {
                zones.forEach { it.close() }
                throw e
            }
            return MultiBank(pack, m.version, inst, zones)
        }
    }
}
