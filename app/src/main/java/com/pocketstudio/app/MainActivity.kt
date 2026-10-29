package com.pocketstudio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.layout.Box
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.pocketstudio.app.onboarding.BrandIntro
import com.pocketstudio.app.onboarding.Intro
import com.pocketstudio.ui.layoutDirectionOf
import com.pocketstudio.app.settings.AppTheme
import com.pocketstudio.app.settings.FoldPrefs
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.app.ui.AppRoot
import com.pocketstudio.app.ui.UiScaled
import com.pocketstudio.ui.ColorblindPalette
import com.pocketstudio.ui.LocalFoldStore
import com.pocketstudio.ui.Palettes
import com.pocketstudio.ui.PocketStudioTheme

class MainActivity : ComponentActivity() {
    /** What the opening plays on its two heartbeats at this start: the sound, two soft taps, or nothing. */
    private var introCue: com.pocketstudio.app.onboarding.IntroCue = com.pocketstudio.app.onboarding.IntroCue.None

    /** The second touch of a double tap on a button that changed the screen is not the new screen's ([DoubleTouchGuard]). */
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        if (com.pocketstudio.app.ui.DoubleTouchGuard.drop(ev.actionMasked, ev.x, ev.y, ev.eventTime, resources.displayMetrics.density)) return true
        return super.dispatchTouchEvent(ev)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // The system splash (Android 12+): BenStudio's pads pop in and beat twice; it leaves at the
        // first frame, nothing holds it (BrandIntro takes over at the same place, the app starts under it).
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // At the hand-over, the intro goes on from where the splash's pads were (same timeline),
        // and the splash leaves at once: the intro draws the same frame under it.
        if (savedInstanceState == null) Intro.splashElapsedMs = 0L
        splash.setOnExitAnimationListener { view ->
            val start = view.iconAnimationStartMillis
            if (start > 0L) Intro.splashElapsedMs = (android.os.SystemClock.uptimeMillis() - start).coerceIn(0L, com.pocketstudio.app.onboarding.IntroTimeline.SPLASH_MS)
            view.remove()
        }
        enableEdgeToEdge()
        // Ask the system to keep CPU clocks stable instead of bursting and throttling.
        runCatching { window.setSustainedPerformanceMode(true) }
        SettingsStore.load(this)
        // The first start: the phone's language when the app has it (English otherwise).
        SettingsStore.adoptPhoneLanguage(this, resources.configuration.locales[0])
        // The intro at each start of the app, not when the screen is rebuilt (rotation, new language).
        if (savedInstanceState == null) {
            Intro.playing = true
            // The heartbeat is felt: two taps of the vibrator on the drawn beats (no sound).
            val prefs = SettingsStore.state.value
            introCue = com.pocketstudio.app.onboarding.IntroCue.forLaunch(
                this, com.pocketstudio.app.onboarding.animationsOff(this), prefs.introSound, prefs.haptics,
            )
            openAsked(intent)
        }
        val folds = FoldPrefs(this)
        setContent {
            val settings by SettingsStore.state.collectAsState()
            val palette = when (settings.theme) {
                AppTheme.Studio -> Palettes.Studio
                AppTheme.Night -> Palettes.Dark
                AppTheme.Colorblind -> ColorblindPalette
            }
            // Réglages › Taille de l'interface scales everything (and the width the screens see);
            // Vibrations off silences haptics.
            val haptics = if (settings.haptics) LocalHapticFeedback.current else NoHaptics
            UiScaled(settings.uiScale) {
                CompositionLocalProvider(
                    LocalHapticFeedback provides haptics,
                    LocalFoldStore provides folds,
                    // The interface language's direction (right to left for Arabic), not the phone's.
                    LocalLayoutDirection provides layoutDirectionOf(settings.language.rtl),
                ) {
                    PocketStudioTheme(palette) {
                        // « Mode retours »: its floating bubble over everything, once the intro is over.
                        com.pocketstudio.app.retours.RetoursHost(show = settings.retoursMode && !Intro.playing) {
                            Box {
                                // The app starts underneath at once (project read off the main thread, « Ouverture
                                // du projet… », audio engine): the intro waits for nothing.
                                com.pocketstudio.app.projects.ProjectGate { com.pocketstudio.app.learn.LearnLayer { AppRoot() } }
                                if (Intro.playing) {
                                    BrandIntro(onDone = { Intro.playing = false }, cue = introCue)
                                    // Gone with the screen (rotated, closed): never left « playing ».
                                    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { Intro.playing = false } }
                                }
                            }
                        }
                        // A new language rebuilds the screen (texts are read when drawn); recreating the
                        // activity keeps the open tab and the other saved screen state.
                        val startLanguage = remember { settings.language }
                        LaunchedEffect(settings.language) { if (settings.language != startLanguage) recreate() }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        openAsked(intent)
    }

    /** The separation's notification: « touche pour l'ouvrir » shows the Remix page. */
    private fun openAsked(intent: android.content.Intent?) {
        if (intent?.getStringExtra(com.pocketstudio.app.remix.SeparationService.EXTRA_OPEN) == com.pocketstudio.app.remix.SeparationService.OPEN_REMIX) {
            intent.removeExtra(com.pocketstudio.app.remix.SeparationService.EXTRA_OPEN)  // not again on a rotation
            com.pocketstudio.app.remix.SeparationJob.requestOpen()
        }
    }

    private object NoHaptics : HapticFeedback {
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) = Unit
    }
}
