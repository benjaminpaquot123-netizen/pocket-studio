package com.pocketstudio.app.settings

import android.content.Context
import com.pocketstudio.ui.MemoryFoldStore

/** Folded cards, kept in the phone's preferences: a card folded once stays folded next time. */
class FoldPrefs(context: Context) : MemoryFoldStore(load(context)) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun setFolded(key: String, folded: Boolean) {
        super.setFolded(key, folded)
        prefs.edit().apply { if (folded) putBoolean(key, true) else remove(key) }.apply()
    }

    override fun unfoldAll() {
        super.unfoldAll()
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS = "folds"

        fun load(context: Context): Map<String, Boolean> =
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).all
                .filterValues { it == true }.mapValues { true }
    }
}
