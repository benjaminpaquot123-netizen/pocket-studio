package com.pocketstudio.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * The recipes' frozen format v2 (grand remplacement v2, lot L1; scripts/packs/recipes_to_kotlin.py's docstring):
 * each new field is read and written into the style, a wrong one is refused with a recipe error, and a recipe
 * without them gives the very same style as before (StyleRecipesUpToDateTest holds the 37 styles byte for byte).
 */
class RecipeFormatV2Test {
    private val root = File("..").canonicalFile
    private val script = File(root, "scripts/packs/recipes_to_kotlin.py")

    private fun trap(): JsonObject {
        val all = Json.parseToJsonElement(File(root, "scripts/packs/recipes/hiphop.json").readText()).jsonArray
        return all.first { (it.jsonObject["id"] as JsonPrimitive).content == "trap" }.jsonObject
    }

    private fun JsonObject.with(vararg fields: Pair<String, JsonElement>) = JsonObject(this + fields)

    private fun json(text: String) = Json.parseToJsonElement(text)

    /** The converter run on [recipe] alone: (exit code, generated text or the error). */
    private fun convert(recipe: JsonObject): Pair<Int, String> {
        assumeTrue("converter not found", script.isFile)
        val input = File.createTempFile("recipe", ".json").apply { deleteOnExit(); writeText(buildJsonArray { add(recipe) }.toString()) }
        val out = File.createTempFile("StyleRecipes", ".kt").apply { deleteOnExit() }
        val process = runCatching { ProcessBuilder("python3", "-I", script.path, out.path, input.path).redirectErrorStream(true).start() }.getOrNull()
        assumeTrue("python3 is not available", process != null)
        val log = process!!.inputStream.bufferedReader().readText()
        assumeTrue("python3 did not finish", process.waitFor(60, TimeUnit.SECONDS))
        return process.exitValue() to if (process.exitValue() == 0) out.readText() else log
    }

    private fun accepted(recipe: JsonObject): String {
        val (code, text) = convert(recipe)
        assertEquals(text, 0, code)
        return text
    }

    private fun refused(recipe: JsonObject, why: String) {
        val (code, text) = convert(recipe)
        assertNotEquals("accepted: $why", 0, code)
        assertTrue("$why: $text", "recipe error" in text)
    }

    private fun drums(recipe: JsonObject, edit: (Int, JsonObject) -> JsonObject?, vararg more: JsonObject): JsonObject {
        val lines = recipe["drums"]!!.jsonArray.mapIndexedNotNull { i, d -> edit(i, d.jsonObject) } + more
        return recipe.with("drums" to JsonArray(lines))
    }

    private fun drum(role: String, extra: String = "") =
        json("""{"role": "$role", "pattern": "....X.......X...", "tip_fr": "t"$extra}""").jsonObject

    @Test
    fun aRecipeWithoutTheNewFieldsIsWrittenAsBefore() {
        val text = accepted(trap())
        for (v2 in listOf("layers =", "kitSounds", "chordDouble", "texture", "loop =", "vocalSample", "mix =", "body =", "Role.Snap")) {
            assertTrue("$v2 in an old recipe", v2 !in text)
        }
    }

