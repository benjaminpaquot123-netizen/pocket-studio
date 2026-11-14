package com.pocketstudio.app.onboarding

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * What the opening does with its two heartbeats besides drawing them. Owner (2026-10-29): the
 * heartbeat is felt, never heard (no sound at all at the opening): two strong taps of the phone's
 * vibrator, on the drawn beats.
 */
enum class IntroCueMode {
    /** The two taps on the beats. */
    Taps,

    /** Nothing: silent phone, animations off, the heartbeat or the vibrations off in Réglages. */
    Quiet,
}

/** Réglages › Battement à l'ouverture and › Vibrations, the animations and the phone's silent mode decide. */
fun introCueMode(reducedMotion: Boolean, beatOn: Boolean, hapticsOn: Boolean, ringerMode: Int): IntroCueMode = when {
    reducedMotion || !beatOn || !hapticsOn -> IntroCueMode.Quiet
    ringerMode == AudioManager.RINGER_MODE_SILENT -> IntroCueMode.Quiet
    else -> IntroCueMode.Taps
}

/** The opening's feedback, driven by its timeline (BrandIntro calls these on the main thread). */
interface IntroCue {
    /** The timeline reached [IntroTimeline.SOUND_START] (at [atMs]). */
    fun start(atMs: Float) {}

    /** Heartbeat [k] (0 or 1) is drawn now. */
    fun beat(k: Int) {}

    /** Skipped or gone. */
    fun stop() {}

    companion object {
        val None = object : IntroCue {}

        /**
         * The cue for this start of the app, decided once in MainActivity.onCreate. The taps go
         * through the vibrator itself (not the view's touch feedback, which the phone's « vibrate
         * on touch » setting can silence: the owner felt nothing): a strong « lub », a lighter « dub ».
         */
        fun forLaunch(context: Context, reducedMotion: Boolean, beatOn: Boolean, hapticsOn: Boolean): IntroCue {
            val audio = context.getSystemService(AudioManager::class.java) ?: return None
            val vibrator = runCatching { context.getSystemService(VibratorManager::class.java)?.defaultVibrator }.getOrNull()
            if (vibrator == null || !vibrator.hasVibrator()) return None
            val mode = runCatching { introCueMode(reducedMotion, beatOn, hapticsOn, audio.ringerMode) }.getOrDefault(IntroCueMode.Quiet)
            if (mode == IntroCueMode.Quiet) return None
            return object : IntroCue {
                override fun beat(k: Int) {
                    runCatching { tap(vibrator, k) }
                }

                override fun stop() {
                    runCatching { vibrator.cancel() }
                }
            }
        }

        /** Beat [k]: 55 ms at full strength (« lub »), then 35 ms lighter (« dub »). */
        private fun tap(vibrator: Vibrator, k: Int) {
            val ms = if (k == 0) 55L else 35L
            val amplitude = if (!vibrator.hasAmplitudeControl()) VibrationEffect.DEFAULT_AMPLITUDE else if (k == 0) 255 else 170
            val effect = VibrationEffect.createOneShot(ms, amplitude)
            if (Build.VERSION.SDK_INT >= 33) {
                vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_MEDIA))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
            }
        }
    }
}
