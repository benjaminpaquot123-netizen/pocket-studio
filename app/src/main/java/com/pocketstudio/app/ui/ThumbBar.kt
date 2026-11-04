package com.pocketstudio.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.ScaledDropdownMenu

/** What the thumb's bar shows: the same state as the top bar's ▶, ↶ and ↷, and the tempo. */
class ThumbBarUi(
    val playing: Boolean,
    val canUndo: Boolean,
    val canRedo: Boolean,
    val tempoBpm: Int,
    /** ↷ offered all the time (the Pro); false: only once there is something to bring back (the Mode simple). */
    val redoAlways: Boolean,
)

class ThumbBarActions(
    val undo: () -> Unit,
    val redo: () -> Unit,
    val togglePlay: () -> Unit,
    val tempo: () -> Unit,
    /** The Mode simple: the top bar's « ⋯ » (Remix, Mes projets, Réglages, the Pro…) at the thumb too; null: ⤴ Partager. */
    val more: List<TopMenuItem>?,
    val share: () -> Unit,
)

/**
 * Audit ultra round 1 (n° 21, four seats held the phone in one hand): ▶, ↶ Annuler, the tempo, Partager and the
 * Mode simple's « ⋯ » were only at the top of the screen, out of the thumb's reach. The same actions in a slim bar
 * right above the tabs (the top bar keeps them too); ↷ Rétablir there too, in the Mode simple once ↶ was used (n° 26).
 */
@Composable
fun ThumbBar(ui: ThumbBarUi, actions: ThumbBarActions) {
    Row(
        Modifier.fillMaxWidth().background(lerp(Neon.Background, Color.Black, 0.06f)).padding(horizontal = 4.dp).testTag("thumb_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ThumbButton(Icons.AutoMirrored.Filled.Undo, tr("Annuler (défaire)"), tr("Annuler la dernière action"), ui.canUndo, "thumb_undo", actions.undo)
        if (ui.redoAlways || ui.canRedo) {
            ThumbButton(Icons.AutoMirrored.Filled.Redo, tr("Rétablir"), tr("Rétablir"), ui.canRedo, "thumb_redo", actions.redo)
        }
        // ▶ / ■ in the middle, as large as the top bar's.
        val playColor = if (ui.playing) Neon.Acid else Neon.Cyan
        Box(
            Modifier.requiredSize(52.dp).clip(CircleShape)
                .clickable(role = Role.Button, onClickLabel = if (ui.playing) tr("Arrêter") else tr("Jouer"), onClick = actions.togglePlay)
                .testTag("thumb_play"),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.requiredSize(44.dp).clip(CircleShape).background(playColor), contentAlignment = Alignment.Center) {
                Icon(
                    if (ui.playing) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = if (ui.playing) tr("Arrêter") else tr("Jouer"),
                    tint = Neon.OnAccent,
                )
            }
        }
        Column(
            Modifier.widthIn(min = 64.dp).heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClickLabel = tr("Changer le tempo"), onClick = actions.tempo)
                .semantics { contentDescription = tr("Tempo : %d BPM", ui.tempoBpm) }
                .padding(horizontal = 6.dp)
                .testTag("thumb_tempo"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("${ui.tempoBpm}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Neon.TextPrimary, maxLines = 1)
            Text(tr("Tempo"), fontSize = 11.sp, lineHeight = 12.sp, color = Neon.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        val more = actions.more
        if (more != null) {
            Box {
                var open by remember { mutableStateOf(false) }
                ThumbButton(Icons.Filled.MoreVert, tr("Plus"), tr("Plus de choix"), true, "thumb_more") { open = true }
                ScaledDropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.testTag("thumb_more_menu")) {
                    more.forEach { item ->
                        DropdownMenuItem(text = { Text(item.label) }, onClick = { open = false; item.onClick() }, modifier = Modifier.testTag("thumb_${item.tag}"))
                    }
                }
            }
        } else {
            ThumbButton(Icons.Filled.IosShare, tr("Partager"), tr("Partager mon morceau"), true, "thumb_share", actions.share)
        }
    }
}

/** An icon over its word, 48 dp at least each way. */
@Composable
private fun ThumbButton(icon: ImageVector, label: String, description: String, enabled: Boolean, tag: String, onClick: () -> Unit) {
    Column(
        Modifier.widthIn(min = 56.dp).heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .alpha(if (enabled) 1f else 0.38f)
            .padding(horizontal = 4.dp)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Neon.TextPrimary, modifier = Modifier.size(22.dp))
        Text(label, fontSize = 11.sp, lineHeight = 12.sp, color = Neon.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}
