package com.pocketstudio.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * Réglages › Taille de l'interface (1 = 100 %), as chosen: read again inside each window of its
 * own (dialog, sheet, menu, popup), which starts over from the device's density.
 */
val LocalUiScale = staticCompositionLocalOf { 1f }

/** The density [UiScaled] gave its tree: a window that still has it needs nothing more. */
private val LocalScaledDensity = staticCompositionLocalOf { 0f }

/**
 * Réglages › Taille de l'interface: everything is drawn [scale] times bigger. The screen then holds
 * fewer dp, so the configuration seen by the screens (width, height, smallest width, density) is
 * scaled too: every « narrow phone / tablet » choice reads the room the app really has.
 */
@Composable
fun UiScaled(scale: Float, content: @Composable () -> Unit) = Scaled(scale, scale, content)

/**
 * The content of a window of its own (a Dialog, a ModalBottomSheet, a menu, a popup): Compose
 * gives it the device's density and configuration again, so the interface size is put back
 * here. Nothing changes where the size already holds (the main window, 100 %).
 */
@Composable
fun RescaledWindow(content: @Composable () -> Unit) {
    val scale = LocalUiScale.current
    val already = LocalDensity.current.density == LocalScaledDensity.current
    Scaled(if (already) 1f else scale, scale, content)
}

@Composable
private fun Scaled(apply: Float, keep: Float, content: @Composable () -> Unit) {
    // Always the same tree, at 100 % too (the locals then unchanged): moving between 100 % and
    // another size must not compose the app again from scratch (its tab, open page and dialogs).
    val density = LocalDensity.current
    val base = LocalConfiguration.current
    val scaled = remember(base, apply) { if (apply == 1f) base else scaledConfiguration(base, apply) }
    val scaledDensity = remember(density, apply) { if (apply == 1f) density else Density(density.density * apply, density.fontScale) }
    CompositionLocalProvider(
        LocalDensity provides scaledDensity,
        LocalConfiguration provides scaled,
        LocalUiScale provides keep,
        LocalScaledDensity provides scaledDensity.density,
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

/** Material's ModalBottomSheet at the interface size (its handle and its content). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaledBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        containerColor = containerColor,
        dragHandle = { RescaledWindow { BottomSheetDefaults.DragHandle() } },
    ) {
        RescaledWindow { Column(Modifier.fillMaxWidth(), content = content) }
    }
}

/** Material's AlertDialog at the interface size (each of its parts). */
@Composable
fun ScaledAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    containerColor: Color = AlertDialogDefaults.containerColor,
    properties: DialogProperties = DialogProperties(),
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = { RescaledWindow(confirmButton) },
        modifier = modifier,
        dismissButton = dismissButton?.let { d -> { RescaledWindow(d) } },
        title = title?.let { t -> { RescaledWindow(t) } },
        text = text?.let { t -> { RescaledWindow(t) } },
        containerColor = containerColor,
        properties = properties,
    )
}

/** A Dialog at the interface size. */
@Composable
fun ScaledDialog(onDismissRequest: () -> Unit, properties: DialogProperties = DialogProperties(), content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismissRequest, properties = properties) { RescaledWindow(content) }
}

/** Material's DropdownMenu at the interface size. */
@Composable
fun ScaledDropdownMenu(expanded: Boolean, onDismissRequest: () -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest, modifier = modifier) {
        RescaledWindow { Column(Modifier.width(IntrinsicSize.Max), content = content) }
    }
}

/** A Popup at the interface size. */
@Composable
fun ScaledPopup(
    alignment: Alignment = Alignment.TopStart,
    offset: IntOffset = IntOffset(0, 0),
    onDismissRequest: (() -> Unit)? = null,
    properties: PopupProperties = PopupProperties(),
    content: @Composable () -> Unit,
) {
    Popup(alignment = alignment, offset = offset, onDismissRequest = onDismissRequest, properties = properties) { RescaledWindow(content) }
}
