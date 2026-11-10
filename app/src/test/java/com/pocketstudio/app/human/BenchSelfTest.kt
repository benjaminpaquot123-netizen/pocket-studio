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

    /**
     * PLAN-RESTE R.8 (hidden defect B09 of defauts-caches.tsv): Benjamin saw nothing telling him the welcome's list goes on
     * below the screen; the bench wrote « ↓ la suite plus bas » under every list that scrolls, an arrow the app does not
     * draw. A person sees a card cut by the screen's edge: the line of what is cut says « coupé », and no arrow is invented.
     */
    @Test
    fun theScreenInTextSaysWhatIsCutNotAnArrowTheAppDoesNotDraw() {
        begin()
        val lines = screenLines()
        assertTrue("an arrow the app does not draw:\n" + lines.joinToString("\n"), lines.none { "la suite plus bas" in it || "la suite sur le côté" in it })
        assertTrue("the card cut by the screen's bottom edge:\n" + lines.joinToString("\n"), lines.any { "Extraire la voix" in it && "coupé" in it })
        assertTrue("whole cards are not cut:\n" + lines.joinToString("\n"), lines.none { "J'apprends" in it && "coupé" in it })
    }

    /**
     * Audit « banc crédible » 2, R2 (a limit, written in the testers' instructions): the app's clock moves only with the
     * gestures. While the tester reads and thinks (real time going by), a message that leaves after 6 s does not leave.
     */
    @Test
    fun theAppsClockStandsStillWhileTheTesterReads() {
        begin()
        val before = SystemClock.uptimeMillis()
        Thread.sleep(1500)
        assertEquals("the app's clock while the tester reads", before, SystemClock.uptimeMillis())
        act("attends 2")
        assertTrue("« attends 2 » lets 2 s of the app go by", SystemClock.uptimeMillis() - before >= 2000)
    }

    /**
     * Fresh review of the « réalisme » lot, G1 (grave): a Pad Live pad's state (it plays, it waits for the grid, it rests)
     * lived only in its TalkBack name, and R1 took it out: every pad read « bouton @ x,y » whatever its light. A person
     * sees the light (the pad lit, its ring) and the faint ring of a pad that waits: the line says what is drawn.
     */
    @Test
    fun aPadLiveLineSaysThePadsLightAsDrawn() {
        begin()
        amenee(AMENEE_PADLIVE, java.io.File.createTempFile("padlight", ".txt").apply { deleteOnExit() })
        fun state(pad: Int) = IntArray(com.pocketstudio.engine.NativeEngine.LIVE_PADS).also(com.pocketstudio.engine.NativeEngine::clipStatesNow)[pad]
        val seen = mutableMapOf<Int, String>()
        fun look(pads: IntRange) = pads.forEach { p ->
            val line = lineAt("padlive_pad_$p")
            val st = state(p)
            seen.putIfAbsent(st, line)
            val drawn = when (st) { 2 -> "allumé"; 1 -> "anneau pâle"; else -> null }
            assertTrue("pad $p, state $st (0 rests, 1 waits, 2 plays): $line", if (drawn == null) "allumé" !in line && "anneau" !in line else "[$drawn" in line || ", $drawn" in line)
        }
        look(0..3)
        // A loop touched: it plays at once (nothing plays yet).
        val loop = centerOf("padlive_pad_0")!!
        act("touche ${loop.first} ${loop.second}")
        look(0..3)
        // Another loop while the first plays: it waits for the loop's end, its ring faint (the engine's own toggle, a
        // tenth of a second later: a finger's gesture takes a second, the wait may be over).
        repeat(4) {
            if (1 in seen) return@repeat
            com.pocketstudio.engine.NativeEngine.toggleClip(2)
            act("attends 0.1")
            look(0..3)
            if (state(2) != 1) { if (state(2) == 2) com.pocketstudio.engine.NativeEngine.toggleClip(2); act("attends 0.1") }
        }
        assertTrue("a pad at rest, a pad playing and a pad waiting were all read: $seen", seen.keys.containsAll(listOf(0, 1, 2)))
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
