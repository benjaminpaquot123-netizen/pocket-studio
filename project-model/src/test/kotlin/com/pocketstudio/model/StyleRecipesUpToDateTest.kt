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

    /**
     * The converter reads the sounds, ready-made effects and effect types from the app's code: every
     * one that exists can be written in a recipe (the phase 25 ones too), and nothing else.
     */
    @Test
    fun theConverterAcceptsEverySoundAndEffectOfTheApp() {
        val root = File("..").canonicalFile
        val script = File(root, "scripts/packs/recipes_to_kotlin.py")
        assumeTrue("converter not found", script.isFile)
        val process = runCatching { ProcessBuilder("python3", "-I", script.path, "--names").redirectErrorStream(true).start() }.getOrNull()
        assumeTrue("python3 is not available", process != null)
        val text = process!!.inputStream.bufferedReader().readText()
        assumeTrue("python3 did not finish", process.waitFor(60, TimeUnit.SECONDS))
        assertEquals(text, 0, process.exitValue())
        val names = kotlinx.serialization.json.Json.parseToJsonElement(text).let { it as kotlinx.serialization.json.JsonObject }
        fun list(key: String) = (names.getValue(key) as kotlinx.serialization.json.JsonArray).map { (it as kotlinx.serialization.json.JsonPrimitive).content }.toSet()
        assertEquals((SoundLibrary.SYNTH + SoundLibrary.PLAITS).map { it.name }.toSet(), list("sounds"))
        assertEquals(SoundLibrary.ACID.map { it.name }.toSet(), list("acid_sounds"))
        assertEquals(FxPresets.ALL.map { it.id }.toSet(), list("fx_presets"))
        assertEquals((EffectType.entries - EffectType.NONE).map { it.name }.toSet(), list("effects"))
        for (wanted in listOf("Sirène", "Souffle")) assertEquals(wanted, true, wanted in list("sounds"))
        for (wanted in listOf("dub_deep", "fat_bass", "scream_lead", "edm_pump", "wide_alive", "pluck_pingpong")) assertEquals(wanted, true, wanted in list("fx_presets"))
        for (wanted in listOf("DISTORTION", "ECHO", "REVERB", "OTT", "TAPE_STOP")) assertEquals(wanted, true, wanted in list("effects"))
    }
}
