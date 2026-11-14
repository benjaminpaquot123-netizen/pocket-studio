package com.pocketstudio.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Tour 5, lot « moyens » C (review of the share, first « moyen »): Tour5AppProofTest composes [rememberProjectZipSender]
 * itself, so nothing said that AppRoot still goes through it. A mutation (the three places written back by hand,
 * `projectsVm.exportZip(…) { z -> WavExporter.share(…) }`, no « C'est prêt ») left every test green. AppRoot is too big
 * to compose here: its source is read instead. Mes projets › ⋯ › « Sauvegarder le morceau en .zip » and both
 * « Prêt à partager ? » (`sendProject`) must call the one sender, and nothing in AppRoot makes a .zip by hand.
 *
 * [rememberProjectZipSender]: com.pocketstudio.app.export.rememberProjectZipSender
 */
class AppRootZipSenderTest {
    private val source = File("src/main/java/com/pocketstudio/app/ui/AppRoot.kt").readText()

    @Test
    fun appRootSendsEveryProjectZipThroughTheOneSender() {
        val made = Regex("""val\s+(\w+)\s*=\s*(?:com\.pocketstudio\.app\.export\.)?rememberProjectZipSender\(""").findAll(source).toList()
        assertEquals("AppRoot makes the one sender once: ${made.map { it.value }}", 1, made.size)
        val sender = made.single().groupValues[1]

        val exportZip = Regex("""exportZip\s*=\s*([^\n]+)""").findAll(source).map { it.groupValues[1].trim() }.toList()
        assertTrue("Mes projets › ⋯ is given an exportZip", exportZip.isNotEmpty())
        val viaSender = Regex("""^\{\s*(\w+)\s*->\s*${sender}\.send\(\s*\1\.id\s*,\s*\1\.name\s*\)\s*},?$""")
        exportZip.forEach { assertTrue("Mes projets › ⋯ makes its .zip by hand, not by $sender.send: $it", viaSender.matches(it)) }

        val sendProject = Regex("""sendProject\s*=\s*([^\n]+)""").findAll(source).map { it.groupValues[1].trim().removeSuffix(",") }.toList()
        assertEquals("both « Prêt à partager ? » (the studio's and the Mode simple's) are given sendProject: $sendProject", 2, sendProject.size)
        sendProject.forEach {
            assertTrue("« Prêt à partager ? » makes its .zip by hand, not by $sender.sendOpen: $it",
                it == "$sender::sendOpen" || Regex("""^\{\s*$sender\.sendOpen\(\)\s*}$""").matches(it))
        }

        assertTrue("AppRoot makes a project's .zip by hand (projectsVm.exportZip)", !Regex("""\.exportZip\(""").containsMatchIn(source))
    }
}
