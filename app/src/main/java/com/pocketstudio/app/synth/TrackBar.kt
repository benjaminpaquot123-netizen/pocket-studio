package com.pocketstudio.app.synth

import com.pocketstudio.ui.ScaledAlertDialog
import com.pocketstudio.ui.ScaledDropdownMenu

import com.pocketstudio.model.i18n.tr

import com.pocketstudio.ui.fadingHorizontalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.TrackColors
import com.pocketstudio.ui.trackColor

/** The instrument tracks for the synth screen's track bar. */
class TracksUi(
    val tracks: List<InstrumentTrack>,
    val selected: Int,
    val select: (Int) -> Unit = {},
    val add: () -> Unit = {},
    /** A track of a kind (null: the plain one of [add]), with its notes written when true; null: "+ Piste" adds the plain one at once. */
    val addKind: ((com.pocketstudio.model.TrackKind, Boolean) -> Unit)? = null,
    val rename: (Int, String) -> Unit = { _, _ -> },
    val setColor: (Int, Int) -> Unit = { _, _ -> },
    val duplicate: (Int) -> Unit = {},
    val remove: (Int) -> Unit = {},
    /** Opens the quick effects of a track (null: no button). */
    val effects: ((Int) -> Unit)? = null,
    /** Empties a track's notes in the scene shown (null: not offered). */
    val clear: ((Int) -> Unit)? = null,
    /** The scene shown, as its chip names it (« B », « Refrain »). */
    val scene: String = "",
    /** « Changer le son » of track (id): its sounds in a sheet (null: not offered). */
    val changeSound: ((Int) -> Unit)? = null,
    /** G3: « Fiche de la piste » of track (id): its five columns, its layers, its history (null: not offered). */
    val sheet: ((Int) -> Unit)? = null,
    /** Phase 37: « Geler la piste » / « Dégeler » (null: not offered), the frozen tracks and the one being frozen. */
    val freeze: ((Int) -> Unit)? = null,
    val unfreeze: ((Int) -> Unit)? = null,
    val frozen: Set<Int> = emptySet(),
    val freezing: Int? = null,
    /** Phase 37: frozen tracks whose song changed since (« regèle-la »). */
    val stale: Set<Int> = emptySet(),
    /** Phase 36: false when the Sons space shows the tracks above every editor (the synth screen then shows none of its own). */
    val showBar: Boolean = true,
    /** Phase 36, Mode simple: « + Piste » offers the everyday kinds only, in everyday words. */
    val simple: Boolean = false,
    /** Phase 36: « + Piste » › « Voix (micro) », an audio track to sing on (null: not offered). */
    val addVoice: (() -> Unit)? = null,
) {
    /** Whether track [id] plays notes in the scene shown. */
    fun hasNotes(id: Int) = tracks.firstOrNull { it.id == id }?.notes?.notes?.isNotEmpty() == true
}

