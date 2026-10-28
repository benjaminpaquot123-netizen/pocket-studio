package com.pocketstudio.app.library

import com.pocketstudio.ui.pressScale
import com.pocketstudio.ui.ScaledAlertDialog

import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.fadingHorizontalScroll
import com.pocketstudio.app.help.Glossary
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.PackManifest
import com.pocketstudio.model.SoundPacks
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.app.rhythm.DrumKitInfo
import com.pocketstudio.app.sounds.SoundLooks
import com.pocketstudio.ui.ChipRow
import com.pocketstudio.ui.FamilyChip
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel
import com.pocketstudio.ui.SoundTile
import com.pocketstudio.ui.TileGrid

class LibraryUi(
    val packs: PacksUi,
    val playing: SoundRef? = null,
    /** Where a chosen sound goes, e.g. "le pad 3 (Caisse)". */
    val target: String,
    /** The kind shown first (the pad's own: kick for pad 1…). */
    val startKind: String = "kick",
    /** Bytes on the phone per installed pack. */
    val sizes: Map<String, Long> = emptyMap(),
    /** The song plays (its Play/Stop stays at hand while the library covers the screen). */
    val songPlaying: Boolean = false,
    /** The style the project was made from (the assistant names it after it), if known: its packs come first. */
    val styleId: String? = null,
    /** On mobile data (not Wi-Fi): downloading everything is announced with a warning. */
    val metered: Boolean = false,
)

class LibraryActions(
    val close: () -> Unit = {},
    val preview: (SoundRef) -> Unit = {},
    val use: (SoundOption) -> Unit = {},
    val download: (String) -> Unit = {},
    val cancelDownload: (String) -> Unit = {},
    val deletePack: (String) -> Unit = {},
    val togglePlay: () -> Unit = {},
    /** « Tout télécharger » (false) or « Tout compléter » (true). */
    val downloadAll: (complete: Boolean) -> Unit = {},
    val cancelAll: () -> Unit = {},
)

/**
 * The sound library: every kind of sound (kicks, snares, stabs…), from the built-in kit and the
 * downloaded packs, to hear and put on a pad; and the packs themselves, to download or delete.
 */
