package com.pocketstudio.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Hands-on audit, chantier 2 (G5, rule 15 of docs/DESIGN.md « Une icône par action, celle d'Android »; recette tel-03: « le
 * logo est nul »): « Partager » had three icons (the iOS box and arrow in the bars and the sheet, a paper plane on the
 * Mode simple's tab, Android's in Mes projets) and three colours (white, orange, green). One icon now, Android's
 * `Icons.Filled.Share`, and never in the green that means « ça joue ».
 */
class ShareIconEverywhereTest {
    private val sources = listOf(File("src/main/java"), File("../ui-components/src/main/java"))
        .flatMap { d -> d.walkTopDown().filter { it.extension == "kt" }.toList() }

    @Test
    fun oneShareIconEverywhere() {
        // No iOS share icon left anywhere (the audit's « grep IosShare = 0 »).
        val ios = sources.filter { "IosShare" in it.readText() }.map { it.name }
        assertTrue("Icons.Filled.IosShare still drawn in: $ios", ios.isEmpty())
        // The Mode simple's « Partager » tab: the share icon, not the paper plane.
        assertEquals("The « Partager » tab's icon", Icons.Filled.Share, SimpleTab.Envoyer.icon)
        assertEquals("The « Partager » page's icon", Icons.Filled.Share, AppTab.Envoyer.icon)
        // Never in green (« ça joue »): Mes projets' « Partager mon morceau » was.
        val green = sources.flatMap { f ->
            f.readLines().withIndex().filter { (_, l) -> "Icons.Filled.Share" in l && "Neon.Acid" in l }.map { "${f.name}:${it.index + 1}" }
        }
        assertTrue("The share icon drawn in green: $green", green.isEmpty())
    }
}
