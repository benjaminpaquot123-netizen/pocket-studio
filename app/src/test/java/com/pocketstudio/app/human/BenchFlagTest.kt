package com.pocketstudio.app.human

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Tour 9 (banc-b, « drapeaux »): each way the bench has of not playing a gesture leaves its [BenchFlag] line in the
 * seat's journal, with where and why. Here, the four refusals of the seat's loop, played as a seat plays them (command
 * files, the app as just installed); the fifth (a covered finger) is [BenchFlagFingerTest]'s.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class BenchFlagTest : HumanTester("drapeaux") {
    @Before
    fun notOnTheBench() = assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())

    @Test
    fun everyRefusedGestureOfTheLoopLeavesItsFlag() {
        begin()
        val m = org.robolectric.RuntimeEnvironment.getApplication().resources.displayMetrics
        val left = SeatOptions.Hand.LEFT
        // A button of this screen that the left thumb cannot reach at any of its points (its corner nearest to the hand).
        val far = compose.onAllNodes(SemanticsMatcher("tagged") { n -> SemanticsProperties.TestTag in n.config })
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
            .firstOrNull { n ->
                val r = BenchWindows.drawnBounds(n, m)
                val tag = n.config[SemanticsProperties.TestTag]
                r.width > 1f && r.height > 1f && r.top >= 0f && r.right <= m.widthPixels &&
                    !SeatOptions.reachable(left, r.left, r.bottom - 1f, m.widthPixels, m.heightPixels, m.density) &&
                    screenText().under(r.center.x, r.center.y).substringBefore(" «") == tag
            }?.config?.get(SemanticsProperties.TestTag)
        assertNotNull("a button out of the left thumb's reach on the first screen:\n" + screenLines().joinToString("\n"), far)
        val shown = screenLines().joinToString("\n")

        val dir = kotlin.io.path.createTempDirectory("drapeaux").toFile()
        val cmds = listOf("touche etiquette_que_rien_ne_porte", "tape 10 20", "touche ${m.widthPixels - 8} 20", "touche $far", "fin")
        cmds.forEachIndexed { i, c -> File(dir, "cmd-${i + 1}.txt").writeText(c) }
        seatLoop(dir, SeatOptions().copy(hand = left), File(dir, "journal.txt"))
        val journal = File(dir, "journal.txt").readText()
        val flags = BenchFlag.of(journal)
        val said = "flags:\n${flags.joinToString("\n")}\njournal:\n$journal"
        fun flag(n: Int) = flags.singleOrNull { BenchFlag.value(it, "n") == "$n" }.also { assertNotNull("one flag for gesture $n; $said", it) }!!

        assertEquals("one flag per refused gesture, none for « fin »; $said", 4, flags.size)

        // 1. A tag nothing carries: not found, and what the screen shows instead.
        flag(1).let { f ->
            assertEquals(said, "gesture:touche", BenchFlag.value(f, "where"))
            assertEquals(said, BenchFlag.TAG_MISSING, BenchFlag.value(f, "why"))
            assertEquals(said, "etiquette_que_rien_ne_porte", BenchFlag.value(f, "target"))
            assertEquals(said, "non", BenchFlag.value(f, "found"))
            val instead = BenchFlag.value(f, "screen").orEmpty()
            assertTrue("what was on the screen instead, with a tag one can touch there (« $far »); $said\nscreen:\n$shown", instead.contains(far!!))
        }
        // 2. Words that are no gesture.
        flag(2).let { f ->
            assertEquals(said, "gesture:tape", BenchFlag.value(f, "where"))
            assertEquals(said, BenchFlag.MALFORMED, BenchFlag.value(f, "why"))
            assertEquals(said, "tape 10 20", BenchFlag.value(f, "target"))
        }
        // 3. Pixels beyond the thumb: where, and which hand.
        flag(3).let { f ->
            assertEquals(said, BenchFlag.OUT_OF_REACH, BenchFlag.value(f, "why"))
            assertEquals(said, "${m.widthPixels - 8},20", BenchFlag.value(f, "at"))
            assertTrue(said, BenchFlag.value(f, "more").orEmpty().contains(left.word))
        }
        // 4. A tag that is there, with no point a finger can touch: found, and where it is drawn.
        flag(4).let { f ->
            assertEquals(said, BenchFlag.NO_POINT, BenchFlag.value(f, "why"))
            assertEquals(said, far, BenchFlag.value(f, "target"))
            assertEquals(said, "oui", BenchFlag.value(f, "found"))
            assertTrue(said, Regex("\\d+,\\d+,\\d+,\\d+").matches(BenchFlag.value(f, "bounds").orEmpty()))
        }
        // The journal's own lines stay as they were (its readers count them).
        assertTrue(said, journal.contains("#1 BANC étiquette introuvable : etiquette_que_rien_ne_porte"))
        assertTrue(said, journal.contains("#2 BANC geste mal écrit : tape 10 20"))
        assertTrue(said, journal.contains("#3 REFUS hors de portée du pouce"))
        assertTrue(said, journal.contains("#4 REFUS « $far »"))
    }
}

