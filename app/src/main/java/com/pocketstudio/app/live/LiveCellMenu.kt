package com.pocketstudio.app.live

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.LiveCells
import com.pocketstudio.model.Scene
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon

/** The empty cell recording what is played (« Jouer et enregistrer ici »): track [key], brick [scene]. */
data class CellRecording(val key: String, val scene: Int)

/** A cell of the grid: track [key], brick [scene]. */
data class LiveCell(val key: String, val scene: Int) {
    /** Kept through a rotation as « key|scene ». */
    fun save(): String = "$key|$scene"

    companion object {
        fun read(s: String?): LiveCell? = s?.split('|')?.takeIf { it.size == 2 }?.let { (k, n) -> n.toIntOrNull()?.let { LiveCell(k, it) } }
    }
}

/**
 * « Acid 303 · brique E — Cette case est vide. Qu'est-ce que tu y mets ? » (Benjamin's choice 1A):
 * play and record there, a sound of the library, or a copy of another cell; « Annuler ». Over the
 * bottom of the page, the grid still seen above (the cell marked « ici »).
 */
@Composable
internal fun EmptyCellMenu(
    track: String,
    cell: LiveCell,
    record: () -> Unit,
    library: () -> Unit,
    copy: () -> Unit,
    cancel: () -> Unit,
) {
    val canRecord = LiveCells.canRecord(cell.key)
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f))
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = cancel).testTag("live_cell_scrim"),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(10.dp).clip(RoundedCornerShape(24.dp)).background(Neon.SurfaceHigh)
                .clickable(remember { MutableInteractionSource() }, indication = null) {}
                .padding(16.dp).testTag("live_cell_menu"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                tr("%1\$s · brique %2\$s", track, Scene.NAMES.getOrElse(cell.scene) { "?" }),
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            )
            Text(tr("Cette case est vide. Qu'est-ce que tu y mets ?"), style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary)
            MenuChoice(
                Icons.Filled.FiberManualRecord, Neon.Red, tr("Jouer et enregistrer ici"),
                if (canRecord) tr("Joue aux pads ou aux notes. Retouche la case pour arrêter : la boucle se cale sur la mesure.")
                else tr("Pas pour cette piste : mets-y un son ou copie une case."),
                enabled = canRecord, accent = true, tag = "live_cell_record", choose = record,
            )
            MenuChoice(Icons.Filled.MusicNote, Neon.Acid, tr("Un son de ma bibliothèque"), tr("Tes sons gardés, tes prises, tes boucles."), tag = "live_cell_library", choose = library)
            MenuChoice(Icons.Filled.ContentCopy, Neon.Cyan, tr("Copier une autre case"), tr("Touche ensuite la case à copier."), tag = "live_cell_copy", choose = copy)
            TextButton(onClick = cancel, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("live_cell_cancel")) {
                Text(tr("Annuler"), style = MaterialTheme.typography.titleMedium, color = Neon.Amber)
            }
        }
    }
}

@Composable
private fun MenuChoice(
    icon: ImageVector, tint: Color, title: String, text: String, tag: String,
    enabled: Boolean = true, accent: Boolean = false, choose: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(shape)
            .background(if (accent && enabled) Neon.Red.copy(alpha = 0.16f) else Neon.Surface)
            .let { if (accent && enabled) it.border(1.5.dp, Neon.Red.copy(alpha = 0.6f), shape) else it }
            .clickable(enabled = enabled, onClick = choose).padding(12.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = if (enabled) 0.22f else 0.08f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = if (enabled) tint else Neon.TextSecondary.copy(alpha = 0.5f))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = if (enabled) Neon.TextPrimary else Neon.TextSecondary)
            Text(text, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        }
    }
}

/** While « Copier une autre case » waits for the cell to copy: what to do, and « Annuler ». */
@Composable
internal fun CopyCellBar(track: String, cell: LiveCell, cancel: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 48.dp).background(Neon.Cyan.copy(alpha = 0.16f)).padding(horizontal = 12.dp).testTag("live_cell_copy_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            tr("Touche la case à copier dans %1\$s · brique %2\$s.", track, Scene.NAMES.getOrElse(cell.scene) { "?" }),
            Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 2,
        )
        TextButton(onClick = cancel, modifier = Modifier.heightIn(min = 48.dp).testTag("live_cell_copy_cancel")) { Text(tr("Annuler"), maxLines = 1) }
    }
}

/** While a cell records: where, and that a touch on it again stops (no maximum length). */
@Composable
internal fun CellRecordingBar(track: String, rec: CellRecording, stop: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 48.dp).background(Neon.Red.copy(alpha = 0.16f)).padding(horizontal = 12.dp).testTag("live_cell_rec_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(Neon.Red))
        Text(
            tr("%1\$s · brique %2\$s : joue, puis retouche la case pour arrêter.", track, Scene.NAMES.getOrElse(rec.scene) { "?" }),
            Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 2,
        )
        TextButton(onClick = stop, modifier = Modifier.heightIn(min = 48.dp).testTag("live_cell_rec_stop")) { Text(tr("■ Fin"), maxLines = 1) }
    }
}

/** The drum pads while the drum cell records (Live › Notes): each sounds and goes into the cell. */
@Composable
internal fun LiveDrumPads(names: List<String>, hit: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.testTag("live_drum_pads"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        names.chunked(4).forEachIndexed { row, chunk ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                chunk.forEachIndexed { i, name ->
                    val lane = row * 4 + i
                    Box(
                        Modifier.weight(1f).heightIn(min = 64.dp).clip(RoundedCornerShape(12.dp)).background(DRUMS_COLOR.copy(alpha = 0.3f))
                            .launchOnPress(press = { hit(lane); true }, retract = {}, tap = {}).padding(4.dp).testTag("live_drum_pad_$lane"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(name, style = MaterialTheme.typography.labelMedium, maxLines = 2, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

/** Lot 4b: what a cell of the grid shows over its content. */
internal enum class CellMark { None, Here, Rec }
