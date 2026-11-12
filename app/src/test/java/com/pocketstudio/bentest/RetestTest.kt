package com.pocketstudio.bentest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** « Retester les corrections »: which steps are ticked, which ones the runner runs, what the report says. */
class RetestTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val ran = ArrayList<String>()
    private fun etape(name: String) = Etape(name, "Étape $name", 1_000) { ran += name; "ok" }
    private val all = listOf("ouverture", "style_dnb", "style_liquid_dnb", "export_wav", "latence", "ecran_eteint").map(::etape)

    @Test
    fun `vert - red and not conclusive steps are ticked, green orange and undone are not`() {
        val last = mapOf("ouverture" to Feu.VERT, "style_dnb" to Feu.ORANGE, "style_liquid_dnb" to Feu.ROUGE, "export_wav" to Feu.GRIS, "latence" to null)
        assertEquals(setOf("style_liquid_dnb", "export_wav"), Retest.parDefaut(last, all))
        assertTrue(Retest.parDefaut(emptyMap(), all).isEmpty())
    }

    @Test
    fun `rouge - the subset keeps the order, the opening first and the screen off step last`() {
        val picked = Retest.choisir(all, setOf("ecran_eteint", "latence", "style_liquid_dnb"))
        assertEquals(listOf("ouverture", "style_liquid_dnb", "latence", "ecran_eteint"), picked.map { it.name })
        // Even when the full list had it in the middle.
        val odd = listOf(etape("ecran_eteint"), etape("latence"))
        assertEquals(listOf("latence", "ecran_eteint"), Retest.choisir(odd, setOf("ecran_eteint", "latence")).map { it.name })
    }

    /** The opening (the studio brought in front) always runs first, unticked: v6 run, 2026-11-12, five steps failed without it. */
    @Test
    fun theRunnerRunsTheOpeningThenOnlyTheChosenSteps() {
        val carnet = Carnet(File(tmp.root, "c.tsv"))
        val steps = Robot.subset(all, Robot.TOUT, setOf("style_liquid_dnb", "ecran_eteint"))
        Passage(carnet, steps).run()
        assertEquals(listOf("ouverture", "style_liquid_dnb", "ecran_eteint"), ran)
        assertEquals(listOf("ouverture", "style_liquid_dnb", "ecran_eteint"), carnet.lines().filter { it.kind == Carnet.DEBUT }.map { it.step })
        // No retest chosen, or the essai éclair: nothing is cut.
        assertEquals(all.size, Robot.subset(all, Robot.TOUT, null).size)
        assertEquals(all.size, Robot.subset(all, Robot.ECLAIR, setOf("latence")).size)
    }

    @Test
    fun `vert - the last run is kept in a file and a retest only replaces the steps it ran`() {
        val f = File(tmp.root, "d/derniere.tsv")
        Retest.ecrire(f, mapOf("a" to Feu.ROUGE, "b" to Feu.VERT, "c" to null))
        assertEquals(mapOf("a" to Feu.ROUGE, "b" to Feu.VERT, "c" to null), Retest.lire(f))
        val merged = Retest.fusion(Retest.lire(f), mapOf("a" to Feu.VERT, "b" to null))
        assertEquals(mapOf("a" to Feu.VERT, "b" to Feu.VERT, "c" to null), merged)
        assertTrue(Retest.lire(File(tmp.root, "none")).isEmpty())
        val choice = File(tmp.root, "retest.txt")
        assertEquals(null, Retest.lireChoix(choice))
        Retest.ecrireChoix(choice, listOf("a", "b"))
        assertEquals(setOf("a", "b"), Retest.lireChoix(choice))
    }

    @Test
    fun `vert - the report header says how many steps were retested`() {
        val md = Rapport.markdown(emptyList(), all.take(3), listOf("Version" to "x"), tout = true, retest = 3)
        assertTrue(md, md.contains("Retest : 3 étapes"))
        assertEquals("Retest : 1 étape", Retest.entete(1))
        assertTrue(!Rapport.markdown(emptyList(), all, emptyList(), tout = true).contains("Retest"))
    }
}
