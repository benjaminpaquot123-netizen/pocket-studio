package com.pocketstudio.app.projects

import com.pocketstudio.model.i18n.tr
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketstudio.app.report.AppLog
import com.pocketstudio.model.Project
import com.pocketstudio.model.asNewProject
import com.pocketstudio.model.ProjectCatalog
import com.pocketstudio.model.ProjectInfo
import com.pocketstudio.model.TrashedProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

data class ProjectsUi(
    val projects: List<ProjectInfo> = emptyList(),
    val loading: Boolean = true,
    /** Trash entry of the project just deleted (for "Annuler"), with its name. */
    val deleted: Pair<String, String>? = null,
    val message: String? = null,
    /** [message] is good news (a project imported), not an error. */
    val messageOk: Boolean = false,
    /** Deleted projects, kept [ProjectCatalog.TRASH_DAYS] days. */
    val trash: List<TrashedProject> = emptyList(),
    /** A change to the list is under way (a deletion, a restore, an import…): the others wait, their buttons off. */
    val busy: Boolean = false,
)

/**
 * The project browser: lists, creates, duplicates, renames and deletes projects. Opening one is
 * done by the screen (it saves the open project, records the choice in [CurrentProject] and
 * rebuilds the screens on it).
 */
class ProjectsViewModel(app: Application) : AndroidViewModel(app) {
    private val catalog = projectCatalog(app)

    /**
     * The catalog's operations run one at a time, in the order asked: a restore tapped while the
     * trash is being emptied waits for it (and finds nothing left), never renames a folder that is
     * half deleted.
     */
    private val operations = Mutex()
    private val running = AtomicInteger(0)

    /** Tests: runs inside each operation, before it touches the catalog. */
    internal var beforeOperationForTest: suspend (String) -> Unit = {}

    private val _state = MutableStateFlow(ProjectsUi())
    val state: StateFlow<ProjectsUi> = _state.asStateFlow()

    init {
        io(tr("Liste des projets"), counts = false) {
            catalog.purgeTrash(ProjectCatalog.TRASH_DAYS * 24 * 3600 * 1000)
            // Folders of a creation, import or copy the app was killed in (sounds, no project): lost room.
            val orphans = runCatching { catalog.cleanOrphans(keep = setOf(CurrentProject.get(app))) }.getOrDefault(0)
            if (orphans > 0) AppLog.add("Dossiers de projet inachevés effacés : $orphans")
            forgetGoneJournals()
        }
    }

    fun refresh() = io(tr("Liste des projets"), counts = false) { refreshNow() }

    /** Live take journals of projects gone for good (deleted, purged from the trash): never written anywhere. */
    private fun forgetGoneJournals() {
        val app = getApplication<Application>()
        com.pocketstudio.app.rhythm.LiveTakeJournals.of(app).dropMissing(catalog.knownIds() + CurrentProject.get(app))
    }

    /** Creates [project] (a blank or a demo) named after the next free number unless it has a name; returns its id. */
    fun create(project: Project, then: (String) -> Unit) = io(tr("Nouveau projet")) {
        val named = if (project.name.isBlank() || project.name == Project().name) project.copy(name = catalog.nextName()) else project
        val id = catalog.create(named.asNewProject())
        refreshNow()
        withContext(Dispatchers.Main) { then(id) }
    }

    /** A new project with its own sounds ([sounds]: file name in its samples folder → audio). */
    fun create(project: Project, sounds: Map<String, com.pocketstudio.app.audio.Pcm>, failed: (String) -> Unit = {}, then: (String) -> Unit) = io(tr("Nouveau morceau"), failed) {
        val id = catalog.create(project.asNewProject()) { dir ->
            dir.mkdirs()
            sounds.forEach { (name, pcm) -> com.pocketstudio.app.audio.FloatWav.write(java.io.File(dir, name), pcm) }
        }
        refreshNow()
        withContext(Dispatchers.Main) { then(id) }
    }

    /**
     * [saveFirst]: saves the open project first (when [info] is it); false (not saved: a full
     * phone) stops here, rather than copying the older version on disk without a word.
     */
    fun duplicate(info: ProjectInfo, saveFirst: suspend () -> Boolean = { true }) = io(tr("Copie du projet")) {
        if (!saved(saveFirst)) return@io
        // Saved in French, shown translated (DefaultNames).
        catalog.duplicate(info.id, "${info.name} (copie)".take(ProjectCatalog.MAX_NAME))
        refreshNow()
    }

    fun rename(id: String, name: String) = io(tr("Renommage")) {
        val clean = name.trim()
        if (clean.isNotEmpty()) catalog.rename(id, clean)
        refreshNow()
    }

    /** Deletes a trashed project for good (« Supprimer définitivement »): its room comes back now. */
    fun deleteForever(item: TrashedProject) = io(tr("Suppression")) {
        catalog.deleteForever(item.entry)
        forgetGoneJournals()
        AppLog.add("Projet supprimé définitivement : « ${item.name} »")
        if (_state.value.deleted?.first == item.entry) _state.value = _state.value.copy(deleted = null)
        refreshNow()
    }

    /** « Vider la corbeille »: every trashed project deleted for good. */
    fun emptyTrash() = io(tr("Suppression")) {
        val n = catalog.emptyTrash()
        forgetGoneJournals()
        AppLog.add("Corbeille vidée : $n projet(s)")
        _state.value = _state.value.copy(deleted = null)
        refreshNow()
    }

