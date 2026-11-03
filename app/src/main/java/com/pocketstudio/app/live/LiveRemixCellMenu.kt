package com.pocketstudio.app.live

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.remix.StemRouting
import com.pocketstudio.model.Project
import com.pocketstudio.model.Remix
import com.pocketstudio.model.RemixCell
import com.pocketstudio.model.Scene
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon

/** The song's bar heard at [seconds] into it (its first beat at [Remix.firstBeat]), 0 at least. */
internal fun Remix.barAt(seconds: Double): Int =
    if (songBpm <= 0.0) 0 else (((seconds - firstBeat) * songBpm / 240.0) + 1e-6).toInt().coerceAtLeast(0)

/** The song's bars ([Remix.seconds] long, 4/4 at its tempo); 1 when not known. */
internal val Remix.songBars: Int get() = if (songBpm <= 0.0 || seconds <= 0.0) 1 else ((seconds - firstBeat) * songBpm / 240.0).toInt().coerceAtLeast(1)

/**
 * Lot D2, « case Remix du Live »: the menu of brick [scene]'s cell of the remix column (a long
 * press on it, or TalkBack's action): which part of the song (all of it or one stem), from which
 * bar, how many bars; « Mettre dans la case » cuts it (whole bars, it follows the tempo), « Toute la
 * chanson ici » gives the brick the whole song again. Over the bottom of the page, like the empty
 * cell's menu.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RemixCellMenu(
    project: Project,
    track: String,
    scene: Int,
    busy: Boolean,
    choose: (stem: Int, fromBar: Int, bars: Int) -> Unit,
    clear: () -> Unit,
    cancel: () -> Unit,
) {
    val remix = project.remix
    val now = remix.cell(scene)
    var stem by rememberSaveable(scene) { mutableIntStateOf(now?.stem ?: RemixCell.WHOLE) }
    var from by rememberSaveable(scene) { mutableIntStateOf(now?.let { remix.barAt(it.from) } ?: 0) }
    var bars by rememberSaveable(scene) { mutableIntStateOf(now?.bars?.takeIf { it in RemixCell.LENGTHS } ?: 4) }
    val last = (remix.songBars - 1).coerceAtLeast(0)
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f))
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = cancel).testTag("remix_cell_scrim"),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(10.dp).clip(RoundedCornerShape(24.dp)).background(Neon.SurfaceHigh)
                .clickable(remember { MutableInteractionSource() }, indication = null) {}
                .verticalScroll(rememberScrollState())
                .padding(16.dp).testTag("remix_cell_menu"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                tr("%1\$s · brique %2\$s", track, Scene.NAMES.getOrElse(scene) { "?" }),
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            )
            Text(
                tr("Un passage de la chanson dans cette case : il part sur la mesure et suit le tempo."),
                style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                (listOf(RemixCell.WHOLE) + Remix.SHOWN_ORDER).forEach { s ->
                    FilterChip(
                        selected = stem == s, onClick = { stem = s },
                        label = { Text(if (s == RemixCell.WHOLE) tr("Tout") else StemRouting.stemLabel(s), maxLines = 1) },
                        modifier = Modifier.heightIn(min = 40.dp).testTag("remix_cell_stem_$s"),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tr("Début : mesure %d", from + 1), Modifier.weight(1f).testTag("remix_cell_from"), style = MaterialTheme.typography.titleMedium, maxLines = 1)
                FilledTonalIconButton(
                    onClick = { from = (from - 1).coerceAtLeast(0) }, enabled = from > 0,
                    modifier = Modifier.size(48.dp).testTag("remix_cell_earlier").semantics { contentDescription = tr("Plus tôt") },
                ) { Text("−", style = MaterialTheme.typography.titleLarge) }
                FilledTonalIconButton(
                    onClick = { from = (from + 1).coerceAtMost(last) }, enabled = from < last,
                    modifier = Modifier.size(48.dp).testTag("remix_cell_later").semantics { contentDescription = tr("Plus tard") },
                ) { Text("+", style = MaterialTheme.typography.titleLarge) }
            }
            Text(tr("Longueur (mesures)"), style = MaterialTheme.typography.labelLarge, color = Neon.TextSecondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RemixCell.LENGTHS.forEach { n ->
                    FilterChip(
                        selected = bars == n, onClick = { bars = n }, label = { Text("$n", maxLines = 1) },
                        modifier = Modifier.heightIn(min = 40.dp).testTag("remix_cell_bars_$n"),
                    )
                }
            }
            Button(
                onClick = { choose(stem, from, bars) }, enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = Neon.Acid, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("remix_cell_put"),
            ) { Text(if (busy) tr("Découpe en cours…") else tr("Mettre dans la case"), style = MaterialTheme.typography.titleMedium, maxLines = 1) }
            if (now != null) {
                OutlinedButton(onClick = clear, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("remix_cell_clear")) {
                    Text(tr("Toute la chanson ici"), maxLines = 1)
                }
            }
            TextButton(onClick = cancel, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("remix_cell_cancel")) {
                Text(tr("Annuler"), style = MaterialTheme.typography.titleMedium, color = Neon.Amber)
            }
        }
    }
}

/** What a remix cell holding a passage shows: « Voix · 4 » (its stem or the song, its bars). */
internal fun RemixCell.label(): String =
    "${if (stem == RemixCell.WHOLE) com.pocketstudio.model.DefaultNames.shownSample(name) else StemRouting.stemLabel(stem)} · $bars"
