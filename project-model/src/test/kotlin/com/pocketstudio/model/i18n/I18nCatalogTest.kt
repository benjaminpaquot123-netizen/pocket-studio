package com.pocketstudio.model.i18n

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Every text the code passes to [tr] has an English and a Dutch translation with the same
 * placeholders, and no text is built with a Kotlin template inside [tr] (its key would never match).
 */
class I18nCatalogTest {
    @After
    fun french() {
        I18n.lang = Lang.FR
    }

    private val roots = listOf("src/main", "../app/src/main", "../ui-components/src/main").map(::File).filter { it.isDirectory }
    private val call = Regex("""(?<![A-Za-z0-9_.])tr\(\s*"((?:[^"\\]|\\.)*)"""")

    /** The French keys used by the code, with where they are. */
    private fun keys(): Map<String, String> {
        val found = linkedMapOf<String, String>()
        roots.forEach { root ->
            root.walkTopDown().filter { it.extension == "kt" }.forEach { f ->
                f.readLines().forEachIndexed { i, line ->
                    call.findAll(line).forEach { m ->
                        val raw = m.groupValues[1]
                        // An unescaped $ in the source is a Kotlin template (%1\$s is a placeholder, not one).
                        val where = "${f.name}:${i + 1}" + if (Regex("""(?<!\\)\$[A-Za-z{]""").containsMatchIn(raw)) " TEMPLATE" else ""
                        found.putIfAbsent(unescape(raw), where)
                    }
                }
            }
        }
        return found
    }

    private fun unescape(s: String): String = buildString {
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (val n = s[i + 1]) {
                    'n' -> append('\n')
                    't' -> append('\t')
                    else -> append(n)
                }
                i += 2
            } else {
                append(c); i++
            }
        }
    }

    private val placeholder = Regex("""%(\d+\$)?[-#+ 0,(]*\d*(\.\d+)?[sdfxc%]""")
    private fun placeholders(s: String) = placeholder.findAll(s).map { it.value }.filter { it != "%%" }.sorted().toList()

    @Test
    fun everyTextHasItsTranslations() {
        val keys = keys()
        assertTrue("no tr() call found", keys.isNotEmpty())
        val problems = mutableListOf<String>()
        keys.forEach { (key, where) ->
            if (where.endsWith(" TEMPLATE")) problems += "$where: template inside tr(\"$key\"): use %s"
        }
        listOf(Lang.EN, Lang.NL).forEach { lang ->
            val table = I18n.table(lang)
            keys.forEach { (key, where) ->
                val t = table[key]
                when {
                    t.isNullOrBlank() -> problems += "$where: no ${lang.code} for \"$key\""
                    placeholders(t) != placeholders(key) -> problems += "$where: ${lang.code} placeholders differ for \"$key\" -> \"$t\""
                }
            }
        }
        // The whole list, for whoever translates an area: build/i18n-problems.txt.
        File("build").mkdirs()
        File("build/i18n-problems.txt").writeText(problems.joinToString("\n"))
        assertTrue("${problems.size} problems (all in project-model/build/i18n-problems.txt):\n" + problems.take(80).joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun theLanguageIsFollowedAndFrenchIsTheFallback() {
        assertEquals(Lang.FR, Lang.of(null))
        assertEquals(Lang.NL, Lang.of("nl"))
        I18n.lang = Lang.FR
        assertEquals("Texte inconnu", tr("Texte inconnu"))
        I18n.lang = Lang.EN
        assertEquals("Texte inconnu", tr("Texte inconnu"))
        assertEquals("Zz 3 (aucune traduction)", tr("Zz %d (aucune traduction)", 3))
    }

    @Test
    fun aFrenchTextHasOneTranslationAcrossAreas() {
        // The areas are merged: the same key with two meanings would show the wrong one somewhere.
        listOf(Lang.EN, Lang.NL).forEach { lang ->
            val seen = mutableMapOf<String, MutableSet<String>>()
            I18n.AREAS.forEach { area -> I18n.area(lang, area).forEach { (k, v) -> seen.getOrPut(k) { mutableSetOf() } += v } }
            val clashes = seen.filterValues { it.size > 1 }
            assertTrue("${lang.code}: ${clashes.size} keys translated differently: $clashes", clashes.isEmpty())
        }
    }

    @Test
    fun aShownTextLeadsBackToItsFrenchKey() {
        I18n.lang = Lang.EN
        val table = I18n.table(Lang.EN)
        val (fr, en) = table.entries.first { e -> table.values.count { it == e.value } == 1 }
        assertEquals(fr, I18n.frenchOf(en))
        assertEquals("Pas traduit", I18n.frenchOf("Pas traduit"))
    }

    @Test
    fun catalogsAreValidAndHaveTheSameKeysInEveryLanguage() {
        I18n.AREAS.forEach { area ->
            val en = I18n.area(Lang.EN, area).keys
            val nl = I18n.area(Lang.NL, area).keys
            assertEquals("area $area: en and nl differ: ${(en - nl) + (nl - en)}", en, nl)
        }
    }
}
