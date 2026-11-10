package com.pocketstudio.app.human

import android.os.SystemClock
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The bench's own helpers, guarded (PLAN-RESTE R.6): what the Bench* tests use to drive the app must cost the bench's
 * clock what a finger costs, or what they measure (a 15 s demo, a count-in, a song's end) is read once it is over.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class BenchSelfTest : HumanTester("selftest") {
    @Before
    fun notOnTheBench() = assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())

    /**
     * R.6 (re-reads of banc-r1): [reachTag] swiped up to 8 times (« glisse » then « attends 1 »: 22 to 26 s of the bench's
     * clock) to bring into the screen's middle band an element already shown whole near an edge (« Mes projets » in the top
     * bar, a button at the bottom). A finger touches such an element where it is: no swipe, no wait, the same place.
     */
    @Test
    fun reachTagTouchesAVisibleElementInPlace() {
        begin()
        val h = screenHeight.toFloat()
        val tagged = compose.onAllNodes(SemanticsMatcher("tagged") { n -> SemanticsProperties.TestTag in n.config })
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
        val candidates = tagged.filter { n -> shownWhole(n) }.mapNotNull { n -> n.config[SemanticsProperties.TestTag].let { t -> centerOf(t)?.let { t to it } } }
            .filter { (_, at) -> at.second < 0.2f * h || at.second > 0.8f * h }.sortedBy { it.first }
        assertTrue("an element shown whole outside the screen's middle band, on the welcome:\n" + screenLines().joinToString("\n"), candidates.isNotEmpty())
        val (tag, at) = candidates.first()
        val clock = SystemClock.uptimeMillis()
        val reached = reachTag(tag)
        val spent = SystemClock.uptimeMillis() - clock
        assertEquals("« $tag » touched where it is", at, reached)
        assertTrue("the bench's clock moved $spent ms to reach « $tag », already shown (a finger: none)", spent < 300)
    }

    /**
     * Audit « banc crédible » 2, R1 (grave): the screen in text told the tester what a person does not see. An icon alone was
     * written by its TalkBack name (« bouton « Plus de choix » » for ⋮), and a sentence one can touch, with no frame, was a
     * « bouton » (Benjamin, recette tel-03: « Mes exports » « ressemble à une simple phrase »). A line says what is drawn:
     * the icon's glyph or shape, « bouton » only for something drawn with a fill or an outline, never TalkBack's name.
     */
    @Test
    fun theScreenInTextWritesWhatIsDrawnNotTalkBacksNames() {
        begin()
        val wrong = mutableListOf<String>()
        fun check(ok: Boolean, line: String, what: String) { if (!ok) wrong += "$what: $line" }
        // The language: an outlined button, its flag, its name and a ⌄ (TalkBack: « Choisis ta langue »).
        val lang = lineAt("lang_button")
        check(lang.startsWith("bouton ") && "Français" in lang && "⌄" in lang && "Choisis ta langue" !in lang, lang, "outlined, its ⌄, no TalkBack name")
        // « Plus tard … »: grey words, no fill, no outline: what a person sees is a sentence.
        assertNotNull(reachTag("welcome_later"))
        val later = lineAt("welcome_later")
        check("Plus tard" in later && !later.startsWith("bouton"), later, "a sentence with no frame is no « bouton »")
        amenee(AMENEE_SONG, java.io.File.createTempFile("screentext", ".txt").apply { deleteOnExit() })
        // ⋮ alone at the top: its glyph, not « Plus de choix » (nowhere on the screen: no word of it is drawn).
        val more = lineAt("top_more")
        check(Regex("^icône ⋮ @ \\d+,\\d+$").matches(more), more, "⋮ written as drawn")
        val lines = screenLines()
        lines.filter { "Plus de choix" in it }.forEach { check(false, it, "TalkBack's name of ⋮ on the screen") }
        // A link row (« Mes fichiers », recette tel-03): drawn filled, a folder before its words, the chevron › after.
        val tab = centerOf("tab_simple_Envoyer") ?: centerOf("tab_Envoyer") ?: error("no « Envoyer » tab\n" + lines.joinToString("\n"))
        act("touche ${tab.first} ${tab.second}")
        assertNotNull(reachTag("share_my_exports"))
        val files = lineAt("share_my_exports")
        check(files.startsWith("bouton (dossier) « Mes fichiers") && Regex("» › @ \\d+,\\d+$").containsMatchIn(files), files, "a link row: filled, its folder, its ›")
        assertTrue("written otherwise than drawn:\n" + wrong.joinToString("\n"), wrong.isEmpty())
    }

    /**
     * R1: [IconShapes] knows every icon the app draws (Material's by their name in the sources, the app's own vectors), so
     * an icon is never written « (forme non décrite) » on a seat's screen for want of a line in the table.
     */
    @Test
    fun everyIconOfTheAppHasItsShape() {
        val roots = listOf(java.io.File("src/main"), java.io.File("../ui-components/src/main"))
        assertTrue("sources not found from ${java.io.File(".").absolutePath}", roots.all { it.isDirectory })
        val used = sortedSetOf<String>()
        roots.forEach { r ->
            r.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { f ->
                val text = f.readText()
                Regex("""Icons\.(?:AutoMirrored\.)?(?:Filled|Outlined|Rounded|Sharp|TwoTone|Default)\.([A-Za-z0-9]+)""").findAll(text).forEach { used += it.groupValues[1] }
                Regex("""ImageVector\.Builder\(\s*(?:name\s*=\s*)?"([^"$]+)"""").findAll(text).forEach { used += it.groupValues[1].substringAfterLast('.') }
            }
        }
        com.pocketstudio.ui.WaveShape.entries.forEach { used += "wave_${it.name}" }
        assertTrue("too few icons read: $used", used.size > 100)
        val missing = used.filter { it !in IconShapes.SHAPES }
        assertTrue("icons with no shape in IconShapes: $missing", missing.isEmpty())
    }

    /** The screen's line drawn at the middle of the node tagged [tag] (± 2 px). */
    private fun lineAt(tag: String): String {
        val (x, y) = centerOf(tag) ?: error("no « $tag »\n" + screenLines().joinToString("\n"))
        val lines = screenLines()
        return lines.firstOrNull { l ->
            Regex("@ (\\d+),(\\d+)$").find(l)?.groupValues?.let { g -> kotlin.math.abs(g[1].toInt() - x) <= 2f && kotlin.math.abs(g[2].toInt() - y) <= 2f } == true
        } ?: error("no line at « $tag » ($x, $y)\n" + lines.joinToString("\n"))
    }
}
