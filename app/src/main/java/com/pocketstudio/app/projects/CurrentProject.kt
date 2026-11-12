package com.pocketstudio.app.projects

import android.content.Context
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.ProjectCatalog
import java.io.File

/**
 * Every project on the phone, and the trash; separated songs live in files/stems (see StemStore).
 * Blocking file work: call it off the main thread.
 */
fun projectCatalog(context: Context) =
    ProjectCatalog(File(context.filesDir, "projects"), File(context.filesDir, "projects-trash"), File(context.filesDir, "stems"), backupMirrors(context))

/**
 * The projects' compact copies for Android's cloud backup (ProjectStore): capped at 25 MB, it
 * holds these and not the projects' folders with their sounds (audit 14/19).
 */
fun backupMirrors(context: Context) = File(context.filesDir, "projects-backup")

/** Which project the app opens: the last one used (the first project's id on a new install). */
object CurrentProject {
    private const val PREFS = "projects"
    private const val KEY = "current"

    /**
     * Lot 1: no project open (the last one deleted). The screens still run on a blank project in
     * memory under this id, never saved ([isNone]); « Mes projets » shows its empty state.
     */
    const val NONE = "no-project"

    fun isNone(id: String) = id == NONE

    fun get(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: RhythmViewModel.PROJECT_ID

    /** Synchronous (commit): the next screen reads it at once. */
    fun set(context: Context, id: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, id).commit()
    }

    private const val DELETE = "delete_after_open"

    /** [id] (the project just left) goes to the trash at the next start of « Mes projets » (see [takeDeleteLater]). */
    fun deleteLater(context: Context, id: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(DELETE, id).commit()
    }

    private const val STARTER_ID = "starter_id"
    private const val STARTER = "starter_json"

    /**
     * Audit ultra round 2 (n° 14, seat B a: « Mon premier rythme », a project never made, in « Mes projets » next to the
     * song the assistant made at the first opening): the install's first project, as written when it was made.
     */
    fun rememberStarter(context: Context, id: String, project: com.pocketstudio.model.Project) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(STARTER_ID, id).putString(STARTER, com.pocketstudio.model.ProjectJson.encode(project)).commit()
    }

    /**
     * The install's first project once another one is open, if it is still exactly as it was made (nobody used it): its
     * id, once; null while it is open, or when it was used (it is then a project like the others, kept). Blocking (reads it).
     */
    fun takeUnusedStarter(
        context: Context,
        load: (String) -> com.pocketstudio.model.Project?,
        /** Its folder on the phone (null: not looked at). */
        folder: (String) -> File? = { null },
        /** A Live take's journal waits for it (written into the song at its next opening). */
        journaled: (String) -> Boolean = { false },
    ): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val id = prefs.getString(STARTER_ID, null) ?: return null
        if (id == get(context)) return null
        val made = prefs.getString(STARTER, null)
        prefs.edit().remove(STARTER_ID).remove(STARTER).commit()
        val now = runCatching { load(id) }.getOrNull() ?: return null
        if (made == null || com.pocketstudio.model.ProjectJson.encode(now) != made) return null
        // Relecture tour 2: work not in project.json yet (a take written but not yet on its track, in takes/ and samples/;
        // a Live take cut short) is work: the project is kept, its next opening puts it in.
        if (runCatching { journaled(id) }.getOrDefault(true)) return null
        if (folder(id)?.let(::holdsMoreThanItsFile) == true) return null
        return id
    }

    /** [dir] holds a file besides the project's own (project.json, its backup, its opening copy): a sound, a take. */
    internal fun holdsMoreThanItsFile(dir: File): Boolean {
        val own = setOf(com.pocketstudio.model.ProjectStore.MAIN, com.pocketstudio.model.ProjectStore.BACKUP, com.pocketstudio.model.ProjectStore.OPENING)
        return dir.walkTopDown().any { it.isFile && it.relativeTo(dir).invariantSeparatorsPath !in own }
    }

    /** The project to trash now, once (never the one open). */
    fun takeDeleteLater(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val id = prefs.getString(DELETE, null) ?: return null
        prefs.edit().remove(DELETE).commit()
        return id.takeIf { it != get(context) }
    }
}
