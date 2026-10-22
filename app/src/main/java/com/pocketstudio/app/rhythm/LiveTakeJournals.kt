package com.pocketstudio.app.rhythm

import android.content.Context
import com.pocketstudio.app.report.AppLog
import com.pocketstudio.model.TakeJournalStore

/**
 * The app's one [TakeJournalStore] (journals of the Live takes in progress, in filesDir): shared
 * by the screens of every project, so the writes of a screen that goes away and the reading of
 * the next one take turns.
 */
object LiveTakeJournals {
    @Volatile private var store: TakeJournalStore? = null

    fun of(context: Context): TakeJournalStore {
        val dir = context.filesDir
        store?.takeIf { it.dir == dir }?.let { return it }
        return synchronized(this) {
            store?.takeIf { it.dir == dir } ?: TakeJournalStore(dir) { AppLog.add("Journal de la prise Live : ${it.message}") }.also { store = it }
        }
    }
}
