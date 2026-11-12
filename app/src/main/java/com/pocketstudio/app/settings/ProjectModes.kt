package com.pocketstudio.app.settings

import android.content.Context

/**
 * Owner (2026-10-29): Simple or Pro is chosen for each new project and kept with it. The app
 * still follows one [AppSettings.simpleMode] (what the screens and words show); this store
 * remembers each project's own choice (SharedPreferences "project_modes", by project id) and the
 * last one made, suggested at the next start (never chosen for the user).
 *
 * A project with no record (made before this, imported, copied) takes the mode it is first
 * opened in: nothing changes for it.
 */
object ProjectModes {
    private const val PREFS = "project_modes"
    private const val LAST = "last"
    private fun key(id: String) = "p:$id"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** [id]'s mode: true Simple, false Pro, null not recorded yet. */
    fun of(context: Context, id: String): Boolean? =
        prefs(context).let { p -> if (p.contains(key(id))) p.getBoolean(key(id), false) else null }

    fun set(context: Context, id: String, simple: Boolean) {
        prefs(context).edit().putBoolean(key(id), simple).apply()
    }

    /** The last mode chosen (at a start or with the top bar's chip), null before any choice. */
    fun last(context: Context): Boolean? =
        prefs(context).let { p -> if (p.contains(LAST)) p.getBoolean(LAST, false) else null }

    /** A choice made by the user: the app shows it now and it is suggested next time. */
    fun choose(context: Context, simple: Boolean) {
        prefs(context).edit().putBoolean(LAST, simple).apply()
        if (SettingsStore.state.value.simpleMode != simple) SettingsStore.update(context) { it.copy(simpleMode = simple) }
    }

    /**
     * [id] is the open project, on its own screens (no start under way): its recorded mode is
     * shown ([apply]), or the mode shown becomes its record when it has none.
     */
    fun follow(context: Context, id: String, shown: Boolean, apply: (Boolean) -> Unit) {
        when (val rec = of(context, id)) {
            null -> set(context, id, shown)
            shown -> Unit
            else -> apply(rec)
        }
    }

    /** A copy of a project keeps its mode. */
    fun copy(context: Context, from: String, to: String) {
        of(context, from)?.let { set(context, to, it) }
    }

    fun forget(context: Context, id: String) {
        prefs(context).edit().remove(key(id)).apply()
    }

    /**
     * Review (MINEUR 2): the records of projects gone for good (deleted, purged from the trash), [keep]
     * the projects there. The records are read first: a project's folder is made before its record,
     * so one created meanwhile is in [keep] when it is read.
     */
    fun dropMissing(context: Context, keep: () -> Set<String>) {
        val p = prefs(context)
        val recorded = p.all.keys.filter { it.startsWith("p:") }
        if (recorded.isEmpty()) return
        val there = keep()
        val gone = recorded.filter { it.removePrefix("p:") !in there }
        if (gone.isNotEmpty()) p.edit().apply { gone.forEach(::remove) }.apply()
    }
}
