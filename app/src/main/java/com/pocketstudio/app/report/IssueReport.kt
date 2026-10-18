package com.pocketstudio.app.report

import android.net.Uri
import com.pocketstudio.app.AppInfo
import com.pocketstudio.engine.StreamInfo
import java.util.Locale

/**
 * Builds pre-filled GitHub "new issue" links. The user only has to press "Submit".
 * GitHub truncates very long URLs, so the body is capped and the log is trimmed first.
 */
object IssueReport {
    private const val MAX_URL_LENGTH = 7800

    data class Context(
        val versionLabel: String,
        val device: String,
        val stream: StreamInfo,
        val roundTripMs: Double?,
        val log: String,
    )

    fun currentContext(stream: StreamInfo, roundTripMs: Double?, log: String) = Context(
        versionLabel = AppInfo.versionLabel,
        device = AppInfo.device,
        stream = stream,
        roundTripMs = roundTripMs,
        log = log,
    )

    fun environmentSection(c: Context): String = buildString {
        appendLine("### Appareil")
        appendLine("- Version : ${c.versionLabel}")
        appendLine("- Appareil : ${c.device}")
        appendLine("- Audio : ${describeStream(c.stream)}")
        appendLine("- Latence de sortie estimée : ${formatMs(c.stream.outputLatencyMs)}")
        appendLine("- Latence aller-retour mesurée : ${c.roundTripMs?.let { formatMs(it) } ?: "non mesurée"}")
        appendLine("- Décrochages audio : ${c.stream.xruns}")
    }

    fun describeStream(s: StreamInfo): String =
        if (s.sampleRate == 0) "flux fermé"
        else listOf(
            if (s.aaudio) "AAudio" else "OpenSL ES",
            if (s.exclusive) "exclusif" else "partagé",
            if (s.mmap) "MMAP" else "legacy",
            "${s.sampleRate} Hz",
            "burst ${s.framesPerBurst}",
            "buffer ${s.bufferSize}/${s.bufferCapacity}",
        ).joinToString(", ")

    fun formatMs(ms: Double): String = if (ms < 0) "inconnue" else String.format(Locale.FRANCE, "%.1f ms", ms)

    fun recetteIssue(milestone: String, lines: List<String>, c: Context): Uri {
        val title = "Recette ${c.versionLabel} : $milestone"
        val head = buildString {
            appendLine("## Recette : $milestone")
            appendLine()
            lines.forEach { appendLine(it) }
            appendLine()
            append(environmentSection(c))
        }
        return issueUri(title, head, c.log, "recette")
    }

    fun problemIssue(description: String, c: Context): Uri {
        val summary = description.lineSequence().firstOrNull()?.take(60)?.ifBlank { null } ?: "Problème signalé"
        val head = buildString {
            appendLine("## Problème")
            appendLine(description.ifBlank { "(pas de description)" })
            appendLine()
            append(environmentSection(c))
        }
        return issueUri("Bug ${c.versionLabel} : $summary", head, c.log, "bug")
    }

    fun crashIssue(crash: String, c: Context): Uri {
        val firstLine = crash.lineSequence().drop(1).firstOrNull()?.take(70) ?: "plantage"
        val head = buildString {
            appendLine("## Plantage")
            appendLine("L'appli s'est fermée brutalement lors de la session précédente.")
            appendLine()
            append(environmentSection(c))
        }
        return issueUri("Crash ${c.versionLabel} : $firstLine", head, crash, "crash")
    }

    private fun issueUri(title: String, head: String, log: String, label: String): Uri {
        var tail = log
        while (true) {
            val body = head + "\n<details><summary>Journal</summary>\n\n```\n" + tail + "\n```\n</details>\n"
            val uri = Uri.parse("https://github.com/${AppInfo.repo}/issues/new").buildUpon()
                .appendQueryParameter("title", title)
                .appendQueryParameter("labels", label)
                .appendQueryParameter("body", body)
                .build()
            if (uri.toString().length <= MAX_URL_LENGTH || tail.isEmpty()) return uri
            // Keep the most recent part of the log.
            tail = tail.substring(minOf(tail.length, maxOf(200, tail.length / 4)))
        }
    }
}
