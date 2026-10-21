@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.pocketstudio.app.session

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketstudio.app.help.Glossary
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.ui.FoldSection
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel
import com.pocketstudio.ui.WithHelp
import com.pocketstudio.ui.fadingHorizontalScroll
import com.pocketstudio.ui.pillColors

class SessionScreenUi(
    /** Every scene slot A..X: null when it does not exist yet. */
    val scenes: List<Scene?>,
    val current: Int,
    val pending: Int?,
    val song: List<SongPart>,
    /** The song part playing, -1 when not playing the song. */
    val songIndex: Int,
    val playing: Boolean,
    val tempoBpm: Double,
    /** Time signature n/4. */
    val beatsPerBar: Int = 4,
    /** What the scene shown is (new and empty, a copy of another…), said under the grid. */
    val sceneInfo: String = "",
    /** The timeline's rows: every track a song part can switch off. */
    val rows: List<TimelineRow> = emptyList(),
    /** The part lit up as playing (-1 while a part taken out of the song finishes). */
    val shownSongIndex: Int = songIndex,
    /** The part that comes next while the song plays (-1: none, or the song ends). */
    val upcoming: Int = -1,
    /** ▶ plays the song (true) or the scene shown (false). */
    val listenSong: Boolean = song.isNotEmpty(),
    /** At the song's end: start over (true) or stop (false). */
    val songLoops: Boolean = false,
    /** While the song plays, the screens follow it (true) or stay on the scene picked (false). */
    val followSong: Boolean = true,
) {
    val stepsPerBar get() = beatsPerBar * 4
    fun barsOf(part: SongPart) = (scenes.getOrNull(part.scene)?.stepsFor(stepsPerBar) ?: stepsPerBar) / stepsPerBar * part.repeats

    /** The section name of scene [i] ("Refrain"), or "". */
    fun label(i: Int): String = scenes.getOrNull(i)?.label.orEmpty()

    /** "A · Refrain", or "A" alone. */
    fun title(i: Int): String = Scene.NAMES.getOrElse(i) { "?" } + label(i).takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
}

/** A timeline row: a track key (SongPart.DRUMS…), its name and colour. */
class TimelineRow(val key: String, val name: String, val color: androidx.compose.ui.graphics.Color)

class SessionActions(
    val select: (Int) -> Unit = {},
    val duplicate: () -> Unit = {},
    /** A new empty scene in the first free slot. */
    val newScene: () -> Unit = {},
    /** Empties the scene shown. */
    val clear: () -> Unit = {},
    /** Names scene (index) after a song section (blank: its letter alone). */
    val rename: (Int, String) -> Unit = { _, _ -> },
    val addPart: (Int) -> Unit = {},
    /** A part playing scene (second) inserted at place (first). */
    val insertPart: (Int, Int) -> Unit = { _, _ -> },
    val duplicatePart: (Int) -> Unit = {},
    /** Part (first) moved to place (second). */
    val movePart: (Int, Int) -> Unit = { _, _ -> },
    val removePart: (Int) -> Unit = {},
    val setRepeats: (Int, Int) -> Unit = { _, _ -> },
    /** A touch on a part: the song plays from it (or it comes next). */
    val playPart: (Int) -> Unit = {},
    val toggleSong: () -> Unit = {},
    val togglePlay: () -> Unit = {},
    val setListenSong: (Boolean) -> Unit = {},
    val setSongLoops: (Boolean) -> Unit = {},
    val setFollowSong: (Boolean) -> Unit = {},
    /** Live master effects (PerformanceFx parameter, value). */
    val performance: (Int, Float) -> Unit = { _, _ -> },
    /** The timeline: a track on or off in a song part. */
    val togglePartTrack: (Int, String) -> Unit = { _, _ -> },
    val setBeatsPerBar: (Int) -> Unit = {},
)

