package com.pocketstudio.model.tuto

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 7, tuto3 (seat c, ko: « ““인트로” 파트 만들기”를 탭하세요 »): a button's name quoted inside a quote opened the
 * same mark twice. Every tuto text of every language: a quoted button is wrapped in other marks, and its own text
 * stays the button's, word for word.
 */
class Tour7TutoNestedQuotesTest {
    /** The same mark twice in a row: a quoted button wrapped in the same quotes (« ““인트로” 파트 만들기” »). */
    private val doubled = Regex("““|””|„„|««|»»|「「|」」")

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
                    if (doubled.containsMatchIn(v)) bad += "${dir.name}/${f.name}: ${v.take(70)}"
                }
            }
        }
        assertTrue("no tuto text read", seen > 1000)
        assertTrue("${bad.size} quotes opened twice:\n" + bad.take(30).joinToString("\n"), bad.isEmpty())
    }
}
