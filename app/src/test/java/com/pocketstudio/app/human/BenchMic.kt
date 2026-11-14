package com.pocketstudio.app.human

import android.os.SystemClock
import org.robolectric.shadows.ShadowAudioRecord
import kotlin.math.PI
import kotlin.math.sin

/**
 * The phone's microphone for the « testeur humain » (audit ultra round 1, lot banc: « Enregistrer » never recorded,
 * A d, C c, E b): Robolectric's AudioRecord hands back silence at once, as fast as it is read (30 s of nothing in a
 * blink: « Rien n'a été entendu »). Here each read waits for the bench's clock (the main thread's, which « attends »
 * and every gesture move) and gives a voice singing near the phone: a 220 Hz tone in syllables (0,3 s sung, 0,2 s of
 * breath) at about −12 dBFS. What the app's [com.pocketstudio.app.audio.MicRecorder] records: « Enregistrer ma voix »
 * on a pad, a live pad, « Chante ou siffle » to notes, an audio cell of the Live. Bench only.
 *
 * The audio tracks' takes do not go through Android's AudioRecord: the engine opens the microphone itself
 * (NativeEngine.micStart / micStatus / micFinish): the bench's engine gives the same voice there ([BenchEngine]).
 */
internal object BenchMic {
    /** Frames delivered to the recorders since they started (tests). */
    @Volatile var delivered = 0L
        private set

    /** One source a recorder: Robolectric asks the provider again at each read (a new one would start again from 0). */
    private val sources = java.util.WeakHashMap<android.media.AudioRecord, Source>()

    /** Microphones the app opened and read (tests: the recorder runs off the main thread, a moment after its button). */
    val opened: Int get() = synchronized(sources) { sources.size }

    fun plugIn() = ShadowAudioRecord.setSourceProvider { rec -> synchronized(sources) { sources.getOrPut(rec) { Source() } } }

    fun unplug() {
        ShadowAudioRecord.clearSource()
        synchronized(sources) { sources.clear() }
    }

    /** One microphone opened by the app: from its first read, the voice as time goes by. */
    private class Source : ShadowAudioRecord.AudioRecordSource {
        private var startMs = -1L
        private var frame = 0L

        override fun readInFloatArray(audioData: FloatArray, offsetInFloats: Int, sizeInFloats: Int, isBlocking: Boolean): Int {
            if (startMs < 0) startMs = SystemClock.uptimeMillis()
            // What the phone has heard by now (mono, 48 kHz); a blocking read waits for some, as the real one does.
            val until = System.currentTimeMillis() + 2_000
            var ready = available()
            while (ready <= 0 && isBlocking && System.currentTimeMillis() < until) {
                Thread.sleep(5)
                ready = available()
            }
            val n = minOf(sizeInFloats.toLong(), ready.coerceAtLeast(0)).toInt()
            for (i in 0 until n) audioData[offsetInFloats + i] = voice(frame + i)
            frame += n
            delivered += n
            return n
        }

        override fun readInShortArray(audioData: ShortArray, offsetInShorts: Int, sizeInShorts: Int, isBlocking: Boolean): Int {
            val f = FloatArray(sizeInShorts)
            val n = readInFloatArray(f, 0, sizeInShorts, isBlocking)
            for (i in 0 until n) audioData[offsetInShorts + i] = (f[i] * 32767f).toInt().toShort()
            return n
        }

        private fun available(): Long = (SystemClock.uptimeMillis() - startMs) * RATE / 1000 - frame
    }

    private const val RATE = 48_000
    /** The voice's peak (the input level a take shows). */
    const val PEAK = 0.25f

    /** The voice at [frame]: syllables of a 220 Hz tone, soft attacks. */
    fun voice(frame: Long): Float {
        val inSyllable = frame % (RATE / 2)
        if (inSyllable >= RATE * 3 / 10) return 0f
        val attack = minOf(1f, inSyllable / 480f)
        return (PEAK * attack * sin(2 * PI * 220.0 * frame / RATE)).toFloat()
    }
}
