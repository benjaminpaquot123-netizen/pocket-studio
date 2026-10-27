package com.pocketstudio.app.projects

import com.pocketstudio.ui.ScaledAlertDialog
import com.pocketstudio.ui.ScaledDropdownMenu

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.i18n.tr
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.ProjectCatalog
import com.pocketstudio.model.ProjectInfo
import com.pocketstudio.model.TrashedProject
import com.pocketstudio.ui.Neon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The headings of the "Nouveau projet" choices (the guided song comes first, apart). */
enum class StarterGroup {
    Style, Demo, Empty;

    val title: String get() = when (this) {
        Style -> tr("Partir d'un style")
        Demo -> tr("Écouter une démo")
        Empty -> tr("Vide")
    }
}

/** A way to start a project: a name, a line saying what it holds, its heading, and how to build it. */
class ProjectStarter(val title: String, val description: String, val tag: String, val group: StarterGroup = StarterGroup.Empty)

class ProjectsActions(
    val back: () -> Unit = {},
    val open: (ProjectInfo) -> Unit = {},
    /** Index in the starters list. */
    val create: (Int) -> Unit = {},
    /** The beginner's step-by-step assistant. */
    val wizard: () -> Unit = {},
    /** A style's whole song in one touch, with the assistant's advised choices (its id). */
    val quickStyle: (String) -> Unit = {},
    /** The assistant opened on that style (« Personnaliser pas à pas »). */
    val customStyle: (String) -> Unit = {},
    val dismissQuickError: () -> Unit = {},
    /** Plays (or stops) a demo as a preview, without creating or opening a project (index in [com.pocketstudio.model.Demos.ALL]). */
    val previewDemo: (Int) -> Unit = {},
    /** The page closed: a demo still playing stops. */
    val stopDemo: () -> Unit = {},
    val rename: (ProjectInfo, String) -> Unit = { _, _ -> },
    val duplicate: (ProjectInfo) -> Unit = {},
    /** Share the project and its sounds as a .zip. */
    val exportZip: (ProjectInfo) -> Unit = {},
    /** A project the app cannot read: its files as they are, as a .zip to send. */
    val exportRaw: (ProjectInfo) -> Unit = {},
    /** « La version d'avant la séance » as a new project. */
    val restoreOpening: (ProjectInfo) -> Unit = {},
    val importZip: () -> Unit = {},
    val delete: (ProjectInfo) -> Unit = {},
    val undoDelete: () -> Unit = {},
    /** Brings a project back from the "Corbeille" list. */
    val restore: (TrashedProject) -> Unit = {},
    /** Deletes a trashed project for good (after the screen's confirmation). */
    val deleteForever: (TrashedProject) -> Unit = {},
    /** Deletes every trashed project for good (after the screen's confirmation). */
    val emptyTrash: () -> Unit = {},
    val dismissDeleted: () -> Unit = {},
    val dismissMessage: () -> Unit = {},
    /** « Partager mon morceau »: the open project's song in M4A, then the share sheet. */
    val shareSong: () -> Unit = {},
    /** The open project touched in the list: back on it (null: as [back]). */
    val resume: (() -> Unit)? = null,
)

