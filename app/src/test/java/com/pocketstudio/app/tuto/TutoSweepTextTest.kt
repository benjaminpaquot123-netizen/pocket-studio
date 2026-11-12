package com.pocketstudio.app.tuto

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Round 6, « tuto-balayage »: every text a tutorial shows, read (not drawn) in the 15 languages, Facile and Pro.
 *
 * The texts: of each course (37 styles), each chapter's title (its Pro title in Pro) and « Pourquoi ? », each
 * step's words (its Pro words in Pro), its « Pourquoi ? » and what « Suivant » says the song misses; and the
 * panel's own words (every `tr("…")` literal of app/src/main/java/com/pocketstudio/app/tuto/).
 * In each language but French, a text is a finding when:
 * - MANQUANT: it has no translation (the French shows);
 * - FRANCAIS: its translation is the French itself;
 * - ANGLAIS: its translation is the English one (another language than English);
 * - RESTE_FR: its translation still holds French words (étape, touche, maintenant…) outside quotes;
 * - ECRITURE: in Hindi, Arabic, Japanese, Korean or Chinese, it has none of that language's letters;
 * - TROUS: its %s / %d differ from the French.
 * The app is not changed by this lot: findings are listed in docs/audits/ultra/sieges/tour-6/tuto/connus-textes.tsv;
 * one not listed fails, and so does a listed one no longer found. All of them: app/build/tuto-sweep/texts.tsv.
 */
class TutoSweepTextTest {
    companion object {
        val KNOWN_FILE = File("../docs/audits/ultra/sieges/tour-6/tuto/connus-textes.tsv")

        private val FRENCH = Regex("(?<![\\p{L}’'])(étape|touche|maintenant|ensuite|écoute|morceau|c'est|c’est|piste|mesures?|puis|avec|chaque|aussi|toujours|joue|ajoute|À toi)(?![\\p{L}])", RegexOption.IGNORE_CASE)
        private val QUOTED = Regex("[«“„「『\"‘][^»”“」』\"’]*[»”“」』\"’]")
        private val SCRIPTS = mapOf(
            Lang.HI to Regex("[\\u0900-\\u097F]"), Lang.AR to Regex("[\\u0600-\\u06FF]"),
            Lang.JA to Regex("[\\u3040-\\u30FF\\u4E00-\\u9FFF]"), Lang.KO to Regex("[\\uAC00-\\uD7AF]"), Lang.ZH_CN to Regex("[\\u4E00-\\u9FFF]"),
        )
        private val HOLES = Regex("""%(\d+\$)?[-#+ 0,(]*\d*(\.\d+)?[sdfxc]""")
        private fun holes(s: String) = HOLES.findAll(s).map { it.value }.sorted().toList()

        /** (where, French text) of every course text, by level: « style level chapter/step field ». */
        fun courseTexts(): List<Pair<String, String>> = TutoCourse.STYLES.flatMap { style ->
            val course = TutoCourse.of(style)!!
            TutoLevel.entries.flatMap { level ->
                course.chaptersFor(level).flatMap { ch ->
                    listOf("$style ${level.name} ${ch.id} titre" to ch.title, "$style ${level.name} ${ch.id} pourquoi" to ch.why) +
                        ch.steps.flatMap { s ->
                            listOf(
                                "$style ${level.name} ${s.id} texte" to s.textFor(level),
                                "$style ${level.name} ${s.id} pourquoi" to s.why.orEmpty(),
                                "$style ${level.name} ${s.id} manque" to s.skip,
                            )
                        }
                }
            }
        }.filter { it.second.isNotBlank() }

        /** The panel's own words: the `tr("…")` literals of the tuto's sources. */
        fun panelTexts(): List<Pair<String, String>> {
            val call = Regex("""(?<![A-Za-z0-9_.])tr\(\s*"((?:[^"\\]|\\.)*)"""")
            return File("src/main/java/com/pocketstudio/app/tuto").listFiles { f -> f.extension == "kt" }.orEmpty().sortedBy { it.name }.flatMap { f ->
                call.findAll(f.readText()).map { "panneau ${f.name}" to it.groupValues[1].replace("\\\"", "\"").replace("\\$", "$").replace("\\n", "\n") }.toList()
            }.distinctBy { it.second }
        }
    }

    @Test
    fun everyTutoTextIsTranslated() {
        val texts = (courseTexts() + panelTexts())
        assertTrue("no text read", texts.size > 1000)
        val en = I18n.table(Lang.EN)
        val found = LinkedHashSet<String>()
        for (lang in Lang.entries - Lang.FR) {
            val table = I18n.table(lang)
            // One finding per (language, French text): the first place it is shown names it.
            val seen = HashSet<String>()
            for ((where, fr) in texts) {
                if (!seen.add(fr)) continue
                val t = table[fr]
                val kind = when {
                    t.isNullOrBlank() -> "MANQUANT"
                    t == fr && fr.any { it.isLetter() } -> "FRANCAIS"
                    lang != Lang.EN && t == en[fr] && t != fr && t.count { it.isLetter() } > 3 -> "ANGLAIS"
                    holes(t) != holes(fr) -> "TROUS"
                    FRENCH.containsMatchIn(QUOTED.replace(t, "")) -> "RESTE_FR"
                    SCRIPTS[lang]?.let { r -> !r.containsMatchIn(t) && t.count { it.isLetter() } >= 3 } == true -> "ECRITURE"
                    else -> null
                } ?: continue
                found += listOf(lang.code, kind, where, fr.replace('\t', ' ').replace('\n', ' '), (t ?: "").replace('\t', ' ').replace('\n', ' ')).joinToString("\t")
            }
        }
        File("build/tuto-sweep").mkdirs()
        File("build/tuto-sweep/texts.tsv").writeText(found.joinToString("\n", postfix = "\n"))
        File("build/tuto-sweep/texts.count").writeText("${texts.size} places, ${texts.map { it.second }.distinct().size} distinct French texts, ${Lang.entries.size - 1} languages\n")
        // Compared on language, kind and French text (the place and the translation may move).
        fun key(line: String) = line.split('\t').let { "${it[0]}\t${it[1]}\t${it[3]}" }
        val keys = found.map(::key).toSet()
        val known = if (KNOWN_FILE.exists()) KNOWN_FILE.readLines().filter { it.isNotBlank() && !it.startsWith("#") }.map(::key).toSet() else emptySet()
        val fresh = keys - known
        val gone = known - keys
        assertTrue(
            "new findings (${fresh.size}):\n" + fresh.take(40).joinToString("\n") +
                "\nlisted but no longer found (${gone.size}, take them out of ${KNOWN_FILE.path}):\n" + gone.take(40).joinToString("\n"),
            fresh.isEmpty() && gone.isEmpty(),
        )
    }
}
