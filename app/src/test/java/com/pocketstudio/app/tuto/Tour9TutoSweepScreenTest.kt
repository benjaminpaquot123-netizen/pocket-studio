package com.pocketstudio.app.tuto

import com.pocketstudio.app.human.HumanTester
import com.pocketstudio.app.human.SeatOptions
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Tour 9, « balayage », family 2 on the phone itself (tour 8, P6-grille: in Japanese at 200 %, the unfolded words
 * covered the whole grid). A grid step of the course on a narrow phone and on a foldable, at 200 %, in a few scripts
 * (Latin, Japanese, Arabic, Devanagari), at both levels: the grid's cells left on screen before and after « Lire la
 * suite » (and at 100 %, 130 % for three of them), and the panel's words the screen cuts. A cell is a button the bench reads with a state (« état « … » »).
 * Runs only with `-Pbench.out=<dir>`; writes `<dir>/screen/<case>.tsv`, never fails.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class Tour9TutoSweepScreenTest(private val lang: String, private val device: String, private val mode: String, private val font: String, private val amenee: String) :
    HumanTester("tour9-balayage") {
    private fun cells(lines: List<String>) = lines.count { it.startsWith("bouton") && "état « " in it && !it.contains("désactivé") }

    @Test
    fun sweep() {
        val dir = System.getProperty("ps.bench.out")?.takeIf { it.isNotBlank() }
        assumeTrue("run with -Pbench.out=<dir> (scripts/bench/tuto-sweep.sh)", dir != null)
        val out = File(dir!!, "screen").apply { mkdirs() }
        val name = "$lang-$device-$font-$mode-${amenee.replace(':', '_')}"
        val o = SeatOptions.parse(mapOf("lang" to lang, "device" to device, "font" to font, "mode" to mode, "amenee" to amenee)).first
        begin(o)
        val rows = ArrayList<String>()
        val (style, chapter) = amenee.removePrefix("tuto:").split(':').let { it[0] to it[1] }
        fun row(sev: String, kind: String, what: String) =
            rows.add(listOf(sev, kind, style, if (mode == "pro") "Pro" else "Facile", chapter, lang, device, font, what.replace('\t', ' ')).joinToString("\t"))
        val ready = runCatching { amenee(amenee, File(out, "$name-journal.txt")) }
        if (ready.isFailure) {
            row("banc", "amenee-ratee", "le banc n'a pas pu ouvrir le chapitre : ${ready.exceptionOrNull()?.message}")
        } else {
            val before = screenLines()
            val more = reachTag("tuto_text_more")
            if (more != null) act("touche ${more.first} ${more.second}")
            val after = screenLines()
            val a = cells(before)
            val b = cells(after)
            File(out, "$name-screen.txt").writeText("== arrive ($a cases)\n" + before.joinToString("\n") + "\n== après « Lire la suite » ($b cases, bouton ${if (more == null) "absent" else "touché"})\n" + after.joinToString("\n") + "\n")
            when {
                a == 0 -> row("grave", "grille-cachee", "aucune case de la grille visible dès l'arrivée sur l'étape, panneau replié")
                more != null && b == 0 -> row("grave", "consigne-depliee-cache-grille", "après « Lire la suite », plus aucune case visible ($a avant) : la consigne couvre toute la grille")
                more != null && b * 2 < a -> row("finition", "consigne-depliee-cache-grille", "après « Lire la suite », $b cases visibles sur $a")
            }
            val cut = (before + after).filter { it.startsWith("texte") && "coupé" in it }.distinct()
            cut.forEach { row("finition", "texte-coupe", "texte coupé à l'écran : ${it.take(160)}") }
        }
        File(out, "$name.tsv").writeText(rows.joinToString("\n") + if (rows.isEmpty()) "" else "\n")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0} {1} {2} {3} {4}")
        fun cases(): List<Array<String>> {
            val out = ArrayList<Array<String>>()
            for (lang in listOf("ja", "fr", "de", "ar", "hi"))
                for (device in listOf("phone360", "foldable"))
                    for (mode in listOf("pro", "simple"))
                        out += arrayOf(lang, device, mode, "200", "tuto:techno:3")
            // The same at 100 % and 130 %: where the folded panel leaves the grid, does the unfolded one hide it?
            for (lang in listOf("ja", "fr", "de")) for (font in listOf("100", "130")) for (device in listOf("phone360", "foldable"))
                out += arrayOf(lang, device, "pro", font, "tuto:techno:3")
            return out
        }
    }
}
