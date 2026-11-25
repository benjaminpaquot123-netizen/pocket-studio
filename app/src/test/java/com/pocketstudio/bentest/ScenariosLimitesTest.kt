package com.pocketstudio.bentest

import com.pocketstudio.model.StarterWizard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The catalogue « Styles, structure et limites » (tour 9): its shape, without a phone. */
class ScenariosLimitesTest {
    private val liste = ScenariosLimites.LISTE

    @Test
    fun theCatalogueHasItsNinetyOneScenariosWithUniqueIds() {
        assertEquals(91, liste.size)
        assertEquals(liste.size, liste.map { it.id }.toSet().size)
        assertTrue(liste.all { it.id.startsWith(ScenariosLimites.PREFIXE) })
        // In the robot's full list, once, and no other scenario takes the prefix.
        assertTrue(Scenarios.LISTE.containsAll(liste))
        assertEquals(liste.size, Scenarios.LISTE.count { it.id.startsWith(ScenariosLimites.PREFIXE) })
        assertEquals(Scenarios.LISTE.size, Scenarios.LISTE.map { it.id }.toSet().size)
    }

    @Test
    fun eachScenarioHasAKnownGestureAPreciseOracleAndAFairTime() {
        val formes = listOf(
            Regex("export:[a-z_]+:(m4a|wav|flac)"), Regex("lim:style_tempo:[a-z_]+"),
            Regex("lim:refaire:part_[a-z_0-9]+:(-1|0|1)(:lecture)?"),
            Regex("lim:pile:\\d+"), Regex("lim:borne:(min|max)(:lecture|:export)?"), Regex("lim:borne:hors"),
            Regex("lim:nom:(long|accents)"), Regex("lim:enorme:\\d+"),
        )
        for (s in liste) {
            assertTrue(s.id + " : " + s.geste, formes.any { it.matches(s.geste.orEmpty()) })
            assertTrue(s.id, s.oracle.length >= 20)
            assertTrue(s.id, s.estimeS > 0 && s.estimeS * 1000L <= s.maxMs)
            assertTrue(s.id, s.niveau in listOf(Scenarios.NIVEAU_DEBUTANT, Scenarios.NIVEAU_MOYEN, Scenarios.NIVEAU_PRO, Scenarios.NIVEAU_MESURE))
        }
        // The whole catalogue holds in about an hour and a half; the conductor cuts it into packs.
        assertTrue(liste.sumOf { it.estimeS }.toString(), liste.sumOf { it.estimeS } <= 95 * 60)
    }

    @Test
    fun everyStyleIsCrossedWithATempoChangeAndAnExport() {
        val styles = StarterWizard.STYLES.map { it.id }
        assertTrue(styles.containsAll(ScenariosLimites.STYLES))
        // Every style is exported: by this catalogue, or already by the full run (its own exports or the « save » theme).
        val exportes = ScenariosLimites.DEJA_EXPORTES
        for (st in ScenariosLimites.STYLES) {
            assertTrue(st, liste.any { it.id == "lim_tempo_$st" && it.geste == "lim:style_tempo:$st" })
            assertTrue(st, st in exportes || liste.any { it.id.startsWith("lim_export_${st}_") && it.geste.orEmpty().startsWith("export:$st:") })
        }
        // Each export is a new pair: none repeats what the full run already exports.
        val deja = Scenarios.LISTE.filterNot { it.id.startsWith(ScenariosLimites.PREFIXE) }.map { it.geste }.toSet()
        assertTrue(liste.filter { it.geste.orEmpty().startsWith("export:") }.none { it.geste in deja })
    }

    @Test
    fun eachStructureGestureIsUndoneAndRedoneAndTheLimitsAreAllThere() {
        for ((tag) in ScenariosLimites.GESTES) {
            val court = tag.removePrefix("part_").substringBefore('_')
            assertTrue(tag, liste.any { it.id == "lim_refaire_$court" })
        }
        val ids = liste.map { it.id }
        assertTrue(ids.containsAll(listOf(
            "lim_tempo_min", "lim_tempo_max", "lim_tempo_min_lecture", "lim_tempo_max_lecture", "lim_tempo_min_export",
            "lim_tempo_max_export", "lim_tempo_hors_bornes", "lim_nom_long", "lim_nom_accents", "lim_projet_enorme",
            "lim_annuler_pile_3", "lim_annuler_pile_5", "lim_annuler_pile_10",
        )))
        assertTrue(ScenariosLimites.NOM_LONG.length > 100)
    }
}