@Composable
fun LibraryScreen(ui: LibraryUi, actions: LibraryActions) {
    var kind by rememberSaveable { mutableStateOf(ui.startKind) }
    var category by rememberSaveable(kind) { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    // A list to read: centred at a comfortable width on a tablet.
    com.pocketstudio.ui.ReadingWidth {
        Column(Modifier.fillMaxSize().background(Neon.Background).testTag("library")) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    com.pocketstudio.ui.WholeWordsText(tr("Bibliothèque de sons"), style = MaterialTheme.typography.titleLarge)
                    Text(tr("Pour %s", ui.target), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                }
                // The song's Play/Stop, as discreet as in the top bar: stop it to hear the sounds alone.
                IconButton(onClick = actions.togglePlay, modifier = Modifier.testTag("library_play")) {
                    Icon(
                        if (ui.songPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = if (ui.songPlaying) tr("Arrêter le morceau") else tr("Jouer le morceau"),
                        tint = if (ui.songPlaying) Neon.Acid else Neon.Cyan,
                    )
                }
                IconButton(onClick = actions.close, modifier = Modifier.testTag("library_close")) { Icon(Icons.Filled.Close, contentDescription = tr("Fermer")) }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Errors and notices at the top, where they are seen (the pack list can be far below).
                ui.packs.message?.let { Text(it, color = Neon.Amber, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("library_message")) }
                ui.packs.readyNote?.let { ReadyLine(it, Modifier.testTag("library_ready")) }
                // New packs or richer versions published: one touch brings them all.
                AllPacksNews(ui, actions)
                // Without its pack, a style has one or two kicks to offer: the pack is proposed first.
                StylePackOffer(ui, actions)
                androidx.compose.material3.OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, contentDescription = tr("Effacer")) }
                    },
                    placeholder = { Text(tr("Chercher un son (doigts, verre, conga…)")) },
                    modifier = Modifier.fillMaxWidth().testTag("library_search"),
                )
                if (query.isNotBlank()) {
                    SearchResults(query, ui, actions)
                    PackList(ui, actions)
                    return@Column
                }
                Row(Modifier.fadingHorizontalScroll(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SoundPacks.KIND_LABELS.filterKeys { it != "loop" }.forEach { (k, label) ->
                        val look = SoundLooks.of(k)
                        FamilyChip(tr(label), look.icon, look.color, selected = kind == k, onClick = { kind = k }, modifier = Modifier.testTag("library_kind_$k"))
                    }
                }
                val categories = libraryChoices(kind, ui.packs.installed, ui.styleId)
                if (categories.isEmpty()) {
                    Text(tr("Aucun son de ce type pour l'instant : télécharge un pack ci-dessous qui en contient."),
                        style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary)
                } else {
                    val shown = categories.firstOrNull { it.id == category } ?: categories.first()
                    val look = SoundLooks.of(kind)
                    // The sources on one line that scrolls: the sounds stay right under them.
                    ChipRow(categories.size, categories.indexOf(shown)) { i ->
                        val c = categories[i]
                        FamilyChip(
                            c.title.substringAfter(" · ", c.title), if (c.id == KIT_CATEGORY || c.id == KIT_OTHER_CATEGORY) Icons.Filled.Apps else Icons.Filled.Inventory2, look.color,
                            selected = c.id == shown.id, onClick = { category = c.id }, modifier = Modifier.testTag("library_source_${c.id}"),
                        )
                    }
                    NeonCard {
                        SectionLabel(shown.title, foldKey = "Sons de la bibliothèque")
                        TileGrid(shown.options.size, minTileWidth = 260.dp) { i, m ->
                            val option = shown.options[i]
                            val playing = ui.playing == option.ref
                            SoundTile(
                                title = tr(option.name.substringBefore(" · ")), icon = if (playing) Icons.Filled.GraphicEq else look.icon,
                                accent = look.color, selected = playing, onClick = { actions.preview(option.ref) },
                                subtitle = if (playing) tr("En écoute… touche pour arrêter") else tr("Touche pour écouter"),
                                trailing = {
                                    FilledTonalButton(onClick = { actions.use(option) }, modifier = Modifier.pressScale().testTag("library_use_$i")) { Text(tr("Poser")) }
                                },
                                modifier = m.testTag("library_sound_$i"),
                            )
                        }
                        Text(tr("Touche un son pour l'écouter, « Poser » pour le mettre sur %s.", ui.target),
                            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                    }
                }
                PackList(ui, actions)
            }
        }
    }
}

/** The id of the "other sounds of the kit" category. */
internal const val KIT_OTHER_CATEGORY = "kit_other"

/**
 * The categories of [kind] in the library: the packs of the project's style first ([styleId]),
 * the kit's own voice(s) for that kind, then the kit's other voices (a tom or a rim can make a
 * kick or a snare), then the other packs.
 */
internal fun libraryChoices(kind: String, installed: List<PackManifest>, styleId: String?): List<SoundCategory> {
    val role = StarterWizard.Role.entries.firstOrNull { it.id == kind } ?: return libraryCategories(kind, installed)
    val categories = soundCategories(role, styleId, installed).toMutableList()
    // Names in French, as a project stores them (shown translated).
    val others = DrumKitInfo.names.indices.filter { it !in role.builtIn }
        .map { SoundOption(SoundRef.Kit(it), com.pocketstudio.model.i18n.I18n.frenchOf(DrumKitInfo.names[it])) }
    val kit = categories.indexOfFirst { it.id == KIT_CATEGORY }
    categories.add(kit + 1, SoundCategory(KIT_OTHER_CATEGORY, tr("Autres sons du kit"), others))
    return categories
}

