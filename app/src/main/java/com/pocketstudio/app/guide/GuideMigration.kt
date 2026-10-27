package com.pocketstudio.app.guide

import android.content.Context

/**
 * G5: the settings the old guidance surfaces left on the phone, read once. The Missions « Mon
 * premier morceau » (SharedPreferences « missions »: on or not, the step, the steps skipped) are now
 * the Guide's first cards of the open song ([com.pocketstudio.model.guide.GuideStart.fromMissions]).
 * « Prochaine étape » (« next_steps ») and « Ton morceau est prêt » (« song_ready ») kept only what
 * was closed or seen: nothing to carry over, they are cleared.
 */
object GuideMigration {
    private const val MISSIONS = "missions"
    private const val ACTIVE = "active"
    private const val STEP = "step"
    private const val SKIPPED = "skipped"
    private val OLD = listOf("next_steps", "song_ready")

    /**
     * The Missions a user was in the middle of (the step reached, the steps skipped), then forgotten
     * for good; null when none were on, or all were done.
     */
    fun missions(context: Context): Pair<Int, Set<Int>>? {
        OLD.forEach { name -> context.getSharedPreferences(name, Context.MODE_PRIVATE).takeIf { it.all.isNotEmpty() }?.edit()?.clear()?.apply() }
        val prefs = context.getSharedPreferences(MISSIONS, Context.MODE_PRIVATE)
        if (prefs.all.isEmpty()) return null
        val active = prefs.getBoolean(ACTIVE, false)
        val step = prefs.getInt(STEP, 0)
        val skipped = prefs.getStringSet(SKIPPED, emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
        prefs.edit().clear().apply()
        return if (active && step in 0 until com.pocketstudio.model.guide.GuideStart.MISSION_CARDS.size) step to skipped else null
    }
}
