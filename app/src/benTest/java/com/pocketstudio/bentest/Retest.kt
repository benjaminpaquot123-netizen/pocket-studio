package com.pocketstudio.bentest

import java.io.File

/**
 * « Retester les corrections »: only a chosen subset of « Tout tester »'s steps. By default the steps that
 * were red, not conclusive or failed in the last saved run (kept in [Derniere], apart from the logbook so
 * that « Tout recommencer » does not lose it). The subset keeps « Tout tester »'s order, the screen-off
 * step last.
 */
object Retest {
    /** The step that must stay last (it ends the run with the screen off). */
    const val DERNIERE = "ecran_eteint"

    /**
     * What every subset needs first, run unseen in the checklist: the studio opened in front. Without it a retest
     * started from BenStudio Test's screen drove the studio stopped under it (v6 run, 2026-11-12: five steps failed
     * in 15 s, « nothing tagged launch », « top_projects is not there once »).
     */
    val PREALABLES = listOf("ouverture")

    /** The steps offered in the checklist: all but the [PREALABLES]. */
    fun proposees(etapes: List<Etape>): List<Etape> = etapes.filter { it.name !in PREALABLES }

    /** The steps to tick by default: red (a failed or crashed step is red), not conclusive. A green, orange or undone step is not. */
    fun parDefaut(derniere: Map<String, Feu?>, etapes: List<Etape>): Set<String> =
        etapes.map { it.name }.filter { derniere[it] == Feu.ROUGE || derniere[it] == Feu.GRIS }.toSet()

    /** [etapes] reduced to the [PREALABLES] then [choisies], in their order, [DERNIERE] last whatever the order was. */
    fun choisir(etapes: List<Etape>, choisies: Set<String>): List<Etape> {
        val kept = etapes.filter { it.name in choisies || it.name in PREALABLES }
        val first = kept.filter { it.name in PREALABLES }
        val rest = kept - first.toSet()
        return first + rest.filter { it.name != DERNIERE } + rest.filter { it.name == DERNIERE }
    }

    /** How many steps Benjamin chose in [etapes] (the [PREALABLES] are not his). */
    fun choisies(etapes: List<Etape>): Int = etapes.count { it.name !in PREALABLES }

    /**
     * The lights to tick from when no run was kept by this version ([Robot]'s derniere.tsv): the last saved
     * logbook, whatever run or version wrote it ([lignes]), read for [etapes]; else the newest report zip's
     * logbook ([rapports]); else nothing.
     */
    fun repli(lignes: List<Carnet.Ligne>, rapports: List<File>, etapes: List<Etape>): Map<String, Feu?> {
        val from = lignes.ifEmpty {
            rapports.filter { it.isFile }.sortedByDescending { it.lastModified() }.firstNotNullOfOrNull { zip ->
                runCatching {
                    java.util.zip.ZipFile(zip).use { z -> z.getEntry("carnet.tsv")?.let { e -> z.getInputStream(e).use { Carnet.lire(it.readBytes().toString(Charsets.UTF_8)) } } }
                }.getOrNull()?.takeIf { it.isNotEmpty() }
            }.orEmpty()
        }
        return Rapport.feux(from, etapes).filterValues { it != null }
    }

    /** « Retest : N étapes » for the report's header. */
    fun entete(n: Int) = "Retest : $n étape" + if (n > 1) "s" else ""

    /** The last run kept: [ancienne] with what [lines] say of [etapes] (a retest only replaces the steps it ran). */
    fun fusion(ancienne: Map<String, Feu?>, nouvelle: Map<String, Feu?>): Map<String, Feu?> = ancienne + nouvelle.filterValues { it != null }

    private fun mot(f: Feu?) = f?.mot ?: "-"

    fun ecrire(file: File, feux: Map<String, Feu?>) {
        file.parentFile?.mkdirs()
        file.writeText(feux.entries.joinToString("\n") { (k, v) -> "$k\t${mot(v)}" }, Charsets.UTF_8)
    }

    fun lire(file: File): Map<String, Feu?> = runCatching {
        file.readLines(Charsets.UTF_8).mapNotNull { l -> l.split('\t').takeIf { it.size == 2 && it[0].isNotBlank() }?.let { it[0] to Feu.of(it[1]) } }.toMap()
    }.getOrDefault(emptyMap())

    /** The chosen ids, one per line. */
    fun ecrireChoix(file: File, ids: Collection<String>) {
        file.parentFile?.mkdirs()
        file.writeText(ids.joinToString("\n"), Charsets.UTF_8)
    }

    fun lireChoix(file: File): Set<String>? = if (!file.isFile) null else runCatching { file.readLines(Charsets.UTF_8).filter { it.isNotBlank() }.toSet() }.getOrNull()
}
