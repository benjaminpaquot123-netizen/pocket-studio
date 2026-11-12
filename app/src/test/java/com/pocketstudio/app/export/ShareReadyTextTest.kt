package com.pocketstudio.app.export

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** Remix testers (Mehdi): the share ended with no word from the app; « C'est prêt » names the file and its copy. */
class ShareReadyTextTest {
    @Test
    fun theFileIsSaidReadyAndWhereItsCopyIs() {
        val song = listOf(File("/x/Jump-up-DnB-bootleg-Mehdi-morceau-20261028-174901.wav"))
        assertEquals(
            "C'est prêt : Jump-up-DnB-bootleg-Mehdi-morceau-20261028-174901.wav, une copie est aussi dans Musique › BenStudio.",
            shareReadyText(song, savedInMusic = true),
        )
        assertEquals("C'est prêt : a.m4a. Choisis l'appli pour l'envoyer.", shareReadyText(listOf(File("a.m4a")), savedInMusic = false))
    }
}
