package com.pocketstudio.app.tuto

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Tour 9, « balayage », families 1 and 2 on the words alone: every text of every course (steps' words at both levels,
 * « Pourquoi ? », « Passer », chapters' titles and « Pourquoi ? ») in each of the 15 languages.
 *  - family 1: a text with no translation (the French is shown), a long text « translated » to its French, a name
 *    quoted in the French (a pad, a button) found neither translated nor as is in the translation, French words left
 *    in a translation;
 *  - family 2: a step's words that do not fit the open panel ([TutoReadability.fits]) at 200 % on a 360 dp and a
 *    320 dp phone (« Lire la suite » appears; counted per course and language in `text-long200.tsv`), and those that do not fit even at 100 % on 360 dp.
 * Runs only with `-Pbench.out=<dir>`; writes `<dir>/text.tsv`, never fails.
 */
class Tour9TutoSweepTextTest {
    private val quoted = Regex("""«\s*([^«»]{1,40}?)\s*»""")
    private val frenchEverywhere = Regex("""(?i)\b(touche|pointillé|à toi|ensuite|appui long|maintenant)\b""")
    private val frenchPair = Regex("""(?i)(?<![\p{L}'])(sur le|sur la|dans le|dans la|avec le|avec la|puis le|puis la|les cases|la case|le pad)(?![\p{L}'])""")
    private val romance = setOf(Lang.ES, Lang.IT, Lang.PT_BR)

    @Test
    fun sweep() {
        val dir = System.getProperty("ps.bench.out")?.takeIf { it.isNotBlank() }
        assumeTrue("run with -Pbench.out=<dir> (scripts/bench/tuto-sweep.sh)", dir != null)
        data class Text(val style: String, val level: TutoLevel?, val chapter: Int, val chId: String, val step: Int, val stepId: String, val field: String, val fr: String)
        val texts = LinkedHashMap<String, Text>()
        for (style in TutoCourse.STYLES) {
            val course = TutoCourse.of(style) ?: continue
            for (level in TutoLevel.entries) course.chaptersFor(level).forEachIndexed { ci, ch ->
                fun add(step: Int, stepId: String, field: String, fr: String?) {
                    if (!fr.isNullOrBlank()) texts.putIfAbsent("$style|$level|$field|$stepId|$fr", Text(style, level, ci, ch.id, step, stepId, field, fr))
                }
                add(0, "", "titre", ch.titleFor(level))
                add(0, "", "pourquoi-chapitre", ch.why)
                ch.steps.forEachIndexed { si, s ->
                    add(si + 1, s.id, "consigne", s.textFor(level))
                    add(si + 1, s.id, "pourquoi", s.why)
                    add(si + 1, s.id, "passer", s.skip)
                }
            }
        }
        val rows = ArrayList<String>()
        val long = sortedMapOf<String, Int>()
        fun row(sev: String, kind: String, t: Text, lang: Lang, what: String) {
            rows += listOf(
                sev, kind, t.style, if (t.level == TutoLevel.Pro) "Pro" else "Facile", "${t.chapter + 1}", t.chId,
                if (t.step == 0) "-" else "${t.step}", t.stepId, t.field, lang.code, what.replace('\t', ' ').replace('\n', ' '),
            ).joinToString("\t")
        }
        for (lang in Lang.entries) {
            val table = I18n.table(lang)
            for (t in texts.values) {
                val shown = if (lang == Lang.FR) t.fr else table[t.fr]?.takeIf { it.isNotEmpty() }
                if (lang != Lang.FR) {
                    if (shown == null) {
                        row("grave", "texte-en-francais", t, lang, "pas de traduction : l'écran montre le français « ${t.fr.take(120)} »")
                        continue
                    }
                    if (shown == t.fr && t.fr.length > 24) {
                        row("grave", "traduction-identique", t, lang, "la traduction est le texte français : « ${t.fr.take(120)} »")
                        continue
                    }
                    // A quoted name (a pad, a button), absent from the translation under its own name and its translation.
                    for (m in quoted.findAll(t.fr)) {
                        val name = m.groupValues[1].trim()
                        if (name.isEmpty() || name.any { it.isDigit() }) continue
                        // Only a name the app has its own key for (a button, a pad): the screen shows that one.
                        val its = table[name]?.takeIf { it.isNotEmpty() } ?: continue
                        val found = shown.contains(name, ignoreCase = true) || shown.contains(its, ignoreCase = true)
                        if (!found) row(
                            "finition", "nom-cite-absent", t, lang,
                            "« $name » (à l'écran : « $its ») n'apparaît pas dans : « ${shown.take(120)} »",
                        )
                    }
                    val left = frenchEverywhere.findAll(shown).map { it.value }.toMutableList()
                    // Two French words in a row that no other language of the app writes so.
                    if (lang !in romance) left += frenchPair.findAll(shown).map { it.value }
                    if (left.isNotEmpty()) row("grave", "mots-francais", t, lang, "mots français restés (${left.distinct().joinToString(", ")}) : « ${shown.take(120)} »")
                }
                // Family 2: the step's words in the open panel.
                if (t.field == "consigne" && shown != null) {
                    val p = TutoReadability.PanelState.OPEN
                    when {
                        !TutoReadability.fits(shown, p, lang, 1f, 360) ->
                            row("finition", "trop-long-100", t, lang, "consigne coupée (« Lire la suite ») même à 100 % sur 360 dp : ${shown.length} signes")
                        // Counted, not listed: at 200 % most words take « Lire la suite », as the panel is made to.
                        !TutoReadability.fits(shown, p, lang, 2f, 360) -> long.merge("${t.style}\t${lang.code}\t360", 1, Int::plus)
                        !TutoReadability.fits(shown, p, lang, 2f, 320) -> long.merge("${t.style}\t${lang.code}\t320", 1, Int::plus)
                    }
                }
            }
        }
        val out = File(dir!!).apply { mkdirs() }
        File(out, "text.tsv").writeText(
            "sev\tfamille\ttuto\tniveau\tchapitre\tchapitre_id\tetape\tetape_id\tchamp\tlangue\tce_que_voit_l_utilisateur\n" + rows.joinToString("\n") + "\n",
        )
        File(out, "text-long200.tsv").writeText("tuto\tlangue\tlargeur_dp\tconsignes_avec_lire_la_suite\n" + long.entries.joinToString("\n") { "${it.key}\t${it.value}" } + "\n")
        File(out, "text-meta.txt").writeText("texts=${texts.size} langs=${Lang.entries.size} rows=${rows.size}\n")
    }
}
