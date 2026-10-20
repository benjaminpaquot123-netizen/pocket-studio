package com.pocketstudio.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon

class TopBarUi(
    val projectName: String,
    val tempoBpm: Double,
    val playing: Boolean,
    val canUndo: Boolean,
    val canRedo: Boolean,
    /** Live recording armed: what is played is written into the loop. */
    val recording: Boolean = false,
)

class TopBarActions(
    val projects: () -> Unit = {},
    val undo: () -> Unit = {},
    val redo: () -> Unit = {},
    val togglePlay: () -> Unit = {},
    val toggleRecord: () -> Unit = {},
)

/** Always on top: the open project (tap for "Mes projets"), undo/redo, tempo and play, on every screen. */
@Composable
fun TopBar(ui: TopBarUi, actions: TopBarActions) {
    Row(
        Modifier.fillMaxWidth().background(Neon.Surface).statusBarsPadding().height(52.dp).padding(horizontal = 4.dp).testTag("top_bar"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).clickable(onClick = actions.projects).padding(vertical = 8.dp).testTag("top_projects"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.FolderOpen, contentDescription = "Mes projets", tint = Neon.Cyan, modifier = Modifier.padding(horizontal = 8.dp))
            Text(ui.projectName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = actions.undo, enabled = ui.canUndo, modifier = Modifier.testTag("top_undo")) {
            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Annuler")
        }
        IconButton(onClick = actions.redo, enabled = ui.canRedo, modifier = Modifier.testTag("top_redo")) {
            Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Rétablir")
        }
        // Record: ● red while armed (blinking), grey otherwise.
        val blink = androidx.compose.animation.core.rememberInfiniteTransition(label = "rec")
        val alpha by blink.animateFloat(
            1f, 0.35f,
            androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(500), androidx.compose.animation.core.RepeatMode.Reverse),
            label = "recAlpha",
        )
        IconButton(onClick = actions.toggleRecord, modifier = Modifier.testTag("top_record")) {
            Icon(
                Icons.Filled.FiberManualRecord,
                contentDescription = if (ui.recording) "Arrêter l'enregistrement" else "Enregistrer ce que je joue",
                tint = if (ui.recording) Neon.Red.copy(alpha = alpha) else Neon.TextSecondary,
            )
        }
        Text("${ui.tempoBpm.toInt()} BPM", style = MonoStyle, color = Neon.TextSecondary, modifier = Modifier.padding(horizontal = 4.dp))
        IconButton(onClick = actions.togglePlay, modifier = Modifier.testTag("top_play")) {
            Icon(
                if (ui.playing) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = if (ui.playing) "Arrêter" else "Jouer",
                tint = if (ui.playing) Neon.Acid else Neon.Cyan,
            )
        }
    }
}