/**
 * One coloured tab per instrument track (name and sound), "+" to add one. The screen below shows
 * the selected track; its menu renames, recolours, duplicates or deletes it.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackBar(
    ui: TracksUi, modifier: Modifier = Modifier,
    /** Phase 36, the Sons space: the drums and the acid before the tracks, the audio tracks after them. */
    leading: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    trailing: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    var menuFor by remember { mutableStateOf<InstrumentTrack?>(null) }
    // The dialogs stay open when the screen turns: kept by track id, then looked up.
    var renamingId by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<Int?>(null) }
    var colouringId by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<Int?>(null) }
    val renaming = renamingId?.let { id -> ui.tracks.firstOrNull { it.id == id } }
    val colouring = colouringId?.let { id -> ui.tracks.firstOrNull { it.id == id } }
    var choosing by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    Row(
        modifier.fadingHorizontalScroll().testTag("track_bar"),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        ui.tracks.forEach { t ->
            val selected = t.id == ui.selected
            val color = trackColor(t.color)
            Box {
                Row(
                    Modifier
                        .height(44.dp)
                        .widthIn(max = 160.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) color.copy(alpha = 0.22f) else Neon.Surface)
                        .border(BorderStroke(if (selected) 2.dp else 1.dp, if (selected) color else Neon.Outline), RoundedCornerShape(10.dp))
                        .combinedClickable(onClick = { if (selected) menuFor = t else ui.select(t.id) }, onLongClick = { ui.select(t.id); menuFor = t })
                        .padding(start = 10.dp, end = 4.dp)
                        .testTag("track_${t.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(Modifier.size(10.dp).background(color, CircleShape))
                    Column(Modifier.weight(1f, fill = false)) {
                        Text(com.pocketstudio.model.DefaultNames.shown(t.name), style = MaterialTheme.typography.labelLarge, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            when {
                                ui.freezing == t.id -> tr("Gel en cours…")
                                t.id in ui.frozen -> "❄ " + tr("Gelée")
                                else -> tr(t.soundLabel)
                            },
                            style = MaterialTheme.typography.labelSmall, color = if (t.id in ui.frozen) Neon.Cyan else Neon.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = if (t.id in ui.frozen) Modifier.testTag("track_frozen_${t.id}") else Modifier,
                        )
                    }
                    if (selected) Icon(Icons.Filled.MoreVert, contentDescription = tr("Options de %s", com.pocketstudio.model.DefaultNames.shown(t.name)), Modifier.size(18.dp), tint = Neon.TextSecondary)
                }
                ScaledDropdownMenu(expanded = menuFor?.id == t.id, onDismissRequest = { menuFor = null }) {
                    ui.sheet?.let { open ->
                        DropdownMenuItem(text = { Text(tr("Fiche de la piste (couches, historique)")) }, onClick = { menuFor = null; open(t.id) }, modifier = Modifier.testTag("track_menu_sheet"))
                    }
                    ui.changeSound?.let { change ->
                        DropdownMenuItem(text = { Text(tr("Changer le son")) }, onClick = { menuFor = null; change(t.id) }, modifier = Modifier.testTag("track_menu_sound"))
                    }
                    DropdownMenuItem(text = { Text(tr("Renommer")) }, onClick = { menuFor = null; renamingId = t.id })
                    DropdownMenuItem(text = { Text(tr("Couleur")) }, onClick = { menuFor = null; colouringId = t.id })
                    DropdownMenuItem(
                        text = { Text(tr("Dupliquer")) }, enabled = ui.tracks.size < InstrumentTrack.MAX,
                        onClick = { menuFor = null; ui.duplicate(t.id) }, modifier = Modifier.testTag("track_duplicate"),
                    )
                    ui.clear?.let { clear ->
                        DropdownMenuItem(
                            text = { Text(tr("Vider dans la partie %s", ui.scene)) }, enabled = ui.hasNotes(t.id),
                            onClick = { menuFor = null; clear(t.id) }, modifier = Modifier.testTag("track_clear"),
                        )
                    }
                    // Phase 37: the track rendered to audio (CPU saved), or back to its notes.
                    if (t.id in ui.frozen) {
                        ui.unfreeze?.let { unfreeze ->
                            DropdownMenuItem(
                                text = { Text(if (t.id in ui.stale) tr("Dégeler (le morceau a changé depuis le gel)") else tr("Dégeler")) },
                                onClick = { menuFor = null; unfreeze(t.id) }, modifier = Modifier.testTag("track_unfreeze"),
                            )
                        }
                    } else {
                        ui.freeze?.let { freeze ->
                            DropdownMenuItem(
                                text = { Text(tr("Geler la piste (rendu audio, moins de calcul)")) }, enabled = ui.freezing == null,
                                onClick = { menuFor = null; freeze(t.id) }, modifier = Modifier.testTag("track_freeze"),
                            )
                        }
                    }
                    if (t.id != 0) {
                        DropdownMenuItem(text = { Text(tr("Supprimer (Annuler possible)")) }, onClick = { menuFor = null; ui.remove(t.id) },
                            modifier = Modifier.testTag("track_delete"))
                    }
                }
            }
            // The chosen track's sound, one touch away (« Changer le son »).
            if (selected) ui.changeSound?.let { change ->
                IconButton(onClick = { change(t.id) }, modifier = Modifier.testTag("track_sound_${t.id}")) {
                    Icon(Icons.Filled.MusicNote, contentDescription = tr("Changer le son de %s", com.pocketstudio.model.DefaultNames.shown(t.name)), tint = color)
                }
            }
        }
        trailing()
        if (ui.tracks.any { it.id == ui.selected }) ui.effects?.let { open ->
            OutlinedButton(onClick = { open(ui.selected) }, modifier = Modifier.height(44.dp).testTag("track_effects")) { Text(tr("Effets")) }
        }
        if (ui.tracks.size < InstrumentTrack.MAX) {
            OutlinedButton(onClick = { if (ui.addKind != null) choosing = true else ui.add() }, modifier = Modifier.height(44.dp).testTag("track_add")) {
                Icon(Icons.Filled.Add, contentDescription = null, Modifier.size(18.dp))
                Text(tr(" Piste"))
            }
        }
    }
    if (choosing) {
        TrackAddDialog(onDismiss = { choosing = false }, voice = ui.addVoice?.let { v -> { choosing = false; v() } }, simple = ui.simple) { kind, write ->
            choosing = false
            if (kind == null) ui.add() else ui.addKind?.invoke(kind, write)
        }
    }
    renaming?.let { t ->
        var name by androidx.compose.runtime.saveable.rememberSaveable(t.id) { mutableStateOf(com.pocketstudio.model.DefaultNames.shown(t.name)) }
        ScaledAlertDialog(
            onDismissRequest = { renamingId = null },
            title = { Text(tr("Renommer la piste")) },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it.take(InstrumentTrack.MAX_NAME) }, singleLine = true,
                    label = { Text(tr("Nom")) }, modifier = Modifier.testTag("track_name_field"))
            },
            confirmButton = { TextButton(onClick = { ui.rename(t.id, name); renamingId = null }, enabled = name.isNotBlank()) { Text(tr("OK")) } },
            dismissButton = { TextButton(onClick = { renamingId = null }) { Text(tr("Annuler")) } },
        )
    }
    colouring?.let { t ->
        ScaledAlertDialog(
            onDismissRequest = { colouringId = null },
            title = { Text(tr("Couleur de « %s »", com.pocketstudio.model.DefaultNames.shown(t.name))) },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TrackColors.forEachIndexed { i, c ->
                        Box(
                            Modifier.size(30.dp).clip(CircleShape).background(c)
                                .border(BorderStroke(if (i == t.color) 3.dp else 0.dp, Neon.TextPrimary), CircleShape)
                                .clickable { ui.setColor(t.id, i); colouringId = null }
                                .testTag("track_color_$i"),
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { colouringId = null }) { Text(tr("Fermer")) } },
        )
    }
}
