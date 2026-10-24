package com.pocketstudio.app.report

import com.pocketstudio.engine.StreamInfo
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** The open project, as the report says it. */
data class ProjectSummary(
    val name: String,
    val style: String?,
    val tempoBpm: Double,
    /** Where the music is, or where ▶ starts (« Drop · mes. 14/60 »), null: nothing to say. */
    val position: String?,
    val playing: Boolean,
    val parts: Int,
    val songBars: Int,
    val tracks: Int,
    val masterDb: Float,
    val target: String,
    /** The master volume has a song automation lane. */
    val masterLane: Boolean,
)

/** A file of the zip besides report.md and journal.json, and what it is (French). */
data class Attachment(val name: String, val what: String)

/** Everything report.md says; built on the phone at « Partager » / « Copier le texte ». */
data class BugReportInput(
    val createdAtMs: Long,
    val userText: String,
    val versionName: String,
    val versionCode: Int,
    val channel: String,
    val device: String,
    val android: String,
    /** Null: no audio engine on this device (or not started). */
    val stream: StreamInfo?,
    val roundTripMs: Double? = null,
    val project: ProjectSummary?,
    val health: List<HealthSample> = emptyList(),
    val events: List<JournalEvent> = emptyList(),
    val errors: List<JournalEvent> = emptyList(),
    /** The previous session's crash (its stack or the system's exit record), when reporting one. */
    val crash: String? = null,
    val attachments: List<Attachment> = emptyList(),
    val language: String = "fr",
    val simpleMode: Boolean = false,
)

/**
 * report.md, in French whatever the interface's language (it is read by the developer): the app and
 * the phone, the audio stream, the project, the last minute of sound as a sparkline and numbers,
 * the errors, the last [EVENTS] events of the journal, and the user's own words first.
 */
object BugReportText {
    const val EVENTS = 50
    private val clock = SimpleDateFormat("HH:mm:ss", Locale.FRANCE)
    private val day = SimpleDateFormat("yyyy-MM-dd 'à' HH:mm:ss", Locale.FRANCE)

    private fun f1(v: Double) = String.format(Locale.FRANCE, "%.1f", v)

