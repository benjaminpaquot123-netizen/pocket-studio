package com.pocketstudio.app.ui

import com.pocketstudio.model.OpenOn
import com.pocketstudio.model.Project

/**
 * The screen a project opens on (Benjamin's list n° 5, choice 1A), in the Mode simple and in Pro
 * alike: « Mon morceau » when its song has something to hear, else the drum pads (Rythme). Applied
 * when the app starts on a project (« Continuer »), when another project opens (« Mes projets »,
 * « Nouveau projet », a project imported then opened) and on « Continuer » from the home screen.
 */
internal object OpeningTab {
    fun of(p: Project): AppTab = if (OpenOn.song(p)) AppTab.Morceau else AppTab.Rythme

    /**
     * The screen once the activity is rebuilt: [saved] (restored with the rest of the state) is kept
     * for the same project (a rotation, the process restored) or when the way that opened the new one
     * asked for a screen ([asked]: the Remix, a song just made, a tutorial); else project [p]'s.
     */
    fun afterRebuild(saved: AppTab, savedFor: String?, projectId: String, asked: Boolean, p: Project): AppTab =
        if (savedFor == projectId || asked) saved else of(p)
}
