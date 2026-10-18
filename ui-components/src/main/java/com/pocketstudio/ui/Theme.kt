package com.pocketstudio.ui

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Palette: calm dark blue-grey, one soft accent, one "playing" colour (see docs/DESIGN.md). */
object Neon {
    val Background = Color(0xFF101318)
    val Surface = Color(0xFF181C23)
    val SurfaceHigh = Color(0xFF222731)
    val Outline = Color(0xFF2E3440)
    val Cyan = Color(0xFF5BD6E3)
    val Magenta = Color(0xFFE879B9)
    val Acid = Color(0xFF9ED36A)
    val Amber = Color(0xFFF2B84B)
    val Red = Color(0xFFEF6F7E)
    val TextPrimary = Color(0xFFECEFF4)
    val TextSecondary = Color(0xFF9AA3B2)
}

private val colors = darkColorScheme(
    primary = Neon.Cyan,
    onPrimary = Color.Black,
    secondary = Neon.Magenta,
    onSecondary = Color.Black,
    tertiary = Neon.Acid,
    onTertiary = Color.Black,
    background = Neon.Background,
    onBackground = Neon.TextPrimary,
    surface = Neon.Surface,
    onSurface = Neon.TextPrimary,
    surfaceVariant = Neon.SurfaceHigh,
    onSurfaceVariant = Neon.TextSecondary,
    surfaceContainer = Neon.Surface,
    surfaceContainerHigh = Neon.SurfaceHigh,
    outline = Neon.Outline,
    error = Neon.Red,
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
fun PocketStudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography) {
        CompositionLocalProvider(LocalContentColor provides Neon.TextPrimary, content = content)
    }
}
