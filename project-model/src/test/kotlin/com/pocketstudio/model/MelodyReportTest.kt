package com.pocketstudio.model

import com.pocketstudio.model.StarterWizard.Mood
import org.junit.Test
import java.io.File
import java.util.Locale

/**
 * Not a check: a report. The melody metrics of every style (its default mood), as a Markdown
 * table, printed and written to build/melodie.md (docs/son/MELODIE.md is made from it), with how
 * many styles pass each of M1–M20 over the four moods.
 */
class MelodyReportTest {
    @Test
    fun printTheMelodyMetricsTable() {
        val judged = MelodyAudit.all(withSong = true)
        val out = StringBuilder()
        out.append("| Style | Lead | Règles | Silences / 4 mes. | Motif ×16 | Vélocité | Ambitus | Notes d'accord | Basse : règles | Basse : silences | Basse : vélocité | Basse : hauteurs |\n")
        out.append("|---|---|---|---|---|---|---|---|---|---|---|---|\n")
        for (style in StarterWizard.STYLES) {
            val mood = style.melody.mood
            val lead = judged.firstOrNull { it.style.id == style.id && it.mood == mood && it.line == "lead" }
            val bass = judged.first { it.style.id == style.id && it.mood == mood && it.line == "bass" }
            val l = lead?.report
            val b = bass.report!!
            fun f(x: Float) = String.format(Locale.ROOT, "%.2f", x)
            out.append("| ${style.id} | ${if (lead == null) if (MelodyAudit.hasLeadArp(style)) "arpège" else "—" else style.melody.leadKind.name.lowercase()} ")
            out.append("| ${if (lead == null) "—" else if (lead.failures.isEmpty()) "✓" else lead.failures.size.toString() + " ✗"} ")
            out.append("| ${l?.restsPerPhrase?.joinToString("/") ?: "—"} | ${l?.motifOccurrences ?: "—"} | ${l?.let { f(it.velocityRange) } ?: "—"} | ${l?.ambitus ?: "—"} | ${l?.let { f(it.chordTones) } ?: "—"} ")
            out.append("| ${if (bass.failures.isEmpty()) "✓" else bass.failures.size.toString() + " ✗"} | ${b.restsPerPhrase.joinToString("/")} | ${f(b.velocityRange)} | ${b.distinctPitches} |\n")
        }
        // How many styles pass each rule, over the four moods (a style passes when every mood does).
        out.append("\nRègle | Styles qui passent (lead) | (basse)\n---|---|---\n")
        val leadStyles = StarterWizard.STYLES.filter { MelodyAudit.hasLeadLine(it) }
        for (m in 1..20) {
            val key = "M$m "
            fun passes(line: String, styles: List<StarterWizard.Style>) = styles.count { s ->
                judged.filter { it.style.id == s.id && it.line == line }.none { j -> j.failures.any { it.startsWith(key) } }
            }
            val lead = when (m) {
                16 -> StarterWizard.STYLES.filter { it.melody.counter != null && MelodyAudit.hasLeadLine(it) }.let { "${passes("counter", it)}/${it.size}" }
                17 -> StarterWizard.STYLES.filter { MelodyAudit.hasLeadArp(it) || it.melody.chordArp != null }.let { s -> "${s.count { st -> judged.filter { it.style.id == st.id && it.line.endsWith("arp") }.none { j -> j.failures.any { it.startsWith(key) } } }}/${s.size}" }
                else -> "${passes("lead", leadStyles)}/${leadStyles.size}"
            }
            val bass = if (m in setOf(1, 3, 4, 9, 11, 12, 13, 14)) "${passes("bass", StarterWizard.STYLES)}/${StarterWizard.STYLES.size}" else "—"
            out.append("M$m | $lead | $bass\n")
        }
        val failing = judged.filter { it.failures.isNotEmpty() }
        out.append("\n${failing.size} lignes en échec sur ${judged.size} (${StarterWizard.STYLES.size} styles × ${Mood.entries.size} ambiances).\n")
        failing.forEach { out.append("- ${it.style.id} ${it.mood.id} ${it.line}: ${it.failures.joinToString("; ")}\n") }
        println(out)
        File("build").mkdirs()
        File("build/melodie.md").writeText(out.toString())
    }
}
