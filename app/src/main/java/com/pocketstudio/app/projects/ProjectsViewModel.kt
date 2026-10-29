package com.pocketstudio.app.projects

import com.pocketstudio.app.settings.ProjectModes

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
            // The open project deleted from the list: trashed now that another one is open.
            CurrentProject.takeDeleteLater(app)?.let { id -> catalog.info(id)?.let(::delete) }
        }
    }

    fun refresh() = io(tr("Liste des projets"), counts = false) { refreshNow() }

    /** Live take journals and Simple/Pro records of projects gone for good (deleted, purged from the trash): never written anywhere. */
    private fun forgetGoneJournals() {
        val app = getApplication<Application>()
        com.pocketstudio.app.rhythm.LiveTakeJournals.of(app).dropMissing(catalog.knownIds() + CurrentProject.get(app))
        ProjectModes.dropMissing(app) { catalog.knownIds() + CurrentProject.get(app) }
    }

    /** Creates [project] (a blank or a demo) named after the next free number unless it has a name; returns its id. */
    fun create(project: Project, replacing: String? = null, then: (String) -> Unit) = modeOfNew().let { simple -> io(tr("Nouveau projet")) {
        // [replacing]: a project about to go to the trash; its name is free again (no « Mon projet 1 » / « 2 » seesaw).
        val named = if (project.name.isBlank() || project.name == Project().name) project.copy(name = catalog.nextName(ignoring = replacing)) else project
        val id = catalog.create(named.asNewProject())
        ProjectModes.set(getApplication(), id, simple)
        refreshNow()
        withContext(Dispatchers.Main) { then(id) }
    } }

    /**
     * The mode the new project is made in (Simple or Pro, just chosen by the user): read when the
     * creation is asked, before the screens that asked for it close (and show the open project's).
     */
    private fun modeOfNew(): Boolean = com.pocketstudio.app.settings.SettingsStore.state.value.simpleMode

    /** A new project with its own sounds ([sounds]: file name in its samples folder → audio). */
    fun create(project: Project, sounds: Map<String, com.pocketstudio.app.audio.Pcm>, failed: (String) -> Unit = {}, then: (String) -> Unit) = modeOfNew().let { simple -> io(tr("Nouveau morceau"), failed) {
        // Never the same name as a song already there (a second « Rétro techno » is « Rétro techno 2 »):
        // the new song is plainly another project, the first one still in the list.
        val named = if (project.name.isBlank()) project.copy(name = catalog.nextName()) else project.copy(name = catalog.freeName(project.name))
        val id = catalog.create(named.asNewProject()) { dir ->
            dir.mkdirs()
            sounds.forEach { (name, pcm) -> com.pocketstudio.app.audio.FloatWav.write(java.io.File(dir, name), pcm) }
        }
        ProjectModes.set(getApplication(), id, simple)
        refreshNow()
        withContext(Dispatchers.Main) { then(id) }
    } }

    /**
     * [saveFirst]: saves the open project first (when [info] is it); false (not saved: a full
     * phone) stops here, rather than copying the older version on disk without a word.
     */
    fun duplicate(info: ProjectInfo, saveFirst: suspend () -> Boolean = { true }) = io(tr("Copie du projet")) {
        if (!saved(saveFirst)) return@io
        // Saved in French, shown translated (DefaultNames).
        val copy = catalog.duplicate(info.id, "${info.name} (copie)".take(ProjectCatalog.MAX_NAME))
        ProjectModes.copy(getApplication(), info.id, copy)
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
        // Still writing after that (a stuck write): nothing moves, the user tries again.
        if (!com.pocketstudio.app.rhythm.ClosingSaves.awaitIdle(CLOSING_WAIT_MS) || !com.pocketstudio.app.rhythm.TakeVault.app.awaitIdle(CLOSING_WAIT_MS)) {
            throw java.io.IOException(tr("une sauvegarde est encore en cours, réessaie dans un instant"))
        }
        // A blank project nobody touched is deleted outright: no trash entry, no « Annuler » for nothing.
        if (untouched(info.id)) {
            val entry = catalog.delete(info.id)
            catalog.deleteForever(entry)
            forgetGoneJournals()
            AppLog.add("Projet vide supprimé : « ${info.name} »")
            refreshNow()
            return@io
        }
        val entry = catalog.delete(info.id)
        AppLog.add("Projet mis à la corbeille : « ${info.name} »")
        _state.value = _state.value.copy(deleted = entry to com.pocketstudio.model.DefaultNames.shown(info.name))
        refreshNow()
    }

    /** [then] (main thread) gets whether project [id], as saved, is a blank one nobody edited. */
    fun checkUntouched(id: String, then: (Boolean) -> Unit) = io(tr("Liste des projets"), counts = false) {
        val untouched = untouched(id)
        withContext(Dispatchers.Main) { then(untouched) }
    }

    /** Blank and never edited, without a Live take in progress (its journal lives outside the project). */
    private fun untouched(id: String): Boolean =
        catalog.isUntouched(id) && !com.pocketstudio.app.rhythm.LiveTakeJournals.of(getApplication()).file(id).exists()

    /** A line under the buttons of « Mes projets ». */
    fun say(message: String, ok: Boolean = false) {
        _state.value = _state.value.copy(message = message, messageOk = ok)
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

    /**
     * A project the app cannot read, its folder as it is (files and sounds, nothing decoded) as a
     * .zip in the export folder; [then] gets the file (to send it, to have it repaired).
     */
    fun exportRaw(info: ProjectInfo, then: (File) -> Unit) = io(tr("Export du projet")) {
        val app = getApplication<Application>()
        val file = com.pocketstudio.app.export.WavExporter.newFile(app, "projet-illisible")
            .let { File(it.parentFile, it.name.removeSuffix(".wav") + ".zip") }
        file.outputStream().use { catalog.exportRawZip(info.id, it) }
        AppLog.add("Projet illisible envoyé tel quel : ${file.length() / 1024} Ko")
        withContext(Dispatchers.Main) { then(file) }
    }

    /**
     * « La version d'avant la séance » of [info] as a new project in the list (« … (avant la
     * séance) »), its sounds copied: the project itself is not touched.
     */
    fun restoreOpening(info: ProjectInfo) = io(tr("Copie du projet")) {
        val name = ProjectCatalog.beforeSessionName(info.name)
        catalog.restoreOpening(info.id, name)
        AppLog.add("Version d'avant la séance copiée")
        _state.value = _state.value.copy(message = tr("Copie faite : « %s ». Touche-la pour l'ouvrir.", com.pocketstudio.model.DefaultNames.shown(name)), messageOk = true)
        refreshNow()
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