/** The style the assistant made the project from, read from its name (« Trap », « Trap 2 »), if any. */
internal fun styleOfProject(name: String): String? =
    StarterWizard.STYLES.filter { name == it.name || name.startsWith(it.name + " ") }.maxByOrNull { it.name.length }?.id

/**
 * The packs that make the project's style sound right, not yet on the device: one button gets
 * them all (the library otherwise offers only the kit's one or two sounds of a kind).
 */
@Composable
private fun StylePackOffer(ui: LibraryUi, actions: LibraryActions) {
    val style = ui.styleId?.let(StarterWizard::style) ?: return
    val index = (ui.packs.index as? IndexState.Ready)?.index ?: return
    val missing = style.packs.mapNotNull { id -> index.packs.firstOrNull { it.id == id } }
        .filter { e -> ui.packs.installed.none { it.id == e.id } }
    if (missing.isEmpty()) return
    val downloading = missing.mapNotNull { ui.packs.downloading[it.id] }
    NeonCard(accent = Neon.Cyan, modifier = Modifier.testTag("library_style_packs")) {
        SectionLabel(tr("Les vrais sons du style"))
        Text(
            tr("Plus de sons %s : %s (%s), à télécharger seulement si tu veux.", tr(style.name), missing.joinToString(", ") { it.name }, mb(missing.sumOf { it.sizeBytes })),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (downloading.isNotEmpty()) {
            LinearProgressIndicator(progress = { downloading.average().toFloat() }, modifier = Modifier.fillMaxWidth(), color = Neon.Cyan)
        } else {
            androidx.compose.material3.Button(
                onClick = { missing.forEach { actions.download(it.id) } },
                modifier = Modifier.pressScale().fillMaxWidth().testTag("library_style_packs_download"),
            ) { Text(tr("Télécharger les sons du style")) }
        }
    }
}

/** Every pack of the index: installed (its size, delete), downloading, or to download. */
@Composable
private fun PackList(ui: LibraryUi, actions: LibraryActions) {
    val packs = ui.packs
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    // The confirmation of « Tout télécharger » (null: closed).
    var asking by rememberSaveable { mutableStateOf<Boolean?>(null) }
    NeonCard(modifier = Modifier.testTag("library_packs")) {
        SectionLabel(tr("Packs de sons"), help = Glossary.SOUND_PACK)
        when (val index = packs.index) {
            IndexState.Loading -> Text(tr("Chargement de la liste…"), color = Neon.TextSecondary)
            IndexState.Unavailable -> Text(tr("Aucun pack à télécharger : les sons intégrés sont tous là, prêts à servir."), color = Neon.TextSecondary)
            is IndexState.Failed -> Text(index.message, color = Neon.Amber)
            is IndexState.Ready -> {
                Text(tr("À télécharger seulement si tu veux ; chaque pack se supprime d'un geste pour libérer la place."),
                    style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                val all = packs.toDownload
                if (all.isNotEmpty() && packs.queue == null) {
                    androidx.compose.material3.Button(
                        onClick = { asking = false },
                        modifier = Modifier.pressScale().fillMaxWidth().testTag("library_download_all"),
                    ) {
                        Icon(Icons.Filled.CloudDownload, contentDescription = null)
                        Text(" " + tr("Tout télécharger (%1\$d packs, %2\$s)", all.size, mb(com.pocketstudio.model.PackCatalog.totalBytes(all))))
                    }
                }
                index.index.packs.sortedBy { it.name.lowercase() }.forEach { entry ->
                    val installed = packs.installed.any { it.id == entry.id }
                    val progress = packs.downloading[entry.id]
                    val outdated = entry.id in packs.outdated
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.testTag("library_pack_${entry.id}")) {
                        Column(Modifier.weight(1f)) {
                            Text(tr(entry.name))
                            Text(
                                if (installed) tr("%d sons · %s sur l'appareil", entry.sounds, mb(ui.sizes[entry.id] ?: entry.sizeBytes))
                                else tr("%d sons · %s", entry.sounds, entry.sizeLabel),
                                style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
                            )
                            if (outdated) Text(tr("Nouvelle version : plus de sons"), style = MaterialTheme.typography.labelSmall, color = Neon.Acid)
                            if (progress != null) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = Neon.Cyan)
                        }
                        when {
                            progress != null -> TextButton(onClick = { actions.cancelDownload(entry.id) }) { Text(tr("Annuler")) }
                            packs.queue != null && !installed -> {}
                            outdated && packs.queue == null -> OutlinedButton(onClick = { actions.download(entry.id) }, modifier = Modifier.testTag("library_update_${entry.id}")) {
                                Text(tr("Mettre à jour"))
                            }
                            installed -> TextButton(onClick = { deleting = entry.id }, modifier = Modifier.testTag("library_delete_${entry.id}")) {
                                Text(tr("Supprimer"), color = Neon.RedText)
                            }
                            else -> OutlinedButton(onClick = { actions.download(entry.id) }, modifier = Modifier.testTag("library_download_${entry.id}")) {
                                Text(tr("Télécharger"))
                            }
                        }
                    }
                }
            }
        }
    }
    asking?.let { complete ->
        DownloadAllDialog(ui, complete, onDismiss = { asking = null }, onConfirm = { asking = null; actions.downloadAll(complete) })
    }
    deleting?.let { id ->
        val name = (packs.index as? IndexState.Ready)?.index?.packs?.firstOrNull { it.id == id }?.name?.let { packName -> tr(packName) } ?: id
        ScaledAlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(tr("Supprimer le pack ?")) },
            text = { Text(tr("Les sons de « %s » quittent l'appareil. Tes projets gardent ceux qu'ils utilisent ; pour les autres, il faudra retélécharger le pack.", name)) },
            confirmButton = {
                TextButton(onClick = { deleting = null; actions.deletePack(id) }, modifier = Modifier.testTag("library_delete_confirm")) {
                    Text(tr("Supprimer"), color = Neon.RedText)
                }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(tr("Annuler")) } },
        )
    }
}

