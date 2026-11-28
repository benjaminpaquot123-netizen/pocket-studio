package com.pocketstudio.app.export

import com.pocketstudio.model.TargetChoice
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.i18n.tr
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 9, F030 (Partager): in Arabic each signed number keeps its minus on its left, glued to its digits: minus and
 * digits sit in one left-to-right isolate (U+2066 … U+2069). Left to right, nothing changes.
 */
class ShareSignedRtlTest {
    @After
    fun back() {
        I18n.lang = Lang.FR
    }

    private val lri = '⁦'
    private val pdi = '⁩'

    /** Every « −digits » of [text] is inside an isolate that starts with its minus. */
    private fun allIsolated(text: String): Boolean =
        Regex("""−[0-9٠-٩]""").findAll(text).all { m -> m.range.first > 0 && text[m.range.first - 1] == lri && text.indexOf(pdi, m.range.first) > 0 }

    @Test
    fun arabicKeepsTheMinusWithItsDigits() {
        I18n.lang = Lang.AR
        val line = shareSigned(tr("Volume : %s, %s", "−14,4", shareLevelWords(-14.4, TargetChoice.STREAMING)))
        assertTrue("« $line »", line.contains("−14") && allIsolated(line))
        val details = shareSigned(tr("%s LUFS · crête %s dBTP", "−9,1", "−1,0"))
        assertTrue("« $details »", allIsolated(details))
        val club = shareSigned(tr("Volume · %s", "−9"))
        assertTrue("« $club »", allIsolated(club))
    }

    @Test
    fun leftToRightUnchanged() {
        I18n.lang = Lang.FR
        val line = tr("Volume : %s, %s", "−14,4", shareLevelWords(-14.4, TargetChoice.STREAMING))
        assertEquals(line, shareSigned(line))
    }
}
