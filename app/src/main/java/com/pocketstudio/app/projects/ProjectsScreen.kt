package com.pocketstudio.app.projects

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
import com.pocketstudio.ui.SectionLabel
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
    val rename: (ProjectInfo, String) -> Unit = { _, _ -> },
    val duplicate: (ProjectInfo) -> Unit = {},
    /** Share the project and its sounds as a .zip. */
    val exportZip: (ProjectInfo) -> Unit = {},
    val importZip: () -> Unit = {},
    val delete: (ProjectInfo) -> Unit = {},
    val undoDelete: () -> Unit = {},
    /** Brings a project back from the "Corbeille" list. */
    val restore: (TrashedProject) -> Unit = {},
    val dismissDeleted: () -> Unit = {},
    val dismissMessage: () -> Unit = {},
    /** « Partager mon morceau »: the open project's song in M4A, then the share sheet. */
    val shareSong: () -> Unit = {},
)

/** "Mes projets": every project on the phone, the open one first in mind; open, create, rename, copy, delete. */
@Composable
fun ProjectsScreen(ui: ProjectsUi, currentId: String, starters: List<ProjectStarter>, actions: ProjectsActions, now: Long = System.currentTimeMillis()) {
    var renaming by remember { mutableStateOf<ProjectInfo?>(null) }
    var choosingStart by remember { mutableStateOf(false) }
    var showTrash by remember { mutableStateOf(false) }
    // A list to read: centred at a comfortable width on a tablet.
    com.pocketstudio.ui.ReadingWidth {
        Column(Modifier.fillMaxSize().padding(12.dp).testTag("projects"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = actions.back, modifier = Modifier.testTag("projects_back")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Retour"))
                }
                Text(tr("Mes projets"), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            }
            // The guided song first: the main action for a beginner.
            Button(onClick = actions.wizard, modifier = Modifier.fillMaxWidth().height(56.dp).testTag("project_wizard")) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                Text("  " + tr("Nouveau morceau guidé"), style = MaterialTheme.typography.titleMedium)
            }
            OutlinedButton(onClick = { choosingStart = true }, modifier = Modifier.fillMaxWidth().height(52.dp).testTag("project_new")) {
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
                        TextButton(onClick = actions.undoDelete, modifier = Modifier.testTag("project_undo_delete")) { Text(tr("Annuler")) }
                        TextButton(onClick = actions.dismissDeleted) { Text("OK") }
                    }
                }
            }
            ui.message?.let {
                Text(it, color = if (ui.messageOk) Neon.Acid else Neon.Amber, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable(onClick = actions.dismissMessage))
            }
            if (!ui.loading && ui.projects.isEmpty()) {
                Text(tr("Aucun projet enregistré pour l'instant."), color = Neon.TextSecondary)
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(ui.projects, key = { it.id }) { p ->
                    ProjectCard(p, p.id == currentId, now, actions, onRename = { renaming = p })
                }
                if (ui.trash.isNotEmpty()) {
                    item(key = "trash") {
                        TextButton(onClick = { showTrash = !showTrash }, modifier = Modifier.fillMaxWidth().testTag("project_trash")) {
                            Text(
                                (if (showTrash) "▾ " else "▸ ") + tr("Corbeille (%d) · gardés %d jours", ui.trash.size, ProjectCatalog.TRASH_DAYS),
                                color = Neon.TextSecondary, modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    if (showTrash) {
                        items(ui.trash, key = { "trash_${it.entry}" }) { t -> TrashedCard(t, now, actions) }
                    }
                }
            }
        }
    }
    renaming?.let { p ->
        var name by remember(p.id) { mutableStateOf(p.name) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text(tr("Renommer")) },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it.take(40) }, singleLine = true, label = { Text(tr("Nom du projet")) },
                    modifier = Modifier.testTag("project_name_field"))
            },
            confirmButton = {
                TextButton(onClick = { actions.rename(p, name); renaming = null }, enabled = name.isNotBlank(), modifier = Modifier.testTag("project_rename_ok")) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text(tr("Annuler")) } },
        )
    }
    if (choosingStart) {
        AlertDialog(
            onDismissRequest = { choosingStart = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            // The phone's whole width; a comfortable width on a tablet.
            modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(horizontal = 10.dp),
            title = { Text(tr("Nouveau projet")) },
            text = {
                Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(tr("Guidé"))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Neon.Cyan.copy(alpha = 0.18f)),
                        modifier = Modifier.fillMaxWidth().clickable { choosingStart = false; actions.wizard() }.testTag("starter_wizard"),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(tr("Nouveau morceau guidé"), style = MaterialTheme.typography.titleMedium, color = Neon.Cyan)
                            Text(tr("Idéal pour débuter : choisis un style, je t'explique chaque son et je prépare l'intro, le couplet et le refrain."),
                                style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                        }
                    }
                    // The other starters under their headings, in the order of the headings.
                    StarterGroup.entries.forEach { group ->
                        val inGroup = starters.withIndex().filter { it.value.group == group }
                        if (inGroup.isEmpty()) return@forEach
                        SectionLabel(group.title, Modifier.padding(top = 8.dp))
                        inGroup.forEach { (i, s) ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Neon.SurfaceHigh),
                                modifier = Modifier.fillMaxWidth().clickable { choosingStart = false; actions.create(i) }.testTag("starter_${s.tag}"),
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(tr(s.title), style = MaterialTheme.typography.titleMedium)
                                    Text(tr(s.description), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { choosingStart = false }) { Text(tr("Annuler")) } },
        )
    }
}