private fun mb(bytes: Long) = when {
    bytes < 1_000_000 -> tr("%d ko", (bytes / 1000).coerceAtLeast(1))
    bytes < 1_000_000_000 -> tr("%.1f Mo", bytes / 1e6)
    else -> tr("%.2f Go", bytes / 1e9)
}

/**
 * At the top of the library: the download-all queue while it runs (which pack, how far, Stop), or
 * a badge when packs were published or enriched since the user last looked (« Tout compléter »).
 */
@Composable
private fun AllPacksNews(ui: LibraryUi, actions: LibraryActions) {
    val packs = ui.packs
    val queue = packs.queue
    var asking by rememberSaveable { mutableStateOf(false) }
    if (queue != null) {
        NeonCard(accent = Neon.Cyan, modifier = Modifier.testTag("library_queue")) {
            SectionLabel(tr("Téléchargement de la bibliothèque"))
            Text(
                if (queue.current != null) tr("Pack %1\$d sur %2\$d : %3\$s", queue.done + 1, queue.count, tr(queue.current)) else tr("Presque fini…"),
                style = MaterialTheme.typography.bodyMedium,
            )
            LinearProgressIndicator(progress = { queue.fraction }, modifier = Modifier.fillMaxWidth(), color = Neon.Cyan)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Tu peux continuer à jouer pendant ce temps."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, modifier = Modifier.weight(1f))
                TextButton(onClick = actions.cancelAll, modifier = Modifier.testTag("library_queue_stop")) { Text(tr("Arrêter")) }
            }
        }
        return
    }
    val news = packs.toComplete
    if (news.isEmpty()) return
    val updates = news.count { it.id in packs.outdated }
    NeonCard(accent = Neon.Acid, modifier = Modifier.testTag("library_news")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(tr("Nouveaux sons"), Modifier.weight(1f))
            // The badge: how many packs are new or richer.
            Text(
                "${news.size}", color = Neon.Background, style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.background(Neon.Acid, androidx.compose.foundation.shape.CircleShape).padding(horizontal = 8.dp, vertical = 2.dp).testTag("library_news_badge"),
            )
        }
        Text(
            when {
                updates == 0 -> tr("Nouveaux packs à découvrir : %1\$d (%2\$s).", news.size, mb(com.pocketstudio.model.PackCatalog.totalBytes(news)))
                updates == news.size -> tr("Packs enrichis de nouveaux sons : %1\$d (%2\$s).", news.size, mb(com.pocketstudio.model.PackCatalog.totalBytes(news)))
                else -> tr("Nouveaux packs : %1\$d · mises à jour : %2\$d (%3\$s).", news.size - updates, updates, mb(com.pocketstudio.model.PackCatalog.totalBytes(news)))
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        androidx.compose.material3.Button(onClick = { asking = true }, modifier = Modifier.pressScale().fillMaxWidth().testTag("library_complete_all")) {
            Text(tr("Tout compléter"))
        }
    }
    if (asking) DownloadAllDialog(ui, complete = true, onDismiss = { asking = false }, onConfirm = { asking = false; actions.downloadAll(true) })
}

