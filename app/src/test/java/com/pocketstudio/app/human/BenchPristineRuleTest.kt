package com.pocketstudio.app.human

import com.pocketstudio.app.tuto.TutoTestHooks
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * The leak detector itself (PLAN-RESTE 4.5, review of banc45): a leak before a bench test is a red test naming the test before
 * (or a test without the rule run in between); a leak before a real seat (the rule a seat class carries, its `drive` test,
 * its folder under `-Phuman.dir`) is put back and written in the seat's journal, the seat runs. Pristine: nothing said.
 */
@RunWith(RobolectricTestRunner::class)
class BenchPristineRuleTest {
    @get:Rule
    val pristine = BenchPristineRule()

    /** The rule around a statement that notes it ran, as JUnit applies it to [method] of [test]. */
    private fun run(rule: BenchPristineRule, test: Class<*> = BenchPristineRuleTest::class.java, method: String = "drive"): Boolean {
        var ran = false
        rule.apply(object : Statement() { override fun evaluate() { ran = true } }, Description.createTestDescription(test, method)).evaluate()
        return ran
    }

    /** What a seat before could leave: the language of its phone, the tutorial's kit offer refused. */
    private fun leak() {
        I18n.lang = Lang.EN
        TutoTestHooks.kitOffer = false
    }

    /**
     * A lot's folder with seat a's start file, `ps.human.dir` on it for the block's time only: left set, the `drive` tests
     * after this one in the fork would run as seats.
     */
    private fun <T> onTheBench(block: (File) -> T): T {
        val lot = kotlin.io.path.createTempDirectory("lot").toFile()
        File(lot, "a").mkdirs()
        File(lot, "a/start.txt").writeText("lang=fr\n")
        val before = System.getProperty("ps.human.dir")
        System.setProperty("ps.human.dir", lot.path)
        try {
            return block(lot)
        } finally {
            if (before == null) System.clearProperty("ps.human.dir") else System.setProperty("ps.human.dir", before)
            lot.deleteRecursively()
        }
    }

    @Test
    fun aLeakBeforeASeatIsPutBackAndWrittenInItsJournalTheSeatRuns() = onTheBench { lot ->
        leak()
        val ran = run(HumanTesterA().pristine, HumanTesterA::class.java)
        assertTrue("the seat runs", ran)
        val line = File(lot, "a/journal.txt").readText()
        assertTrue(line, line.startsWith("#0 BANC : état hérité remis (laissé par le test d'avant, ") && line.endsWith("\n") && line.count { it == '\n' } == 1)
        assertTrue(line, "ou par un test sans la règle" in line && "I18n.lang = EN" in line && "TutoTestHooks.kitOffer" in line)
        assertEquals(Lang.FR, I18n.lang)
        assertTrue(TutoTestHooks.kitOffer)
        assertTrue("pristine again: ${BenchPristine.leaks()}", BenchPristine.leaks().isEmpty())
    }

    @Test
    fun aLeakBeforeABenchTestIsRedNamingTheTestBeforeOrOneWithoutTheRule() {
        leak()
        val e = runCatching { run(BenchPristineRule()) }.exceptionOrNull()
        assertTrue("an AssertionError: $e", e is AssertionError)
        val m = e!!.message.orEmpty()
        assertTrue(m, "I18n.lang = EN" in m && "TutoTestHooks.kitOffer" in m && "left by the test before (" in m && "or by a test without the rule run in between" in m)
        // Put back all the same: one red test, not the rest of the run.
        assertEquals(Lang.FR, I18n.lang)
        assertTrue(TutoTestHooks.kitOffer)
    }

    /** Only a seat's `drive` with its folder is spared: another test of a HumanTester class, or a seat without a start file, is red. */
    @Test
    fun onTheBenchOnlyASeatsDriveIsSpared() = onTheBench { lot ->
        leak()
        val other = runCatching { run(HumanTesterA().pristine, HumanTesterA::class.java, "somethingElse") }.exceptionOrNull()
        assertTrue("another test than drive is red: $other", other is AssertionError)
        leak()
        val noSeat = runCatching { run(HumanTesterB().pristine, HumanTesterB::class.java) }.exceptionOrNull()
        assertTrue("a seat without its start file is red: $noSeat", noSeat is AssertionError)
        assertFalse("nothing written for them", File(lot, "a/journal.txt").exists() || File(lot, "b").exists())
    }

    @Test
    fun aPristineSeatWritesNothing() = onTheBench { lot ->
        assertTrue(run(HumanTesterA().pristine, HumanTesterA::class.java))
        assertFalse("no journal line for a pristine seat", File(lot, "a/journal.txt").exists())
    }
}
