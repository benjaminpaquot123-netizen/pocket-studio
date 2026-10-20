package com.pocketstudio.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

/**
 * Plays a short sound on its own, outside the project (Android's player, not the engine): the
 * previews of a library or of a generated sound. Main thread.
 */
class PcmPlayer(private val onStop: () -> Unit = {}) {
    private var track: AudioTrack? = null

    val isPlaying: Boolean get() = track != null

    /** False when the phone refused to play it. */
    fun play(pcm: Pcm): Boolean {
        stop()
        val mask = if (pcm.channels == 2) AudioFormat.CHANNEL_OUT_STEREO else AudioFormat.CHANNEL_OUT_MONO
        val t = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_FLOAT).setSampleRate(pcm.sampleRate).setChannelMask(mask).build())
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.data.size * 4)
                .build()
        }.getOrNull() ?: return false
        if (t.write(pcm.data, 0, pcm.data.size, AudioTrack.WRITE_BLOCKING) != pcm.data.size) {
            t.release()
            return false
        }
        t.setNotificationMarkerPosition(pcm.frames)
        t.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(at: AudioTrack) {
                if (track === at) stop()
            }
            override fun onPeriodicNotification(at: AudioTrack) = Unit
        })
        track = t
        t.play()
        return true
    }

    fun stop() {
        val t = track ?: return
        track = null
        runCatching { t.stop() }
        t.release()
        onStop()
    }
}