@Composable
private fun ProjectCard(p: ProjectInfo, open: Boolean, now: Long, actions: ProjectsActions, onRename: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Neon.Surface),
        border = BorderStroke(if (open) 2.dp else 1.dp, if (open) Neon.Acid else Neon.Outline),
        modifier = Modifier.fillMaxWidth().clickable { if (!open) actions.open(p) else actions.back() }.testTag("project_${p.id}"),
    ) {
        Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (open) SectionLabel("  " + tr("ouvert"), color = Neon.Acid)
                }
                Text(
                    listOfNotNull(
                        whenText(p.modified, now),
                        if (p.tempoBpm > 0) "${p.tempoBpm.toInt()} BPM" else null,
                        if (p.bars > 0) tr("%d mes.", p.bars) else null,
                        if (p.scenes > 1) tr("%d scènes", p.scenes) else null,
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
                    Icon(Icons.Filled.MoreVert, contentDescription = tr("Options de %s", p.name))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(tr("Renommer")) }, onClick = { menu = false; onRename() })
                    DropdownMenuItem(text = { Text(tr("Dupliquer")) }, onClick = { menu = false; actions.duplicate(p) })
                    DropdownMenuItem(text = { Text(tr("Envoyer / sauvegarder (.zip)")) }, onClick = { menu = false; actions.exportZip(p) })
                    DropdownMenuItem(
                        text = { Text(if (open) tr("Supprimer (ouvre d'abord un autre projet)") else tr("Supprimer")) },
                        enabled = !open,
                        onClick = { menu = false; actions.delete(p) },
                        modifier = Modifier.testTag("project_delete_${p.id}"),
                    )
                }
            }
        }
    }
}

/** A deleted project: its name, when it was deleted, and a way back. */
@Composable
private fun TrashedCard(t: TrashedProject, now: Long, actions: ProjectsActions) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Neon.Surface),
        border = BorderStroke(1.dp, Neon.Outline),
        modifier = Modifier.fillMaxWidth().testTag("trashed_${t.entry}"),
    ) {
        Row(Modifier.padding(start = 14.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(t.name, style = MaterialTheme.typography.titleMedium, color = Neon.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(tr("supprimé %s", whenText(t.deletedAt, now)), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
            }
            TextButton(onClick = { actions.restore(t) }, modifier = Modifier.testTag("restore_${t.entry}")) { Text(tr("Restaurer")) }
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
        else -> tr("le %s", SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(time)))
    }
}
