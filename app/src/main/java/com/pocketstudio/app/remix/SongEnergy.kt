package com.pocketstudio.app.remix

import com.pocketstudio.app.audio.FloatWav
import com.pocketstudio.model.SongSection
import com.pocketstudio.model.SongSections

/**
 * Step 5c: each stem's loudness bar by bar, on the song's beat grid (bars from its first beat),
 * read from the stems' files a piece at a time (never a whole stem in memory). Not on the main thread.
 */
object SongEnergy {
    /** Drums, bass, other, vocals (the engine's order); null when the tempo or a file is missing. */
    fun perBar(set: StemSet): List<FloatArray>? {
        val bpm = set.info.bpm
        if (bpm <= 0) return null
        val barSeconds = 240.0 / bpm
        return listOf("drums", "bass", "other", "vocals").map { name ->
            val file = set.stem(name)
            if (!file.isFile) return null
            var channels = 2
            var rate = 44_100
            var sums = DoubleArray(0)
            var counts = IntArray(0)
            var frame = 0L
            runCatching {
                FloatWav.forEachChunk(file, start = { floats, ch, r ->
                    channels = ch
                    rate = r
                    val bars = ((floats / ch / r.toDouble() - set.info.firstBeat) / barSeconds).toInt().coerceAtLeast(0) + 1
                    sums = DoubleArray(bars)
                    counts = IntArray(bars)
                }) { data, n ->
                    var i = 0
                    while (i + channels <= n) {
                        val t = frame.toDouble() / rate - set.info.firstBeat
                        if (t >= 0) {
                            val bar = (t / barSeconds).toInt()
                            if (bar < sums.size) {
                                var v = 0.0
                                for (c in 0 until channels) v += data[i + c].toDouble() * data[i + c]
                                sums[bar] += v / channels
                                counts[bar]++
                            }
                        }
                        frame++
                        i += channels
                    }
                }
            }.getOrElse { return null }
            // The last bar, when only a bit of it is there, is left out.
            val whole = counts.indices.filter { counts[it] > rate * barSeconds * 0.5 }
            FloatArray(whole.size) { kotlin.math.sqrt(sums[whole[it]] / counts[whole[it]]).toFloat() }
        }
    }

    /** The song's parts ([SongSections.detect]); empty when they cannot be told. */
    fun sections(set: StemSet): List<SongSection> = perBar(set)?.let(SongSections::detect).orEmpty()
}
