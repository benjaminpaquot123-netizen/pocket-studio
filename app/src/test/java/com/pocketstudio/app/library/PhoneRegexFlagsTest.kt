package com.pocketstudio.app.library

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Round 6, lot « emulateur »: the phone's pattern engine (ICU) refuses what the JVM of the tests accepts. An inline
 * « U » flag (as in `(?iU)`) or UNICODE_CHARACTER_CLASS throws on Android, so the Mode simple wizard crashed on a real
 * phone while every JVM test stayed green. Every Kotlin source shipped in the app is read here: only the inline flags
 * Android knows (i, m, s, x, u, d) may be written.
 */
class PhoneRegexFlagsTest {
    private val roots = listOf("app/src/main", "project-model/src/main", "ui-components/src/main", "engine/src/main/java")

    private fun sources(): List<File> {
        val top = generateSequence(File("").absoluteFile) { it.parentFile }.first { File(it, "settings.gradle.kts").exists() }
        return roots.map { File(top, it) }.filter { it.exists() }.flatMap { r -> r.walkTopDown().filter { it.extension == "kt" || it.extension == "json" }.toList() }
    }

    @Test
    fun noPatternUsesAFlagThePhoneRefuses() {
        val inline = Regex("""\(\?([a-zA-Z]+)(-[a-zA-Z]+)?[):]""")
        val bad = mutableListOf<String>()
        val files = sources()
        assertTrue("no source found", files.size > 100)
        for (f in files) f.readLines().forEachIndexed { n, line ->
            if ("UNICODE_CHARACTER_CLASS" in line) bad += "${f.name}:${n + 1} UNICODE_CHARACTER_CLASS"
            for (m in inline.findAll(line)) {
                val flags = m.groupValues[1] + m.groupValues[2].removePrefix("-")
                if (flags.any { it !in "imsxud" }) bad += "${f.name}:${n + 1} ${m.value}"
            }
        }
        assertTrue("patterns the phone refuses: $bad", bad.isEmpty())
    }

    @Test
    fun tonaliteIsStillExplainedWithItsAccent() {
        assertTrue(PlainWords.of("Choisis la tonalité").any { it.startsWith("Tonalité") })
        assertTrue(PlainWords.of("Les Tonalités du morceau").any { it.startsWith("Tonalité") })
        assertTrue(PlainWords.of("LA TONALITÉ").any { it.startsWith("Tonalité") })
        assertTrue(PlainWords.of("Une atonalitée").none { it.startsWith("Tonalité") })
    }
}
