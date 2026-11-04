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
 */
@Implements(AudioTrack::class)
class BenchAudioTrack : ShadowAudioTrack() {
    @RealObject private lateinit var track: AudioTrack
    private var marker = 0
    private var listener: AudioTrack.OnPlaybackPositionUpdateListener? = null

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
        Handler(Looper.getMainLooper()).postDelayed({ l.onMarkerReached(track) }, ms)
    }
}
