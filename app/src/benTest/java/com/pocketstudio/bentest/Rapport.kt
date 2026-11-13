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

    /** « 34 verts, 2 oranges, 1 rouge » (and the inconclusive, the steps not done). */
    data class BilanFeux(val verts: Int, val oranges: Int, val rouges: Int, val gris: Int, val restantes: Int) {
        fun verdict(): String = buildList {
            add("$verts vert" + if (verts > 1) "s" else "")
            add("$oranges orange" + if (oranges > 1) "s" else "")
            add("$rouges rouge" + if (rouges > 1) "s" else "")
            if (gris > 0) add("$gris non concluante" + if (gris > 1) "s" else "")
            if (restantes > 0) add("$restantes pas faite" + if (restantes > 1) "s" else "")
        }.joinToString(", ")
    }

    /** Each step's light: its worst [Carnet.FEU] since it last began, red when it failed or crashed, green when it passed without one; null: not done. */
    fun feux(lines: List<Carnet.Ligne>, etapes: List<Etape>): Map<String, Feu?> = etapes.associate { e ->
        val end = lines.lastOrNull { it.step == e.name && it.kind in ENDS }?.kind
        e.name to when (end) {
            null -> null
            Carnet.ECHEC, Carnet.PLANTAGE -> Feu.ROUGE
            else -> if (faussee(lines, e.name)) Feu.GRIS else Feu.pire(lumieres(lines, e.name).map { it.first })
        }
    }

    fun bilanFeux(lines: List<Carnet.Ligne>, etapes: List<Etape>): BilanFeux {
        val f = feux(lines, etapes).values
        return BilanFeux(f.count { it == Feu.VERT }, f.count { it == Feu.ORANGE }, f.count { it == Feu.ROUGE }, f.count { it == Feu.GRIS }, f.count { it == null })
    }

    /** [step] lost its timing to an exit from the app: its lights do not count. */
    private fun faussee(lines: List<Carnet.Ligne>, step: String): Boolean {
        val from = lines.indexOfLast { it.step == step && it.kind == Carnet.DEBUT }.coerceAtLeast(0)
        return lines.drop(from).any { it.step == step && it.kind == Carnet.FAUSSE }
    }

    /** [step]'s lights since it last began, with their reasons. */
    private fun lumieres(lines: List<Carnet.Ligne>, step: String): List<Pair<Feu, String>> {
        val from = lines.indexOfLast { it.step == step && it.kind == Carnet.DEBUT }.coerceAtLeast(0)
        return lines.drop(from).filter { it.step == step && it.kind == Carnet.FEU }.mapNotNull { l ->
            val (mot, why) = l.text.split(';', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
            Feu.of(mot)?.let { it to why }
        }
    }

    fun bilan(lines: List<Carnet.Ligne>, etapes: List<Etape>): Bilan {
        fun last(step: String) = lines.lastOrNull { it.step == step && it.kind in ENDS }?.kind
        val ends = etapes.map { last(it.name) }
        return Bilan(ends.count { it == Carnet.REUSSI }, ends.count { it == Carnet.ECHEC }, ends.count { it == Carnet.PLANTAGE }, ends.count { it == null })
    }

    /** [about]: the app's version and commit, the device, Android. */
    /** [tout]: « Tout tester » (each step's light, the verdict in lights). */
    /** [micro]: « Écoute au micro » (lights, its section). */
    fun markdown(lines: List<Carnet.Ligne>, etapes: List<Etape>, about: List<Pair<String, String>>, tout: Boolean = false, retest: Int? = null, oreille: Boolean = false, micro: Boolean = false): String = buildString {
        appendLine(if (micro) "# BenStudio Test : écoute au micro" else if (oreille) "# BenStudio Test : questions à l'oreille" else if (tout) "# BenStudio Test : tout tester" else "# BenStudio Test : essai éclair")
        appendLine()
        if (retest != null) { appendLine("**${Retest.entete(retest)}**"); appendLine() }
        about.forEach { (k, v) -> appendLine("- $k : $v") }
        appendLine()
        appendLine("**Verdict : ${if (oreille) QuestionsOreille.verdict(lines) else bilan(lines, etapes).verdict()}**")
        val feux = if (tout) feux(lines, etapes) else emptyMap()
        if (tout) appendLine("\n**Feux : ${bilanFeux(lines, etapes).verdict()}**")
        appendLine()
        appendLine(if (tout) "| Étape | Feu | Résultat | Durée | Ce qu'il a vu |" else "| Étape | Résultat | Durée | Ce qu'il a vu |")
        appendLine(if (tout) "|---|---|---|---|---|" else "|---|---|---|---|")
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
            val light = if (tout) " ${feux[e.name]?.let { f -> PASTILLES.getValue(f) + " " + f.mot + (pourquoi(lines, e.name)?.let { " ($it)" } ?: "") } ?: ""} |" else ""
            appendLine("| ${e.label} |$light $result | $took | ${end?.text.orEmpty().replace("|", "/").replace("\n", " ")} |")
        }
        QuestionsOreille.section(lines, etapes).takeIf { it.isNotEmpty() }?.let { appendLine(); append(it) }
        EcouteMicro.section(lines).takeIf { it.isNotEmpty() }?.let { appendLine(); append(it) }
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
        val steps = lines.filter { it.kind != Carnet.MESURE && it.kind != Carnet.MICRO }.map { l ->
            JsonText.obj("t" to JsonText.string(stamp.format(Date(l.atMs))), "kind" to JsonText.string(l.kind), "step" to JsonText.string(l.step), "text" to JsonText.string(l.text))
        }
        val reads = lines.filter { it.kind == Carnet.MESURE }.map { l ->
            val fields = l.text.split(';').mapNotNull { f -> f.split('=', limit = 2).takeIf { it.size == 2 } }
                .map { (k, v) -> k to (v.toFloatOrNull()?.let { JsonText.number(it) } ?: JsonText.string(v)) }
            JsonText.obj("ms" to l.atMs.toString(), "step" to JsonText.string(l.step), *fields.toTypedArray())
        }
        val head = about.map { (k, v) -> k to JsonText.string(v) }
        // The answers of « Questions à l'oreille » (only when the run asked some).
        val answers = QuestionsOreille.reponses(lines).map { a ->
            JsonText.obj("question" to JsonText.string(a.id), "reponse" to JsonText.string(a.verdict.mot), "commentaire" to JsonText.string(a.commentaire), "capture" to JsonText.string(a.capture))
        }
        val questions = if (answers.isEmpty()) emptyArray() else arrayOf("questions" to JsonText.array(answers))
        // « Écoute au micro »: the speaker's curve and the fingerprints (scripts/bentest/empreinte.py reads them).
        val micro = EcouteMicro.json(lines)?.let { arrayOf("micro" to it) } ?: emptyArray()
        return JsonText.obj(*head.toTypedArray(), "steps" to JsonText.array(steps), "reads" to JsonText.array(reads), *questions, *micro)
    }

    /** Why a step is not green: its worst light's reason. */
    private fun pourquoi(lines: List<Carnet.Ligne>, step: String): String? {
        if (faussee(lines, step)) return "chronométrage faussé : sortie de l'appli"
        val l = lumieres(lines, step)
        val worst = Feu.pire(l.map { it.first })
        return l.filter { it.first == worst && it.second.isNotBlank() }.joinToString(", ") { it.second }.ifEmpty { null }
    }

    private val PASTILLES = mapOf(Feu.VERT to "🟢", Feu.ORANGE to "🟠", Feu.ROUGE to "🔴", Feu.GRIS to "⚪")

    private val ENDS = setOf(Carnet.REUSSI, Carnet.ECHEC, Carnet.PLANTAGE)
}
