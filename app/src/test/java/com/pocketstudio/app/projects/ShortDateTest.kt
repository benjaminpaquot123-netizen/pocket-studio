package com.pocketstudio.app.projects

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class ShortDateTest {
    @After
    fun french() {
        I18n.lang = Lang.FR
    }

    private val time = Calendar.getInstance().apply { clear(); set(2026, Calendar.OCTOBER, 21, 12, 0) }.timeInMillis

    @Test
    fun datesAreWrittenTheLanguagesWay() {
        assertEquals("21/10/2026", shortDate(time, Lang.FR))
        assertEquals("21/10/2026", shortDate(time, Lang.EN))  // British: day first
        assertEquals("21-10-2026", shortDate(time, Lang.NL))
        I18n.lang = Lang.NL
        assertEquals("op 21-10-2026", whenText(time, time + 3 * 24 * 3_600_000L))
    }
}
