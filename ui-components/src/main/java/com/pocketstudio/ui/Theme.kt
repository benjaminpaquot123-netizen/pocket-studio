package com.pocketstudio.ui

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** One set of UI colours (see docs/DESIGN.md). */
data class Palette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val outline: Color,
    val accent: Color,
    val playing: Color,
    val good: Color,
    val warning: Color,
    val bad: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val onAccent: Color,
)

object Palettes {
    /** Calm dark blue-grey. */
    val Dark = Palette(
        isDark = true,
        background = Color(0xFF101318), surface = Color(0xFF181C23), surfaceHigh = Color(0xFF222731),
        outline = Color(0xFF2E3440), accent = Color(0xFF5BD6E3), playing = Color(0xFFE879B9),
        good = Color(0xFF9ED36A), warning = Color(0xFFF2B84B), bad = Color(0xFFEF6F7E),
        textPrimary = Color(0xFFECEFF4), textSecondary = Color(0xFF9AA3B2), onAccent = Color(0xFF0B0D10),
    )

    /**
     * Default look (Benjamin's choice): inspired by FL Studio Mobile, but lighter.
     * Neutral mid-grey panels, orange accent, green for "playing".
     */
    val Studio = Palette(
        isDark = true,
        background = Color(0xFF3A3E44), surface = Color(0xFF474C53), surfaceHigh = Color(0xFF555B63),
        outline = Color(0xFF626973), accent = Color(0xFFFFA347), playing = Color(0xFF7FD46B),
        good = Color(0xFF9BE07F), warning = Color(0xFFFFD166), bad = Color(0xFFFF7B7B),
        textPrimary = Color(0xFFF5F6F7), textSecondary = Color(0xFFC3C8CF), onAccent = Color(0xFF1E2023),
    )
}

/** Studio greys with Okabe-Ito colours: "playing" is blue, never told from orange by red-green alone. */
val ColorblindPalette = Palettes.Studio.copy(
    accent = Color(0xFFE69F00), playing = Color(0xFF56B4E9), good = Color(0xFF56B4E9),
    warning = Color(0xFFF0E442), bad = Color(0xFFD55E00),
)

val LocalPalette = staticCompositionLocalOf { Palettes.Studio }

/** Theme colours by role. Names kept from phase 0; values follow the active [Palette]. */
object Neon {
    val Background: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.background
    val Surface: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.surface
    val SurfaceHigh: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.surfaceHigh
    val Outline: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.outline
    val Cyan: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.accent
    val Magenta: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.playing
    val Acid: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.good
    val Amber: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.warning
    val Red: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.bad
    val TextPrimary: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.textPrimary
    val TextSecondary: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.textSecondary
}

private fun colorScheme(p: Palette) = (if (p.isDark) darkColorScheme() else lightColorScheme()).copy(
    primary = p.accent,
    onPrimary = p.onAccent,
    secondary = p.playing,
    onSecondary = p.onAccent,
    tertiary = p.good,
    background = p.background,
    onBackground = p.textPrimary,
    surface = p.surface,
    onSurface = p.textPrimary,
    surfaceVariant = p.surfaceHigh,
    secondaryContainer = p.surfaceHigh,
    onSecondaryContainer = p.textPrimary,
    onSurfaceVariant = p.textSecondary,
    surfaceContainer = p.surface,
    surfaceContainerHigh = p.surfaceHigh,
    outline = p.outline,
    error = p.bad,
)

private val typography = Typography().run {
    copy(
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        labelSmall = labelSmall.copy(letterSpacing = 1.2.sp),
    )
}

val MonoStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)

@Composable
fun PocketStudioTheme(palette: Palette = Palettes.Studio, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = colorScheme(palette), typography = typography) {
            CompositionLocalProvider(LocalContentColor provides palette.textPrimary, content = content)
        }
    }
}
