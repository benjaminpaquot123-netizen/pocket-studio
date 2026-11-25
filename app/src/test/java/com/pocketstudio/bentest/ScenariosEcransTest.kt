package com.pocketstudio.bentest

import com.pocketstudio.model.StarterWizard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The catalogue's « screens » and « endurance » themes (tour 9, lot cat-400): their shape, without a phone. */
class ScenariosEcransTest {
    private val liste = ScenariosEcrans.LISTE

    @Test
    fun theThemesHaveUniqueIdsTheirPrefixesAndAKnownGesture() {
        assertEquals(liste.size, liste.map { it.id }.toSet().size)
        assertTrue(liste.all { it.id.startsWith(ScenariosEcrans.PREFIXE_ECRANS) || it.id.startsWith(ScenariosEcrans.PREFIXE_ENDURANCE) })
        assertTrue(Scenarios.LISTE.containsAll(liste))
        assertEquals(Scenarios.LISTE.size, Scenarios.LISTE.map { it.id }.toSet().size)
        // No two scenarios of the whole catalogue play the same gesture.
        val gestes = Scenarios.LISTE.mapNotNull { it.geste }
        assertEquals(gestes.groupBy { it }.filterValues { it.size > 1 }.keys.toString(), gestes.size, gestes.toSet().size)
        val formes = listOf(
            Regex("ecr:[a-z]+(,[a-z]+)*:\\d+"), Regex("ecr:tout:\\d+"), Regex("ecr:fond:[a-z]+"),
            Regex("end:(jouer|fond|partie):\\d+"), Regex("end:long:[a-z_]+:\\d+"),
        )
        for (s in liste) {
            assertTrue(s.id + " : " + s.geste, formes.any { it.matches(s.geste.orEmpty()) })
            assertTrue(s.id, s.oracle.length >= 20)
            assertTrue(s.id, s.estimeS > 0 && s.estimeS * 1000L <= s.maxMs)
        }
    }

    @Test
    fun eachScreenLineNamesKnownTabsAndEachListenAKnownStyle() {
        val styles = StarterWizard.STYLES.map { it.id }.toSet()
        for (s in liste) {
            val g = s.geste.orEmpty().split(':')
            when {
                g[0] == "ecr" && g[1] == "fond" -> assertTrue(s.id, g[2] in ScenariosEcrans.ECRANS)
                g[0] == "ecr" -> assertTrue(s.id, !ScenariosEcrans.parcours(g).isNullOrEmpty())
                g[1] == "long" -> assertTrue(s.id, g[2] in styles && g[3].toInt() >= 60)
            }
        }
        // Every style is listened to for a minute at least, once.
        for (st in styles) assertEquals(st, 1, liste.count { it.geste.orEmpty().startsWith("end:long:$st:") && !it.id.endsWith("_10min") })
        // A wrong line has no work.
        assertNull(ScenariosEcrans.parcours(listOf("ecr", "cuisine", "2")))
        assertNull(ScenariosEcrans.parcours(listOf("ecr", "mix", "0")))
        assertEquals(10, ScenariosEcrans.parcours(listOf("ecr", "tout", "2"))?.size)
    }

    @Test
    fun theCatalogueReachesAboutFourHundredInPartsOfFiftyUnderTheHourEach() {
        val n = Scenarios.LISTE.size
        assertTrue("$n scénarios", n in 390..420)
        for (k in 1..Scenarios.NB_PARTIES) {
            val p = Scenarios.liste(Scenarios.partie(k))
            assertTrue("partie $k: ${p.size}", p.size <= Scenarios.TAILLE_PARTIE)
            assertTrue("partie $k: ${p.sumOf { it.estimeS }} s", p.sumOf { it.estimeS } <= Scenarios.DUREE_COMPLET_MAX_S)
        }
    }
}
