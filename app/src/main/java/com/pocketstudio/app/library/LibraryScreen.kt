package com.pocketstudio.app.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.SoundPacks
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel

class LibraryUi(
    val packs: PacksUi,
    val playing: SoundRef? = null,
    /** Where a chosen sound goes, e.g. "le pad 3 (Caisse)". */
    val target: String,
    /** The kind shown first (the pad's own: kick for pad 1…). */
    val startKind: String = "kick",
    /** Bytes on the phone per installed pack. */
    val sizes: Map<String, Long> = emptyMap(),
)

class LibraryActions(
    val close: () -> Unit = {},
    val preview: (SoundRef) -> Unit = {},
    val use: (SoundOption) -> Unit = {},
    val download: (String) -> Unit = {},
    val cancelDownload: (String) -> Unit = {},
    val deletePack: (String) -> Unit = {},
)

/**
 * The sound library: every kind of sound (kicks, snares, stabs…), from the built-in kit and the
 * downloaded packs, to hear and put on a pad; and the packs themselves, to download or delete.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(ui: LibraryUi, actions: LibraryActions) {
    var kind by rememberSaveable { mutableStateOf(ui.startKind) }
    var category by rememberSaveable(kind) { mutableStateOf<String?>(null) }
    val chipColors = FilterChipDefaults.filterChipColors(selectedContainerColor = Neon.Cyan.copy(alpha = 0.25f), selectedLabelColor = Neon.Cyan)
    Column(Modifier.fillMaxSize().background(Neon.Background).testTag("library")) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Bibliothèque de sons", style = MaterialTheme.typography.titleLarge)
                Text("Pour ${ui.target}", style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
            }
            IconButton(onClick = actions.close, modifier = Modifier.testTag("library_close")) { Icon(Icons.Filled.Close, contentDescription = "Fermer") }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SoundPacks.KIND_LABELS.filterKeys { it != "fx" && it != "loop" }.forEach { (k, label) ->
                    FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(label) }, colors = chipColors,
                        modifier = Modifier.testTag("library_kind_$k"))
                }
            }
            val categories = libraryCategories(kind, ui.packs.installed)
            if (categories.isEmpty()) {
                Text("Aucun son de ce type pour l'instant : télécharge un pack ci-dessous (les packs avec 808, basses ou stabs en ont).",
                    style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary)
            } else {
                val shown = categories.firstOrNull { it.id == category } ?: categories.first()
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { c ->
                        FilterChip(selected = c.id == shown.id, onClick = { category = c.id }, label = { Text(c.title.substringAfter(" · ", c.title)) },
                            modifier = Modifier.testTag("library_source_${c.id}"))
                    }
                }
                NeonCard {
                    SectionLabel(shown.title)
                    shown.options.forEachIndexed { i, option ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Neon.Surface)
                                .clickable { actions.preview(option.ref) }.testTag("library_sound_$i"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = { actions.preview(option.ref) }) {
                                Icon(if (ui.playing == option.ref) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = "Écouter")
                            }
                            Text(option.name.substringBefore(" · "), modifier = Modifier.weight(1f))
                            TextButton(onClick = { actions.use(option) }, modifier = Modifier.testTag("library_use_$i")) { Text("Poser") }
                        }
                    }
                    Text("Touche un son pour l'écouter, « Poser » pour le mettre sur ${ui.target}.",
                        style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                }
            }
            PackList(ui, actions)
        }
    }
}

/** Every pack of the index: installed (its size, delete), downloading, or to download. */
@Composable
private fun PackList(ui: LibraryUi, actions: LibraryActions) {
    val packs = ui.packs
    NeonCard(modifier = Modifier.testTag("library_packs")) {
        SectionLabel("Packs par style")
        when (val index = packs.index) {
            IndexState.Loading -> Text("Chargement de la liste…", color = Neon.TextSecondary)
            IndexState.Unavailable -> Text("Les packs de sons arrivent bientôt.", color = Neon.TextSecondary)
            is IndexState.Failed -> Text(index.message, color = Neon.Amber)
            is IndexState.Ready -> {
                Text("À télécharger seulement si tu veux ; chaque pack se supprime d'un geste pour libérer la place.",
                    style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                index.index.packs.forEach { entry ->
                    val installed = packs.installed.any { it.id == entry.id }
                    val progress = packs.downloading[entry.id]
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.testTag("library_pack_${entry.id}")) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.name)
                            Text(
                                if (installed) "${entry.sounds} sons · ${mb(ui.sizes[entry.id] ?: entry.sizeBytes)} sur le téléphone"
                                else "${entry.sounds} sons · ${entry.sizeLabel}",
                                style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
                            )
                            if (progress != null) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = Neon.Cyan)
                        }
                        when {
                            progress != null -> TextButton(onClick = { actions.cancelDownload(entry.id) }) { Text("Annuler") }
                            installed -> TextButton(onClick = { actions.deletePack(entry.id) }, modifier = Modifier.testTag("library_delete_${entry.id}")) {
                                Text("Supprimer", color = Neon.Red)
                            }
                            else -> OutlinedButton(onClick = { actions.download(entry.id) }, modifier = Modifier.testTag("library_download_${entry.id}")) {
                                Text("Télécharger")
                            }
                        }
                    }
                }
            }
        }
        packs.message?.let { Text(it, color = Neon.Amber, style = MaterialTheme.typography.bodySmall) }
    }
}

private fun mb(bytes: Long) =
    if (bytes < 1_000_000) "${(bytes / 1000).coerceAtLeast(1)} ko" else String.format(java.util.Locale.FRANCE, "%.1f Mo", bytes / 1e6)
