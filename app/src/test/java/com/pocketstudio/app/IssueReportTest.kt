package com.pocketstudio.app

import com.pocketstudio.app.report.IssueReport
import com.pocketstudio.engine.StreamInfo
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
}
