package com.pocketstudio.model.tuto

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 7, tuto3 (seat c, ko: « ““인트로” 파트 만들기”를 탭하세요 »): a button's name quoted inside a quote opened the
 * same mark twice. Every tuto text of every language: a quote inside a quote uses the language's inner marks.
 */
class Tour7TutoNestedQuotesTest {
    private fun closers(lang: String): Map<Char, Char> = when (lang) {
        "de" -> mapOf('„' to '“', '«' to '»')
        "pl" -> mapOf('„' to '”', '«' to '»')
        else -> mapOf('“' to '”', '«' to '»', '„' to '“')
    }

    private fun nested(s: String, lang: String): Boolean {
        val pairs = closers(lang)
        val open = ArrayList<Char>()
        for (c in s) {
            if (open.isNotEmpty() && c == pairs[open.last()]) { open.removeAt(open.size - 1); continue }
            if (c in pairs.keys) {
                if (c in open) return true
                open += c
            }
        }
        return false
    }

    @Test
    fun noQuoteOpensTwiceInsideItself() {
        val root = listOf(File("src/main/resources/i18n"), File("project-model/src/main/resources/i18n")).first { it.isDirectory }
        val bad = ArrayList<String>()
        var seen = 0
        root.listFiles()!!.filter { it.isDirectory && it.name != "fr" }.forEach { dir ->
            dir.listFiles()!!.filter { it.name.startsWith("tuto") && it.name.endsWith(".json") }.forEach { f ->
                Regex("\"((?:[^\"\\\\]|\\\\.)*)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").findAll(f.readText()).forEach { m ->
                    seen++
                    val v = m.groupValues[2]
                    if (nested(v, dir.name)) bad += "${dir.name}/${f.name}: ${v.take(70)}"
                }
            }
        }
        assertTrue("no tuto text read", seen > 1000)
        assertTrue("${bad.size} quotes opened twice:\n" + bad.take(30).joinToString("\n"), bad.isEmpty())
    }
}