/** "Mes projets": every project on the phone, the open one first in mind; open, create, rename, copy, delete. */
@Composable
fun ProjectsScreen(
    ui: ProjectsUi, currentId: String, starters: List<ProjectStarter>, actions: ProjectsActions, now: Long = System.currentTimeMillis(),
    /** The song made in one touch from a style (« Nouveau projet »). */
    quick: com.pocketstudio.app.library.QuickUi = com.pocketstudio.app.library.QuickUi(),
    /**
     * Opened on « Nouveau projet » alone (« Nouveau morceau » of « Que veux-tu faire ? »): its
     * Retour closes the whole screen ([ProjectsActions.back]), and it stays under the assistant.
     */
    newOnly: Boolean = false,
) {
    // Kept when the screen turns: by id (a project's info is not saveable), then looked up.
    var renamingId by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    val renaming = renamingId?.let { id -> ui.projects.firstOrNull { it.id == id } }
    // « Nouveau projet » is open for the project that was open when it was asked: once another one is
    // created or opened (the screens are rebuilt on it) the page is closed, back on the list.
    var startedFor by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(if (newOnly) currentId else null) }
    val choosingStart = startedFor == currentId
    val setChoosingStart = { open: Boolean -> startedFor = if (open) currentId else null }
    var showTrash by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    // A deletion for good is asked first: the trash entry (or EMPTY_ALL for the whole trash).
    var confirmingDelete by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    // A list to read: centred at a comfortable width on a tablet.
    com.pocketstudio.ui.ReadingWidth {
        // « Nouveau projet » takes the screen (a song being prepared keeps it there).
        if (choosingStart) {
            val closePage = { if (newOnly) actions.back() else setChoosingStart(false) }
            androidx.activity.compose.BackHandler { if (quick.creating == null) closePage() }
            NewProjectPage(starters, quick, actions, close = closePage, leave = { if (!newOnly) setChoosingStart(false) })
            return@ReadingWidth
        }
        Column(Modifier.fillMaxSize().padding(12.dp).testTag("projects"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = actions.back, modifier = Modifier.testTag("projects_back")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Retour"))
                }
                Text(tr("Mes projets"), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            }
            // The guided song first: the main action for a beginner.
            Button(onClick = actions.wizard, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("project_wizard")) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                Text("  " + tr("Nouveau morceau guidé"), style = MaterialTheme.typography.titleMedium)
            }
            OutlinedButton(onClick = { setChoosingStart(true) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("project_new")) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  " + tr("Nouveau projet : style, démo ou vide"), style = MaterialTheme.typography.titleSmall)
            }
            TextButton(onClick = actions.importZip, modifier = Modifier.fillMaxWidth().testTag("project_import")) {
                Text(tr("Importer un projet (.zip)"))
            }
            ui.deleted?.let { (_, name) ->
                Card(colors = CardDefaults.cardColors(containerColor = Neon.SurfaceHigh), modifier = Modifier.testTag("project_deleted")) {
                    Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("« %s » est dans la corbeille (30 jours).", name), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = actions.undoDelete, enabled = !ui.busy, modifier = Modifier.testTag("project_undo_delete")) { Text(tr("Annuler")) }
                        TextButton(onClick = actions.dismissDeleted) { Text("OK") }
                    }
                }
            }
            ui.message?.let {
                Text(it, color = if (ui.messageOk) Neon.Acid else Neon.Amber, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable(onClick = actions.dismissMessage))
            }
            // Lot 1: no project at all (the last one deleted, or none saved yet): what to do next, big.
            if (!ui.loading && ui.projects.isEmpty()) EmptyProjects(newSong = { setChoosingStart(true) }, importZip = actions.importZip)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(ui.projects, key = { it.id }) { p ->
                    ProjectCard(p, p.id == currentId, now, actions, busy = ui.busy, onRename = { renamingId = p.id })
                }
                if (ui.trash.isNotEmpty()) {
                    item(key = "trash") {
                        // « Vider la corbeille » beside its heading (lot 1): there without unfolding it, asked first.
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { showTrash = !showTrash }, modifier = Modifier.weight(1f).testTag("project_trash")) {
                                // Its name on one line, how long it keeps them under it (room for « Vider la corbeille »).
                                Column(Modifier.weight(1f)) {
                                    Text((if (showTrash) "▾ " else "▸ ") + tr("Corbeille (%d)", ui.trash.size), color = Neon.TextSecondary, maxLines = 1)
                                    Text(tr("gardés %d jours", ProjectCatalog.TRASH_DAYS), color = Neon.TextSecondary, style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1, modifier = Modifier.padding(start = 14.dp))
                                }
                            }
                            TextButton(onClick = { confirmingDelete = EMPTY_ALL }, enabled = !ui.busy, modifier = Modifier.testTag("project_empty_trash")) {
                                Icon(Icons.Filled.DeleteSweep, contentDescription = null, tint = Neon.Amber, modifier = Modifier.padding(end = 6.dp))
                                Text(tr("Vider la corbeille"), color = Neon.Amber, maxLines = 1)
                            }
                        }
                    }
                    if (showTrash) {
                        // While a deletion or a restore runs, the trash waits (its buttons off).
                        items(ui.trash, key = { "trash_${it.entry}" }) { t -> TrashedCard(t, now, actions, busy = ui.busy, onDelete = { confirmingDelete = t.entry }) }
                    }
                }
            }
        }
    }
    renaming?.let { p ->
        var name by androidx.compose.runtime.saveable.rememberSaveable(p.id) { mutableStateOf(com.pocketstudio.model.DefaultNames.shown(p.name)) }
        ScaledAlertDialog(
            onDismissRequest = { renamingId = null },
            title = { Text(tr("Renommer")) },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it.take(40) }, singleLine = true, label = { Text(tr("Nom du projet")) },
                    modifier = Modifier.testTag("project_name_field"))
            },
            confirmButton = {
                TextButton(onClick = { actions.rename(p, name); renamingId = null }, enabled = name.isNotBlank(), modifier = Modifier.testTag("project_rename_ok")) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { renamingId = null }) { Text(tr("Annuler")) } },
        )
    }
    confirmingDelete?.let { which ->
        val all = which == EMPTY_ALL
        val item = ui.trash.firstOrNull { it.entry == which }
        if (!all && item == null) return@let  // restored or gone meanwhile
        ScaledAlertDialog(
            onDismissRequest = { confirmingDelete = null },
            title = { Text(if (all) tr("Vider la corbeille ?") else tr("Supprimer définitivement ?")) },
            text = {
                Text(
                    if (all && ui.trash.size == 1) tr("Le projet de la corbeille et ses sons seront effacés pour de bon. Impossible de revenir en arrière.")
                    else if (all) tr("Les %d projets de la corbeille et leurs sons seront effacés pour de bon. Impossible de revenir en arrière.", ui.trash.size)
                    else tr("« %s » et ses sons seront effacés pour de bon. Impossible de revenir en arrière.", com.pocketstudio.model.DefaultNames.shown(item!!.name)),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (all) actions.emptyTrash() else item?.let(actions.deleteForever)
                        confirmingDelete = null
                    },
                    enabled = !ui.busy,
                    modifier = Modifier.testTag("project_delete_forever_ok"),
                ) { Text(if (all) tr("Vider") else tr("Supprimer"), color = Neon.Amber) }
            },
            dismissButton = { TextButton(onClick = { confirmingDelete = null }) { Text(tr("Annuler")) } },
        )
    }
}

