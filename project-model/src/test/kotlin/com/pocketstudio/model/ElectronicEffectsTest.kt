package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ElectronicEffectsTest {
    private val core = File("../engine/src/main/cpp/core")

    @Test
    fun theAppAndTheEngineCountTheSameEffects() {
        val header = File(core, "Effects.h").readText()
        val kTypes = Regex("Fx = \\d+, kTypes = (\\d+)").find(header)!!.groupValues[1].toInt()
        assertEquals(kTypes, EffectType.entries.size)
        // The numbers of the new ones, as the engine's enum names them.
        for ((type, name) in listOf(
            EffectType.DISTORTION to "DistortionFx", EffectType.ECHO to "EchoFx", EffectType.PING_PONG to "PingPongFx",
            EffectType.REVERB to "ReverbFx", EffectType.AUTO_PAN to "AutoPanFx", EffectType.OTT to "OttFx",
            EffectType.FREQ_SHIFT to "FreqShiftFx", EffectType.TAPE_STOP to "TapeStopFx",
        )) {
            assertEquals(name, type.ordinal, Regex("$name = (\\d+)").find(header)!!.groupValues[1].toInt())
        }
        // A long effect borrowed late starts from the engine's defaults: the same as the app's.
        val cpp = File(core, "Effects.cpp").readText()
        for ((type, name) in listOf(EffectType.ECHO to "EchoFx", EffectType.PING_PONG to "PingPongFx", EffectType.REVERB to "ReverbFx", EffectType.TAPE_STOP to "TapeStopFx")) {
            val values = Regex("case InsertSlot::$name: return \\{([^}]*)\\}").find(cpp)!!.groupValues[1]
                .split(",").map { it.trim().removeSuffix("f").toFloat() }
            assertEquals(name, type.defaults, values)
        }
        // The pool sizes the app warns about.
        val pool = Regex("kEchoes = (\\d+), kReverbs = (\\d+), kTapeStops = (\\d+)").find(header)!!.groupValues
        assertEquals(pool[1].toInt(), EffectType.longLimit(EffectType.ECHO))
        assertEquals(pool[2].toInt(), EffectType.longLimit(EffectType.REVERB))
        assertEquals(pool[3].toInt(), EffectType.longLimit(EffectType.TAPE_STOP))
    }

    @Test
    fun everyEffectIsComplete() {
        for (t in EffectType.entries.drop(1)) {
            assertEquals(t.name, EffectType.PARAMS, t.defaults.size)
            assertTrue(t.name, t.params.size in 2..4)
            t.params.forEachIndexed { k, p -> assertTrue(t.name, p.format(t.defaults[k]).isNotBlank() && p.format(0f).isNotBlank() && p.format(1f).isNotBlank()) }
        }
        val new = listOf(EffectType.DISTORTION, EffectType.ECHO, EffectType.PING_PONG, EffectType.REVERB, EffectType.AUTO_PAN, EffectType.OTT, EffectType.FREQ_SHIFT, EffectType.TAPE_STOP)
        assertTrue(new.all { it.blurb.isNotBlank() })
        // Every family has something in it.
        EffectGroup.entries.forEach { g -> assertTrue(g.label, EffectType.entries.any { it.group == g && it != EffectType.NONE }) }
        // The echo's note values: the dotted eighth by default.
        assertEquals("1/8 pointée", EffectType.ECHO.params[0].format(EffectType.ECHO.defaults[0]))
        assertEquals("1/8", EffectType.PING_PONG.params[0].format(EffectType.PING_PONG.defaults[0]))
        assertEquals("1/4", EffectType.AUTO_PAN.params[0].format(EffectType.AUTO_PAN.defaults[0]))
        assertEquals(200f, EffectType.shiftHz(0.5f + 0.5f * kotlin.math.sqrt(0.1f)), 0.1f)
    }

    @Test
    fun bypassIsSavedAndOldProjectsLoad() {
        val ins = Insert.of(EffectType.DISTORTION).copy(bypass = true)
        val p = Project(mixer = Mixer(channels = List(Mixer.CHANNELS) { if (it == 2) Channel(inserts = listOf(ins, Insert(), Insert(), Insert())) else Channel() }))
        val back = ProjectJson.decode(ProjectJson.encode(p))
        assertTrue(back.mixer.channels[2].inserts[0].bypass)
        assertEquals(0, back.mixer.channels[2].inserts[0].heardType)
        assertEquals(EffectType.DISTORTION.ordinal, Insert.of(EffectType.DISTORTION).heardType)
        // A project saved before the bypass existed: its effects play.
        val old = ProjectJson.encode(Project(mixer = Mixer(channels = List(Mixer.CHANNELS) { Channel(inserts = listOf(Insert.of(EffectType.EQ), Insert(), Insert(), Insert())) })))
            .replace(Regex(""",\s*"bypass"\s*:\s*false"""), "")
        assertFalse("really a file without the field", old.contains("bypass"))
        assertFalse(ProjectJson.decode(old).mixer.channels[0].inserts[0].bypass)
        // An empty slot is never "bypassed".
        assertFalse(Insert(0, EffectType.NONE.defaults, true).sanitized().bypass)
    }

    @Test
    fun newEffectsGoAfterTheOthers() {
        val e = Insert.of(EffectType.EQ)
        assertEquals(0, freeInsertSlot(List(4) { Insert() }))
        assertEquals(2, freeInsertSlot(listOf(e, e, Insert(), Insert())))
        assertEquals(1, freeInsertSlot(listOf(e, Insert(), Insert(), Insert())))
        // A hole before the last: the new one goes last, after the effect still there.
        assertEquals(3, freeInsertSlot(listOf(e, Insert(), e, Insert())))
        // Last slot taken: the first hole.
        assertEquals(1, freeInsertSlot(listOf(e, Insert(), e, e)))
        assertNull(freeInsertSlot(List(4) { e }))
    }

    @Test
    fun movingAnEffectTakesItsAutomation() {
        val dist = Insert.of(EffectType.DISTORTION)
        val echo = Insert.of(EffectType.ECHO)
        val lane = AutomationLane(AutoTarget(AutoTarget.INSERT_PARAM, 2, 0, 1))
        val other = AutomationLane(AutoTarget(AutoTarget.INSERT_PARAM, 5, 0, 1))
        val p = Project(
            mixer = Mixer(channels = List(Mixer.CHANNELS) { if (it == 2 || it == 5) Channel(inserts = listOf(dist, echo, Insert(), Insert())) else Channel() }),
            automation = listOf(lane, other),
        )
        val moved = p.withInsertsSwapped(2, 0, 1)
        assertEquals(listOf(echo, dist), moved.mixer.channels[2].inserts.take(2))
        assertEquals(1, moved.automation[0].target.slot)
        assertEquals(0, moved.automation[1].target.slot)  // another strip: untouched
        assertEquals(listOf(dist, echo), moved.mixer.channels[5].inserts.take(2))
        // The master too.
        val m = Project(mixer = Mixer(master = listOf(dist, echo, Insert(), Insert())))
        assertEquals(echo, m.withInsertsSwapped(Mixer.MASTER, 0, 1).mixer.master[0])
        assertEquals(p, p.withInsertsSwapped(2, 0, 9))
        // A MIDI knob learned on the distortion's drive keeps driving it once moved; others stay.
        val knob = MidiMapping(21, AutoTarget(AutoTarget.INSERT_PARAM, 2, 0, 0))
        val volume = MidiMapping(22, AutoTarget(AutoTarget.VOLUME, 2))
        val otherStrip = MidiMapping(23, AutoTarget(AutoTarget.INSERT_PARAM, 5, 1, 0))
        val learned = p.copy(midiMap = listOf(knob, volume, otherStrip)).withInsertsSwapped(2, 0, 1)
        assertEquals(listOf(knob.copy(target = knob.target.copy(slot = 1)), volume, otherStrip), learned.midiMap)
    }

    @Test
    fun theLongEffectLimitCountsEveryWayOneComesOn() {
        val echo = Insert.of(EffectType.ECHO)
        val limit = EffectType.longLimit(EffectType.ECHO)
        fun mixer(echoes: Int, offAtTheEnd: Int = 0) = Mixer(
            channels = List(Mixer.CHANNELS) { ch ->
                Channel(inserts = List(Mixer.INSERTS) { slot ->
                    val i = ch * Mixer.INSERTS + slot
                    when {
                        i < echoes -> echo
                        i < echoes + offAtTheEnd -> echo.copy(bypass = true)
                        else -> Insert()
                    }
                })
            },
        )
        assertNull(mixer(limit).longFxOverLimit())
        assertEquals(EffectType.ECHO, mixer(limit + 1).longFxOverLimit())
        // Refused only when an edit adds one past the limit: switching one on, not one off.
        assertEquals(EffectType.ECHO, mixer(limit + 1).longFxOverLimit(mixer(limit, 1)))
        assertNull(mixer(limit + 1).longFxOverLimit(mixer(limit + 2)))
        assertNull(mixer(limit, 5).longFxOverLimit(mixer(limit)))
        // The master counts too, and each pool on its own.
        assertEquals(EffectType.ECHO, mixer(limit).copy(master = listOf(Insert.of(EffectType.PING_PONG))).longFxOverLimit())
        assertNull(mixer(limit).copy(master = listOf(Insert.of(EffectType.REVERB))).longFxOverLimit())
        assertTrue(longFxLimitNotice(EffectType.TAPE_STOP).contains("${EffectType.longLimit(EffectType.TAPE_STOP)}"))
    }

    @Test
    fun longEffectsAreCountedPerPool() {
        val echo = Insert.of(EffectType.ECHO)
        val ping = Insert.of(EffectType.PING_PONG)
        val m = Mixer(channels = List(Mixer.CHANNELS) { if (it < 3) Channel(inserts = listOf(echo, ping.copy(bypass = it == 2), Insert(), Insert())) else Channel() })
        assertEquals(5, m.longFxCount(EffectType.ECHO))
        assertEquals(5, m.longFxCount(EffectType.PING_PONG))
        assertEquals(0, m.longFxCount(EffectType.REVERB))
    }

    @Test
    fun theRightVibesComeFirst() {
        for (kind in SoundKind.entries) {
            val list = FxPresets.suggestedFor(kind)
            assertTrue(kind.name, list.size >= 4 && list.toSet().size == list.size)
        }
        assertEquals("fat_bass", FxPresets.suggestedFor(SoundKind.BASS).first().id)
        assertEquals("scream_lead", FxPresets.suggestedFor(SoundKind.LEAD).first().id)
        assertEquals("wide_alive", FxPresets.suggestedFor(SoundKind.PAD).first().id)
        assertEquals("pluck_pingpong", FxPresets.suggestedFor(SoundKind.PLUCK).first().id)
        // The new vibes use the new effects.
        val used = FxPresets.ALL.flatMap { p -> p.inserts.map { it.effect } }.toSet()
        assertTrue(used.containsAll(listOf(EffectType.DISTORTION, EffectType.ECHO, EffectType.PING_PONG, EffectType.REVERB, EffectType.AUTO_PAN, EffectType.OTT)))
    }

    @Test
    fun aTrackKnowsWhatKindOfSoundItPlays() {
        val bassSound = SoundLibrary.SYNTH.first { it.category == "Basses" }.sound
        assertEquals(SoundKind.BASS, InstrumentTrack(1, "Piste 2", synth = bassSound).soundKind())
        val padSound = SoundLibrary.PLAITS.first { it.category == "Nappes" }.sound
        assertEquals(SoundKind.PAD, InstrumentTrack(1, "Piste 2", engine = 1, plaits = padSound).soundKind())
        assertEquals(SoundKind.PAD, InstrumentTrack(1, "Nappe / accords", engine = 2).soundKind())
        assertEquals(SoundKind.BASS, InstrumentTrack(1, "Ma 808", engine = 2).soundKind())
        assertEquals(SoundKind.KEYS, InstrumentTrack(1, "Piste 3", instrument = InstrumentChoice(0, 0, "Piano")).soundKind())
        assertEquals(SoundKind.BASS, InstrumentTrack(1, "Piste 3", instrument = InstrumentChoice(0, 33, "Bass")).soundKind())
        assertEquals(SoundKind.LEAD, InstrumentTrack(1, "Piste 3", engine = 2).soundKind())
        val p = Project()
        assertEquals(SoundKind.DRUMS, p.soundKindOf(0))
        assertEquals(SoundKind.DRUMS, p.soundKindOf(Mixer.padChannel(3)))
        assertEquals(SoundKind.ACID, p.soundKindOf(1))
        assertEquals(SoundKind.AUDIO, p.soundKindOf(3))
    }
}
