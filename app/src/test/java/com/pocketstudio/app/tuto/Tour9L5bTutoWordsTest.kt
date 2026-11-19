package com.pocketstudio.app.tuto

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 9, lot L5b: the tuto's words in each language name what the screen shows (a pad, a tab, a sheet, a part)
 * and read as a correct sentence. One test per finding (or small group of a same finding).
 */
class Tour9L5bTutoWordsTest {
    private val others = Lang.entries.filter { it != Lang.FR }

    /** Every French text a course shows: titles at both levels, « Pourquoi ? », steps' words at both levels, « Passer ». */
    private val tutoTexts: Set<String> by lazy {
        val out = LinkedHashSet<String>()
        for (style in TutoCourse.STYLES) {
            val course = TutoCourse.of(style) ?: continue
            for (level in TutoLevel.entries) course.chaptersFor(level).forEach { ch ->
                listOfNotNull(ch.titleFor(level), ch.why).forEach { if (it.isNotBlank()) out += it }
                ch.steps.forEach { s -> listOfNotNull(s.textFor(level), s.why, s.skip).forEach { if (it.isNotBlank()) out += it } }
            }
        }
        out
    }

    private fun shown(lang: Lang, fr: String): String = I18n.table(lang)[fr]?.takeIf { it.isNotEmpty() } ?: fr

    /** The tuto texts of [lang] whose French matches [frFilter] and whose translation matches [bad]. */
    private fun offenders(lang: Lang, bad: Regex, frFilter: (String) -> Boolean = { true }): List<String> =
        tutoTexts.filter(frFilter).map { shown(lang, it) }.filter { bad.containsMatchIn(it) }.distinct()

    private fun assertNoneLeft(what: String, found: Map<Lang, List<String>>) {
        val bad = found.filterValues { it.isNotEmpty() }
        assertTrue("$what:\n" + bad.entries.joinToString("\n") { (l, v) -> "${l.code}: ${v.size} : ${v.take(3)}" }, bad.isEmpty())
    }

    /** F068: the Roll step says what to do with « ×4 » (a verb), in every language, not only « …, and “×4” under “Roll” ». */
    @Test
    fun f068RollStepNamesTheTap() {
        val verb = mapOf(
            Lang.EN to "tap", Lang.DE to "tippe", Lang.ES to "toca", Lang.HI to "टैप", Lang.ID to "ketuk", Lang.IT to "tocca",
            Lang.NL to "tik", Lang.PL to "dotknij", Lang.PT_BR to "toque", Lang.TR to "dokun", Lang.AR to "المس",
            Lang.JA to "タップ", Lang.KO to "탭", Lang.ZH_CN to "点",
        )
        val keys = tutoTexts.filter { "sous « Roulement »" in it }
        assertTrue("the Roll steps are found", keys.size >= 3)
        assertNoneLeft("Roll step without the tap verb", others.associateWith { l ->
            val v = verb[l] ?: error("no verb for ${l.code}")
            keys.map { shown(l, it) }.filter { v !in it.substringAfterLast(',').substringAfterLast('،').substringAfterLast('，') }
        })
    }

    /** F073: in Hindi the « part created » message says the listed instruments are removed (« हटाए »), not just listed. */
    @Test
    fun f073PartCopyMessageSaysWhatIsRemoved() {
        val v = I18n.table(Lang.HI)["Partie « %1\$s » créée (copie de « %2\$s ») : sans %3\$s."].orEmpty()
        assertTrue("hi: $v", "हटाए" in v && v.contains("%1\$s") && v.contains("%2\$s") && v.contains("%3\$s"))
    }

    /** F075: a step that names the « Mix » tab writes it as the tab reads (Arabic said « مزج », the tab « المكس »). */
    @Test
    fun f075MixTabNamedAsTheTab() {
        val keys = tutoTexts.filter { "« Mix »" in it }
        assertTrue("the Mix steps are found", keys.isNotEmpty())
        assertNoneLeft("step naming Mix otherwise than the tab", others.associateWith { l ->
            val tab = I18n.table(l)["Mix"] ?: "Mix"
            keys.map { shown(l, it) }.filter { tab !in it }
        })
        val title = shown(Lang.AR, "Le mix")
        assertTrue("ar chapter « Le mix »: $title", I18n.table(Lang.AR)["Mix"].orEmpty() in title)
    }
}