    fun delete(info: ProjectInfo) = io(tr("Suppression")) {
        // The last saves of the screen just closed on it (another project opened) first: a save
        // after the move would bring its folder back, in the list and in the trash at once.
        com.pocketstudio.app.rhythm.ClosingSaves.awaitIdle(CLOSING_WAIT_MS)
        com.pocketstudio.app.rhythm.TakeVault.app.awaitIdle(CLOSING_WAIT_MS)
        val entry = catalog.delete(info.id)
        AppLog.add("Projet mis à la corbeille : « ${info.name} »")
        _state.value = _state.value.copy(deleted = entry to com.pocketstudio.model.DefaultNames.shown(info.name))
        refreshNow()
    }

    /** The project and its sounds as a .zip in the export folder; [then] gets the file (to share it). [saveFirst]: see [duplicate]. */
    fun exportZip(info: ProjectInfo, saveFirst: suspend () -> Boolean = { true }, then: (File) -> Unit) = io(tr("Export du projet")) {
        if (!saved(saveFirst)) return@io
        val app = getApplication<Application>()
        // Dated like the audio exports (old ones cleaned): a share still reading an earlier export is never overwritten.
        val file = com.pocketstudio.app.export.WavExporter.newFile(app, com.pocketstudio.model.DefaultNames.shown(info.name).ifBlank { "projet" })
            .let { File(it.parentFile, it.name.removeSuffix(".wav") + ".pocketstudio.zip") }
        val missing = file.outputStream().use { catalog.exportZip(info.id, it) }
        AppLog.add("Projet exporté : ${file.length() / 1024} Ko, sons absents : $missing")
        if (missing > 0) _state.value = _state.value.copy(message = tr("%d son(s) du projet ne sont plus sur l'appareil : le .zip part sans eux.", missing), messageOk = false)
        withContext(Dispatchers.Main) { then(file) }
    }

    /** A project .zip (from this app) becomes a new project in the list. */
    fun importZip(uri: android.net.Uri) = io(tr("Import du projet")) {
        val app = getApplication<Application>()
        val stream = app.contentResolver.openInputStream(uri) ?: throw java.io.IOException(tr("fichier introuvable"))
        val imported = stream.use { catalog.importZip(it) }
        AppLog.add("Projet importé, sons absents : ${imported.missingSounds}")
        val name = catalog.info(imported.id)?.name?.let { com.pocketstudio.model.DefaultNames.shown(it) }
        _state.value = if (imported.missingSounds == 0) {
            _state.value.copy(message = tr("Projet importé : « %s ». Touche-le pour l'ouvrir.", name), messageOk = true)
        } else {
            // Imported all the same: a sound missing from the .zip no longer refuses the whole project.
            _state.value.copy(message = tr("Projet importé : « %s », sans %d son(s) absent(s) du .zip.", name, imported.missingSounds), messageOk = false)
        }
        refreshNow()
    }

    fun undoDelete() {
        val (entry, _) = _state.value.deleted ?: return
        io(tr("Restauration")) {
            catalog.restore(entry)
            _state.value = _state.value.copy(deleted = null)
            refreshNow()
        }
    }

    /** Brings a project back from the trash ("Corbeille" list). */
    fun restore(item: TrashedProject) = io(tr("Restauration")) {
        catalog.restore(item.entry)
        AppLog.add("Projet restauré : « ${item.name} »")
        if (_state.value.deleted?.first == item.entry) _state.value = _state.value.copy(deleted = null)
        refreshNow()
    }

    fun dismissDeleted() {
        _state.value = _state.value.copy(deleted = null)
    }

    fun dismissMessage() {
        _state.value = _state.value.copy(message = null, messageOk = false)
    }

    /** Runs [saveFirst]; false: says the open project could not be saved (and the caller stops). */
    private suspend fun saved(saveFirst: suspend () -> Boolean): Boolean {
        if (withContext(Dispatchers.Main) { saveFirst() }) return true
        AppLog.add("Projet ouvert non sauvegardé : copie ou envoi refusé")
        _state.value = _state.value.copy(message = tr("Le projet n'a pas pu être sauvegardé (appareil plein ?) : libère de la place (la corbeille peut se vider), puis réessaie."), messageOk = false)
        return false
    }

    private fun refreshNow() {
        _state.value = _state.value.copy(projects = catalog.list(), trash = catalog.trashed(), loading = false)
    }

    /**
     * [block] on the catalog, after the operations asked before ([operations]); [counts]: a change
     * to the list ([ProjectsUi.busy] while it waits or runs), not just a look at it.
     */
    private fun io(what: String, failed: (String) -> Unit = {}, counts: Boolean = true, block: suspend () -> Unit) {
        if (counts) {
            running.incrementAndGet()
            _state.update { it.copy(busy = true) }
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                operations.withLock {
                    beforeOperationForTest(what)
                    block()
                }
            } catch (e: Exception) {
                AppLog.add("$what impossible : ${e.message}")
                val message = tr("%s impossible (%s).", what, e.message ?: tr("erreur"))
                _state.value = _state.value.copy(message = message, messageOk = false, loading = false)
                withContext(Dispatchers.Main) { failed(message) }
            } finally {
                if (counts) {
                    running.decrementAndGet()
                    _state.update { it.copy(busy = running.get() > 0) }
                }
            }
        }
    }

    private companion object {
        /** How long a deletion waits for the last saves of a screen that closed. */
        const val CLOSING_WAIT_MS = 10_000L
    }
}
