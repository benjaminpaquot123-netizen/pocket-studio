package com.pocketstudio.app.tuto

import com.pocketstudio.app.tuto.TutoReadability.PanelState.GRID
import com.pocketstudio.app.tuto.TutoReadability.PanelState.OPEN
import com.pocketstudio.model.i18n.Lang
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 7, P6 (plan § 1.1, 1.3): the readability rule out of Compose. The budget reproduces the counts measured on the
 * phone; [TutoReadability.layout] keeps a line of the instruction in every case, shows « Lire la suite » when it cuts,
 * folds the examples, then the kit card, then « Pourquoi ? » before cutting, and never cuts a text that fits.
 */
class TutoReadabilityRuleTest {
    private val scales = listOf(1f, 1.3f, 2f)

    @Test fun linesPerStateAndScriptAreThePlans() {
        val want = mapOf(
            Lang.FR to listOf(8 to 3, 6 to 2, 4 to 1),
            Lang.JA to listOf(7 to 3, 5 to 2, 3 to 1),
            Lang.ZH_CN to listOf(7 to 3, 5 to 2, 3 to 1),
            Lang.KO to listOf(7 to 3, 5 to 2, 3 to 1),
            Lang.AR to listOf(7 to 3, 5 to 2, 3 to 1),
            Lang.HI to listOf(7 to 2, 5 to 2, 3 to 1),
        )
        for ((lang, rows) in want) for ((i, s) in scales.withIndex()) {
            assertEquals("$lang ${s * 100}% OPEN", rows[i].first, TutoReadability.budget(OPEN, lang, s, 360).maxLines)
            assertEquals("$lang ${s * 100}% GRID", rows[i].second, TutoReadability.budget(GRID, lang, s, 360).maxLines)
        }
    }

    @Test fun charactersPerLineAreTheMeasuredOnes() {
        // 360 dp then 320 dp, at 100 / 130 / 200 % (plan § 1.3).
        val want = mapOf(
            Lang.FR to listOf(45 to 40, 35 to 30, 22 to 20),
            Lang.DE to listOf(44 to 38, 34 to 29, 22 to 19),
            Lang.JA to listOf(21 to 19, 16 to 14, 10 to 9),
            Lang.ZH_CN to listOf(23 to 20, 18 to 15, 11 to 10),
            Lang.KO to listOf(29 to 26, 23 to 20, 14 to 13),
        )
        for ((lang, rows) in want) for ((i, s) in scales.withIndex()) {
            assertEquals("$lang ${s * 100}% 360", rows[i].first, TutoReadability.budget(OPEN, lang, s, 360).charsPerLine)
            assertEquals("$lang ${s * 100}% 320", rows[i].second, TutoReadability.budget(OPEN, lang, s, 320).charsPerLine)
        }
    }

    @Test fun everyLanguageKeepsALineEvenInTheSmallestState() {
        for (lang in Lang.entries) for (w in listOf(280, 320, 360)) {
            val b = TutoReadability.budget(GRID, lang, 2f, w)
            assertTrue("$lang $w", b.maxLines >= 1 && b.charsPerLine >= 1)
        }
    }

    @Test fun linesWrapWordByWordAndCharacterByCharacterInJapaneseAndChinese() {
        assertEquals(1, TutoReadability.lines("Touche la case 1.", 45, Lang.FR))
        assertEquals(2, TutoReadability.lines("aaaa bbbb", 6, Lang.FR))
        assertEquals(2, TutoReadability.lines("un\ndeux", 45, Lang.FR))
        assertEquals(3, TutoReadability.lines("x".repeat(25), 10, Lang.FR))
        assertEquals(2, TutoReadability.lines("あ".repeat(22), 21, Lang.JA))
        assertEquals(1, TutoReadability.lines("あ".repeat(21), 21, Lang.JA))
        assertTrue(TutoReadability.fits("Touche la case 1.", GRID, Lang.FR, 1f, 360))
        assertFalse(TutoReadability.fits("mot ".repeat(60), GRID, Lang.FR, 1f, 360))
    }

    @Test fun aLineOfTheInstructionIsAlwaysInSightAndCutMeansReadOn() {
        for (room in listOf(10, 24, 40, 84, 120, 240, 290)) for (lh in listOf(24f, 26.4f, 48f, 57.6f))
            for (lines in 1..20) for (kit in listOf(0, 60)) for (why in listOf(0, 36)) for (ex in listOf(0, 80)) {
                val l = TutoReadability.layout(room, lh, lines, kit, why, ex)
                val case = "room $room lh $lh lines $lines kit $kit why $why ex $ex"
                assertTrue("(a) $case", l.wordsLines >= 1)
                if (l.cut) assertTrue("(b) $case", l.showMore)
                if (l.cut) assertTrue("cut shows fewer lines $case", l.wordsLines < lines)
                if (lines * lh <= room) assertFalse("(d) $case", l.cut)
            }
    }

    @Test fun theBlocksFoldInOrderBeforeTheWordsAreCut() {
        val lh = 24f
        // Everything fits: all kept.
        assertEquals(TutoReadability.PanelLayout(3, false, false, kitCard = true, why = true, examples = true),
            TutoReadability.layout(240, lh, 3, 60, 36, 60))
        // The examples go first.
        TutoReadability.layout(240, lh, 4, 60, 36, 60).let { assertFalse(it.examples); assertTrue(it.kitCard); assertTrue(it.why); assertFalse(it.cut) }
        // Then the kit card.
        TutoReadability.layout(240, lh, 7, 60, 36, 60).let { assertFalse(it.examples); assertFalse(it.kitCard); assertTrue(it.why); assertFalse(it.cut) }
        // Then « Pourquoi ? », still not cutting the words.
        TutoReadability.layout(240, lh, 9, 60, 36, 60).let { assertFalse(it.why); assertFalse(it.kitCard); assertFalse(it.cut); assertEquals(9, it.wordsLines) }
        // Only then the words are cut, with « Lire la suite » under them; folding the kit card and « Pourquoi ? » spared
        // nothing, so they are back (bloc 0b), the examples stay folded.
        TutoReadability.layout(240, lh, 12, 60, 36, 60).let {
            assertTrue(it.cut); assertTrue(it.showMore); assertEquals(4, it.wordsLines)
            assertTrue(it.why); assertTrue(it.kitCard); assertFalse(it.examples)
        }
        // A block is never folded while a later one in the order is kept.
        for (lines in 1..12) TutoReadability.layout(240, lh, lines, 60, 36, 60).let {
            if (it.examples) assertTrue(it.kitCard && it.why)
            if (it.kitCard) assertTrue(it.why)
        }
    }

    /**
     * Bloc 0b: a block folds only when folding it spares the words a cut. Cut anyway, the kit card the step needs and
     * « Pourquoi ? » stay (folding them saved nothing: the kit card hidden, « Pourquoi ? » out of reach in Arabic at 130 %).
     */
    @Test fun cutAnywayTheKitAndWhyStay() {
        // Arabic at 130 %: a line of 24 × 1.3 × 1.1 dp; twelve lines never fit in 240 dp.
        TutoReadability.layout(240, 34.3f, 12, 48, 36, 40).let {
            assertTrue(it.cut); assertTrue(it.showMore)
            assertTrue("the kit card stays", it.kitCard)
            assertTrue("« Pourquoi ? » stays", it.why)
            assertFalse("the examples fold first", it.examples)
            assertTrue("a line in sight above them", it.wordsLines >= 1 && it.wordsLines * 34.3f + 48 + 36 + 32 <= 240)
        }
    }
}
