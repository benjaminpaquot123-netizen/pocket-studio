package com.pocketstudio.model.tuto

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Tour 7, tuto3 (seat de): « Schon erledigt! » then « nichts zu tun »: a step the song already met read as « done » and
 * « nothing to do » at once. The German panel says the song already has it, and that nothing needs changing.
 */
class Tour7TutoAlreadyDoneWordsTest {
    @Test
    fun germanAlreadyDoneDoesNotSayDoneAndNothingToDo() {
        val de = I18n.table(Lang.DE)
        val head = de.getValue("Déjà fait !")
        val body = de.getValue("Ton morceau avait déjà ce que demande cette étape : rien à toucher.")
        assertFalse("« $head »", "erledigt" in head)
        assertFalse("« $body »", "nichts zu tun" in body)
    }
}