/** What « Tout télécharger » / « Tout compléter » will fetch, its size, and a word about mobile data. */
@Composable
private fun DownloadAllDialog(ui: LibraryUi, complete: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val entries = if (complete) ui.packs.toComplete else ui.packs.toDownload
    val size = mb(com.pocketstudio.model.PackCatalog.totalBytes(entries))
    ScaledAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (complete) tr("Tout compléter ?") else tr("Tout télécharger ?")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tr("%1\$d packs, %2\$s en tout. Ils arrivent l'un après l'autre ; si la connexion coupe, ça reprend où ça s'était arrêté.", entries.size, size))
                if (ui.metered) {
                    Text(tr("Tu n'es pas en Wi-Fi : ça compte sur ton forfait mobile. Mieux vaut attendre le Wi-Fi."),
                        color = Neon.Amber, modifier = Modifier.testTag("library_all_metered"))
                } else {
                    Text(tr("Conseil : en Wi-Fi, c'est plus rapide et ça ne touche pas ton forfait."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm, modifier = Modifier.testTag("library_all_confirm")) { Text(tr("Télécharger")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Annuler")) } },
    )
}

/** The sounds of the installed packs that match [query] (name, tags, kind, pack), to hear and place. */
@Composable
private fun SearchResults(query: String, ui: LibraryUi, actions: LibraryActions) {
    val hits = androidx.compose.runtime.remember(query, ui.packs.installed) { com.pocketstudio.model.PackSearch.find(query, ui.packs.installed) }
    NeonCard(modifier = Modifier.testTag("library_results")) {
        SectionLabel(tr("Résultats"))
        if (hits.isEmpty()) {
            Text(
                if (ui.packs.installed.isEmpty()) tr("La recherche fouille les packs téléchargés : télécharge-en ci-dessous.")
                else tr("Aucun son trouvé : essaie un autre mot, ou télécharge d'autres packs ci-dessous."),
                style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary,
            )
            return@NeonCard
        }
        TileGrid(hits.size, minTileWidth = 260.dp) { i, m ->
            val hit = hits[i]
            val option = SoundOption(SoundRef.Pack(hit.pack.id, hit.sound), "${hit.sound.name} · ${hit.pack.name}")
            val look = SoundLooks.of(hit.sound.kind)
            val playing = ui.playing == option.ref
            SoundTile(
                title = hit.sound.name, icon = if (playing) Icons.Filled.GraphicEq else look.icon,
                accent = look.color, selected = playing, onClick = { actions.preview(option.ref) },
                subtitle = tr(hit.pack.name),
                trailing = {
                    FilledTonalButton(onClick = { actions.use(option) }, modifier = Modifier.pressScale().testTag("library_result_use_$i")) { Text(tr("Poser")) }
                },
                modifier = m.testTag("library_result_$i"),
            )
        }
    }
}
