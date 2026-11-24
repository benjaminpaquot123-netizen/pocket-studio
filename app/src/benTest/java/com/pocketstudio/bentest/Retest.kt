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

    /**
     * The steps to tick by default: red (a failed or crashed step is red), not conclusive. A green, orange or undone
     * step is not. A step of [nouveaux] never played ([derniere] does not have it) is ticked too.
     */
    fun parDefaut(derniere: Map<String, Feu?>, etapes: List<Etape>, nouveaux: Set<String> = emptySet()): Set<String> =
        etapes.map { it.name }.filter { derniere[it] == Feu.ROUGE || derniere[it] == Feu.GRIS || (it in nouveaux && it !in derniere) }.toSet()

    /** The tests added by lot A2 (the phone's trials, the load tests): ticked until a run has played them. */
    val NOUVEAUX: Set<String> = setOf(
        Epreuves.PERTE, Epreuves.KIT, Epreuves.ENDURANCE, Epreuves.FUITE, Epreuves.FROID, Epreuves.BRUTAL, Epreuves.VIDEO, Epreuves.TAMPON, Epreuves.IMAGES,
    ) + CHARGE

    /** The load tests (the scenarios that repeat a gesture). */
    val CHARGE: Set<String> get() = setOf("jouer_arreter_10", "fond_reprise_5", "changer_partie_10", "quitter_en_jouant")

    /**
     * « Tout tester »'s steps then every scenario « Tout tester » does not already have (the same id runs once,
     * its « Tout tester » version): what the checklist offers.
     */
    fun avecScenarios(tout: List<Etape>, scenarios: List<Etape>): List<Etape> {
        val ids = tout.map { it.name }.toSet()
        return tout + scenarios.filter { it.name !in ids }
    }

    /** The checklist's title for [id]. */
    fun groupe(id: String): String = when {
        id in CHARGE -> "Charge"
        id.startsWith("style") -> "Styles"
        id.startsWith("partie") || id == "structure" -> "Parties"
        id.startsWith("effet") || id == "annuler_effet" -> "Effets"
        id.startsWith("export") -> "Exports"
        id.startsWith("guide") -> "Guide"
        id.startsWith("pad") || id == "live" -> "Live"
        id in NOUVEAUX -> "Épreuves du téléphone"
        else -> "Autres"
    }

    /** The groups' order on the checklist. */
    val GROUPES = listOf("Styles", "Charge", "Parties", "Effets", "Live", "Exports", "Guide", "Épreuves du téléphone", "Autres")

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
