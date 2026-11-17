package com.pocketstudio.model.tuto

import java.io.File
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every word a course gives a step ([TutoStep.check]) names a kind: none falls to [Unknown], each is parsed once. */
class TutoKindTest {
    private val files = File("src/main/resources/tutos").listFiles { f -> f.extension == "json" }.orEmpty().sortedBy { it.name }
    private val word = Regex(""""check"\s*:\s*"([^"]*)"""")
    private val words = files.flatMap { f -> word.findAll(f.readText()).map { it.groupValues[1] }.toList() }.distinct()

    @Test
    fun `every check word of every course names a kind`() {
        assertTrue("no course found", files.isNotEmpty() && words.size > 20)
        val unknown = words.filter { TutoKind.of(it) === Unknown }
        assertTrue("${unknown.size} check words no kind reads: $unknown", unknown.isEmpty())
    }

    @Test
    fun `a word is parsed once`() {
        for (w in words) assertSame(w, TutoKind.of(w), TutoKind.of(w))
    }

    @Test
    fun `a word no kind reads is the unknown kind`() {
        assertSame(Unknown, TutoKind.of("no such kind"))
    }
}
