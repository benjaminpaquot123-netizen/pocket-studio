package com.pocketstudio.app.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable

/**
 * Réglages › Taille de l'interface: everything is drawn [scale] times bigger, the windows of their
 * own too (dialogs, sheets, menus: [com.pocketstudio.ui.RescaledWindow]). See [com.pocketstudio.ui.UiScaled].
 */
@Composable
fun UiScaled(scale: Float, content: @Composable () -> Unit) = com.pocketstudio.ui.UiScaled(scale, content)

/** [base] as it looks once each dp is [scale] times bigger. */
fun scaledConfiguration(base: Configuration, scale: Float): Configuration = com.pocketstudio.ui.scaledConfiguration(base, scale)
