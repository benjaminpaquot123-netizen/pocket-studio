package com.pocketstudio.app.export

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.pocketstudio.app.projects.ProjectsViewModel
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.i18n.tr

/**
 * « Sauvegarder le morceau en .zip » (« Envoyer le projet » until the owner's answer 1A): a project packed with its
 * sounds, given to Android's share menu and copied to Documents › BenStudio, then said (« Projet sauvegardé : … »,
 * « Voir mes fichiers »).
 *
 * T5-ZIP (recette tel-03, and the review of the bench's lot 3): « Prêt à partager ? » and Mes projets › ⋯ each wrote
 * this by hand in AppRoot, so a fix could reach one and miss the other. Both now call this one sender.
 */
class ProjectZipSender internal constructor(
    private val context: Context, private val projectsVm: ProjectsViewModel, private val rhythmVm: RhythmViewModel,
) {
    /** The .zip just given to Android's share menu (its name, its copy kept in Documents › BenStudio): said until dismissed. */
    var saved by mutableStateOf<Pair<String, Boolean>?>(null)
        internal set

    /** The open project, from « Prêt à partager ? » (a failure is said in a toast: no list to say it in). */
    fun sendOpen() = send(rhythmVm.projectId, rhythmVm.project.value.name, toast = true)

    /** Project [id] named [name], from Mes projets › ⋯ (the list says a failure). The open project is saved first. */
    fun send(id: String, name: String, toast: Boolean = false) {
        projectsVm.exportZip(
            id, name, saveFirst = { id != rhythmVm.projectId || rhythmVm.flushNow() },
            failed = { if (toast) Toast.makeText(context, it, Toast.LENGTH_LONG).show() },
        ) { zip ->
            if (WavExporter.share(context, listOf(zip.file), tr("Sauvegarder le morceau en .zip"))) saved = zip.file.name to zip.kept
        }
    }
}

/** What « C'est prêt » says once a project's .zip is made: its file, and where its copy is. */
internal fun zipSavedLines(name: String, kept: Boolean): List<String> = listOfNotNull(
    tr("Projet sauvegardé : %s", name),
    if (kept) tr("Une copie est dans Documents › BenStudio : tu la retrouves dans « Mes fichiers ».") else null,
)

/** The app's one [ProjectZipSender], with its « C'est prêt » window (the same as a shared song's) and « Mes fichiers » from it. */
@Composable
fun rememberProjectZipSender(rhythmVm: RhythmViewModel): ProjectZipSender {
    val context = LocalContext.current
    val projectsVm: ProjectsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val sender = remember(rhythmVm, projectsVm) { ProjectZipSender(context, projectsVm, rhythmVm) }
    var files by rememberSaveable { mutableStateOf(false) }
    sender.saved?.let { (name, kept) ->
        ShareBilanDialog(zipSavedLines(name, kept), dismiss = { sender.saved = null }, seeFiles = { sender.saved = null; files = true })
    }
    if (files) MyExportsDialog { files = false }
    return sender
}
