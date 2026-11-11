package com.pocketstudio.bentest

import com.pocketstudio.app.report.JsonText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * rapport.md (what Benjamin and the agent read first) and mesures.json (the numbers, for the
 * agent's tables) from the logbook, with the verdict in plain words.
 */
object Rapport {
    data class Bilan(val reussies: Int, val ratees: Int, val plantages: Int, val restantes: Int) {
        fun verdict(): String = buildList {
            add("$reussies réussie" + if (reussies > 1) "s" else "")
            add("$ratees ratée" + if (ratees > 1) "s" else "")
            add("$plantages plantage" + if (plantages > 1) "s" else "")
            if (restantes > 0) add("$restantes pas faite" + if (restantes > 1) "s" else "")
        }.joinToString(", ")
    }

    fun bilan(lines: List<Carnet.Ligne>, etapes: List<Etape>): Bilan {
        fun last(step: String) = lines.lastOrNull { it.step == step && it.kind in ENDS }?.kind
        val ends = etapes.map { last(it.name) }
        return Bilan(ends.count { it == Carnet.REUSSI }, ends.count { it == Carnet.ECHEC }, ends.count { it == Carnet.PLANTAGE }, ends.count { it == null })
    }

    /** [about]: the app's version and commit, the device, Android. */
    fun markdown(lines: List<Carnet.Ligne>, etapes: List<Etape>, about: List<Pair<String, String>>): String = buildString {
        appendLine("# BenStudio Test : essai éclair")
        appendLine()
        about.forEach { (k, v) -> appendLine("- $k : $v") }
        appendLine()
        appendLine("**Verdict : ${bilan(lines, etapes).verdict()}**")
        appendLine()
        appendLine("| Étape | Résultat | Durée | Ce qu'il a vu |")
        appendLine("|---|---|---|---|")
        for (e in etapes) {
            val begin = lines.lastOrNull { it.step == e.name && it.kind == Carnet.DEBUT }
            val end = lines.lastOrNull { it.step == e.name && it.kind in ENDS }
            val result = when (end?.kind) {
                Carnet.REUSSI -> "✅ réussie"
                Carnet.ECHEC -> "❌ ratée"
                Carnet.PLANTAGE -> "💥 plantage"
                else -> "pas faite"
            }
            val took = if (begin != null && end != null && end.kind != Carnet.PLANTAGE) "${(end.atMs - begin.atMs) / 1000} s" else ""
            appendLine("| ${e.label} | $result | $took | ${end?.text.orEmpty().replace("|", "/").replace("\n", " ")} |")
        }
        val notes = lines.filter { it.kind == Carnet.NOTE }
        if (notes.isNotEmpty()) {
            appendLine()
            appendLine("## Notes")
            notes.forEach { appendLine("- ${it.step} : ${it.text}") }
        }
    }

    /** Every reading of the logbook (mesures), by step, with its time. */
    fun json(lines: List<Carnet.Ligne>, about: List<Pair<String, String>>): String {
        val stamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.ROOT)
        val steps = lines.filter { it.kind != Carnet.MESURE }.map { l ->
            JsonText.obj("t" to JsonText.string(stamp.format(Date(l.atMs))), "kind" to JsonText.string(l.kind), "step" to JsonText.string(l.step), "text" to JsonText.string(l.text))
        }
        val reads = lines.filter { it.kind == Carnet.MESURE }.map { l ->
            val fields = l.text.split(';').mapNotNull { f -> f.split('=', limit = 2).takeIf { it.size == 2 } }
                .map { (k, v) -> k to (v.toFloatOrNull()?.let { JsonText.number(it) } ?: JsonText.string(v)) }
            JsonText.obj("ms" to l.atMs.toString(), "step" to JsonText.string(l.step), *fields.toTypedArray())
        }
        val head = about.map { (k, v) -> k to JsonText.string(v) }
        return JsonText.obj(*head.toTypedArray(), "steps" to JsonText.array(steps), "reads" to JsonText.array(reads))
    }

    private val ENDS = setOf(Carnet.REUSSI, Carnet.ECHEC, Carnet.PLANTAGE)
}
