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

    /**
     * The trap recipe stripped of every format v2 field (body, texture, loop, mix, vocal sample, doubled
     * chords, layers, lib sounds, the new roles): an old-format recipe whatever the styles' teams convert.
     */
    private fun trap(): JsonObject {
        val all = Json.parseToJsonElement(File(root, "scripts/packs/recipes/hiphop.json").readText()).jsonArray
        return plain(all.first { (it.jsonObject["id"] as JsonPrimitive).content == "trap" }.jsonObject)
    }

    private fun plain(recipe: JsonObject): JsonObject {
        fun JsonObject.without(vararg keys: String) = JsonObject(this - keys.toSet())
        fun JsonObject.plainSound(): JsonObject {
            val sound = (this["sound"] as? JsonPrimitive)?.content ?: return this
            if (!sound.startsWith("lib:")) return this
            val preset = sound.substringAfter('|', "")
            return if (preset.isEmpty()) without("sound") else JsonObject(this + ("sound" to JsonPrimitive(preset)))
        }
        val drums = recipe["drums"]!!.jsonArray.map { it.jsonObject }
            .filter { (it["role"] as JsonPrimitive).content !in setOf("snap", "rim", "conga", "shaker") }
            .map { d ->
                val source = (d["source"] as? JsonPrimitive)?.content
                val plainSource = if (source != null && source.startsWith("lib:")) source.removePrefix("lib:").substringBefore(':') else null
                d.without("layers").let { if (plainSource != null) JsonObject(it + ("source" to JsonPrimitive(plainSource))) else it }
            }
        val vocal = recipe["vocal"]
        return JsonObject(
            recipe.without("body", "texture", "loop", "mix") + listOfNotNull(
                "drums" to JsonArray(drums),
                (vocal as? JsonObject)?.let { "vocal" to it["type"]!! },
                (recipe["chords"] as? JsonObject)?.let { "chords" to it.without("double").plainSound() },
                (recipe["bass"] as? JsonObject)?.let { "bass" to it.plainSound() },
                (recipe["lead"] as? JsonObject)?.let { "lead" to it.plainSound() },
            ),
        )
    }

    private fun JsonObject.with(vararg fields: Pair<String, JsonElement>) = JsonObject(this + fields)

    private fun json(text: String) = Json.parseToJsonElement(text)

    /** The converter run on [recipe] alone: (exit code, generated text or the error). */
    private fun convert(recipe: JsonObject, catalogues: File? = null): Pair<Int, String> {
        assumeTrue("converter not found", script.isFile)
        val input = File.createTempFile("recipe", ".json").apply { deleteOnExit(); writeText(buildJsonArray { add(recipe) }.toString()) }
        val out = File.createTempFile("StyleRecipes", ".kt").apply { deleteOnExit() }
        val process = runCatching {
            ProcessBuilder("python3", "-I", script.path, out.path, input.path).redirectErrorStream(true)
                .apply { catalogues?.let { environment()["PS_PACK_CATALOGUES"] = it.path } }.start()
        }.getOrNull()
        assumeTrue("python3 is not available", process != null)
        val log = process!!.inputStream.bufferedReader().readText()
        assumeTrue("python3 did not finish", process.waitFor(60, TimeUnit.SECONDS))
        return process.exitValue() to if (process.exitValue() == 0) out.readText() else log
    }

    private fun accepted(recipe: JsonObject, catalogues: File? = null): String {
        val (code, text) = convert(recipe, catalogues)
        assertEquals(text, 0, code)
        return text
    }

    private fun refused(recipe: JsonObject, why: String, catalogues: File? = null) {
        val (code, text) = convert(recipe, catalogues)
        assertNotEquals("accepted: $why", 0, code)
        assertTrue("$why: $text", "recipe error" in text)
    }

    private fun drums(recipe: JsonObject, edit: (Int, JsonObject) -> JsonObject?, vararg more: JsonObject): JsonObject {
        val lines = recipe["drums"]!!.jsonArray.mapIndexedNotNull { i, d -> edit(i, d.jsonObject) } + more
        return recipe.with("drums" to JsonArray(lines))
    }

    private fun drum(role: String, extra: String = "") =
        json("""{"role": "$role", "pattern": "....X.......X...", "tip_fr": "t"$extra}""").jsonObject

    /** Catalogues of test: a voice pack and an instrument open to recipes, a pack that is not. */
    private fun catalogues(): File {
        val dir = kotlin.io.path.createTempDirectory("catalogues").toFile().apply { deleteOnExit() }
        File(dir, "libraries.json").writeText(
            """{"sources": [], "packs": [
              {"id": "voix_test", "recipes": true, "rules": [{"slug": "yeah", "kind": "vocal"}, {"slug": "skrrt", "kind": "vocal"}]},
              {"id": "perc_test", "recipes": true, "rules": [{"slug": "shaker", "kind": "perc"}, {"slug": "doigts", "kind": "clap"}]},
              {"id": "ferme", "rules": [{"slug": "x", "kind": "perc"}]}]}""",
        )
        File(dir, "instruments.json").writeText(
            """{"sources": [], "instruments": [
              {"id": "instrument_test", "recipes": {"flute": [67, 100]}, "category": "woodwinds", "kind": "lead", "fallback": "Lead"},
              {"id": "instrument_ferme", "category": "piano", "kind": "keys", "fallback": "Piano FM"}]}""",
        )
        return dir
    }

    @Test
    fun everyPackOpenToRecipesIsAccepted() {
        val cat = catalogues()
        val lead = { sound: String -> trap().with("lead" to JsonObject(trap()["lead"]!!.jsonObject + ("sound" to JsonPrimitive(sound)))) }
        val recipe = drums(lead("lib:instrument_test:flute"), { i, d ->
            when (i) {
                0 -> d.with("layers" to json("""["lib:perc_test:doigts"]"""))
                else -> d
            }
        }, drum("shaker", ""","source": "lib:perc_test:shaker"""")).with("vocal" to json("""{"type": "answer", "sample": "voix_test:yeah"}"""))
        val text = accepted(recipe, cat)
        assertTrue(text, "Part.Lead to \"instrument_test:flute\"" in text)
        assertTrue(text, "lead = \"Lead\"" in text || "\"Lead\"" in text)
        assertTrue(text, "Role.Shaker to \"perc_test:shaker\"" in text)
        assertTrue(text, "lib:perc_test:doigts" in text)
        assertTrue(text, "vocalSample = \"voix_test:yeah\"" in text)
        val shaker = { src: String -> drums(trap(), { _, d -> d }, drum("shaker", ""","source": "$src"""")) }
        refused(shaker("lib:perc_test:cloche"), "a sound the pack does not have", cat)
        refused(shaker("lib:ferme:x"), "a pack not open to recipes", cat)
        refused(shaker("lib:instrument_test:flute"), "an instrument as a drum sound", cat)
        refused(lead("lib:instrument_test:hautbois"), "a part the instrument does not have", cat)
        refused(lead("lib:instrument_ferme:piano"), "an instrument not open to recipes", cat)
        refused(lead("lib:voix_test:yeah"), "a sample pack as a melodic instrument", cat)
        refused(trap().with("vocal" to json("""{"type": "answer", "sample": "voix_test:nope"}""")), "an unknown chop", cat)
    }

    @Test
    fun anInstrumentNamedByTheRecipeComesFirst() {
        val style = StarterWizard.STYLES.first()
        val flute = style.copy(melody = style.melody.copy(packSounds = mapOf(StarterWizard.Part.Lead to "instrument_test:flute")))
        val installed = listOf(PackManifest("instrument_test", "Bois test", instrument = MultiInstrument(category = "woodwinds")))
        assertEquals("instrument_test", StarterWizard.soundsFor(StarterWizard.Part.Lead, flute, installed).first().multi?.pack)
    }

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
        refused(kick("source" to JsonPrimitive("lib:tr808:..")), "the parent folder")
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
