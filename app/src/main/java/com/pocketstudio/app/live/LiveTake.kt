package com.pocketstudio.app.live

/**
 * The ● of the Live page (« Enregistrer la performance »): ready; armed, waiting for the launch
 * line the take starts on ([start], a transport step); recording ([seconds] and the song's
 * [bar] so far); writing the song; done ([parts] song parts and [settings] curves written, the
 * take's first part being [firstPart]).
 */
sealed interface TakeUi {
    data object Ready : TakeUi
    data class Armed(val start: Int, val stepsLeft: Int = 0) : TakeUi
    data class Recording(val start: Int, val seconds: Int = 0, val bar: Int = 1) : TakeUi
    data object Writing : TakeUi
    data class Done(val parts: Int, val settings: Int, val firstPart: Int) : TakeUi

    /** Armed or recording: Annuler waits, gestures are written. */
    val running: Boolean get() = this is Armed || this is Recording
}
