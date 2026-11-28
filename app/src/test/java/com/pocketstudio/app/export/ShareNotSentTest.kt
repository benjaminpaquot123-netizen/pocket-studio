package com.pocketstudio.app.export

import com.pocketstudio.model.i18n.tr
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tour 7 lot 5 D2: back from Android's share menu with nothing sent, the app said « C'est prêt » again, never that
 * nothing left. Back with no app picked: one message, « Pas envoyé », and where the file is.
 */
class ShareNotSentTest {
    private val ready = listOf(tr("%s. Choisis l'appli pour l'envoyer.", "Mon morceau.m4a"), "Pas de saturation.")

    @Test
    fun backWithNothingSentSaysSoOnce() {
        val (title, lines) = shareBack(ready, listOf("Mon morceau.m4a"), returned = true, picked = false)
        assertEquals(tr("Pas envoyé"), title)
        assertEquals(listOf(tr("Le fichier est dans Mes fichiers : %s.", "Mon morceau.m4a")), lines)
    }

    @Test
    fun sentOrStillInTheMenuSaysReady() {
        assertEquals(tr("C'est prêt") to ready, shareBack(ready, listOf("Mon morceau.m4a"), returned = true, picked = true))
        assertEquals(tr("C'est prêt") to ready, shareBack(ready, listOf("Mon morceau.m4a"), returned = false, picked = false))
    }
}
