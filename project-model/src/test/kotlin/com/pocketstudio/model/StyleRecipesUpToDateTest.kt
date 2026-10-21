package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

/** StyleRecipes.kt is generated from the JSON recipes in scripts/packs/recipes: an edit of one without the other fails here. */
class StyleRecipesUpToDateTest {
    @Test
    fun theGeneratedStylesAreTheRecipes() {
        val root = File("..").canonicalFile
        val script = File(root, "scripts/packs/recipes_to_kotlin.py")
        val recipes = File(root, "scripts/packs/recipes").listFiles { f -> f.name.endsWith(".json") }.orEmpty().sortedBy { it.name }
        assumeTrue("recipes not found", script.isFile && recipes.isNotEmpty())
        val out = File.createTempFile("StyleRecipes", ".kt").apply { deleteOnExit() }
        val process = runCatching {
            ProcessBuilder(listOf("python3", "-I", script.path, out.path) + recipes.map { it.path }).redirectErrorStream(true).start()
        }.getOrNull()
        assumeTrue("python3 is not available", process != null)
        val log = process!!.inputStream.bufferedReader().readText()
        assumeTrue("python3 did not finish", process.waitFor(60, TimeUnit.SECONDS))
        assertEquals("the converter failed:\n$log", 0, process.exitValue())
        val committed = File(root, "project-model/src/main/kotlin/com/pocketstudio/model/StyleRecipes.kt").readText()
        assertEquals("regenerate StyleRecipes.kt with scripts/packs/recipes_to_kotlin.py", out.readText(), committed)
    }
}