/** The song first (the arrangement), then the scenes it is made of, the timeline, and the live tools. */
@Composable
fun SessionScreen(ui: SessionScreenUi, actions: SessionActions, live: LivePadsUi? = null, liveActions: LivePadsActions = LivePadsActions()) {
    var renaming by rememberSaveable { mutableStateOf<Int?>(null) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp).testTag("session"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SongCard(ui, actions)
        ScenesCard(ui, actions, rename = { renaming = it })
        if (ui.song.isNotEmpty() && ui.rows.isNotEmpty()) {
            NeonCard {
                SectionLabel("Ligne de temps", help = Glossary.TIMELINE)
                Timeline(ui, actions)
            }
        }
        FoldSection("Jouer en direct") {
            if (live != null) LivePadsCard(live, liveActions)
            LivePad(actions.performance)
        }
    }
    renaming?.let { i -> RenameSceneDialog(i, ui.label(i), onDone = { label -> actions.rename(i, label); renaming = null }, onDismiss = { renaming = null }) }
}

@Composable
private fun SongCard(ui: SessionScreenUi, actions: SessionActions) {
    var inserting by rememberSaveable { mutableStateOf<Int?>(null) }
    NeonCard(accent = Neon.Magenta) {
        SectionLabel("Le morceau", color = Neon.Magenta, help = Glossary.SONG)
        Text(
            when {
                ui.song.isEmpty() -> "Ton morceau est vide : ajoute tes scènes dans l'ordre où elles jouent (Intro, Couplet, Refrain…)."
                ui.songIndex >= 0 -> "Touche une partie : elle joue juste après celle en cours."
                else -> "Touche une partie pour écouter le morceau à partir d'elle."
            },
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
        )
        if (ui.song.isNotEmpty()) {
            ChoiceRow("Écouter :", "la scène", "le morceau", ui.listenSong, actions.setListenSong, "listen")
        }
        ui.song.forEachIndexed { i, part -> PartRow(i, part, ui, actions, insertAfter = { inserting = it }) }
        Text(if (ui.song.isEmpty()) "Ajouter :" else "Ajouter à la fin :", style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ui.scenes.forEachIndexed { i, s ->
                if (s != null) OutlinedButton(onClick = { actions.addPart(i) }, enabled = ui.song.size < SongPart.MAX_PARTS, modifier = Modifier.testTag("add_part_$i")) {
                    Text("+ ${ui.title(i)}", maxLines = 1, softWrap = false)
                }
            }
        }
        val totalBars = ui.song.sumOf { ui.barsOf(it) }
        if (totalBars > 0) {
            val seconds = (totalBars * ui.beatsPerBar * 60 / ui.tempoBpm).toInt()
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
        FoldSection("Réglages du morceau", subtle = true, spacing = 8.dp) {
            ChoiceRow("À la fin :", "s'arrêter", "recommencer", ui.songLoops, actions.setSongLoops, "song_end")
            ChoiceRow("Pendant la lecture :", "suivre le morceau", "rester sur ma scène", !ui.followSong, { actions.setFollowSong(!it) }, "follow")
            Text(
                if (ui.followSong) "Les écrans montrent la partie qui joue."
                else "Les écrans restent sur la scène choisie : tu la modifies pendant que le morceau continue.",
                style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
            )
            WithHelp(Glossary.TIME_SIGNATURE) {
                Text("Temps par mesure :", style = MaterialTheme.typography.bodyMedium)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (2..7).forEach { b ->
                    FilterChip(
                        selected = ui.beatsPerBar == b, onClick = { actions.setBeatsPerBar(b) }, label = { Text("$b/4") },
                        colors = pillColors(), modifier = Modifier.testTag("meter_$b"),
                    )
                }
            }
            Text(
                "4/4 pour la plupart des morceaux, 3/4 pour une valse, 6/4 ou 7/4 pour des rythmes plus rares.",
                style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
            )
        }
    }
    inserting?.let { after ->
        ScenePickerDialog(
            title = "Insérer après la partie ${after + 1}",
            ui = ui,
            onPick = { scene -> actions.insertPart(after + 1, scene); inserting = null },
            onDismiss = { inserting = null },
        )
    }
}

/** A label and two choices side by side ([second] chosen when [secondOn]). */
@Composable
private fun ChoiceRow(label: String, first: String, second: String, secondOn: Boolean, choose: (Boolean) -> Unit, tag: String) {
    FlowRow(itemVerticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = 2.dp))
        FilterChip(selected = !secondOn, onClick = { choose(false) }, label = { Text(first) }, colors = pillColors(), modifier = Modifier.testTag("${tag}_0"))
        FilterChip(selected = secondOn, onClick = { choose(true) }, label = { Text(second) }, colors = pillColors(), modifier = Modifier.testTag("${tag}_1"))
    }
}

