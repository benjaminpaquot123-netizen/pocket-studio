package com.pocketstudio.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Audit stage 2, point 4 (« allers-retours de toutes les données »), the list of fields read from the
 * code (the serialised project itself): the format has one version number ([Project.FORMAT_VERSION]
 * = 1) and grows by fields added with a default, so « each older version » is « a file without any
 * one of today's fields ». For every field of every class the reference projects reach (each
 * kind of field once, wherever it sits), a file without it opens, and saving it again then opening
 * it gives the same project; and every reference project survives a save and reopen unchanged.
 */
class FormatMigrationTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun references(): Map<String, String> {
        val dir = File("../app/src/test/resources/reference-projects")
        val files = listOf("vide.json", "petit/project.json", "enorme/project.json").associateWith { File(dir, it).readText() }
        // Pad Live's project (its own fields: pads, pages, scenes) and an assistant's song with its Guide.
        return files + mapOf(
            "padlive" to ProjectJson.encode(PadLive.newProject()),
            "assistant" to ProjectJson.encode(StarterWizard.style("acidcore")!!.let { style ->
                StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? }, style.melody.mood,
                    StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() })
            }),
        )
    }

    /** Every key path of [e] once per kind ("song[].scene"): one removal each. */
    private fun paths(e: JsonElement, at: List<Any> = emptyList(), kind: String = "", out: MutableMap<String, List<Any>> = LinkedHashMap()): Map<String, List<Any>> {
        when (e) {
            is JsonObject -> e.forEach { (k, v) ->
                val kk = "$kind.$k"
                if (kk !in out) out[kk] = at + k
                paths(v, at + k, kk, out)
            }
            // The first few elements of an array: enough to reach each kind of element (a scene, a track…).
            is JsonArray -> e.take(3).forEachIndexed { i, v -> paths(v, at + i, "$kind[]", out) }
            else -> {}
        }
        return out
    }

    private fun without(e: JsonElement, path: List<Any>): JsonElement {
        val head = path.first()
        val rest = path.drop(1)
        return when {
            e is JsonObject && head is String ->
                if (rest.isEmpty()) JsonObject(e - head) else JsonObject(e.toMutableMap().also { it[head] = without(e.getValue(head), rest) })
            e is JsonArray && head is Int -> JsonArray(e.toMutableList().also { it[head] = without(e[head], rest) })
            else -> e
        }
    }

    /**
     * The fields with no default: each is what its class is (a note's start, pitch and length, a track's id
     * and name, a sound's file), there since the class was made, so no older file lacks it. A field added
     * later always comes with a default (Project.kt's rule); one added without fails here.
     */
    private val IDENTITY = setOf(
        "AudioClip.file", "AudioClip.name", "AudioTrack.id", "AudioTrack.name", "AutoTarget.kind", "AutomationLane.target",
        "InstrumentTrack.id", "InstrumentTrack.name", "LiveClip.file", "LiveClip.name", "Note.length", "Note.pitch", "Note.start",
        "PadSample.file", "PadSample.name", "PadVoice.kind",
    )

    @Test
    fun everyReferenceProjectSurvivesSaveAndReopen() {
        references().forEach { (name, text) ->
            val opened = ProjectJson.decode(text)
            val again = ProjectJson.decode(ProjectJson.encode(opened))
            assertEquals("$name: reopened after a save", opened, again)
            assertEquals("$name: the file written twice is the same", ProjectJson.encode(opened), ProjectJson.encode(again))
        }
    }

    @Test
    fun aFileWithoutAnyOneFieldStillOpens() {
        var checked = 0
        val broken = mutableListOf<String>()
        references().forEach { (name, text) ->
            val tree = json.parseToJsonElement(text)
            paths(tree).forEach { (kind, path) ->
                val old = without(tree, path).toString()
                val result = runCatching {
                    val p = ProjectJson.decode(old)
                    // Saved again and reopened: the same project (a default filled in once stays).
                    assertEquals(p, ProjectJson.decode(ProjectJson.encode(p)))
                }
                checked++
                result.exceptionOrNull()?.let { e ->
                    val required = Regex("""Field '(\w+)' is required for type with serial name 'com\.pocketstudio\.model\.(\w+)'""").find(e.message.orEmpty())
                        ?.let { it.groupValues[2] + "." + it.groupValues[1] }
                    if (required !in IDENTITY) broken += "$name $kind: ${e.javaClass.simpleName} ${e.message?.take(160)}"
                }
            }
        }
        println("FormatMigrationTest: $checked files each missing one field")
        assertTrue("checked only $checked", checked > 300)
        assertTrue("files of an older version (one field missing) that no longer open:\n" + broken.joinToString("\n"), broken.isEmpty())
    }
}
