package com.pocketstudio.app.rhythm

/**
 * A message for a project about to open (lot « tuto 5 »: the assistant replaced a pack sound it
 * could not read): opening a project builds the screens again, so it is kept here, by project,
 * until that project's [RhythmViewModel] shows it as its notice. Gone with the process.
 */
object PendingNotice {
    private val waiting = java.util.concurrent.ConcurrentHashMap<String, String>()

    fun put(projectId: String, text: String?) {
        if (text != null) waiting[projectId] = text
    }

    fun take(projectId: String): String? = waiting.remove(projectId)
}
