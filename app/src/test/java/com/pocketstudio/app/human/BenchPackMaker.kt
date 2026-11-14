package com.pocketstudio.app.human

import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.padlive.PackMaker
import com.pocketstudio.app.padlive.PackSound
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LivePadPlan
import com.pocketstudio.model.LivePads
import com.pocketstudio.model.LiveStarter
import com.pocketstudio.model.PadLive
import kotlin.math.exp
import kotlin.math.sin

/**
 * Pad Live's « Pack… » as a phone makes it (audit ultra tour 0, seat c: « Techno » on page B left it « vide »):
 * the phone's engine renders a style's loops (one per track of a song the assistant builds in that style) and
 * its party hits; Robolectric has none, so the app got no sound and said nothing. The same pads come here, by
 * the app's own plan (names, bars, hits), each a soft tone of its length; the bench's [engine] is put in before
 * they are stored (the app keeps them with the engine) and stays while the pack is made. Bench only.
 */
internal class BenchPackMaker(private val engine: CommandSink) : PackMaker {
    override suspend fun make(pack: PadLive.Pack, bpm: Double, beatsPerBar: Int): List<PackSound> {
        NativeEngine.sink = NativeEngine.sink ?: engine
        val style = pack.style ?: return LiveStarter.PADS.mapIndexed { i, pad ->
            val beats = if (pad.oneShot) 1 else pad.beats
            PackSound(i, tone(if (pad.oneShot) HIT_FRAMES else framesOf(beats, bpm), i), pad.name) { file ->
                LiveClip(file, pad.name, beats, pad.oneShot, pad.gain, bpm = if (pad.oneShot) 0.0 else bpm)
            }
        }
        // The app's own song for the pack (audit ultra round 1, n° 5).
        val song = PadLive.packSong(pack, bpm) ?: return emptyList()
        val loops = LivePadPlan.of(song, max = PadLive.MAX_LOOPS).mapIndexed { slot, spec ->
            val beats = (spec.bars * song.beatsPerBar).coerceAtMost(LivePads.MAX_BEATS)
            val frames = if (spec.oneShot) HIT_FRAMES else OfflineExport.framesFor(spec.bars, song.tempoBpm, song.beatsPerBar).toInt()
            PackSound(slot, tone(frames, slot), spec.name) { file ->
                LiveClip(file, spec.name, if (spec.oneShot) 1 else beats, spec.oneShot, bpm = if (spec.oneShot) 0.0 else song.tempoBpm)
            }
        }
        val hits = pack.hits.mapIndexed { i, hit ->
            PackSound(PadLive.HITS_FROM + i, tone(HIT_FRAMES, 20 + i), hit.name) { file -> PadLive.hitClip(hit, file) }
        }
        return loops + hits
    }

    private fun framesOf(beats: Int, bpm: Double): Int = (beats * 60.0 / bpm.coerceAtLeast(1.0) * OfflineExport.SAMPLE_RATE).toInt()

    private fun tone(frames: Int, k: Int): Pcm = Pcm(
        FloatArray(frames.coerceAtLeast(1) * 2) { j -> 0.3f * sin(j / 2 * (0.01f + k * 0.002f)) * exp(-(j / 2 % 24_000) / 12_000f) },
        2,
        OfflineExport.SAMPLE_RATE,
    )

    private companion object {
        const val HIT_FRAMES = 9_600
    }
}
