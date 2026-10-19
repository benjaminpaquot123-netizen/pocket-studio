package com.pocketstudio.app

import com.pocketstudio.app.report.AppLog
import com.pocketstudio.app.report.LogTail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLogTest {
    @Test
    fun keepsTheLastLinesAndRedacts() {
        AppLog.clear()
        repeat(AppLog.CAPACITY + 5) { AppLog.add("ligne $it") }
        AppLog.add("contact benjamin@example.com")
        val all = AppLog.dump(maxLines = 1000).lines()
        assertEquals(AppLog.CAPACITY, all.size)
        assertFalse(all.any { it.endsWith(" ligne 5") })
        assertTrue(all.last().endsWith("contact <email>"))
    }

    @Test
    fun reportJournalIsReadable() {
        AppLog.clear()
        AppLog.add("Lecture (120 BPM)")
        val journal = LogTail.read()
        assertTrue(journal.contains("Lecture (120 BPM)"))
        assertFalse(journal.contains("(HKS)"))
    }
}