    @Test
    fun theNewRolesHaveTheirPadAndTheirKitVoice() {
        val text = accepted(drums(trap(), { _, d -> d }, drum("snap"), drum("rim"), drum("conga"), drum("shaker", ""","voice": "zap"""")))
        assertTrue(text, "Line(Role.Snap, \"....X.......X...\", \"t\", voice = 4)" in text)
        assertTrue(text, "Line(Role.Rim, \"....X.......X...\", \"t\", voice = 10)" in text)
        assertTrue(text, "Line(Role.Conga, \"....X.......X...\", \"t\", voice = 14)" in text)
        assertTrue("a voice given wins", "Line(Role.Shaker, \"....X.......X...\", \"t\", voice = 15)" in text)
        val roles = StarterWizard.Role.entries
        assertEquals("one pad per role", roles.size, roles.map { it.lane }.toSet().size)
        assertEquals(listOf(9, 11, 14, 15), listOf(StarterWizard.Role.Snap, StarterWizard.Role.Rim, StarterWizard.Role.Conga, StarterWizard.Role.Shaker).map { it.lane })
        assertEquals("the snap is a clap of a pack", "clap", StarterWizard.Role.Snap.packKind)
        refused(drums(trap(), { _, d -> d }, drum("bongo")), "an unknown role")
    }

    @Test
    fun aLibSourceAndLayersAreRead() {
        val recipe = drums(trap(), { i, d ->
            when (i) {
                0 -> d.with("source" to JsonPrimitive("lib:tr808:kick_long-1.wav"), "layers" to json("""["kit:kick2", "lib:modern_808:kick_punch"]"""))
                1 -> d.with("layers" to json("""["tr909"]"""))
                else -> d
            }
        })
        val text = accepted(recipe)
        assertTrue(text, "layers = listOf(\"kit:1\", \"lib:modern_808:kick_punch\"))" in text)
        assertTrue(text, "layers = listOf(\"tr909\"))" in text)
        assertTrue(text, "kitSounds = mapOf(Role.Kick to \"tr808:kick_long-1.wav\")," in text)
        assertTrue("the pack is the line's kit", "Role.Kick to \"tr808\"" in text)
        val kick = { extra: Pair<String, JsonElement> -> drums(trap(), { i, d -> if (i == 0) d.with(extra) else d }) }
        refused(kick("source" to JsonPrimitive("lib:nowhere:kick")), "an unknown pack")
        refused(kick("source" to JsonPrimitive("lib:tr808:")), "no sound")
        refused(kick("source" to JsonPrimitive("lib:tr808:../kick")), "a path")
        refused(kick("layers" to json("[]")), "no layer")
        refused(kick("layers" to json("""["tr909", "tr808", "tr707", "cr78"]""")), "4 layers on a line")
        refused(kick("layers" to json("""["tr909", "tr909"]""")), "twice the same")
        refused(kick("layers" to json("""["kit:bongo"]""")), "an unknown kit voice")
        refused(kick("layers" to json("""["nowhere"]""")), "an unknown pack")
        refused(
            drums(trap(), { _, d -> d.with("layers" to json("""["tr909", "tr808"]""")) }),
            "more than 4 layers in a style",
        )
    }

    @Test
    fun doubleTextureLoopVocalMixAndBodyAreRead() {
        val t = trap()
        val chords = t["chords"]!!.jsonObject
        val text = accepted(
            t.with(
                "chords" to chords.with("double" to JsonPrimitive("both")),
                "texture" to json("""{"sound": "Souffle"}"""),
                "loop" to json("""{"sound": "trap:loop_bells", "bars": 4}"""),
                "vocal" to json("""{"type": "stab", "sample": "vocal_chops:hey_01"}"""),
                "mix" to json("""{"pump": ["808", "chords"], "glue_blend": 0.7, "bus_drive": 0.3}"""),
                "body" to JsonPrimitive(true),
            ),
        )
        for (wanted in listOf(
            "chordDouble = StarterWizard.ChordDouble.Both,",
            "texture = \"Souffle\",",
            "loop = StarterWizard.LoopSpec(\"trap\", \"loop_bells\", bars = 4),",
            "vocal = Vocal.Stab,",
            "vocalSample = \"vocal_chops:hey_01\",",
            "mix = StarterWizard.StyleMix(pump = setOf(\"808\", \"chords\"), glueBlend = 0.7f, busDrive = 0.3f),",
            "body = true,",
        )) assertTrue("$wanted\n$text", wanted in text)
        // The same values, typed in Kotlin: the names the converter writes exist.
        val style = StarterWizard.Style(
            "x", "x", "x", 140.0, "", emptyList(),
            melody = StarterWizard.Melody(chordDouble = StarterWizard.ChordDouble.Both, texture = "Souffle"),
            kitSounds = mapOf(StarterWizard.Role.Kick to "tr808:kick"),
            vocalSample = "vocal_chops:hey_01",
            loop = StarterWizard.LoopSpec("trap", "loop_bells", bars = 4),
            mix = StarterWizard.StyleMix(pump = setOf("808", "chords"), glueBlend = 0.7f, busDrive = 0.3f),
            body = true,
        )
        assertEquals("the loop's and the chop's packs are wanted", listOf("trap", "vocal_chops", "x"), style.packs)

        refused(t.with("chords" to chords.with("double" to JsonPrimitive("triple"))), "an unknown double")
        refused(t.with("texture" to json("""{"sound": "Pas un son"}""")), "an unknown texture sound")
        refused(t.with("texture" to JsonPrimitive("Souffle")), "a texture not as an object")
        refused(t.with("loop" to json("""{"sound": "trap:loop", "bars": 3}""")), "3 bars")
        refused(t.with("loop" to json("""{"sound": "nowhere:loop", "bars": 1}""")), "an unknown loop pack")
        refused(t.with("loop" to json("""{"bars": 1}""")), "a loop without sound")
        refused(t.with("vocal" to json("""{"type": "stab"}""")), "a vocal object without sample")
        refused(t.with("vocal" to json("""{"type": "sing", "sample": "vocal_chops:hey"}""")), "an unknown vocal type")
        refused(t.with("vocal" to json("""{"sample": "vocal_chops"}""")), "a sample without its sound")
        refused(t.with("mix" to json("""{"pump": ["lead"]}""")), "a pump on the lead")
        refused(t.with("mix" to json("""{"glue_blend": 1.5}""")), "a blend over 1")
        refused(t.with("mix" to json("""{"bus_drive": "fort"}""")), "a drive as a word")
        refused(t.with("mix" to json("""{"punch": 1}""")), "an unknown mix field")
        refused(t.with("body" to JsonPrimitive("yes")), "body not a boolean")
    }
}
