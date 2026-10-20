package com.pocketstudio.app.projects

import android.content.Context
import com.pocketstudio.app.rhythm.RhythmViewModel

/** Which project the app opens: the last one used (the first project's id on a new install). */
object CurrentProject {
    private const val PREFS = "projects"
    private const val KEY = "current"

    fun get(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: RhythmViewModel.PROJECT_ID

    /** Synchronous (commit): the next screen reads it at once. */
    fun set(context: Context, id: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, id).commit()
    }
}
