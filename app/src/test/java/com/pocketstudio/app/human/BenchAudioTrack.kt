package com.pocketstudio.app.human

import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.shadows.ShadowAudioTrack

/**
 * Android's player of a short sound as a phone has it, for the « testeur humain » (audit ultra tour 0, seat a:
 * every « Écouter » of the assistant said « Cette écoute n'a pas pu être préparée »): Robolectric's track takes
 * no float samples (the app's previews are float), so the app was told the phone refused to play them, and its
 * end-of-sound marker never came. Here a float write is taken whole and the marker comes once the sound's
 * length has gone by on the main thread's clock (the bench's « attends »). Bench only.
 *
 * R.1 review (G1): a sound stopped by the app before its end is counted with its cause ([cutsThrough]), apart from one
 * that just ended: a single gesture can move the bench's clock by 26 s, past a 15 s demo, so « no longer heard » proves nothing.
 */
@Implements(AudioTrack::class)
class BenchAudioTrack : ShadowAudioTrack() {
    @RealObject private lateinit var track: AudioTrack
    private var marker = 0
    private var listener: AudioTrack.OnPlaybackPositionUpdateListener? = null
    /** Its end marker came: a stop after it is the sound's end, not the app cutting it. */
    private var ended = false
    /** Counted once (release() stops it again). */
    private var cut = false

    /** AudioTrack.stop() reaches the player here: counted when the sound had not reached its end. */
    @Implementation
    protected fun native_stop() {
        if (!ended && !cut) {
            cut = true
            // What cut it, for a guard that wants one cause (the app has several: a page left, its ViewModel cleared).
            causes.add(Throwable().stackTrace.map { it.methodName }.toSet())
        }
    }

    @Implementation
    protected fun native_write_float(audioData: FloatArray, offsetInFloats: Int, sizeInFloats: Int, format: Int, isBlocking: Boolean): Int = sizeInFloats

    @Implementation
    protected fun native_set_marker_pos(marker: Int): Int {
        this.marker = marker
        return AudioTrack.SUCCESS
    }

    @Implementation
    protected fun setPlaybackPositionUpdateListener(listener: AudioTrack.OnPlaybackPositionUpdateListener?, handler: Handler?) {
        this.listener = listener
    }

    @Implementation
    override fun play() {
        super.play()
        val l = listener ?: return
        if (marker <= 0) return
        val ms = marker * 1000L / track.sampleRate.coerceAtLeast(1)
        Handler(Looper.getMainLooper()).postDelayed({ ended = true; l.onMarkerReached(track) }, ms)
    }

    companion object {
        /** For each sound the app stopped before its end (since the JVM started), the methods on the stack then. */
        private val causes = java.util.concurrent.CopyOnWriteArrayList<Set<String>>()

        /** Sounds the app stopped before their end so far (compare two reads). */
        @JvmStatic fun cutsBeforeTheEnd(): Int = causes.size

        /** Of those, the ones stopped through [method] (e.g. « stopDemoPreview »). */
        @JvmStatic fun cutsThrough(method: String): Int = causes.count { method in it }
    }
}
