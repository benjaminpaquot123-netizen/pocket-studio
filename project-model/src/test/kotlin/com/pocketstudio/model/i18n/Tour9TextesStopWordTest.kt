package com.pocketstudio.model.i18n

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 9, lot « textes » (finition t9-L5b-F084, seat t9-47, es): the same stop gesture had two words
 * (« Detener » on the stop button, « Parar » on a pad row). Every « Arrêter … » text uses the word of « Arrêter ».
 */
class Tour9TextesStopWordTest {
    /**
     * Languages whose « Arrêter » word stays the same inside a sentence. The others conjugate the verb
     * (ar, hi, ja, ko, pl, id, nl), so a plain search would be wrong there.
     */
    private val sameForm = listOf(Lang.EN, Lang.ES, Lang.PT_BR, Lang.DE, Lang.IT, Lang.TR, Lang.ZH_CN)

    @Test
    fun everyStopTextUsesTheStopWord() {
        val wrong = sameForm.flatMap { l ->
            val all = I18n.AREAS.flatMap { a -> I18n.area(l, a).map { Triple(a, it.key, it.value) } }
            val stops = all.filter { it.second == "Arrêter" }.map { it.third.lowercase(l.locale) }.toSet()
            all.filter { it.second.startsWith("Arrêter ") && stops.none { s -> it.third.lowercase(l.locale).contains(s) } }
                .map { "${l.code} ${it.first}: « ${it.second} » → « ${it.third} » (stop: $stops)" }
        }
        assertTrue("stop texts without the stop word (${wrong.size}):\n" + wrong.joinToString("\n"), wrong.isEmpty())
    }
}
