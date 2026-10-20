package com.pocketstudio.app.projects

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketstudio.app.report.AppLog
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectCatalog
import com.pocketstudio.model.ProjectInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class ProjectsUi(
    val projects: List<ProjectInfo> = emptyList(),
    val loading: Boolean = true,
    /** Trash entry of the project just deleted (for "Annuler"), with its name. */
    val deleted: Pair<String, String>? = null,
    val message: String? = null,
)

/**
 * The project browser: lists, creates, duplicates, renames and deletes projects. Opening one is
 * done by the screen (it saves the open project, records the choice in [CurrentProject] and
 * rebuilds the screens on it).
 */
class ProjectsViewModel(app: Application) : AndroidViewModel(app) {
    private val catalog = ProjectCatalog(File(app.filesDir, "projects"), File(app.filesDir, "projects-trash"))

    private val _state = MutableStateFlow(ProjectsUi())
    val state: StateFlow<ProjectsUi> = _state.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) { catalog.purgeTrash(ProjectCatalog.TRASH_DAYS * 24 * 3600 * 1000) }
    }

    fun refresh() = io("Liste des projets") {
        _state.value = _state.value.copy(projects = catalog.list(), loading = false)
    }

    /** Creates [project] (a blank or a demo) named after the next free number unless it has a name; returns its id. */
    fun create(project: Project, then: (String) -> Unit) = io("Nouveau projet") {
        val named = if (project.name.isBlank() || project.name == Project().name) project.copy(name = catalog.nextName()) else project
        val id = catalog.create(named)
        refreshNow()
        withContext(Dispatchers.Main) { then(id) }
    }

    /** A new project with its own sounds ([sounds]: file name in its samples folder → audio). */
    fun create(project: Project, sounds: Map<String, com.pocketstudio.app.audio.Pcm>, then: (String) -> Unit) = io("Nouveau morceau") {
        val id = catalog.create(project) { dir ->
            dir.mkdirs()
            sounds.forEach { (name, pcm) -> com.pocketstudio.app.audio.FloatWav.write(java.io.File(dir, name), pcm) }
        }
        refreshNow()
        withContext(Dispatchers.Main) { then(id) }
    }

    fun duplicate(info: ProjectInfo) = io("Copie du projet") {
        catalog.duplicate(info.id, "${info.name} (copie)".take(ProjectCatalog.MAX_NAME))
        refreshNow()
    }

    fun rename(id: String, name: String) = io("Renommage") {
        val clean = name.trim()
        if (clean.isNotEmpty()) catalog.rename(id, clean)
        refreshNow()
    }

    fun delete(info: ProjectInfo) = io("Suppression") {
        val entry = catalog.delete(info.id)
        AppLog.add("Projet mis à la corbeille : « ${info.name} »")
        _state.value = _state.value.copy(deleted = entry to info.name)
        refreshNow()
    }

    /** The project and its sounds as a .zip in the export folder; [then] gets the file (to share it). */
    fun exportZip(info: ProjectInfo, then: (File) -> Unit) = io("Export du projet") {
        val app = getApplication<Application>()
        val dir = File(app.cacheDir, "exports").apply { mkdirs() }
        val safe = info.name.replace(Regex("[^\\p{L}\\p{N} _-]"), "").trim().replace(' ', '-').take(40).ifEmpty { "projet" }
        val file = File(dir, "$safe.pocketstudio.zip")
        file.outputStream().use { catalog.exportZip(info.id, it) }
        AppLog.add("Projet exporté : ${file.length() / 1024} Ko")
        withContext(Dispatchers.Main) { then(file) }
    }

    /** A project .zip (from this app) becomes a new project in the list. */
    fun importZip(uri: android.net.Uri) = io("Import du projet") {
        val app = getApplication<Application>()
        val stream = app.contentResolver.openInputStream(uri) ?: throw java.io.IOException("fichier introuvable")
        val id = stream.use { catalog.importZip(it) }
        AppLog.add("Projet importé")
        _state.value = _state.value.copy(message = "Projet importé : « ${catalog.info(id)?.name} ». Touche-le pour l'ouvrir.")
        refreshNow()
    }

    fun undoDelete() {
        val (entry, _) = _state.value.deleted ?: return
        io("Restauration") {
            catalog.restore(entry)
            _state.value = _state.value.copy(deleted = null)
            refreshNow()
        }
    }

    fun dismissDeleted() {
        _state.value = _state.value.copy(deleted = null)
    }

    fun dismissMessage() {
        _state.value = _state.value.copy(message = null)
    }

    private fun refreshNow() {
        _state.value = _state.value.copy(projects = catalog.list(), loading = false)
    }

    private fun io(what: String, block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                block()
            } catch (e: Exception) {
                AppLog.add("$what impossible : ${e.message}")
                _state.value = _state.value.copy(message = "$what impossible (${e.message ?: "erreur"}).", loading = false)
            }
        }
    }
}
