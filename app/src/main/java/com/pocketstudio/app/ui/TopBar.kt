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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.sp
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
        Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(lerp(Neon.Surface, Color.Black, 0.12f), lerp(Neon.Background, Color.Black, 0.1f))))
            .drawBehind { drawLine(Color.White.copy(alpha = 0.07f), Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), 1f) }
            .statusBarsPadding().height(56.dp).padding(horizontal = 6.dp).testTag("top_bar"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(onClick = actions.projects).padding(vertical = 6.dp, horizontal = 4.dp)
                .testTag("top_projects"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(Neon.Cyan, lerp(Neon.Cyan, Color.Black, 0.3f)))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.FolderOpen, contentDescription = "Mes projets", tint = Neon.OnAccent, modifier = Modifier.size(18.dp))
            }
            Text(ui.projectName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = actions.undo, enabled = ui.canUndo, modifier = Modifier.testTag("top_undo")) {
            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Annuler")
        }
        IconButton(onClick = actions.redo, enabled = ui.canRedo, modifier = Modifier.testTag("top_redo")) {
            Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Rétablir")
        }
        // Record: a red dot, pulsing and glowing while armed.
        val blink = rememberInfiniteTransition(label = "rec")
        val alpha by blink.animateFloat(1f, 0.35f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "recAlpha")
        IconButton(onClick = actions.toggleRecord, modifier = Modifier.testTag("top_record")) {
            Box(
                Modifier.size(30.dp).clip(CircleShape)
                    .background(if (ui.recording) Neon.Red.copy(alpha = 0.18f) else Color.Transparent)
                    .border(1.5.dp, if (ui.recording) Neon.Red.copy(alpha = alpha) else Neon.TextSecondary.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.FiberManualRecord,
                    contentDescription = if (ui.recording) "Arrêter l'enregistrement" else "Enregistrer ce que je joue",
                    tint = if (ui.recording) Neon.Red.copy(alpha = alpha) else Neon.Red.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        // The tempo in a small dark window, like a hardware display.
        Row(
            Modifier.padding(horizontal = 4.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.35f))
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text("${ui.tempoBpm.toInt()}", style = MonoStyle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = Neon.Cyan)
            Text(" BPM", style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary)
        }
        val playColor = if (ui.playing) Neon.Acid else Neon.Cyan
        Box(
            Modifier.padding(start = 4.dp, end = 2.dp).size(42.dp)
                .shadow(if (ui.playing) 14.dp else 6.dp, CircleShape, ambientColor = playColor, spotColor = playColor)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(lerp(playColor, Color.White, 0.2f), playColor)))
                .clickable(role = Role.Button, onClickLabel = if (ui.playing) "Arrêter" else "Jouer", onClick = actions.togglePlay)
                .testTag("top_play"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (ui.playing) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = if (ui.playing) "Arrêter" else "Jouer",
                tint = Neon.OnAccent,
            )
        }
    }
}
