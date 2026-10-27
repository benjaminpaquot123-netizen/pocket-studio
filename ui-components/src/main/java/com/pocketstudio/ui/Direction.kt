package com.pocketstudio.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * Music reads left to right in every language: time runs to the right on the piano roll, the step
 * grids, the timeline and the waveforms, and low notes sit on the left of a keyboard. In a
 * right-to-left language (Arabic) the text screens turn around, these canvases do not.
 */
@Composable
fun LeftToRight(content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr, content = content)

/** The layout direction of an interface language (right to left for Arabic, Hebrew…). */
fun layoutDirectionOf(rtl: Boolean): LayoutDirection = if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
