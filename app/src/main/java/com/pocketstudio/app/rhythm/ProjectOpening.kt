package com.pocketstudio.app.rhythm

import android.content.Context
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.report.AppLog
import com.pocketstudio.model.PendingTakes
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.TakeJournalStore
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.withTakes
import com.pocketstudio.model.writeJournal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** A project read from disk: what the screens open on, and what to tell the user (null: nothing). */
class OpenedProject(val id: String, val project: Project, val notice: String? = null, val rescued: Boolean = false)

/**
 * Reading the project to open, with what waited for it (takes finished while the app closed, a
 * Live take cut short). Blocking file work: the app runs it off the main thread ([prepare]) before
 * the screens are made; the screen's ViewModel then only [take]s the result.
 */
class ProjectOpener(private val id: String, private val store: ProjectStore, private val journals: TakeJournalStore) {
    private var notice: String? = null

    fun open(): OpenedProject = OpenedProject(id, loadOrCreate(), notice)

    private fun loadOrCreate(): Project {
        if (CurrentProject.isNone(id)) return RhythmViewModel.starterProject()
        try {
            val loaded = store.load(id)
            if (loaded != null) return withWaitingLiveTake(withWaitingTakes(loaded))
        } catch (e: Exception) {
            AppLog.add("Projet illisible : ${e.message} / ${e.cause?.message}")
            // Never let the first autosave overwrite files we could not read: move them aside.
            val aside = File(store.folder(id).parentFile, "$id${com.pocketstudio.model.ProjectCatalog.UNREADABLE}${System.currentTimeMillis()}")
            val moved = store.folder(id).renameTo(aside)
            notice = if (moved) {
                tr("Le projet n'a pas pu être relu. Il est mis de côté (%s) et un nouveau projet commence.", aside.name)
            } else {
                tr("Le projet n'a pas pu être relu ; un nouveau projet commence.")
            }
        }
        return RhythmViewModel.starterProject()
    }

    /**
     * Takes finished while the app was closing ([TakeVault]) put on their tracks, saved with the
     * project, then forgotten. The project as loaded when that fails (they wait for next time).
     */
    private fun withWaitingTakes(loaded: Project): Project {
        val folder = store.folder(id)
        val takes = runCatching { PendingTakes.read(folder) }.getOrElse { emptyList() }
        if (takes.isEmpty()) return loaded
        val p = loaded.withTakes(takes).sanitized()
        return try {
            if (p != loaded) store.save(id, p)
            PendingTakes.clear(folder, takes)
            if (p != loaded) {
                AppLog.add("Prise récupérée : ${takes.size}")
                notice = tr("Ta dernière prise a été gardée : elle est sur sa piste audio.")
            }
            p
        } catch (e: Exception) {
            AppLog.add("Prise en attente : ${e.message}")
            loaded
        }
    }

    /**
     * A Live take the app could not finish (closed or killed while it recorded) written into the
     * song as it was played up to there, saved, then forgotten; the project as loaded when it is
     * another project's, already in the song ([Project.liveTake]: a journal left by a kill after
     * its take was saved) or that fails (an unreadable journal is dropped).
     */
    private fun withWaitingLiveTake(loaded: Project): Project {
        // After the writes of the screen that closed (the writer keeps their order).
        val journal = journals.read(id) ?: return loaded
        if (journal.projectId != id) return loaded  // another project's: when it opens
        val written = loaded.writeJournal(journal, id)
        return try {
            if (written.parts > 0) store.save(id, written.project)
            // Only once the song holding it is on disk (a kill before: written at the next opening).
            journals.drop(id, journal.takeId.ifEmpty { null })
            if (written.parts > 0) {
                AppLog.add("Prise Live récupérée : ${written.parts} parties")
                notice = tr("Ta prise Live a été gardée : elle est dans le morceau (%d parties).", written.parts)
                written.project
            } else {
                if (written.already) AppLog.add("Prise Live déjà dans le morceau : journal effacé")
                loaded
            }
        } catch (e: Exception) {
            AppLog.add("Prise Live en attente : ${e.message}")
            loaded
        }
    }
}

