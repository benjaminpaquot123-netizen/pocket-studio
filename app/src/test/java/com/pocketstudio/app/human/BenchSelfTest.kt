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

    /**
     * Fresh review of the « réalisme » lot, M1 and M2: roles only TalkBack hears (« case » for a chip, « interrupteur »
     * and « coché » for the round « ? ») and marks the app draws with no icon (the Pad Live card's grid, the ⚠ of a
     * warning line outside any button) were still written otherwise than drawn.
     */
    @Test
    fun chipsTogglesAndDrawnMarksAreWrittenAsDrawn() {
        begin()
        val wrong = mutableListOf<String>()
        fun check(ok: Boolean, line: String, what: String) { if (!ok) wrong += "$what: $line" }
        assertNotNull(reachTag("launch_padlive"))
        val card = lineAt("launch_padlive")
        check("(grille de pads)" in card, card, "the Pad Live card's grid drawn")
        amenee(AMENEE_SONG, java.io.File.createTempFile("marks", ".txt").apply { deleteOnExit() })
        val help = lineAt("help_labels_button")
        check(help.startsWith("bouton « ? »") && "interrupteur" !in help && "coché" !in help, help, "the round « ? »: a framed button, no switch")
        val tab = centerOf("tab_simple_Envoyer") ?: centerOf("tab_Envoyer") ?: error("no « Envoyer » tab")
        act("touche ${tab.first} ${tab.second}")
        val chip = lineAt("share_bars_8")
        check(chip.startsWith("bouton « 8 »") && "sélectionné" in chip, chip, "a chip: a framed button, selected")
        val lines = screenLines()
        val warn = lines.indexOfFirst { "Rien à entendre" in it }
        check(warn > 0 && Regex("^image (⚠|\\(i dans un rond\\)) @ ").containsMatchIn(lines[warn - 1]), lines.getOrElse(warn - 1) { "-" }, "the check's icon before « Rien à entendre »")
        assertTrue("written otherwise than drawn:\n" + wrong.joinToString("\n") + "\n\n" + lines.joinToString("\n"), wrong.isEmpty())
    }

    /**
     * PLAN-RESTE 4.6 (seats D b, F a, F c of round 5: a first screen almost empty, « l'éditeur arrive après une attente »):
     * the app reads its project off the main thread (« Ouverture du projet… ») after the saves still being written. On a
     * loaded machine that work runs late: the bench showed the tester the opening's ring, and a phone would not have. The
     * slowness is injected here: a save that keeps a worker running (CPU) for 12 s. The bench waits for the first screen
     * (30 s at most) only while its own workers are busy: the screen it shows is the app's opened screen.
     */
    @Test
    fun aBlankFirstScreenIsWaitedForOnlyWhenTheBenchIsBusy() {
        val stop = java.util.concurrent.atomic.AtomicBoolean(false)
        com.pocketstudio.app.rhythm.ClosingSaves.launch(20_000) {
            val end = System.nanoTime() + 12_000_000_000L
            while (System.nanoTime() < end && !stop.get()) Thread.onSpinWait()
        }
        try {
            begin()
            val lines = screenLines()
            assertTrue("the first screen, the bench's workers busy, is the app's opened one:\n" + lines.joinToString("\n"), lines.none { "Ouverture du projet" in it })
        } finally {
            stop.set(true)
        }
    }

    /**
     * PLAN-RESTE 4.6, the other side: the same blank screen while nothing of the bench works (the save sleeps, as a phone's
     * flash memory may): not the bench's to hide. The screen is shown as it is (recette tel-20 « premier écran après Pro »).
     */
    @Test
    fun aBlankFirstScreenWithTheBenchAtRestIsShownAsItIs() {
        com.pocketstudio.app.rhythm.ClosingSaves.launch(20_000) { Thread.sleep(9_000) }
        begin()
        val lines = screenLines()
        assertTrue("the bench at rest does not wait the opening away:\n" + lines.joinToString("\n"), lines.any { "Ouverture du projet" in it })
    }

    /**
     * PLAN-RESTE 5.10 (control 11): an exception thrown by the test's own code (Compose's test API, Robolectric, the bench)
     * is never the app's crash. Sorted by its frames, never by its class: the first frame of com.pocketstudio outside the
     * bench (`.human`) is the app's; none, it is the bench's: « BANC : … » to the tester, the seat goes on, and ends
     * « FIN : banc » after 4 of them. An exception thrown by the app's code is still a crash (« plantage »).
     */
    @Test
    fun aTestExceptionIsNeverTheAppsCrash() {
        begin()
        fun seat(vararg cmds: String): java.io.File {
            val dir = kotlin.io.path.createTempDirectory("siege").toFile()
            cmds.forEachIndexed { i, c -> java.io.File(dir, "cmd-${i + 1}.txt").writeText(c) }
            seatLoop(dir, SeatOptions(), java.io.File(dir, "journal.txt"))
            return dir
        }
        var thrown = 0
        beforeGesture = { cmd ->
            if (cmd.startsWith("attends")) {
                thrown++
                when (thrown) {
                    1 -> compose.onNode(androidx.compose.ui.test.hasTestTag("no_such_node_on_any_screen")).assertExists()
                    2 -> error("the bench's own helper failed")
                    3 -> listOf<String>()[2].length
                    else -> java.io.File("/no/such/folder/of/the/bench.txt").readText()
                }
            }
        }
        val bench = seat("attends 0.1", "attends 0.1", "attends 0.1", "attends 0.1", "regarde", "fin")
        val journal = java.io.File(bench, "journal.txt").readText()
        val end = java.io.File(bench, "fin.txt").readText()
        assertEquals("4 exceptions of the test's code, each a « BANC » line:\n$journal", 4, Regex("(?m)^#\\d+ BANC exception").findAll(journal).count())
        assertTrue("never a crash of the app:\n$journal", "CRASH" !in journal && "plantage" !in end)
        assertTrue("the seat ends « banc » after 4 of them: $end", end.startsWith("banc"))
        // The app's own code throwing (an enum read from a wrong name, inside the app's class): still the app's crash.
        beforeGesture = { cmd -> if (cmd.startsWith("attends")) com.pocketstudio.app.rhythm.ExportFormat.valueOf("NoSuchFormat") }
        val app = seat("attends 0.1", "fin")
        assertEquals(java.io.File(app, "journal.txt").readText(), "plantage", java.io.File(app, "fin.txt").readText())
        // Review of « banc6b » (grave): the app's exception wrapped by a frame of the bench (a looper or a helper that
        // rethrows it with a cause) is still the app's crash: any link of the chain thrown by the app's code makes it so.
        beforeGesture = { cmd ->
            if (cmd.startsWith("attends")) {
                val cause = runCatching { com.pocketstudio.app.rhythm.ExportFormat.valueOf("NoSuchFormat") }.exceptionOrNull()
                throw IllegalStateException("rethrown by the bench", cause)
            }
        }
        val wrapped = seat("attends 0.1", "fin")
        assertEquals(java.io.File(wrapped, "journal.txt").readText(), "plantage", java.io.File(wrapped, "fin.txt").readText())
        beforeGesture = null
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