@Composable
private fun ProjectCard(p: ProjectInfo, open: Boolean, now: Long, actions: ProjectsActions, busy: Boolean = false, onRename: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Neon.Surface),
        border = BorderStroke(if (open) 2.dp else 1.dp, if (open) Neon.Acid else Neon.Outline),
        // An unreadable project is never opened (that would set it aside once more): its menu instead.
        modifier = Modifier.fillMaxWidth().clickable {
            when {
                p.unreadable -> menu = true
                !open -> actions.open(p)
                else -> (actions.resume ?: actions.back)()
            }
        }.testTag("project_${p.id}"),
    ) {
        Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(com.pocketstudio.model.DefaultNames.shown(p.name), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    // A plain badge: a section heading's accent bar drew a stray « ▌ » after the name (APK 181).
                    if (open) {
                        Text(tr("ouvert").uppercase(), style = MaterialTheme.typography.labelSmall, color = Neon.Acid, maxLines = 1,
                            modifier = Modifier.padding(start = 10.dp).testTag("project_open_badge"))
                    }
                }
                if (p.unreadable) {
                    Text(
                        tr("Ce fichier ne peut pas être relu. Envoie-le tel quel pour le faire réparer : il n'est jamais effacé tout seul."),
                        style = MaterialTheme.typography.bodySmall, color = Neon.Amber,
                    )
                } else Text(
                    listOfNotNull(
                        whenText(p.modified, now),
                        if (p.tempoBpm > 0) "${p.tempoBpm.toInt()} BPM" else null,
                        if (p.bars > 0) com.pocketstudio.app.ui.barsShort(p.bars) else null,
                        if (p.scenes > 1) tr("%d parties", p.scenes) else null,
                        if (p.hasRemix) "remix" else null,
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
                )
                if (open) {
                    TextButton(onClick = actions.shareSong, modifier = Modifier.testTag("project_share_song")) {
                        Icon(Icons.Filled.Share, contentDescription = null, tint = Neon.Acid, modifier = Modifier.padding(end = 6.dp))
                        Text(tr("Partager mon morceau"), color = Neon.Acid)
                    }
                }
            }
            Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.testTag("project_menu_${p.id}")) {
                    Icon(Icons.Filled.MoreVert, contentDescription = tr("Options de %s", com.pocketstudio.model.DefaultNames.shown(p.name)))
                }
                ScaledDropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    if (p.unreadable) {
                        DropdownMenuItem(
                            text = { Text(tr("Envoyer le fichier brut (.zip)")) },
                            onClick = { menu = false; actions.exportRaw(p) },
                            modifier = Modifier.testTag("project_raw_${p.id}"),
                        )
                    } else {
                        DropdownMenuItem(text = { Text(tr("Renommer")) }, onClick = { menu = false; onRename() })
                        DropdownMenuItem(text = { Text(tr("Dupliquer")) }, onClick = { menu = false; actions.duplicate(p) })
                        DropdownMenuItem(text = { Text(tr("Envoyer / sauvegarder (.zip)")) }, onClick = { menu = false; actions.exportZip(p) })
                        if (p.hasOpening) {
                            DropdownMenuItem(
                                text = { Text(if (open) tr("Copie d'avant cette séance") else tr("Copie d'avant la dernière séance")) },
                                onClick = { menu = false; actions.restoreOpening(p) },
                                modifier = Modifier.testTag("project_opening_${p.id}"),
                            )
                        }
                    }
                    DropdownMenuItem(
                        text = { Text(tr("Supprimer")) },
                        enabled = !busy,
                        onClick = { menu = false; actions.delete(p) },
                        modifier = Modifier.testTag("project_delete_${p.id}"),
                    )
                }
            }
        }
    }
}

