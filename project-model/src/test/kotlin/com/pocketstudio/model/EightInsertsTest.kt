package com.pocketstudio.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Eight effects per strip: the app, the engine bridge and the engine agree; old projects (four) load. */
class EightInsertsTest {
    @Test
    fun theAppTheBridgeAndTheEngineCountEightSlots() {
        assertEquals(8, Mixer.INSERTS)
        val mixerH = File("../engine/src/main/cpp/core/Mixer.h").readText()
        assertEquals(Mixer.INSERTS, Regex("kInserts = (\\d+);").find(mixerH)!!.groupValues[1].toInt())
        // The bridge packs channel x INSERTS + slot; the engine unpacks with its kInserts.
        val bridge = File("../engine/src/main/java/com/pocketstudio/engine/NativeEngine.kt").readText()
        assertEquals(Mixer.INSERTS, Regex("const val INSERTS = (\\d+)").find(bridge)!!.groupValues[1].toInt())
        assertTrue(bridge.contains("ch * INSERTS + slot"))
        val engine = File("../engine/src/main/cpp/core/Engine.cpp").readText()
        assertTrue(engine.contains("command.a / Mixer::kInserts, command.a % Mixer::kInserts"))
    }

    @Test
    fun slotSevenIsAutomatedLikeTheOthers() {
        val t = AutoTarget(AutoTarget.INSERT_PARAM, 15, 7, 3)
        assertTrue(t.isValid)
        // Engine side: kind << 16 | ch << 12 | slot << 8 | param, slot on 4 bits.
        assertEquals((3 shl 16) or (15 shl 12) or (7 shl 8) or 3, t.pack())
        assertEquals(7, (t.pack() shr 8) and 0xF)
        assertEquals(15, (t.pack() shr 12) and 0xF)
        // No ninth slot: a damaged file's lane is dropped.
        assertFalse(AutoTarget(AutoTarget.INSERT_PARAM, 2, 8, 0).isValid)
        assertEquals(1, AutomationLane.sanitize(listOf(AutomationLane(t), AutomationLane(AutoTarget(AutoTarget.INSERT_PARAM, 2, 8, 0)))).size)
        // A send's or bus's slot is its bus, not an effect slot: unchanged.
        assertTrue(AutoTarget(AutoTarget.SEND, 2, 1).isValid)
        // MIDI knobs reach slot 7 too.
        val map = MidiMapping.sanitize(listOf(MidiMapping(20, AutoTarget(AutoTarget.INSERT_PARAM, 2, 7, 0)), MidiMapping(21, AutoTarget(AutoTarget.INSERT_PARAM, 2, 8, 0))))
        assertEquals(listOf(20), map.map { it.controller })
    }

    @Test
    fun oldProjectsWithFourSlotsLoadUnchangedAndSaveEight() {
        val eq = Insert.of(EffectType.EQ)
        val dist = Insert.of(EffectType.DISTORTION).copy(bypass = true)
        val lane = AutomationLane(AutoTarget(AutoTarget.INSERT_PARAM, 2, 3, 1)).withPoint(4, 0.25f)
        val now = Project(
            mixer = Mixer(
                channels = List(Mixer.CHANNELS) { if (it == 2) Channel(inserts = listOf(eq, Insert(), Insert(), dist)) else Channel() },
                master = listOf(Insert(), Insert.of(EffectType.COMPRESSOR), Insert(), Insert()),
            ),
            automation = listOf(lane),
        )
        // The file as the four-slot app wrote it: four inserts on every strip and on the master.
        val json = Json { ignoreUnknownKeys = true }
        val written = json.parseToJsonElement(ProjectJson.encode(now)).jsonObject
        fun fourSlots(e: JsonElement): JsonElement = when (e) {
            is JsonObject -> JsonObject(e.mapValues { (k, v) -> if ((k == "inserts" || k == "master") && v is JsonArray) JsonArray(v.take(4)) else fourSlots(v) })
            is JsonArray -> JsonArray(e.map(::fourSlots))
            else -> e
        }
        val oldFile = fourSlots(written).toString()
        val oldMixer = json.parseToJsonElement(oldFile).jsonObject["mixer"]!!.jsonObject
        assertEquals(4, oldMixer["master"]!!.jsonArray.size)
        assertEquals(4, oldMixer["channels"]!!.jsonArray[2].jsonObject["inserts"]!!.jsonArray.size)

        val loaded = ProjectJson.decode(oldFile)
        assertEquals(listOf(eq, Insert(), Insert(), dist), loaded.mixer.channels[2].inserts.take(4))
        assertEquals(Mixer.INSERTS, loaded.mixer.channels[2].inserts.size)
        assertTrue(loaded.mixer.channels[2].inserts.drop(4).all { it == Insert() })
        assertEquals(EffectType.COMPRESSOR, loaded.mixer.master[1].effect)
        assertEquals(Mixer.INSERTS, loaded.mixer.master.size)
        assertEquals(listOf(lane), loaded.automation)
        // What plays is the same: the four new slots are empty (the engine hears nothing there).
        assertEquals(now.mixer.channels.map { c -> c.inserts.take(4).map { it.heardType } }, loaded.mixer.channels.map { c -> c.inserts.take(4).map { it.heardType } })
        assertTrue(loaded.mixer.channels.all { c -> c.inserts.drop(4).all { it.heardType == 0 } })

        // Saved again: eight on every strip and on the master, and it reads back the same.
        val saved = json.parseToJsonElement(ProjectJson.encode(loaded)).jsonObject["mixer"]!!.jsonObject
        assertEquals(8, saved["master"]!!.jsonArray.size)
        assertTrue(saved["channels"]!!.jsonArray.all { it.jsonObject["inserts"]!!.jsonArray.size == 8 })
        assertEquals(loaded, ProjectJson.decode(ProjectJson.encode(loaded)))
    }

    @Test
    fun theLastSlotsAreFilledMovedAndFull() {
        val e = Insert.of(EffectType.EQ)
        val seven = List(7) { e } + Insert()
        assertEquals(7, freeInsertSlot(seven))
        assertNull(freeInsertSlot(List(Mixer.INSERTS) { e }))
        // Moving the effect of slot 7 up takes its lane along.
        val chorus = Insert.of(EffectType.CHORUS)
        val lane = AutomationLane(AutoTarget(AutoTarget.INSERT_PARAM, 2, 7, 0))
        val p = Project(
            mixer = Mixer(channels = List(Mixer.CHANNELS) { if (it == 2) Channel(inserts = List(7) { e } + chorus) else Channel() }),
            automation = listOf(lane),
        )
        val moved = p.withInsertsSwapped(2, 7, 6)
        assertEquals(chorus, moved.mixer.channels[2].inserts[6])
        assertEquals(6, moved.automation.single().target.slot)
        // A ready-made vibe fills the first slots and empties the others, all eight.
        val fx = Channel(inserts = List(Mixer.INSERTS) { e }).withFx(FxPresets.ALL.first())
        assertEquals(Mixer.INSERTS, fx.inserts.size)
        assertEquals(FxPresets.ALL.first().inserts.size, fx.inserts.count { it.effect != EffectType.NONE })
    }
}
