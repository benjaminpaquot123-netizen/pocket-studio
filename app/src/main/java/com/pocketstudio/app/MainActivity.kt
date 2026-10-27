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
            if (start > 0L) Intro.splashElapsedMs = (android.os.SystemClock.uptimeMillis() - start).coerceIn(0L, 1000L)
            view.remove()
        }
        enableEdgeToEdge()
        // Ask the system to keep CPU clocks stable instead of bursting and throttling.
        runCatching { window.setSustainedPerformanceMode(true) }
        SettingsStore.load(this)
        // The first start: the phone's language when the app has it (English otherwise).
        SettingsStore.adoptPhoneLanguage(this, resources.configuration.locales[0])
        // The intro at each start of the app, not when the screen is rebuilt (rotation, new language).
        if (savedInstanceState == null) Intro.playing = true
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
                        Box {
                            // The app starts underneath at once (project, audio engine): the intro waits for nothing.
                            com.pocketstudio.app.learn.LearnLayer { AppRoot() }
                            if (Intro.playing) {
                                BrandIntro(onDone = { Intro.playing = false })
                                // Gone with the screen (rotated, closed): never left « playing ».
                                androidx.compose.runtime.DisposableEffect(Unit) { onDispose { Intro.playing = false } }
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

    private object NoHaptics : HapticFeedback {
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) = Unit
    }
}