/** One line of the song: a touch plays from it; repeats; a menu to move, copy, insert or remove. */
@Composable
private fun PartRow(i: Int, part: SongPart, ui: SessionScreenUi, actions: SessionActions, insertAfter: (Int) -> Unit) {
    val active = i == ui.shownSongIndex
    val next = i == ui.upcoming
    val shape = RoundedCornerShape(10.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clip(shape)
            .background(if (active) Neon.Magenta.copy(alpha = 0.16f) else Color.Transparent)
            .border(1.dp, if (next) Neon.Amber else if (active) Neon.Magenta.copy(alpha = 0.6f) else Color.Transparent, shape)
            .testTag("part_$i"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f).heightIn(min = 56.dp).clip(shape)
                .clickable(role = Role.Button, onClickLabel = "Jouer à partir de cette partie") { actions.playPart(i) }
                .padding(horizontal = 6.dp)
                .testTag("part_play_$i"),
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = if (active) Neon.Magenta else Neon.TextSecondary, modifier = Modifier.size(20.dp))
            Column(Modifier.padding(start = 6.dp)) {
                Text(
                    ui.title(part.scene), fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (active) Neon.Magenta else Neon.TextPrimary,
                )
                Text(
                    "× ${part.repeats}  ·  ${ui.barsOf(part)} mes." + when {
                        active -> "  ·  joue"
                        next -> "  ·  ensuite"
                        else -> ""
                    },
                    style = MonoStyle, maxLines = 1,
                    color = when {
                        next -> Neon.Amber
                        active -> Neon.Magenta
                        else -> Neon.TextSecondary
                    },
                )
            }
        }
        IconButton(onClick = { actions.setRepeats(i, part.repeats - 1) }, enabled = part.repeats > 1) { Icon(Icons.Filled.Remove, contentDescription = "Une fois de moins") }
        IconButton(onClick = { actions.setRepeats(i, part.repeats + 1) }, enabled = part.repeats < SongPart.MAX_REPEATS) { Icon(Icons.Filled.Add, contentDescription = "Une fois de plus") }
        Box {
            var menu by remember { mutableStateOf(false) }
            IconButton(onClick = { menu = true }, modifier = Modifier.testTag("part_menu_$i")) { Icon(Icons.Filled.MoreVert, contentDescription = "Plus d'actions") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                val full = ui.song.size >= SongPart.MAX_PARTS
                DropdownMenuItem(
                    text = { Text("Monter") }, leadingIcon = { Icon(Icons.Filled.ArrowUpward, null) }, enabled = i > 0,
                    onClick = { menu = false; actions.movePart(i, i - 1) }, modifier = Modifier.testTag("part_up_$i"),
                )
                DropdownMenuItem(
                    text = { Text("Descendre") }, leadingIcon = { Icon(Icons.Filled.ArrowDownward, null) }, enabled = i < ui.song.lastIndex,
                    onClick = { menu = false; actions.movePart(i, i + 1) }, modifier = Modifier.testTag("part_down_$i"),
                )
                DropdownMenuItem(
                    text = { Text("Dupliquer") }, leadingIcon = { Icon(Icons.Filled.ContentCopy, null) }, enabled = !full,
                    onClick = { menu = false; actions.duplicatePart(i) }, modifier = Modifier.testTag("part_duplicate_$i"),
                )
                DropdownMenuItem(
                    text = { Text("Insérer une scène après") }, leadingIcon = { Icon(Icons.Filled.Add, null) }, enabled = !full,
                    onClick = { menu = false; insertAfter(i) }, modifier = Modifier.testTag("part_insert_$i"),
                )
                DropdownMenuItem(
                    text = { Text("Retirer du morceau") }, leadingIcon = { Icon(Icons.Filled.Delete, null) },
                    onClick = { menu = false; actions.removePart(i) }, modifier = Modifier.testTag("part_remove_$i"),
                )
            }
        }
    }
}

