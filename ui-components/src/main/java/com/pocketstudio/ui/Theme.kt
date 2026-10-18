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

object Neon {
    val Background = Color(0xFF0A0A12)
    val Surface = Color(0xFF14141F)
    val SurfaceHigh = Color(0xFF1E1E2D)
    val Outline = Color(0xFF2E2E44)
    val Cyan = Color(0xFF00E5FF)
    val Magenta = Color(0xFFFF2BD6)
    val Acid = Color(0xFFB6FF3B)
    val Amber = Color(0xFFFFB300)
    val Red = Color(0xFFFF4D6D)
    val TextPrimary = Color(0xFFF2F2F7)
    val TextSecondary = Color(0xFF9A9AB0)
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
