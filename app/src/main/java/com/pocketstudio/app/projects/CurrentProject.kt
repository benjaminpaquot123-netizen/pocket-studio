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
    fun takeUnusedStarter(context: Context, load: (String) -> com.pocketstudio.model.Project?): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val id = prefs.getString(STARTER_ID, null) ?: return null
        if (id == get(context)) return null
        val made = prefs.getString(STARTER, null)
        prefs.edit().remove(STARTER_ID).remove(STARTER).commit()
        val now = runCatching { load(id) }.getOrNull() ?: return null
        return id.takeIf { made != null && com.pocketstudio.model.ProjectJson.encode(now) == made }
    }

    /** The project to trash now, once (never the one open). */
    fun takeDeleteLater(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val id = prefs.getString(DELETE, null) ?: return null
        prefs.edit().remove(DELETE).commit()
        return id.takeIf { it != get(context) }
    }
}
