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
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Ask the system to keep CPU clocks stable instead of bursting and throttling.
        runCatching { window.setSustainedPerformanceMode(true) }
        SettingsStore.load(this)
        if (savedInstanceState == null) openAsked(intent)
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
                ) {
                    PocketStudioTheme(palette) {
                        // The project is read off the main thread first (« Ouverture du projet… »).
                        com.pocketstudio.app.projects.ProjectGate { com.pocketstudio.app.learn.LearnLayer { AppRoot() } }
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