    fun markdown(r: BugReportInput): String = buildString {
        appendLine("# Rapport de bug : Pocket Studio")
        appendLine()
        appendLine("Créé le ${day.format(Date(r.createdAtMs))}, sur le téléphone, et envoyé par l'utilisateur lui-même.")
        appendLine()
        appendLine("## Ce qui s'est passé")
        appendLine(r.userText.trim().ifEmpty { "(rien d'écrit)" })
        appendLine()
        r.crash?.takeIf { it.isNotBlank() }?.let { crash ->
            appendLine("## Plantage de la session précédente")
            appendLine("L'appli s'est fermée d'un coup.")
            appendLine()
            appendLine("```")
            appendLine(crash.trim().lineSequence().take(80).joinToString("\n"))
            appendLine("```")
            appendLine()
        }
        appendLine("## Appli et téléphone")
        appendLine("- Version : ${r.versionName} (code ${r.versionCode}, canal ${r.channel})")
        appendLine("- Téléphone : ${r.device}")
        appendLine("- Android : ${r.android}")
        appendLine("- Langue : ${r.language} · mode ${if (r.simpleMode) "simple" else "complet"}")
        val s = r.stream
        if (s == null) {
            appendLine("- Audio : moteur absent")
        } else {
            appendLine("- Audio : ${IssueReport.describeStream(s)}${if (s.running) "" else " (arrêté)"}")
            appendLine("- Latence de sortie : ${IssueReport.formatMs(s.outputLatencyMs)}")
            r.roundTripMs?.let { appendLine("- Latence aller-retour mesurée : ${IssueReport.formatMs(it)}") }
            appendLine("- Décrochages audio depuis le démarrage : ${s.xruns} · son rouvert par Android : ${s.restarts} fois")
            if (s.nonFinite) appendLine("- Alerte : une valeur invalide (NaN) a atteint la sortie")
        }
        r.health.takeIf { it.isNotEmpty() }?.let { h ->
            appendLine("- Charge du fil audio : ${percent(h.last().cpu)} maintenant, ${percent(h.maxOf { it.cpu })} au plus sur la dernière minute")
        }
        appendLine()
        appendLine("## Projet")
        val p = r.project
        if (p == null) {
            appendLine("Aucun projet ouvert.")
        } else {
            appendLine("- Nom : « ${p.name} »")
            appendLine("- Style : ${p.style ?: "aucun"}")
            appendLine("- Tempo : ${f1(p.tempoBpm)} BPM")
            appendLine("- Position : ${p.position ?: "début"}${if (p.playing) " (en lecture)" else " (à l'arrêt)"}")
            appendLine("- Morceau : ${p.parts} parties, ${p.songBars} mesures, ${p.tracks} pistes")
            appendLine("- Master : ${String.format(Locale.FRANCE, "%+.1f", p.masterDb)} dB, volume visé ${p.target}, automation du volume master : ${if (p.masterLane) "oui" else "non"}")
        }
        appendLine()
        health(r)
        appendLine("## Erreurs (${r.errors.size})")
        if (r.errors.isEmpty()) appendLine("Aucune erreur relevée.")
        r.errors.forEach { appendLine("- ${clock.format(Date(it.atMs))} ${it.text.replace("\n", "\n  ")}") }
        appendLine()
        val shown = r.events.takeLast(EVENTS)
        appendLine("## Journal (${shown.size} derniers événements sur ${r.events.size})")
        if (shown.isEmpty()) appendLine("Journal vide.")
        shown.forEach { appendLine("- ${clock.format(Date(it.atMs))} [${it.kind.label}] ${it.text.lineSequence().first()}") }
        appendLine()
        appendLine("## Pièces jointes")
        appendLine("- report.md : ce rapport")
        appendLine("- journal.json : le journal complet (${Journal.EVENTS} événements au plus) et les mesures du son, 4 par seconde")
        r.attachments.forEach { appendLine("- ${it.name} : ${it.what}") }
    }

    private fun percent(v: Float) = "${(v * 100).toInt()} %"

    private fun StringBuilder.health(r: BugReportInput) {
        appendLine("## Son des 60 dernières secondes")
        if (r.health.isEmpty()) {
            appendLine("Aucune mesure (le son n'était pas ouvert).")
            appendLine()
            return
        }
        val seconds = HealthSummary.perSecond(r.health, r.createdAtMs)
        appendLine("Une colonne par seconde, de -60 s (à gauche) à maintenant (à droite).")
        appendLine()
        appendLine("```")
        appendLine("crête   |${HealthSummary.sparkline(seconds)}|")
        appendLine("repères |${HealthSummary.markers(seconds)}|")
        appendLine("```")
        appendLine("Échelle : `${HealthSummary.RAMP.first()}` silence (sous ${HealthSummary.FLOOR_DB.toInt()} dBFS) jusqu'à `${HealthSummary.RAMP.last()}` 0 dBFS, 6 dB par cran.")
        appendLine("Repères : `>` lecture, `x` décrochage audio, `R` son rouvert par Android, `!` valeur invalide (NaN), `.` à l'arrêt, espace : pas de mesure.")
        appendLine()
        numbers("Crête (dBFS)", seconds.map { it.peakDb?.coerceAtLeast(-99f) })
        numbers("Volume court terme (LUFS)", seconds.map { it.lufs })
        numbers("Charge du fil audio (%)", seconds.map { it.cpu?.times(100f) })
        val last = r.health.last()
        appendLine("Dernière mesure : crête ${dbText(HealthSummary.db(last.peak))} dBFS, limiteur ${dbText(HealthSummary.db(last.limiterGain))} dB" +
            (last.masterLane?.let { ", automation du volume master à ${String.format(Locale.FRANCE, "%.2f", it)}" } ?: "") + ".")
        appendLine()
    }

    private fun dbText(db: Float) = if (db <= -99f) "−∞" else String.format(Locale.FRANCE, "%.1f", db)

    /** Ten values a line: « -60 s : -12 -11 … ». */
    private fun StringBuilder.numbers(title: String, values: List<Float?>) {
        appendLine("$title, une valeur par seconde :")
        appendLine("```")
        values.chunked(10).forEachIndexed { i, chunk ->
            val from = values.size - i * 10
            appendLine(String.format(Locale.ROOT, "%4d s : ", -from) + chunk.joinToString(" ") { v -> v?.let { String.format(Locale.ROOT, "%4.0f", it) } ?: "   ·" })
        }
        appendLine("```")
    }
}

/** The zip « Partager » sends: report.md, journal.json, the screenshots and, if asked, the project. */
object BugReportZip {
    /** A picture for the zip: [file] on the phone, named [name] in the zip. */
    data class Shot(val file: File, val name: String)

    fun fileName(atMs: Long): String = "rapport-pocket-studio-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(Date(atMs)) + ".zip"

    /** Writes the zip at [out] (through a temporary file), with [extra] text files; the pictures that went missing are left out. */
    fun write(out: File, reportMd: String, journalJson: String, shots: List<Shot>, projectJson: String?, extra: List<Pair<String, String>> = emptyList()): File {
        out.parentFile?.mkdirs()
        val tmp = File(out.parentFile, out.name + ".tmp")
        ZipOutputStream(FileOutputStream(tmp)).use { zip ->
            fun text(name: String, body: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(body.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            text("report.md", reportMd)
            text("journal.json", journalJson)
            shots.filter { it.file.isFile }.forEach { s ->
                zip.putNextEntry(ZipEntry(s.name))
                s.file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
            projectJson?.let { text("project.json", it) }
            extra.forEach { (name, body) -> text(name, body) }
        }
        out.delete()
        check(tmp.renameTo(out)) { "zip not moved" }
        return out
    }
}
