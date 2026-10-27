package com.pocketstudio.app.synth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.pocketstudio.model.i18n.tr
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How long a note or chord played from TalkBack's double tap sounds. */
internal const val ACCESSIBLE_TAP_MS = 400L

/**
 * Audit final (22): a key, a scale key or a chord pad played by touch only (press and release)
 * is named for TalkBack and plays with its double tap: [start], then [stop] a moment later.
 */
@Composable
internal fun Modifier.playable(name: String, enabled: Boolean = true, start: () -> Unit, stop: () -> Unit): Modifier {
    val scope = rememberCoroutineScope()
    val label = tr("Jouer")
    return semantics(mergeDescendants = true) {
        contentDescription = name
        role = Role.Button
        if (enabled) {
            onClick(label) {
                start()
                scope.launch {
                    delay(ACCESSIBLE_TAP_MS)
                    stop()
                }
                true
            }
        } else {
            disabled()
        }
    }
}
