package com.pocketstudio.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
        background = Color(0xFF0E1116), surface = Color(0xFF181C24), surfaceHigh = Color(0xFF232833),
        outline = Color(0xFF2C3341), accent = Color(0xFF5BD6E3), playing = Color(0xFFF07AC0),
        good = Color(0xFF9ED36A), warning = Color(0xFFF2B84B), bad = Color(0xFFEF6F7E),
        textPrimary = Color(0xFFEEF1F6), textSecondary = Color(0xFF9CA6B6), onAccent = Color(0xFF0B0D10),
    )

    /**
     * Default look (Benjamin's choice): inspired by FL Studio Mobile, but lighter. Graphite panels
     * that stand out from a deeper background, a vivid orange accent, green for "playing".
     */
    val Studio = Palette(
        isDark = true,
        background = Color(0xFF2B2F35), surface = Color(0xFF41464E), surfaceHigh = Color(0xFF50565F),
        outline = Color(0xFF5A616B), accent = Color(0xFFFF9F43), playing = Color(0xFF6EE07A),
        good = Color(0xFF8FE07A), warning = Color(0xFFFFD166), bad = Color(0xFFFF6B6B),
        textPrimary = Color(0xFFF6F7F9), textSecondary = Color(0xFFBBC1CA), onAccent = Color(0xFF1C1E22),
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
    val OnAccent: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.onAccent
}

private fun colorScheme(p: Palette) = (if (p.isDark) darkColorScheme() else lightColorScheme()).copy(
    primary = p.accent,
    onPrimary = p.onAccent,
    primaryContainer = p.accent.copy(alpha = 0.22f),
    onPrimaryContainer = p.textPrimary,
    secondary = p.playing,
    onSecondary = p.onAccent,
    tertiary = p.good,
    background = p.background,
    onBackground = p.textPrimary,
    surface = p.surface,
    onSurface = p.textPrimary,
    surfaceVariant = p.surfaceHigh,
    secondaryContainer = p.accent,
    onSecondaryContainer = p.onAccent,
    onSurfaceVariant = p.textSecondary,
    surfaceContainerLowest = p.background,
    surfaceContainerLow = p.surface,
    surfaceContainer = p.surface,
    surfaceContainerHigh = p.surfaceHigh,
    surfaceContainerHighest = p.surfaceHigh,
    outline = p.outline,
    outlineVariant = p.outline,
    error = p.bad,
)

/** Inter (SIL OFL 1.1): legible at small sizes, figures of one width for tempos and decibels. */
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_extrabold, FontWeight.ExtraBold),
)

private val typography = Typography().run {
    fun TextStyle.inter(weight: FontWeight, spacing: Float? = null) =
        copy(fontFamily = Inter, fontWeight = weight, letterSpacing = spacing?.sp ?: letterSpacing)
    copy(
        displayLarge = displayLarge.inter(FontWeight.ExtraBold, -1f),
        displayMedium = displayMedium.inter(FontWeight.ExtraBold, -0.8f),
        displaySmall = displaySmall.inter(FontWeight.Bold, -0.5f),
        headlineLarge = headlineLarge.inter(FontWeight.ExtraBold, -0.5f),
        headlineMedium = headlineMedium.inter(FontWeight.ExtraBold, -0.4f),
        headlineSmall = headlineSmall.inter(FontWeight.Bold, -0.3f),
        titleLarge = titleLarge.inter(FontWeight.Bold, -0.2f),
        titleMedium = titleMedium.inter(FontWeight.SemiBold, 0f),
        titleSmall = titleSmall.inter(FontWeight.SemiBold, 0f),
        bodyLarge = bodyLarge.inter(FontWeight.Normal, 0f),
        bodyMedium = bodyMedium.inter(FontWeight.Normal, 0f),
        bodySmall = bodySmall.inter(FontWeight.Normal, 0.1f),
        labelLarge = labelLarge.inter(FontWeight.SemiBold, 0.1f),
        labelMedium = labelMedium.inter(FontWeight.SemiBold, 0.3f),
        labelSmall = labelSmall.inter(FontWeight.Bold, 1.3f),
    )
}

/** Chips are pills; cards and sheets have soft corners. */
private val shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(percent = 50),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/** Numbers that change (tempo, decibels, times): tabular figures, so they do not jiggle. */
val MonoStyle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, fontFeatureSettings = "tnum")

@Composable
fun PocketStudioTheme(palette: Palette = Palettes.Studio, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = colorScheme(palette), typography = typography, shapes = shapes) {
            CompositionLocalProvider(LocalContentColor provides palette.textPrimary, content = content)
        }
    }
}

/** Instrument track colours (InstrumentTrack.color indexes them): distinct on light and dark. */
val TrackColors = listOf(
    Color(0xFF4FC3F7), Color(0xFFFF8A65), Color(0xFF81C784), Color(0xFFBA68C8),
    Color(0xFFFFD54F), Color(0xFF4DB6AC), Color(0xFFF06292), Color(0xFF9575CD),
)

fun trackColor(index: Int): Color = TrackColors[Math.floorMod(index, TrackColors.size)]
