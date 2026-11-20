package com.pocketstudio.model.tuto

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 9, lot L16, F093 (seat t9-68, zh): a dotted cell said « 点一下 » (tap it) while the step's words ask for a long
 * press there (techno, ch.3 « clap_do » in Pro: the clap's ghost, « appui long sur la dernière case », « Doux »).
 */
class Tour9L16LongPressCellsTest {
    private val target = TutoTarget.of("techno")!!
    private val clap = TutoCourse.of("techno")!!.chapters.flatMap { it.steps }.first { it.id == "clap_do" }

    @Test
    fun theClapGhostIsALongPressInPro() {
        val dotted = TutoPlay.hint(clap, com.pocketstudio.model.Project(), target, 16, null, false)?.second.orEmpty()
        val long = TutoPlay.longPressCells(clap, target, 16, TutoLevel.Pro)
        assertTrue("Pro: the clap's last dotted cell asks a long press, got $long of $dotted", dotted.isNotEmpty() && long == setOf(dotted.max()))
    }

    @Test
    fun aTapIsEnoughWhereTheWordsSayTouch() {
        assertEquals("Facile says « touche »: every cell is a tap", emptySet<Int>(), TutoPlay.longPressCells(clap, target, 16, TutoLevel.Facile))
    }

    /** The new state reads in every language, and says « long press » in Chinese (« 长按 »), not « 点一下 ». */
    @Test
    fun theLongPressStateIsTranslated() {
        val key = "vide, en pointillé : appui long"
        val wrong = Lang.entries.filter { it != Lang.FR }.mapNotNull { l ->
            val t = I18n.AREAS.firstNotNullOfOrNull { I18n.area(l, it)[key] }
            "${l.code}: $t".takeIf { t.isNullOrBlank() || (l == Lang.ZH_CN && ("长按" !in t || "点一下" in t)) }
        }
        assertTrue("languages without the long-press state:\n" + wrong.joinToString("\n"), wrong.isEmpty())
    }
}
