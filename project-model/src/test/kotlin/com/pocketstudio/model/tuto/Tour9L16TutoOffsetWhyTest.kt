package com.pocketstudio.model.tuto

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 9, lot L16, F086 (seat t9-75, nl): uk_garage's « Décalage du coup » step had no « why » of its own, so the panel
 * showed the chapter's (the kick skipping beat 3). Every step that sets a hit's offset says why that offset, in every
 * language.
 */
class Tour9L16TutoOffsetWhyTest {
    @Test
    fun everyOffsetStepExplainsItsOwnSetting() {
        val wrong = mutableListOf<String>()
        for (style in TutoCourse.STYLES) for (s in TutoCourse.of(style)!!.chapters.flatMap { it.steps }.filter { it.check.startsWith("offset:") }) {
            val why = s.why
            if (why.isNullOrBlank()) { wrong += "$style/${s.id}: no why of its own"; continue }
            if (!why.contains("retard", ignoreCase = true) && !why.contains("traîne")) wrong += "$style/${s.id}: « $why » does not speak of the late hit"
            for (l in Lang.entries.filter { it != Lang.FR }) {
                if (I18n.AREAS.none { a -> I18n.area(l, a).containsKey(why) }) wrong += "$style/${s.id}: no ${l.code} text for « $why »"
            }
        }
        assertTrue("offset steps without their own why (${wrong.size}):\n" + wrong.take(40).joinToString("\n"), wrong.isEmpty())
    }
}
