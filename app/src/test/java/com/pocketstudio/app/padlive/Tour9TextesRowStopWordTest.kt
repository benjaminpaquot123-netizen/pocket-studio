package com.pocketstudio.app.padlive

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.i18n.tr
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 9, lot « textes », F084 (es): Pad live's row stop button read « Parar la fila 1 » while every other stop
 * button reads « Detener ». The row's stop says the same word as the stop button (PadLiveScreen).
 */
class Tour9TextesRowStopWordTest {
    @After
    fun back() {
        I18n.lang = Lang.FR
    }

    @Test
    fun theRowStopSaysTheStopButtonsWord() {
        val wrong = listOf(Lang.EN, Lang.ES, Lang.PT_BR, Lang.DE, Lang.IT, Lang.TR, Lang.ZH_CN).mapNotNull { l ->
            I18n.lang = l
            val stop = tr("Arrêter").lowercase()
            val row = tr("Arrêter la ligne %d", 1)
            "${l.code}: « $row » without « $stop »".takeIf { !row.lowercase().contains(stop) }
        }
        assertTrue("row stop buttons with another word (${wrong.size}):\n" + wrong.joinToString("\n"), wrong.isEmpty())
    }
}
