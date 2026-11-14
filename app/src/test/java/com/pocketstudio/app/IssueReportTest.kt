package com.pocketstudio.app

import com.pocketstudio.app.report.IssueReport
import com.pocketstudio.engine.StreamInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IssueReportTest {
    private val stream = StreamInfo(
        sampleRate = 48000, framesPerBurst = 96, bufferSize = 192, bufferCapacity = 3072,
        exclusive = true, aaudio = true, mmap = true, lowLatency = true, xruns = 2, outputLatencyMs = 9.6,
        running = true,
    )

    @Test
    fun longLogsAreTrimmedToFitGitHubUrlLimit() {
        val hugeLog = (1..2000).joinToString("\n") { "10-18 12:00:00.000 I/PocketStudio( 123): line $it" }
        val context = IssueReport.Context("0.0.7 (preview)", "HONOR test", stream, 18.4, hugeLog)
        val uri = IssueReport.recetteIssue("Phase 0", listOf("- ✅ test"), context)
        assertTrue(uri.toString().length <= 7800)
        val body = uri.getQueryParameter("body")!!
        assertTrue(body.contains("- ✅ test"))
        assertTrue(body.contains("Décrochages audio : 2"))
        assertTrue(body.contains("18,4 ms"))
        assertTrue("keeps the most recent log lines", body.contains("line 2000"))
        assertTrue(uri.toString().startsWith("https://github.com/benjaminpaquot123-netizen/pocket-studio/issues/new"))
    }

    @Test
    fun problemIssueUsesFirstLineAsTitle() {
        val context = IssueReport.Context("0.0.7 (preview)", "HONOR test", stream, null, "log")
        val uri = IssueReport.problemIssue("Le son coupe\nquand je tourne l'écran", context)
        assertTrue(uri.getQueryParameter("title")!!.endsWith("Le son coupe"))
        assertTrue(uri.getQueryParameter("body")!!.contains("non mesurée"))
    }

    @Test
    fun veryLongCommentsAreCutTooAndTheEnvironmentStays() {
        val context = IssueReport.Context("0.0.7 (preview)", "HONOR test", stream, 18.4, "log")
        // Accents take 6 characters each once encoded in the link.
        val lines = (1..60).map { "- ❌ écran $it : à revérifier, ça décroche après l'été, très gênant (é è à ç ù)" }
        val uri = IssueReport.recetteIssue("Phase 28", lines, context)
        assertTrue("${uri.toString().length}", uri.toString().length <= 7800)
        val body = uri.getQueryParameter("body")!!
        assertTrue(body.contains("écran 1 :"))
        assertTrue(body.contains("(suite coupée)"))
        assertTrue(body.contains("Décrochages audio : 2"))
    }

    // Audit final 19 M1: the repository is public, so the journal sent with a checklist or a
    // problem must not carry the user's song and file names (written between « » by the app).
    @Test
    fun publicTicketsHideNamesBetweenGuillemets() {
        val log = "12:00:00.000 Pad live 3 : « Anniversaire Léa.m4a »\n12:00:01.000 Projet mis à la corbeille : « Voix chambre »"
        val context = IssueReport.Context("0.0.7 (preview)", "HONOR test", stream, null, log)
        for (uri in listOf(
            IssueReport.problemIssue("Le pad ne joue pas", context),
            IssueReport.recetteIssue("Phase 22", listOf("- ✅ test"), context),
        )) {
            val body = uri.getQueryParameter("body")!!
            assertFalse(body, body.contains("Anniversaire"))
            assertFalse(body, body.contains("Voix chambre"))
            assertTrue(body, body.contains("Pad live 3 : «…»"))
        }
    }

    @Test
    fun crashTicketsHideNamesAndJsonExcerpts() {
        val crash = "Version 0.22.1 · date · thread main\n" +
            "java.lang.IllegalStateException: piste « Mémo Marie » introuvable\n" +
            "kotlinx.serialization.json.internal.JsonDecodingException: Unexpected JSON token at offset 3\n" +
            "JSON input: {\"name\":\"Anniversaire Léa\"}\n\tat com.pocketstudio.X.y(X.kt:1)"
        val context = IssueReport.Context("0.0.7 (preview)", "HONOR test", stream, null, "")
        val uri = IssueReport.crashIssue(crash, context)
        val all = uri.getQueryParameter("title")!! + uri.getQueryParameter("body")!!
        assertFalse(all, all.contains("Marie"))
        assertFalse(all, all.contains("Anniversaire"))
        assertTrue(all, all.contains("JSON input: …"))
        assertTrue(all, all.contains("X.kt:1"))
    }
}
