package com.pocketstudio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Density
import com.pocketstudio.app.settings.AppTheme
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.app.ui.AppRoot
import com.pocketstudio.ui.ColorblindPalette
import com.pocketstudio.ui.Palettes
import com.pocketstudio.ui.PocketStudioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Ask the system to keep CPU clocks stable instead of bursting and throttling.
        runCatching { window.setSustainedPerformanceMode(true) }
        SettingsStore.load(this)
        setContent {
            val settings by SettingsStore.state.collectAsState()
            val palette = when (settings.theme) {
                AppTheme.Studio -> Palettes.Studio
                AppTheme.Night -> Palettes.Dark
                AppTheme.Colorblind -> ColorblindPalette
            }
            // Réglages › Taille de l'interface scales everything; Vibrations off silences haptics.
            val density = LocalDensity.current
            val haptics = if (settings.haptics) LocalHapticFeedback.current else NoHaptics
            CompositionLocalProvider(
                LocalDensity provides Density(density.density * settings.uiScale, density.fontScale),
                LocalHapticFeedback provides haptics,
            ) {
                PocketStudioTheme(palette) {
                    AppRoot()
                }
            }
        }
    }

    private object NoHaptics : HapticFeedback {
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) = Unit
    }
}
