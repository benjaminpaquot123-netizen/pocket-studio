package com.pocketstudio.app.human

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * PLAN-RESTE 7.5 (rejeu lisible): a replay that ends « oracle non tenu » (or « divergé ») says why in words a team can act
 * on: the last screen that differs from the origin seat (its step, its fingerprint, its first different line), and, for a
 * gesture in pixels, the button the origin's finger was on and that is no longer under it (« bouton … absent »).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class BenchReplayReadableTest : HumanTester("rejeu-lisible") {
    @Before
    fun notOnTheBench() = assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())

    @Test
    fun aMovedButtonIsSaidInTheVerdict() {
        begin()
        // A button shown on this screen (it is still there, elsewhere: « déplacé »), one never shown (« absent »), and a
        // point under which there is nothing to touch.
        val tag = compose.onAllNodes(SemanticsMatcher("tagged") { n -> SemanticsProperties.TestTag in n.config })
            .fetchSemanticsNodes(atLeastOneRootRequired = false).map { it.config[SemanticsProperties.TestTag] }
            .firstOrNull { t -> centerOf(t)?.let { (x, y) -> screenText().under(x, y).substringBefore(" «") == t } == true }
        assertTrue("a tagged button on the first screen:\n" + screenLines().joinToString("\n"), tag != null)
        val (bx, by) = centerOf(tag!!)!!
        val buttonLine = screenText().under(bx, by)
        val w = org.robolectric.RuntimeEnvironment.getApplication().resources.displayMetrics.widthPixels
        val empty = (1..19).flatMap { i -> (1..19).map { j -> w * i / 20f to screenHeight * j / 20f } }
            .firstOrNull { (x, y) -> screenText().under(x, y).startsWith("rien") }
        assertTrue("a point with nothing under it:\n" + screenLines().joinToString("\n"), empty != null)
        val (ex, ey) = empty!!
        val touch = "touche ${ex.toInt()} ${ey.toInt()}"

        // The origin: a seat played on this very screen, its journal then made to say that its finger was on the button
        // (step 1) and on « Suivant » (step 2), and its second screen made to differ by one line.
        val origin = kotlin.io.path.createTempDirectory("origine").toFile()
        listOf(touch, touch, "fin").forEachIndexed { i, c -> File(origin, "cmd-${i + 1}.txt").writeText(c) }
        seatLoop(origin, SeatOptions(), File(origin, "journal.txt"))
        val journal = File(origin, "journal.txt")
        journal.writeText(
            journal.readText()
                .replace(Regex("(?m)^#1 DOIGT .*$"), Regex.escapeReplacement("#1 DOIGT $buttonLine"))
                .replace(Regex("(?m)^#2 DOIGT .*$"), Regex.escapeReplacement("#2 DOIGT wizard_next « Suivant »")),
        )
        val screen2 = File(origin, "screen-2.txt")
        val lines = screen2.readLines()
        screen2.writeText((lines.take(2) + "« Suivant » bouton @ 1,1" + lines.drop(2)).joinToString("\n", postfix = "\n"))

        // The replay test, as scripts/bench/rejeu-defauts.sh writes it: its gestures in a folder of their own, the origin
        // named by `rejeu.origine=` (oracle.txt's `source=`), an oracle the screen never holds.
        val replayDir = kotlin.io.path.createTempDirectory("rejeu").toFile()
        listOf(touch, touch).forEachIndexed { i, c -> File(replayDir, "cmd-${i + 1}.txt").writeText(c) }
        val seat = kotlin.io.path.createTempDirectory("siege").toFile()
        File(seat, "start.txt").writeText("rejeu=${replayDir.path}\nrejeu.origine=${origin.path}\nrejeu.n=2\nrejeu.attendu=jamais écrit sur aucun écran\n")
        val (o, _) = SeatOptions.read(File(seat, "start.txt"))
        seatLoop(seat, o, File(seat, "journal.txt"))
        val verdict = File(seat, "rejeu.txt").readText()
        val replayJournal = File(seat, "journal.txt").readText()
        val under = Regex("(?m)^#2 DOIGT (.*)$").find(replayJournal)?.groupValues?.get(1)
        val print = Regex("(?m)^#2 ROUTE .*\\| EMPREINTE (\\w+) ").find(replayJournal)?.groupValues?.get(1)
        val said = "verdict:\n$verdict\njournal:\n$replayJournal"
        assertTrue(said, verdict.startsWith("oracle non tenu : « jamais écrit sur aucun écran » absent"))
        assertTrue("step 2: the origin's « Suivant » is said absent, with what the finger is on now; $said",
            verdict.contains("pas 2 : bouton « Suivant » (wizard_next) absent, sous le doigt : $under"))
        val words = Regex("« (.*) »").find(buttonLine)?.groupValues?.get(1)
        val named = if (words != null) "bouton « $words » ($tag)" else "bouton ($tag)"
        // Where the button is drawn now (the welcome may have settled since it was first read).
        val (nx, ny) = centerOf(tag)!!
        assertTrue("step 1: the origin's button ($buttonLine) is said moved, with where it is now; $said",
            verdict.contains("pas 1 : $named déplacé (maintenant en ${nx.toInt()},${ny.toInt()}), sous le doigt : $under"))
        assertTrue("the last screen that differs: screen 2 (after gesture 1), its fingerprint, the first different line; $said",
            verdict.contains("dernier écran différent de l'origine : écran 2, empreinte $print") &&
                verdict.contains("première ligne différente : origine « « Suivant » bouton @ 1,1 »"))
    }
}
