package com.pocketstudio.app.remix

import com.pocketstudio.app.audio.Pcm
import kotlin.math.max
import kotlin.math.min

/**
 * MERGE (B3): a stand-in for B2's `StemEditor` so this branch builds and runs on its own. At the
 * merge, [RhythmViewModel.stemBoard][com.pocketstudio.app.rhythm.RhythmViewModel.stemBoard] takes
 * `StemEditor` instead and this file goes. It cuts with straight fades and finds phrases as
 * stretches louder than a tenth of the loudest 10 ms, no beat snapping.
 */
internal object ProvisionalStemEdits : StemEdits {
    override fun cut(stem: Pcm, sel: StemSelection): Pcm {
        val ch = stem.channels
        val total = stem.frames.toLong()
        val start = sel.startFrame.coerceIn(0L, total)
        val end = sel.endFrame.coerceIn(start, total)
        val out = stem.data.copyOfRange((start * ch).toInt(), (end * ch).toInt())
        val n = (end - start).toInt()
        val fadeIn = min(n / 2, sel.fadeInMs.coerceAtLeast(1) * stem.sampleRate / 1000)
        val fadeOut = min(n - fadeIn, sel.fadeOutMs.coerceAtLeast(1) * stem.sampleRate / 1000)
        for (i in 0 until fadeIn) for (c in 0 until ch) out[i * ch + c] *= i.toFloat() / fadeIn
        for (i in 0 until fadeOut) for (c in 0 until ch) out[(n - 1 - i) * ch + c] *= i.toFloat() / fadeOut
        return Pcm(out, ch, stem.sampleRate)
    }

    override fun phrases(stem: Pcm, beatFrames: Long?, max: Int): List<LongRange> {
        val ch = stem.channels
        val win = max(1, stem.sampleRate / 100)
        val windows = (stem.frames + win - 1) / win
        if (windows == 0 || max <= 0) return emptyList()
        val power = DoubleArray(windows) { w ->
            val from = w * win * ch
            val to = min(stem.data.size, (w + 1) * win * ch)
            var sum = 0.0
            for (i in from until to) sum += stem.data[i].toDouble() * stem.data[i]
            sum / max(1, to - from)
        }
        val loudest = power.maxOrNull() ?: 0.0
        if (loudest <= 1e-8) return emptyList()
        val loud = BooleanArray(windows) { power[it] > loudest * 0.01 }  // a tenth of the level
        val gap = 25  // 250 ms of quiet part two phrases
        val shortest = 30  // 300 ms
        val found = ArrayList<LongRange>()
        var w = 0
        while (w < windows) {
            if (!loud[w]) { w++; continue }
            val a = w
            var end = w + 1
            var quiet = 0
            while (w < windows && quiet < gap) {
                if (loud[w]) { end = w + 1; quiet = 0 } else quiet++
                w++
            }
            if (end - a >= shortest) found += (a.toLong() * win) until min(stem.frames.toLong(), end.toLong() * win)
        }
        return found.sortedByDescending { it.last - it.first }.take(max).sortedBy { it.first }
    }
}