@Composable
private fun ScenesCard(ui: SessionScreenUi, actions: SessionActions, rename: (Int) -> Unit) {
    NeonCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Scènes", Modifier.weight(1f), help = Glossary.SCENE)
            IconButton(onClick = actions.togglePlay, modifier = Modifier.testTag("session_play")) {
                Icon(if (ui.playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = if (ui.playing) "Arrêter" else "Jouer", tint = Neon.Cyan)
            }
        }
        Text(
            when {
                ui.playing && ui.songIndex >= 0 && !ui.followSong -> "Touche une scène pour la modifier : le morceau continue de jouer."
                ui.playing -> "Touche une scène : elle prend le relais à la mesure suivante."
                else -> "Une scène = la batterie, l'acide et le piano roll. Touche-en une pour la modifier, appui long pour la nommer."
            },
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
        )
        // 8 slots by default; 16 or 24 on demand, and never fewer than the scenes in use.
        val used = ui.scenes.indexOfLast { it != null } + 1
        var chosen by rememberSaveable { mutableIntStateOf(Scene.GRID_SIZES.first()) }
        val shown = Scene.GRID_SIZES.firstOrNull { it >= maxOf(chosen, used, ui.current + 1) } ?: Scene.MAX
        FlowRow(itemVerticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Afficher :", style = MaterialTheme.typography.bodyMedium)
            Scene.GRID_SIZES.forEach { size ->
                FilterChip(
                    selected = size == shown,
                    onClick = { chosen = size },
                    enabled = size >= used,
                    label = { Text("$size") },
                    modifier = Modifier.testTag("scene_grid_$size"),
                )
            }
        }
        (0 until shown step 4).forEach { start ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in start until start + 4) SceneButton(i, ui, actions, rename, Modifier.weight(1f))
            }
        }
        // What the scene shown is: a new one starts empty, a copy says whose copy it is.
        if (ui.sceneInfo.isNotEmpty()) {
            Text(ui.sceneInfo, style = MaterialTheme.typography.bodyMedium, color = Neon.Cyan, modifier = Modifier.testTag("scene_info"))
        }
        val letter = Scene.NAMES[ui.current]
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { rename(ui.current) }, modifier = Modifier.testTag("scene_rename")) {
                Icon(Icons.Filled.Edit, contentDescription = null)
                Text(if (ui.label(ui.current).isBlank()) "  Nommer la scène $letter" else "  Renommer la scène $letter", maxLines = 1, softWrap = false)
            }
            OutlinedButton(onClick = actions.newScene, modifier = Modifier.testTag("scene_new")) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Nouvelle scène vide", maxLines = 1, softWrap = false)
            }
            OutlinedButton(onClick = actions.duplicate, modifier = Modifier.testTag("scene_duplicate")) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null)
                Text("  Dupliquer la scène $letter", maxLines = 1, softWrap = false)
            }
            val empty = ui.scenes.getOrNull(ui.current)?.isEmpty != false
            OutlinedButton(onClick = actions.clear, enabled = !empty, modifier = Modifier.testTag("scene_clear")) {
                Icon(Icons.Filled.DeleteSweep, contentDescription = null)
                Text("  Vider la scène $letter", maxLines = 1, softWrap = false)
            }
        }
    }
}

@Composable
private fun SceneButton(i: Int, ui: SessionScreenUi, actions: SessionActions, rename: (Int) -> Unit, modifier: Modifier) {
    val scene = ui.scenes.getOrNull(i)
    val current = i == ui.current
    val pending = i == ui.pending
    val empty = scene == null || scene.isEmpty
    // The scene shown glows in its colour; filled scenes stand out, empty ones are hollow.
    val lit = if (ui.playing) Neon.Magenta else Neon.Cyan
    val (container, content) = when {
        current -> lit to Neon.OnAccent
        empty -> Color.Black.copy(alpha = 0.22f) to Neon.TextSecondary
        else -> Neon.SurfaceHigh to Neon.TextPrimary
    }
    val shape = RoundedCornerShape(12.dp)
    val label = ui.label(i)
    Box(
        modifier.aspectRatio(1.2f)
            .then(if (current) Modifier.shadow(if (ui.playing) 16.dp else 8.dp, shape, ambientColor = lit, spotColor = lit) else Modifier)
            .clip(shape)
            .background(container)
            .border(
                when {
                    pending -> BorderStroke(3.dp, Neon.Amber)
                    current -> BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                    empty -> BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                    else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                },
                shape,
            )
            // A touch shows (or launches) the scene; a long touch names it.
            .combinedClickable(
                role = Role.Button,
                onClickLabel = "Choisir la scène",
                onLongClickLabel = "Nommer la scène",
                onLongClick = { rename(i) },
                onClick = { actions.select(i) },
            )
            .padding(2.dp)
            .testTag("scene_$i"),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(Scene.NAMES[i], fontSize = 22.sp, fontWeight = FontWeight.Bold, color = content, lineHeight = 24.sp)
            if (label.isNotBlank()) {
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = content, maxLines = 1, overflow = TextOverflow.Ellipsis, lineHeight = 14.sp)
            }
            Text(
                when {
                    pending -> "ensuite"
                    empty -> "vide"
                    else -> "${scene!!.stepsFor(ui.stepsPerBar) / ui.stepsPerBar} mes."
                },
                fontSize = 11.sp, color = content.copy(alpha = 0.8f), maxLines = 1, lineHeight = 13.sp,
            )
        }
    }
}

