package com.pocketstudio.app.session

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel

class SessionScreenUi(
    /** Every scene slot A..H: null when it does not exist yet. */
    val scenes: List<Scene?>,
    val current: Int,
    val pending: Int?,
    val song: List<SongPart>,
    /** The song part playing, -1 when not playing the song. */
    val songIndex: Int,
    val playing: Boolean,
    val tempoBpm: Double,
)

class SessionActions(
    val select: (Int) -> Unit = {},
    val duplicate: () -> Unit = {},
    val addPart: (Int) -> Unit = {},
    val removePart: (Int) -> Unit = {},
    val setRepeats: (Int, Int) -> Unit = { _, _ -> },
    val toggleSong: () -> Unit = {},
    val togglePlay: () -> Unit = {},
    /** Live master effects (PerformanceFx parameter, value). */
    val performance: (Int, Float) -> Unit = { _, _ -> },
)

/** Scenes (session) and song: build variations, then chain them. */
@Composable
fun SessionScreen(ui: SessionScreenUi, actions: SessionActions) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp).testTag("session"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NeonCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Scènes", Modifier.weight(1f))
                IconButton(onClick = actions.togglePlay, modifier = Modifier.testTag("session_play")) {
                    Icon(if (ui.playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = if (ui.playing) "Arrêter" else "Jouer", tint = Neon.Cyan)
                }
            }
            Text(
                if (ui.playing) "Touche une scène : elle prend le relais à la mesure suivante."
                else "Une scène = la batterie, l'acide et le piano roll. Touche-en une pour l'afficher et la modifier.",
                style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
            )
            listOf(0, 4).forEach { start ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in start until start + 4) SceneButton(i, ui, actions, Modifier.weight(1f))
                }
            }
            OutlinedButton(onClick = actions.duplicate, modifier = Modifier.fillMaxWidth().testTag("scene_duplicate")) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null)
                Text("  Dupliquer la scène ${Scene.NAMES[ui.current]} (pour une variante)")
            }
        }

        LivePad(actions.performance)

        NeonCard(accent = Neon.Magenta) {
            SectionLabel("Morceau", color = Neon.Magenta)
            Text(
                "Enchaîne tes scènes : chacune joue le nombre de fois choisi, puis la suivante (le morceau boucle).",
                style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
            )
            ui.song.forEachIndexed { i, part ->
                val active = i == ui.songIndex
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.testTag("part_$i")) {
                    Text(
                        Scene.NAMES[part.scene],
                        fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        color = if (active) Neon.Magenta else Neon.TextPrimary,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    val bars = (ui.scenes.getOrNull(part.scene)?.steps ?: 16) / 16 * part.repeats
                    Text("× ${part.repeats}  ·  $bars mes.", style = MonoStyle, color = if (active) Neon.Magenta else Neon.TextSecondary, modifier = Modifier.weight(1f))
                    IconButton(onClick = { actions.setRepeats(i, part.repeats - 1) }, enabled = part.repeats > 1) { Icon(Icons.Filled.Remove, contentDescription = "Moins") }
                    IconButton(onClick = { actions.setRepeats(i, part.repeats + 1) }) { Icon(Icons.Filled.Add, contentDescription = "Plus") }
                    IconButton(onClick = { actions.removePart(i) }) { Icon(Icons.Filled.Delete, contentDescription = "Retirer") }
                }
            }
            Text("Ajouter :", style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ui.scenes.forEachIndexed { i, s ->
                    if (s != null) OutlinedButton(onClick = { actions.addPart(i) }, modifier = Modifier.testTag("add_part_$i")) { Text("+ ${Scene.NAMES[i]}") }
                }
            }
            val totalBars = ui.song.sumOf { (ui.scenes.getOrNull(it.scene)?.steps ?: 16) / 16 * it.repeats }
            if (totalBars > 0) {
                val seconds = (totalBars * 4 * 60 / ui.tempoBpm).toInt()
                Text("$totalBars mesures · ${seconds / 60}:%02d".format(seconds % 60), style = MonoStyle, color = Neon.TextSecondary)
            }
            Button(
                onClick = actions.toggleSong,
                enabled = ui.song.isNotEmpty() || ui.playing,
                colors = ButtonDefaults.buttonColors(containerColor = if (ui.songIndex >= 0) Neon.Magenta else Neon.Cyan),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("song_play"),
            ) {
                Icon(if (ui.playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null)
                Text(if (ui.playing) "  Arrêter" else "  Jouer le morceau")
            }
        }
    }
}

@Composable
private fun SceneButton(i: Int, ui: SessionScreenUi, actions: SessionActions, modifier: Modifier) {
    val scene = ui.scenes.getOrNull(i)
    val current = i == ui.current
    val pending = i == ui.pending
    val empty = scene == null || scene.isEmpty
    val colors = when {
        current -> ButtonDefaults.buttonColors(containerColor = if (ui.playing) Neon.Magenta else Neon.Cyan)
        else -> ButtonDefaults.outlinedButtonColors(contentColor = if (empty) Neon.TextSecondary else Neon.TextPrimary)
    }
    Button(
        onClick = { actions.select(i) },
        colors = colors,
        shape = RoundedCornerShape(10.dp),
        border = when {
            pending -> BorderStroke(3.dp, Neon.Amber)
            !current -> BorderStroke(1.dp, if (empty) Neon.Outline else Neon.TextSecondary)
            else -> null
        },
        contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp),
        modifier = modifier.aspectRatio(1.2f).testTag("scene_$i"),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(Scene.NAMES[i], fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                when {
                    pending -> "ensuite"
                    scene == null -> "vide"
                    empty -> "vide"
                    else -> "${scene.steps / 16} mes."
                },
                fontSize = 11.sp,
                maxLines = 1,
            )
        }
    }
}
