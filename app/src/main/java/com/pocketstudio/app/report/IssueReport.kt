package com.pocketstudio.app.report

import com.pocketstudio.model.i18n.tr
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
        val latencyDiagnostic: String? = null,
    )

    fun currentContext(stream: StreamInfo, roundTripMs: Double?, log: String) = Context(
        versionLabel = AppInfo.versionLabel,
        device = AppInfo.device,
        stream = stream,
        roundTripMs = roundTripMs,
        log = log,
        latencyDiagnostic = com.pocketstudio.app.Metrics.latencyDiagnostic.value,
    )

    fun environmentSection(c: Context): String = buildString {
        appendLine("### Appareil")
        appendLine("- Version : ${c.versionLabel}")
        appendLine("- Appareil : ${c.device}")
        appendLine("- Audio : ${describeStream(c.stream)}")
        appendLine("- Latence de sortie estimée : ${formatMs(c.stream.outputLatencyMs)}")
        appendLine("- Latence aller-retour mesurée : ${c.roundTripMs?.let { formatMs(it) } ?: "non mesurée"}")
        appendLine("- Décrochages audio : ${c.stream.xruns}")
        c.latencyDiagnostic?.let { appendLine("- Dernière mesure de latence : $it") }
    }

    /** The audio stream in a few words: in French for the report, in the interface's language [onScreen]. */
    fun describeStream(s: StreamInfo, onScreen: Boolean = false): String =
        if (s.sampleRate == 0) (if (onScreen) tr("flux fermé") else "flux fermé")
        else listOf(
            if (s.aaudio) "AAudio" else "OpenSL ES",
            if (s.exclusive) (if (onScreen) tr("exclusif") else "exclusif") else (if (onScreen) tr("partagé") else "partagé"),
            if (s.mmap) "MMAP" else "legacy",
            "${s.sampleRate} Hz",
            "burst ${s.framesPerBurst}",
            "buffer ${s.bufferSize}/${s.bufferCapacity}",
        ).joinToString(", ")

    /** "12,3 ms" for the report; [onScreen], in the interface's language. */
    fun formatMs(ms: Double, onScreen: Boolean = false): String = when {
        ms < 0 -> if (onScreen) tr("inconnue") else "inconnue"
        onScreen -> tr("%.1f ms", ms)
        else -> String.format(Locale.FRANCE, "%.1f ms", ms)
    }

    fun recetteIssue(milestone: String, lines: List<String>, c: Context): Uri {
        val title = "Recette ${c.versionLabel} : $milestone"
        val head = buildString {
            appendLine("## Recette : $milestone")
            appendLine()
            lines.forEach { appendLine(it) }
            appendLine()
        }
        return issueUri(title, head, environmentSection(c), c.log, "recette")
    }

    fun problemIssue(description: String, c: Context): Uri {
        val summary = description.lineSequence().firstOrNull()?.take(60)?.ifBlank { null } ?: "Problème signalé"
        val head = buildString {
            appendLine("## Problème")
            appendLine(description.ifBlank { "(pas de description)" })
            appendLine()
        }
        return issueUri("Bug ${c.versionLabel} : $summary", head, environmentSection(c), c.log, "bug")
    }

    fun crashIssue(crash: String, c: Context): Uri {
        val firstLine = crash.lineSequence().drop(1).firstOrNull()?.let { LogTail.forPublicIssue(it) }?.take(70) ?: "plantage"
        val head = buildString {
            appendLine("## Plantage")
            appendLine("L'appli s'est fermée brutalement lors de la session précédente.")
            appendLine()
        }
        return issueUri("Crash ${c.versionLabel} : $firstLine", head, environmentSection(c), crash, "crash")
    }

    /**
     * The new issue's link, under GitHub's length limit: the log is shortened first (its most
     * recent part kept), then, for very long comments or descriptions, [head] itself is cut with
     * « … (suite coupée) ». The environment ([env]) always stays. The repository is public: the
     * log loses the names between « » and JSON excerpts first ([LogTail.forPublicIssue]).
     */
    private fun issueUri(title: String, head: String, env: String, log: String, label: String): Uri {
        var tail = LogTail.forPublicIssue(log)
        var text = head
        while (true) {
            val body = text + env + "\n<details><summary>Journal</summary>\n\n```\n" + tail + "\n```\n</details>\n"
            val uri = Uri.parse("https://github.com/${AppInfo.repo}/issues/new").buildUpon()
                .appendQueryParameter("title", title)
                .appendQueryParameter("labels", label)
                .appendQueryParameter("body", body)
                .build()
            if (uri.toString().length <= MAX_URL_LENGTH) return uri
            if (tail.isNotEmpty()) {
                // Keep the most recent part of the log.
                tail = tail.substring(minOf(tail.length, maxOf(200, tail.length / 4)))
            } else {
                val keep = text.removeSuffix(CUT).length * 3 / 4
                if (keep < 40) return uri
                text = text.take(keep) + CUT
            }
        }
    }

    private const val CUT = "\n… (suite coupée)\n\n"
}