/** The fifth refusal (the finger on a covered target), and the flag's own writing. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h852dp-port-hdpi")
class BenchFlagFingerTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aCoveredFingerLeavesItsFlag() {
        compose.setContent {
            Box(Modifier.fillMaxSize()) {
                Button(onClick = {}, modifier = Modifier.testTag("target")) { Text("Valider") }
                Card(Modifier.size(200.dp)) { Text("Fait") }
            }
        }
        compose.waitForIdle()
        val screen = ApplicationProvider.getApplicationContext<android.app.Application>().resources.displayMetrics
        val r = BenchWindows.drawnBounds(compose.onNodeWithTag("target", useUnmergedTree = true).fetchSemanticsNode(), screen)
        val f = ScreenText(compose, screen, reads = true, rtl = false).finger(7, r.center.x, r.center.y)
        val said = f.lines.joinToString("\n")
        assertTrue(said, f.refused)
        val flag = BenchFlag.of(said).singleOrNull()
        assertNotNull("one flag in the finger's lines:\n$said", flag)
        assertEquals(said, "7", BenchFlag.value(flag!!, "n"))
        assertEquals(said, BenchFlag.COVERED, BenchFlag.value(flag, "why"))
        assertEquals(said, "Fait", BenchFlag.value(flag, "covered_by"))
        assertEquals(said, "oui", BenchFlag.value(flag, "found"))
        assertTrue(said, BenchFlag.value(flag, "target").orEmpty().startsWith("target"))
        assertEquals(said, BenchFlag.bounds(r.left, r.top, r.right, r.bottom), BenchFlag.value(flag, "bounds"))
        assertEquals(said, "${r.center.x.toInt()},${r.center.y.toInt()}", BenchFlag.value(flag, "at"))
    }

    @Test
    fun aFingerOnAFreeTargetLeavesNoFlag() {
        compose.setContent { Box(Modifier.fillMaxSize()) { Button(onClick = {}, modifier = Modifier.testTag("target")) { Text("Valider") } } }
        compose.waitForIdle()
        val screen = ApplicationProvider.getApplicationContext<android.app.Application>().resources.displayMetrics
        val r = BenchWindows.drawnBounds(compose.onNodeWithTag("target", useUnmergedTree = true).fetchSemanticsNode(), screen)
        val f = ScreenText(compose, screen, reads = true, rtl = false).finger(7, r.center.x, r.center.y)
        assertTrue(f.lines.joinToString("\n"), !f.refused && BenchFlag.of(f.lines.joinToString("\n")).isEmpty())
    }

    @Test
    fun aFlagIsOneLineThatReadsBack() {
        val f = BenchFlag.line(
            12, "Touche", "strip_more_1 « Acid\n303 »", found = true, why = BenchFlag.COVERED,
            coveredBy = "Batterie \"1\"", bounds = BenchFlag.bounds(162.4f, 313f, 246f, 577.9f), at = 204.6f to 445f, screen = "a « b », c",
        )
        assertTrue(f, f.startsWith("BENCH-FLAG where=gesture:touche n=12 target="))
        assertTrue(f, '\n' !in f)
        assertEquals("strip_more_1 « Acid 303 »", BenchFlag.value(f, "target"))
        assertEquals("Batterie '1'", BenchFlag.value(f, "covered_by"))
        assertEquals("162,313,246,577", BenchFlag.value(f, "bounds"))
        assertEquals("204,445", BenchFlag.value(f, "at"))
        assertEquals("a « b », c", BenchFlag.value(f, "screen"))
        // Nothing to say: « - », and the keys the fiche names are always there.
        val bare = BenchFlag.line(3, "glisse", "glisse 1 2", found = false, why = BenchFlag.MALFORMED)
        assertEquals("BENCH-FLAG where=gesture:glisse n=3 target=\"glisse 1 2\" found=non covered_by=- bounds=- why=geste-mal-ecrit", bare)
        assertNull(BenchFlag.value(bare, "screen"))
        assertEquals(5, BenchFlag.REASONS.toSet().size)
        assertEquals("Fait", BenchFlag.coverName("« Fait » couvre « Valider »"))
        // A journal's « #n … » lines are not flags, and a flag is no « #n » line (the journal's readers skip it).
        assertEquals(listOf(bare), BenchFlag.of("#3 > glisse 1 2\n#3 BANC geste mal écrit : glisse 1 2\n$bare\n#4 > fin\n"))
    }
}