/** Names scene [index]: a song section in one touch, or any name typed. */
@Composable
internal fun RenameSceneDialog(index: Int, current: String, onDone: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember(index) { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nommer la scène ${Scene.NAMES.getOrElse(index) { "?" }}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Quel moment du morceau est-ce ?", style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Scene.SECTIONS.forEach { s ->
                        FilterChip(
                            selected = name == s, onClick = { name = s }, label = { Text(s) },
                            colors = pillColors(), modifier = Modifier.testTag("section_$s"),
                        )
                    }
                }
                OutlinedTextField(
                    value = name, onValueChange = { name = it.take(Scene.MAX_LABEL) }, singleLine = true,
                    label = { Text("Ou un autre nom") }, modifier = Modifier.fillMaxWidth().testTag("scene_name_field"),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onDone(name) }, modifier = Modifier.testTag("scene_name_ok")) { Text("Valider") } },
        dismissButton = {
            Row {
                if (current.isNotBlank()) TextButton(onClick = { onDone("") }) { Text("Sans nom") }
                TextButton(onClick = onDismiss) { Text("Annuler") }
            }
        },
    )
}

/** Picks one of the scenes in use (to insert it in the song). */
@Composable
private fun ScenePickerDialog(title: String, ui: SessionScreenUi, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ui.scenes.forEachIndexed { i, s ->
                    if (s != null) OutlinedButton(onClick = { onPick(i) }, modifier = Modifier.heightIn(min = 48.dp).testTag("pick_scene_$i")) {
                        Text(ui.title(i), maxLines = 1, softWrap = false)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

/**
 * The song as a timeline: one column per part (as wide as its bars), one row per track. A lit
 * block plays; tap it to silence that track in that part (tap again to bring it back).
 */
@Composable
private fun Timeline(ui: SessionScreenUi, actions: SessionActions) {
    val barWidth = 14.dp
    Text(
        "Touche un bloc pour couper ou remettre une piste dans une partie.",
        style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
    )
    Row(Modifier.testTag("timeline")) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(Modifier.height(22.dp))
            ui.rows.forEach { r ->
                Text(r.name, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(76.dp).height(26.dp).padding(top = 5.dp))
            }
        }
        Row(Modifier.fadingHorizontalScroll(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            ui.song.forEachIndexed { i, part ->
                val width = (barWidth * ui.barsOf(part)).coerceAtLeast(48.dp)
                val active = i == ui.shownSongIndex
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(
                        Modifier.width(width).height(22.dp).clip(RoundedCornerShape(4.dp))
                            .background(if (active) Neon.Magenta.copy(alpha = 0.35f) else Neon.SurfaceHigh)
                            .padding(horizontal = 3.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${ui.title(part.scene)}${if (part.repeats > 1) " ×${part.repeats}" else ""}",
                            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    ui.rows.forEach { r ->
                        val on = part.plays(r.key)
                        Box(
                            Modifier.width(width).height(26.dp).clip(RoundedCornerShape(4.dp))
                                .background(if (on) r.color.copy(alpha = if (active) 0.95f else 0.7f) else Neon.Surface)
                                .border(1.dp, if (on) r.color else Neon.Outline, RoundedCornerShape(4.dp))
                                .clickable { actions.togglePartTrack(i, r.key) }
                                .testTag("timeline_${i}_${r.key}"),
                        )
                    }
                }
            }
        }
    }
}
