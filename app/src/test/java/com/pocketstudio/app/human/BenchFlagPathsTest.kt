package com.pocketstudio.app.human

import com.pocketstudio.app.library.IndexState
import com.pocketstudio.app.library.PacksUi
import com.pocketstudio.app.tuto.TutoTestHooks
import com.pocketstudio.model.PackEntry
import com.pocketstudio.model.PackIndex
import com.pocketstudio.model.tuto.TutoTarget
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
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
 * Tour 9, banc A (drapeaux): each way a seat fails writes its « BENCH-FLAG » (where, want, saw, waited, screen) and the
 * seat ends on one « BENCH-VERDICT », just before « FIN ». Each failure is provoked here and its lines read back.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class BenchFlagPathsTest : HumanTester("drapeaux") {
    @Before
    fun notOnTheBench() = assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())

    @After
    fun noHook() {
        beforeGesture = null
        TutoTestHooks.packs = null
    }

    private fun seat(o: SeatOptions = SeatOptions(), vararg cmds: String): List<String> {
        val dir = kotlin.io.path.createTempDirectory("siege").toFile()
        cmds.forEachIndexed { i, c -> File(dir, "cmd-${i + 1}.txt").writeText(c) }
        seatLoop(dir, o, File(dir, "journal.txt"))
        return File(dir, "journal.txt").readLines()
    }

    private fun flags(lines: List<String>, where: String) = lines.mapNotNull { BenchFlags.parse(it) }.filter { it["kind"] == BenchFlags.FLAG && it["where"] == where }

    /** One verdict, on the line before « FIN », with [result] and [where]; a failed one has its flag with every field. */
    private fun assertEnds(lines: List<String>, result: String, where: String): Map<String, String> {
        val said = lines.joinToString("\n")
        val verdicts = lines.mapNotNull { BenchFlags.parse(it) }.filter { it["kind"] == BenchFlags.VERDICT }
        assertEquals("one verdict\n$said", 1, verdicts.size)
        assertTrue("the verdict just before « FIN », the journal's last line\n$said", lines.last().contains(" FIN : ") && BenchFlags.VERDICT in lines[lines.size - 2])
        assertEquals(said, result to where, verdicts[0]["result"] to verdicts[0]["where"])
        if (result == "ok") {
            assertTrue("no flag of an end that is not a failure\n$said", flags(lines, where).isEmpty())
            return verdicts[0]
        }
        val flag = flags(lines, where).lastOrNull()
        assertTrue("the flag of « $where »\n$said", flag != null)
        listOf("want", "saw", "waited", "screen").forEach { k -> assertTrue("« $k » in the flag\n$said", !flag!![k].isNullOrEmpty()) }
        assertTrue("the screen's route, read\n$said", flag!!["screen"] != "?" && flag["screen"] != "-")
        return flag
    }

    @Test
    fun eachEndOfTheSeatLoopSaysItsVerdictAndEachFailureItsFlag() {
        begin()
        assertEnds(seat(SeatOptions(), "attends 0.1", "fin"), "ok", "seat.fin")

        // No gesture comes: the seat gives up, and says what it waited for.
        val gaveUp = assertEnds(seat(SeatOptions(timeoutMinutes = 0)), "fail", "seat.abandon")
        assertEquals("cmd-1.txt", gaveUp["want"])

        // A tag no screen has: said, and the seat goes on to its end.
        val noTag = seat(SeatOptions(), "touche no_such_tag_on_any_screen", "fin")
        val missing = flags(noTag, "seat.etiquette")
        assertTrue(noTag.joinToString("\n"), missing.size == 1 && missing[0]["saw"] == "no_such_tag_on_any_screen absent" && missing[0]["screen"] != "?")

        // The same screen five times.
        val stuck = assertEnds(seat(SeatOptions(), *Array(8) { "ecris a" }, "fin"), "fail", "seat.rien-ne-change")
        assertEquals("5 écrans identiques", stuck["saw"])

        // An oracle the screen never holds (a replay test, as scripts/bench/rejeu-defauts.sh writes it).
        val origin = kotlin.io.path.createTempDirectory("origine").toFile()
        listOf("attends 0.1", "attends 0.1", "fin").forEachIndexed { i, c -> File(origin, "cmd-${i + 1}.txt").writeText(c) }
        seatLoop(origin, SeatOptions(), File(origin, "journal.txt"))
        val replayDir = kotlin.io.path.createTempDirectory("rejeu").toFile()
        listOf("attends 0.1", "attends 0.1").forEachIndexed { i, c -> File(replayDir, "cmd-${i + 1}.txt").writeText(c) }
        val replay = kotlin.io.path.createTempDirectory("siege").toFile()
        File(replay, "start.txt").writeText("rejeu=${replayDir.path}\nrejeu.origine=${origin.path}\nrejeu.n=2\nrejeu.attendu=jamais écrit sur aucun écran\n")
        seatLoop(replay, SeatOptions.read(File(replay, "start.txt")).first, File(replay, "journal.txt"))
        val oracle = assertEnds(File(replay, "journal.txt").readLines(), "fail", "seat.oracle")
        assertEquals("« jamais écrit sur aucun écran » à l'écran", oracle["want"])
        assertTrue(oracle.toString(), oracle["saw"]!!.startsWith("oracle non tenu"))

        // The bench's own exceptions: one flag each, with the frame it came from; four of them end the seat.
        beforeGesture = { cmd -> if (cmd.startsWith("attends")) error("the bench's own helper failed") }
        val bench = seat(SeatOptions(), "attends 0.1", "attends 0.1", "attends 0.1", "attends 0.1", "fin")
        val each = flags(bench, "seat.banc-exception")
        assertTrue(bench.joinToString("\n"), each.size == 4 && each.all { "the bench's own helper failed" in it["saw"]!! && "BenchFlagPathsTest" in it["saw"]!! })
        assertEnds(bench, "fail", "seat.banc")

        // The app's own code throwing: the crash, with its exception and its frame.
        beforeGesture = { cmd -> if (cmd.startsWith("attends")) com.pocketstudio.app.rhythm.ExportFormat.valueOf("NoSuchFormat") }
        val crash = assertEnds(seat(SeatOptions(), "attends 0.1", "fin"), "fail", "seat.crash")
        assertTrue(crash.toString(), "IllegalArgumentException" in crash["saw"]!! && "ExportFormat" in crash["saw"]!!)
    }

    /** An amenée's journal after it refused: its flag at [where], then its verdict, the journal's two last lines. */
    private fun assertRefused(journal: File, failed: Throwable?, where: String): Map<String, String> {
        val lines = journal.readLines()
        val said = "${failed?.message}\n" + lines.joinToString("\n")
        assertTrue("the amenée refused\n$said", failed != null)
        val flag = BenchFlags.parse(lines[lines.size - 2])
        val verdict = BenchFlags.parse(lines.last())
        assertTrue("flag then verdict, last\n$said", flag?.get("kind") == BenchFlags.FLAG && verdict?.get("kind") == BenchFlags.VERDICT)
        assertEquals(said, where, flag!!["where"])
        assertEquals(said, "fail" to where, verdict!!["result"] to verdict["where"])
        assertEquals("the verdict's reason is the refusal's\n$said", BenchFlags.value(failed!!.message).removeSurrounding("\""), verdict["why"])
        assertTrue("the screen's route, read\n$said", flag["screen"] != "?" && flag["screen"] != "-")
        return flag
    }

    /**
     * The refusal of tour 9 itself (seats t9-16 and t9-38), with the second touch switched off: the kit's card comes
     * under the finger, the touch goes beside « Chapitres », the list never comes. The journal says which state was
     * waited for, after which touch, where that button is now, for how long and on which screen.
     */
    @Test
    fun anAmeneeWhoseStateNeverComesSaysWhatItWaitedForAndAfterWhichTouch() {
        val packs = MutableStateFlow(PacksUi())
        TutoTestHooks.packs = packs
        begin(SeatOptions.parse(mapOf("lang" to "es", "device" to "phone", "font" to "130", "mode" to "simple", "amenee" to "tuto:deep_house:7")).first)
        val journal = File.createTempFile("amenee", ".txt").apply { deleteOnExit() }
        ameneeCapMs = 1_500
        ameneeRetaps = 0
        var came = false
        beforeGesture = { cmd ->
            if (!came && cmd.startsWith("touche") && "construits" in journal.readText()) {
                came = true
                val s = TutoTarget.of("deep_house")!!.style
                val ids = (s.lines.filter { it.voice == null }.mapNotNull { s.kit[it.role] } + s.id).distinct()
                packs.value = PacksUi(IndexState.Ready(PackIndex(ids.map { PackEntry(it, "Pack $it", "$it.zip", "a".repeat(64), 1_500_000, 12) })))
            }
        }
        val failed = runCatching { amenee("tuto:deep_house:7", journal) }.exceptionOrNull()
        val flag = assertRefused(journal, failed, "amenee.wait")
        assertEquals("les chapitres", flag["want"])
        assertTrue(flag.toString(), flag["saw"]!!.startsWith("après touche tuto_chapters@") && flag["waited"]!!.toLong() >= 1_500 && "tuto_panel" in flag["screen"]!!)
    }

    @Test
    fun anAmeneeWhoseButtonIsNotThereSaysWhichOne() {
        begin()
        val journal = File.createTempFile("amenee", ".txt").apply { deleteOnExit() }
        amenee(AMENEE_SONG, journal)
        assertTrue(journal.readText(), journal.readLines().last().startsWith("#0 AMENEE prête"))
        // In the project, the welcome's doors are gone: the Remix's amenée has no button to touch.
        val failed = runCatching { amenee(AMENEE_REMIX, journal) }.exceptionOrNull()
        val flag = assertRefused(journal, failed, "amenee.tag")
        assertEquals("welcome_remix absent launch_remix absent", flag["saw"])
    }
}