/**
 * The project read before the screens exist (audit 14/15/16: reading it in the ViewModel's
 * constructor held the main thread up to 6 s), and the rescue from a project that crashes the app
 * as it opens: a mark is written before each opening and taken away once the screens have run a
 * moment ([settled]); found twice in a row, the app opens on no project (« Mes projets ») and says
 * why, the project itself untouched.
 */
object ProjectOpening {
    private const val PREFS = "opening"
    private const val KEY_ID = "id"
    private const val KEY_TRIES = "tries"
    private const val KEY_SET_UP = "backup_checked"
    /** Openings of the same project that never settled before the rescue. */
    const val RESCUE_AFTER = 2
    /** How long the saves and takes of a screen just closed are waited for (their own cap). */
    const val WAIT_MS = 10_000L

    @Volatile private var prepared: OpenedProject? = null

    /**
     * Reads the current project (off the main thread), after the last saves and takes of a screen
     * just closed; kept for the next [take]. Returns it too.
     */
    suspend fun prepare(context: Context): OpenedProject {
        val app = context.applicationContext
        if (!TakeVault.app.awaitIdle(WAIT_MS)) AppLog.add("Ouverture : une prise s'écrit encore")
        if (!ClosingSaves.awaitIdle(WAIT_MS)) AppLog.add("Ouverture : une sauvegarde s'écrit encore")
        val opened = withContext(Dispatchers.IO) {
            restoreFromBackupOnce(app)
            val id = CurrentProject.get(app)
            if (!CurrentProject.isNone(id) && markOpening(app, id) > RESCUE_AFTER) {
                AppLog.add("Ouverture sautée : le projet a fermé l'appli deux fois")
                clearMark(app)
                CurrentProject.set(app, CurrentProject.NONE)
                val name = runCatching { projectStore(app).load(id)?.name }.getOrNull()
                    ?.let { com.pocketstudio.model.DefaultNames.shown(it) } ?: id
                OpenedProject(
                    CurrentProject.NONE, RhythmViewModel.starterProject(),
                    tr("BenStudio s'est fermé deux fois en ouvrant « %s ». Le projet est gardé tel quel : rouvre-le depuis Mes projets, ou commence un projet vide.", name),
                    rescued = true,
                )
            } else {
                ProjectOpener(id, projectStore(app), LiveTakeJournals.of(app)).open()
            }
        }
        prepared = opened
        return opened
    }

    /** The project [prepare]d for [id], once (null: read it now). */
    fun take(id: String): OpenedProject? {
        val p = prepared ?: return null
        prepared = null
        return p.takeIf { it.id == id }
    }

    /** A project prepared that no screen took (the activity gone first) is forgotten. */
    fun forget(p: OpenedProject?) {
        if (p != null && prepared === p) prepared = null
    }

    /** The screens have run a moment on the project opened: no crash at its opening. */
    fun settled(context: Context) = clearMark(context.applicationContext)

    /** Notes one more opening of [id]; returns how many in a row never settled, this one included. */
    internal fun markOpening(context: Context, id: String): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val tries = if (prefs.getString(KEY_ID, null) == id) prefs.getInt(KEY_TRIES, 0) + 1 else 1
        // Synchronous: a crash right after must find it.
        prefs.edit().putString(KEY_ID, id).putInt(KEY_TRIES, tries).commit()
        return tries
    }

    private fun clearMark(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_ID).remove(KEY_TRIES).commit()
    }

    fun projectStore(context: Context) = ProjectStore(File(context.filesDir, "projects"), backup = com.pocketstudio.app.projects.backupMirrors(context))

    /**
     * Once per install (this mark is never backed up): the projects Android's cloud backup brought
     * back as compact copies alone (a phone set up again) are written back, without their sounds.
     */
    private fun restoreFromBackupOnce(app: Context) {
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_SET_UP, false)) return
        val catalog = com.pocketstudio.app.projects.projectCatalog(app)
        val n = runCatching { catalog.restoreFromBackup() }.getOrDefault(0)
        // An app updated from a version without the copies: every project gets one now.
        runCatching { catalog.mirrorMissing() }
        if (n > 0) AppLog.add("Projets retrouvés dans la sauvegarde Android : $n")
        prefs.edit().putBoolean(KEY_SET_UP, true).commit()
    }
}
