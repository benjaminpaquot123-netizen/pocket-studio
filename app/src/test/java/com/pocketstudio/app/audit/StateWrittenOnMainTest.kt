package com.pocketstudio.app.audit

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Lots « coquille3 » and « coquille4 » (control seat Cq3, the portal of batch 8): « Mes fichiers » stayed on « Chargement
 * de la liste… » for good. Its effect read the files off the main thread and wrote the result into the screen's state from
 * the reading thread (where a composition effect resumes when its dispatcher does not take it back): that write never
 * reached the screen. A screen's effect (LaunchedEffect, produceState) that keeps the
 * * result of a `withContext(Dispatchers.IO)` (or `Dispatchers.Default`) writes it on the main thread (`withContext(Dispatchers.Main)` in the block).
 */
class StateWrittenOnMainTest {
    private val sources = File("src/main/java").walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    /** The block that opens at [open] (its « { »), as written. */
    private fun block(text: String, open: Int): String {
        var depth = 0
        for (i in open until text.length) {
            when (text[i]) {
                '{' -> depth++
                '}' -> if (--depth == 0) return text.substring(open, i + 1)
            }
        }
        return text.substring(open)
    }

    @Test
    fun aScreensEffectKeepsWhatItReadOffTheMainThreadOnTheMainThread() {
        assertTrue("no sources read from ${File(".").absolutePath}", sources.size > 100)
        val start = Regex("""\b(LaunchedEffect|produceState)\s*\([^{]*\{""")
        val keptFromIo = Regex("""(^|[\n{;])\s*(val\s+\w+(\s*:[^=]+)?|var\s+\w+|\w+|value)\s*=\s*(kotlinx\.coroutines\.)?withContext\(\s*(kotlinx\.coroutines\.)?Dispatchers\.(IO|Default)""")
        val found = sources.filter { "@Composable" in it.readText() }.flatMap { f ->
            val text = f.readText()
            start.findAll(text).mapNotNull { m ->
                val body = block(text, m.range.last)
                if (keptFromIo.containsMatchIn(body) && "Dispatchers.Main" !in body) {
                    "${f.path}:${text.substring(0, m.range.first).count { it == '\n' } + 1}"
                } else null
            }.toList()
        }
        assertTrue("effects keeping a result read off the main thread, written from the reading thread:\n" + found.joinToString("\n"), found.isEmpty())
    }
}
