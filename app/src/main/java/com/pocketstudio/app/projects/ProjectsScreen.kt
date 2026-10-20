package com.pocketstudio.app.projects

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.pocketstudio.model.ProjectInfo
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.SectionLabel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** A way to start a project: a name, a line saying what it holds, and how to build it. */
class ProjectStarter(val title: String, val description: String, val tag: String)

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
    val dismissDeleted: () -> Unit = {},
    val dismissMessage: () -> Unit = {},
)

/** "Mes projets": every project on the phone, the open one first in mind; open, create, rename, copy, delete. */
@Composable
fun ProjectsScreen(ui: ProjectsUi, currentId: String, starters: List<ProjectStarter>, actions: ProjectsActions, now: Long = System.currentTimeMillis()) {
    var renaming by remember { mutableStateOf<ProjectInfo?>(null) }
    var choosingStart by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(12.dp).testTag("projects"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = actions.back, modifier = Modifier.testTag("projects_back")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
            }
            Text("Mes projets", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        }
        Button(onClick = { choosingStart = true }, modifier = Modifier.fillMaxWidth().height(52.dp).testTag("project_new")) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text("  Nouveau projet", style = MaterialTheme.typography.titleMedium)
        }
        TextButton(onClick = actions.importZip, modifier = Modifier.fillMaxWidth().testTag("project_import")) {
            Text("Importer un projet (.zip)")
        }
        ui.deleted?.let { (_, name) ->
            Card(colors = CardDefaults.cardColors(containerColor = Neon.SurfaceHigh), modifier = Modifier.testTag("project_deleted")) {
                Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("« $name » est dans la corbeille (30 jours).", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = actions.undoDelete, modifier = Modifier.testTag("project_undo_delete")) { Text("Annuler") }
                    TextButton(onClick = actions.dismissDeleted) { Text("OK") }
                }
            }
        }
        ui.message?.let {
            Text(it, color = if (it.startsWith("Projet importé")) Neon.Acid else Neon.Amber, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.clickable(onClick = actions.dismissMessage))
        }
        if (!ui.loading && ui.projects.isEmpty()) {
            Text("Aucun projet enregistré pour l'instant.", color = Neon.TextSecondary)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(ui.projects, key = { it.id }) { p ->
                ProjectCard(p, p.id == currentId, now, actions, onRename = { renaming = p })
            }
        }
    }
    renaming?.let { p ->
        var name by remember(p.id) { mutableStateOf(p.name) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Renommer") },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it.take(40) }, singleLine = true, label = { Text("Nom du projet") },
                    modifier = Modifier.testTag("project_name_field"))
            },
            confirmButton = {
                TextButton(onClick = { actions.rename(p, name); renaming = null }, enabled = name.isNotBlank(), modifier = Modifier.testTag("project_rename_ok")) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("Annuler") } },
        )
    }
    if (choosingStart) {
        AlertDialog(
            onDismissRequest = { choosingStart = false },
            title = { Text("Nouveau projet") },
            text = {
                Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Neon.Cyan.copy(alpha = 0.18f)),
                        modifier = Modifier.fillMaxWidth().clickable { choosingStart = false; actions.wizard() }.testTag("starter_wizard"),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Assistant pas à pas", style = MaterialTheme.typography.titleMedium, color = Neon.Cyan)
                            Text("Idéal pour débuter : choisis un style, je t'explique chaque son et je prépare la grille.",
                                style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                        }
                    }
                    starters.forEachIndexed { i, s ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Neon.SurfaceHigh),
                            modifier = Modifier.fillMaxWidth().clickable { choosingStart = false; actions.create(i) }.testTag("starter_${s.tag}"),
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(s.title, style = MaterialTheme.typography.titleMedium)
                                Text(s.description, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { choosingStart = false }) { Text("Annuler") } },
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
                    if (open) SectionLabel("  ouvert", color = Neon.Acid)
                }
                Text(
                    listOfNotNull(
                        whenText(p.modified, now),
                        if (p.tempoBpm > 0) "${p.tempoBpm.toInt()} BPM" else null,
                        if (p.bars > 0) "${p.bars} mes." else null,
                        if (p.scenes > 1) "${p.scenes} scènes" else null,
                        if (p.hasRemix) "remix" else null,
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
                )
            }
            Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.testTag("project_menu_${p.id}")) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Options de ${p.name}")
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Renommer") }, onClick = { menu = false; onRename() })
                    DropdownMenuItem(text = { Text("Dupliquer") }, onClick = { menu = false; actions.duplicate(p) })
                    DropdownMenuItem(text = { Text("Envoyer / sauvegarder (.zip)") }, onClick = { menu = false; actions.exportZip(p) })
                    DropdownMenuItem(
                        text = { Text(if (open) "Supprimer (ouvre d'abord un autre projet)" else "Supprimer") },
                        enabled = !open,
                        onClick = { menu = false; actions.delete(p) },
                        modifier = Modifier.testTag("project_delete_${p.id}"),
                    )
                }
            }
        }
    }
}

/** "à l'instant", "il y a 5 min", "il y a 3 h", "hier", "le 12/10/2026". */
fun whenText(time: Long, now: Long): String {
    val minutes = (now - time) / 60_000
    return when {
        minutes < 1 -> "à l'instant"
        minutes < 60 -> "il y a $minutes min"
        minutes < 24 * 60 -> "il y a ${minutes / 60} h"
        minutes < 48 * 60 -> "hier"
        else -> "le " + SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(time))
    }
}
