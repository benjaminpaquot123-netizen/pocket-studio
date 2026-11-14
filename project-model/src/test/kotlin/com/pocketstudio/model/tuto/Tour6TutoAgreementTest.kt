package com.pocketstudio.model.tuto

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fresh review of lot 5 after lot 2's merge: the pass that made the tutorial say the screens' words changed the noun but
 * not what agrees with it. Italian « kick » (masculine) became « cassa » (feminine) after « il tuo », « questo », before
 * « accordato », « corto e secco », « da solo »; Arabic « Intro » (said as a masculine loan) became « المقدمة », a
 * feminine word, before a masculine verb (« المقدمة يشوّق »), and « Fill » became « الحشوة » the same way. And the grid's
 * « Remplir » (fill the cells) said « حشوة », the drum fill, on the button and in every tutorial step that quotes it.
 */
class Tour6TutoAgreementTest {
    private val areas = I18n.AREAS.filter { it.startsWith("tuto") }

    private fun tuto(lang: Lang): Map<String, String> = areas.flatMap { I18n.area(lang, it).entries }.associate { it.key to it.value }

    private fun report(what: String, wrong: List<String>) = assertTrue("$what (${wrong.size}):\n" + wrong.take(40).joinToString("\n"), wrong.isEmpty())

    @Test
    fun theItalianCassaIsFeminineEverywhere() {
        val quoted = Regex("“[^”]*”|«[^»]*»")
        val rules = listOf(
            Regex("""(?iU)\b(il|un|lo|del|al|dal|nel|sul|col|tuo|suo|mio|questo|quel|quello|secondo|primo|nuovo|nessun)\s+cassa\b"""),
            Regex("""(?iU)\bcassa\b(?:\s+[\w'’-]+){0,4}?\s+(accordato|corto|lungo|secco|saturato|preciso|anticipato|distorto|pieno|grosso|duro|rotondo|annegato)\b"""),
            Regex("""(?iU)\bcassa(?: \([^)]*\))?(?: d(?:el|ella|ello|ei) [^,:;.]+)?,? da solo\b"""),
            Regex("""(?iU)\buna cassa e l'altro\b"""),
            Regex("""(?iU)\bcassa \(la cassa\b"""),
            Regex("""(?iU)\bcassa\b[^.]*\bda sola: (corto|secco|preciso)\b"""),
        )
        val wrong = ArrayList<String>()
        tuto(Lang.IT).forEach { (_, v) ->
            val t = quoted.replace(v, "“”")
            rules.forEach { r -> r.find(t)?.let { wrong += "it: « ${it.value} » in « ${v.take(90)} »" } }
        }
        report("Italian « cassa » with a masculine word", wrong)
    }

    @Test
    fun theArabicIntroAndFillAgreeAsFeminineWords() {
        val after = Regex("""(المقدمة|الحشوة)(?: \d+)? (ي[ء-ْ]+)""")
        val before = Regex("""(سيكون|يفقد|يبدأ|ينتهي|يعلن) (المقدمة|الحشوة)""")
        val wrong = ArrayList<String>()
        tuto(Lang.AR).forEach { (_, v) ->
            (after.findAll(v) + before.findAll(v)).forEach { wrong += "ar: « ${it.value} » in « ${v.take(90)} »" }
        }
        report("Arabic feminine parts with a masculine verb", wrong)
    }

    @Test
    fun theArabicGridsFillIsNotTheDrumFill() {
        val ar = I18n.table(Lang.AR)
        val fill = ar.getValue("Remplir")
        val wrong = ArrayList<String>()
        if (fill == ar["Fill"]) wrong += "ar: « Remplir » (fill the cells) says « $fill », the drum fill"
        tuto(Lang.AR).forEach { (fr, v) ->
            if ("« Remplir" in fr && "«$fill" !in v) wrong += "ar: « Remplir » quoted without « $fill » in « ${v.take(90)} »"
        }
        report("Arabic « Remplir »", wrong)
    }
}
