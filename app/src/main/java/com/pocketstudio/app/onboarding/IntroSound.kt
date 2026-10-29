package com.pocketstudio.app.onboarding

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * What the opening does with its two heartbeats besides drawing them. Owner (2026-10-29): the
 * heartbeat is felt, not heard (two taps of the phone's vibration on the drawn beats), and only the
 * small « ting » when the light runs over the name is heard (assets/intro/ting.wav).
 */
enum class IntroCueMode {
    /** The two taps on the beats, and the ting. */
    Sound,

    /** The ting alone: vibrations off in Réglages. */
    Ting,

    /** The two taps alone: the phone is on vibrate, the opening's sound is off, or the media is muted or busy. */
    Taps,

    /** Nothing: silent phone, animations off, or neither sound nor vibrations allowed. */
    Quiet,
}

/**
 * The phone's state and the app's settings decide: never a sound with the phone on silent or
 * vibrate, never over the music of another app, never anything with the animations off (the still
 * image fades out silently); the taps follow Réglages › Vibrations, the ting Réglages › Son
 * d'ouverture.
 */
fun introCueMode(
    reducedMotion: Boolean,
    soundOn: Boolean,
    hapticsOn: Boolean,
    ringerMode: Int,
    mediaVolume: Int,
    otherMusic: Boolean,
): IntroCueMode {
    if (reducedMotion || ringerMode == AudioManager.RINGER_MODE_SILENT) return IntroCueMode.Quiet
    val ting = soundOn && ringerMode != AudioManager.RINGER_MODE_VIBRATE && mediaVolume > 0 && !otherMusic
    return when {
        ting && hapticsOn -> IntroCueMode.Sound
        ting -> IntroCueMode.Ting
        hapticsOn -> IntroCueMode.Taps
        else -> IntroCueMode.Quiet
    }
}

/** The opening's feedback, driven by its timeline (BrandIntro calls these on the main thread). */
interface IntroCue {
    /** The timeline reached [IntroTimeline.SOUND_START] (at [atMs]): the sound starts, in step. */
    fun start(atMs: Float) {}

    /** Heartbeat [k] (0 or 1) is drawn now. */
    fun beat(k: Int) {}

    /** Skipped or gone: a short fade, then everything is let go. */
    fun stop() {}

    companion object {
        val None = object : IntroCue {}

        /**
         * The cue for this start of the app, decided once in MainActivity.onCreate; the sound is
         * read and made ready on its own thread at once, so it never holds the opening up.
         */
        fun forLaunch(context: Context, view: () -> View, reducedMotion: Boolean, soundOn: Boolean, hapticsOn: Boolean): IntroCue {
            val audio = context.getSystemService(AudioManager::class.java) ?: return None
            val mode = runCatching {
                introCueMode(reducedMotion, soundOn, hapticsOn, audio.ringerMode, audio.getStreamVolume(AudioManager.STREAM_MUSIC), audio.isMusicActive)
            }.getOrDefault(IntroCueMode.Quiet)
            val taps = { k: Int ->
                // Soft: a key's tick, then a lighter clock tick for the « dub ».
                view().performHapticFeedback(if (k == 0) HapticFeedbackConstants.VIRTUAL_KEY else HapticFeedbackConstants.CLOCK_TICK)
                Unit
            }
            return when (mode) {
                IntroCueMode.Sound, IntroCueMode.Ting -> {
                    val sound = IntroSound(context)
                    val felt = mode == IntroCueMode.Sound
                    object : IntroCue {
                        override fun start(atMs: Float) = sound.play(atMs)
                        override fun beat(k: Int) { if (felt) taps(k) }
                        override fun stop() = sound.stop()
                    }
                }
                IntroCueMode.Taps -> object : IntroCue {
                    override fun beat(k: Int) = taps(k)
                }
                IntroCueMode.Quiet -> None
            }
        }
    }
}

/** A WAV file's 16-bit PCM and its cue points (frames), as scripts/brand/render_heartbeat.py writes it (--ting). */
class Wav(val sampleRate: Int, val channels: Int, val bytes: ByteArray, val dataOffset: Int, val dataLength: Int, val cues: IntArray) {
    val frames: Int get() = dataLength / (2 * channels)

    /** Sample [i] of channel 0, -1..1. */
    fun sample(i: Int): Float {
        val p = dataOffset + i * 2 * channels
        return ((bytes[p].toInt() and 0xFF) or (bytes[p + 1].toInt() shl 8)).toShort() / 32768f
    }

    companion object {
        /** Walks the RIFF chunks (a cue point names the « data » chunk too: never search for it). */
        fun parse(b: ByteArray): Wav {
            fun u32(p: Int) = (b[p].toInt() and 0xFF) or ((b[p + 1].toInt() and 0xFF) shl 8) or ((b[p + 2].toInt() and 0xFF) shl 16) or ((b[p + 3].toInt() and 0xFF) shl 24)
            fun u16(p: Int) = (b[p].toInt() and 0xFF) or ((b[p + 1].toInt() and 0xFF) shl 8)
            fun id(p: Int) = String(b, p, 4, Charsets.US_ASCII)
            require(b.size >= 12 && id(0) == "RIFF" && id(8) == "WAVE") { "not a WAV file" }
            var rate = 0
            var channels = 0
            var bits = 0
            var data = -1
            var length = 0
            var cues = IntArray(0)
            var p = 12
            while (p + 8 <= b.size) {
                val size = u32(p + 4)
                val body = p + 8
                require(size >= 0 && body + size <= b.size) { "truncated chunk ${id(p)}" }
                when (id(p)) {
                    "fmt " -> {
                        require(u16(body) == 1) { "PCM only" }
                        channels = u16(body + 2)
                        rate = u32(body + 4)
                        bits = u16(body + 14)
                    }
                    "cue " -> cues = IntArray(u32(body)) { u32(body + 4 + it * 24 + 20) }
                    "data" -> {
                        data = body
                        length = size
                    }
                }
                p = body + size + (size and 1)
            }
            require(data >= 0 && bits == 16 && channels > 0 && rate > 0) { "16-bit PCM data expected" }
            return Wav(rate, channels, b, data, length, cues)
        }
    }
}

/**
 * The opening's sound, played by Android (an AudioTrack, static, low latency) beside the audio
 * engine, never through it: nothing touches the engine's audio thread. Read, made ready, started
 * and let go on its own small thread (one per start of the app, gone once it has played), so the
 * main thread never waits on it. Media volume; never started late (if it is not ready when the
 * first beat is near, the opening stays silent).
 */
class IntroSound(context: Context, private val asset: String = ASSET) {
    private val thread = HandlerThread("intro-sound").also { it.start() }
    private val handler = Handler(thread.looper)

    // The sound's thread only.
    private var track: AudioTrack? = null
    private var frames = 0
    private var started = false
    private var done = false

    init {
        val app = context.applicationContext
        handler.post {
            val wav = runCatching { Wav.parse(app.assets.open(asset).use { it.readBytes() }) }.getOrNull()
            if (wav == null || done) return@post
            frames = wav.frames
            track = runCatching {
                AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    .setAudioFormat(
                        AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(wav.sampleRate)
                            .setChannelMask(if (wav.channels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO).build(),
                    )
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(wav.dataLength)
                    .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                    .build()
                    .also { it.write(wav.bytes, wav.dataOffset, wav.dataLength) }
            }.getOrNull()
        }
    }

    /**
     * The timeline is at [atMs] (≥ [IntroTimeline.SOUND_START]): start the file at the frame that
     * plays now, so each beat lands on its drawn beat (the time the message takes to reach the
     * sound's thread, a file still being read, are counted in).
     */
    fun play(atMs: Float) {
        val asked = SystemClock.elapsedRealtimeNanos()
        handler.post {
            if (started) return@post
            val t = track
            if (t == null || done) {
                letGo()
                return@post
            }
            val now = atMs + (SystemClock.elapsedRealtimeNanos() - asked) / 1e6f
            val from = IntroTimeline.soundFrame(now).coerceAtLeast(0)
            if (now > IntroTimeline.SOUND_LATEST || from >= frames) {
                letGo()
                return@post
            }
            runCatching {
                t.setPlaybackHeadPosition(from)
                t.play()
                started = true
            }.onFailure { letGo() }
            // Let go once it has played out.
            handler.postDelayed({ letGo() }, (frames - from) * 1000L / IntroTimeline.SAMPLE_RATE + 250L)
        }
    }

    /** Skipped or gone: a short fade if it plays (no click), then let go. */
    fun stop() {
        if (!thread.isAlive) return
        handler.post {
            if (done) return@post
            if (track == null || !started) {
                letGo()
                return@post
            }
            done = true
            for (k in 1..FADE_STEPS) {
                handler.postDelayed({ runCatching { track?.setVolume(1f - k.toFloat() / FADE_STEPS) } }, k * FADE_STEP_MS)
            }
            handler.postDelayed({ letGo() }, (FADE_STEPS + 1) * FADE_STEP_MS)
        }
    }

    /** On the sound's thread: the track and the thread go. */
    private fun letGo() {
        done = true
        track?.let { runCatching { it.stop() }; it.release() }
        track = null
        thread.quitSafely()
    }

    companion object {
        const val ASSET = "intro/ting.wav"

        /** Volume steps of the fade when skipped (every [FADE_STEP_MS]). */
        private const val FADE_STEPS = 8
        private const val FADE_STEP_MS = 15L
    }
}
