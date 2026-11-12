package com.pocketstudio.app

import com.pocketstudio.app.help.Glossary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Audit stage 2, point 1: the map of functions (docs/audits/CARTE-FONCTIONS.md) stays true. Each test it
 * names exists (Kotlin `Class::test`, engine `host::name`), each row has its 13 cells, and the empty
 * cells (« VIDE ») never grow in number: a new gap is a wiring forgotten.
 */
class CarteFonctionsTest {
    private val map = File("../docs/audits/CARTE-FONCTIONS.md").readText()

    /** The cells « VIDE » when the map was made (2026-11-03); lower it as they are filled, never raise it. */
    private val emptyCellsAtMost = 25

    private fun rows(): List<List<String>> = map.lines()
        .filter { it.startsWith("| ") && !it.startsWith("| Fonction") && !it.startsWith("|---") }
        .map { line -> line.trim().trim('|').split(" | ").map { it.trim() } }

    private val kotlinTests: Set<String> by lazy {
        val files = (File("src/test/java").walkTopDown() + File("../project-model/src/test/kotlin").walkTopDown()).filter { it.extension == "kt" }.toList()
        val methods = files.flatMap { f -> Regex("""fun\s+`?(\w+)`?\s*\(""").findAll(f.readText()).map { it.groupValues[1] } }.toSet()
        val classes = files.flatMap { f -> Regex("""class\s+(\w+)""").findAll(f.readText()).map { it.groupValues[1] } }.toSet()
        methods.map { "m:$it" }.toSet() + classes.map { "c:$it" }
    }

    private val hostTests: Set<String> by lazy {
        File("../engine/host-tests").listFiles()!!.filter { it.isFile }.flatMap { f ->
            Regex("""\{"([a-z0-9_]+)",\s""").findAll(f.readText()).map { it.groupValues[1] }
        }.toSet()
    }

    @Test
    fun everyCellNamesATestThatExists() {
        val rows = rows()
        assertTrue("rows: ${rows.size}", rows.size >= 20)
        val missing = mutableListOf<String>()
        rows.forEach { cells ->
            assertEquals("« ${cells.first()} » has ${cells.size - 1} cells, not 13", 14, cells.size)
            cells.drop(1).forEach { cell ->
                Regex("""host::([a-z0-9_]+)""").findAll(cell).forEach { if (it.groupValues[1] !in hostTests) missing += it.value }
                Regex("""\b([A-Z]\w+)::(\w+)""").findAll(cell).forEach { m ->
                    val (cls, method) = m.destructured
                    if (cls == "host") return@forEach
                    if ("c:$cls" !in kotlinTests || "m:$method" !in kotlinTests) missing += m.value
                }
                assertTrue("« ${cells.first()} »: a cell says nothing: « $cell »", cell == "—" || cell == "VIDE" || "::" in cell)
            }
        }
        assertTrue("tests named by the map that do not exist: $missing", missing.isEmpty())
    }

    @Test
    fun theEmptyCellsNeverGrow() {
        val empty = rows().sumOf { r -> r.drop(1).count { it == "VIDE" } }
        val filled = rows().sumOf { r -> r.drop(1).count { "::" in it } }
        println("CarteFonctions: $filled cells with a test, $empty empty")
        assertTrue("$empty empty cells (at most $emptyCellsAtMost): a new gap in the map", empty <= emptyCellsAtMost)
    }

    /** Column « Aide »: each function has its word in the Lexique (Outils › Aide), with its plain explanation. */
    @Test
    fun everyFunctionHasItsWordInTheLexicon() {
        val words = mapOf(
            "Pad Live" to "Pads live", "Cases du Live" to "Live", "Briques et parties" to "Brique", "Copier une brique" to "Brique",
            "Remix (séparation" to "Remix", "Découpe du Remix" to "Remix", "Cases Remix du Live" to "Remix", "Tempo" to "Tempo (BPM)",
            "Effets" to "Effets live", "Acid 303" to "Acid 303", "Notes" to "Piano roll", "Accords" to "Accord", "Batterie" to "Pad",
            "Mix" to "Muet et solo", "Enregistrer (voix" to "Piste audio", "Enregistrer la performance" to "Enregistrer ma performance",
            "Bibliothèque" to "Bibliothèque de sons", "Export" to "WAV, FLAC, M4A", "MIDI" to "MIDI",
        )
        val terms = Glossary.ALL.associate { it.term to it.text }
        words.forEach { (function, term) ->
            assertTrue("$function: « $term » is not in the Lexique", terms[term]?.isNotBlank() == true)
            assertTrue("$function: no row of the map", rows().any { it.first().startsWith(function) })
        }
    }
}
