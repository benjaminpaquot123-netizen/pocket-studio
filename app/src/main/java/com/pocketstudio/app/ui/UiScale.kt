package com.pocketstudio.app.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * Réglages › Taille de l'interface: everything is drawn [scale] times bigger. The screen then holds
 * fewer dp, so the configuration seen by the screens (width, height, smallest width, density) is
 * scaled too: every « narrow phone / tablet » choice reads the room the app really has.
 */
@Composable
fun UiScaled(scale: Float, content: @Composable () -> Unit) {
    // Always the same tree, at 100 % too (the locals then unchanged): moving between 100 % and
    // another size must not compose the app again from scratch (its tab, open page and dialogs).
    val density = LocalDensity.current
    val base = LocalConfiguration.current
    val scaled = remember(base, scale) { if (scale == 1f) base else scaledConfiguration(base, scale) }
    val scaledDensity = remember(density, scale) { if (scale == 1f) density else Density(density.density * scale, density.fontScale) }
    CompositionLocalProvider(
        LocalDensity provides scaledDensity,
        LocalConfiguration provides scaled,
        content = content,
    )
}

/** [base] as it looks once each dp is [scale] times bigger. */
fun scaledConfiguration(base: Configuration, scale: Float): Configuration = Configuration(base).apply {
    fun shrink(dp: Int) = if (dp == Configuration.SCREEN_WIDTH_DP_UNDEFINED) dp else (dp / scale).toInt()
    screenWidthDp = shrink(base.screenWidthDp)
    screenHeightDp = shrink(base.screenHeightDp)
    smallestScreenWidthDp = shrink(base.smallestScreenWidthDp)
    densityDpi = (base.densityDpi * scale).toInt()
}