/** Lot 1: no project on the phone: a new song or a project brought in, nothing else to read. */
@Composable
private fun EmptyProjects(newSong: () -> Unit, importZip: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Neon.Surface),
        border = BorderStroke(1.dp, Neon.Outline),
        modifier = Modifier.fillMaxWidth().testTag("projects_empty"),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(tr("Aucun projet pour l'instant"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(tr("Commence un morceau, ou ramène un projet envoyé depuis un autre téléphone."), style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary)
            Button(onClick = newSong, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("projects_empty_new")) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  " + tr("Nouveau morceau"), style = MaterialTheme.typography.titleSmall)
            }
            OutlinedButton(onClick = importZip, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("projects_empty_import")) {
                Text(tr("Importer un projet"))
            }
        }
    }
}

/** [ProjectsScreen]'s confirmation for the whole trash, not one entry (an entry always holds a « @ »). */
private const val EMPTY_ALL = "*"

/** A deleted project: its name, when it was deleted, a way back, and a way to delete it for good. */
@Composable
private fun TrashedCard(t: TrashedProject, now: Long, actions: ProjectsActions, busy: Boolean = false, onDelete: () -> Unit) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Neon.Surface),
        border = BorderStroke(1.dp, Neon.Outline),
        modifier = Modifier.fillMaxWidth().testTag("trashed_${t.entry}"),
    ) {
        Row(Modifier.padding(start = 14.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(com.pocketstudio.model.DefaultNames.shown(t.name), style = MaterialTheme.typography.titleMedium, color = Neon.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(tr("supprimé %s", whenText(t.deletedAt, now)), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
            }
            TextButton(onClick = { actions.restore(t) }, enabled = !busy, modifier = Modifier.testTag("restore_${t.entry}")) { Text(tr("Restaurer")) }
            IconButton(onClick = onDelete, enabled = !busy, modifier = Modifier.testTag("delete_forever_${t.entry}")) {
                Icon(Icons.Filled.DeleteForever, contentDescription = tr("Supprimer définitivement"), tint = Neon.TextSecondary)
            }
        }
    }
}

/** "à l'instant", "il y a 5 min", "il y a 3 h", "hier", "le 12/10/2026". */
fun whenText(time: Long, now: Long): String {
    val minutes = (now - time) / 60_000
    return when {
        minutes < 1 -> tr("à l'instant")
        minutes < 60 -> tr("il y a %d min", minutes)
        minutes < 24 * 60 -> tr("il y a %d h", minutes / 60)
        minutes < 48 * 60 -> tr("hier")
        else -> tr("le %s", shortDate(time))
    }
}

/** A date the way the interface's language writes it: 21/10/2026 in French and British English, 21-10-2026 in Dutch. */
fun shortDate(time: Long, lang: Lang = I18n.lang): String {
    val pattern = if (lang == Lang.NL) "dd-MM-yyyy" else "dd/MM/yyyy"
    return SimpleDateFormat(pattern, lang.locale).format(Date(time))
}
