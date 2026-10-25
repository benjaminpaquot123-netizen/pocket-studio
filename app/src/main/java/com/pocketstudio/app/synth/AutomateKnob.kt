package com.pocketstudio.app.synth

import com.pocketstudio.ui.ScaledDropdownMenu

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import com.pocketstudio.model.i18n.tr

/**
 * Phase 37: a knob's own menu on a long press on its name: « Automatiser » makes (or opens) the
 * lane of that setting. Only the name takes the long press (the slider stays free for the
 * finger), and it is a wrapper of its own: other hints on a knob (MIDI learn, phase 39) sit
 * beside it. [automate] null: the knob has no menu (nothing is wrapped).
 */
@Composable
fun AutomatableKnob(automate: (() -> Unit)?, tag: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    if (automate == null) {
        Box(modifier) { content() }
        return
    }
    var open by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val label = tr("Automatiser")
    val latest by rememberUpdatedState(automate)
    Box(
        modifier
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    open = true
                })
            }
            .semantics { onLongClick(label) { open = true; true } }
            .testTag(tag),
    ) {
        content()
        ScaledDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(label) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null) },
                onClick = { open = false; latest() },
                modifier = Modifier.testTag("knob_automate"),
            )
        }
    }
}
