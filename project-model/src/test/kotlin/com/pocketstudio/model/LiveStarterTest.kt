package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The ready-to-play live pads and the party sounds (Benjamin's list n° 12). */
class LiveStarterTest {
    @Test
    fun eightPadsLoopsAndPartyHits() {
        assertEquals(8, LiveStarter.PADS.size)
        val sources = LiveStarter.PADS.map { it.source }
        assertEquals(listOf(LiveStarter.Source.KickLoop, LiveStarter.Source.Clap, LiveStarter.Source.AcidLoop, LiveStarter.Source.BassLoop), sources.take(4))
        // The loops loop, the hits play once; every party hit is a sound of the app.
        LiveStarter.PADS.forEach { pad ->
            assertEquals(pad.name, pad.source in setOf(LiveStarter.Source.KickLoop, LiveStarter.Source.AcidLoop, LiveStarter.Source.BassLoop), !pad.oneShot)
            if (pad.source == LiveStarter.Source.Party) assertNotNull(pad.name, pad.party?.let(PartySounds::byId))
        }
        val party = LiveStarter.PADS.mapNotNull { it.party?.let(PartySounds::byId) }
        assertTrue(party.any { it.group == PartySounds.HORNS })
        assertTrue(party.any { it.group == PartySounds.LASERS })
        assertTrue(party.any { it.group == PartySounds.SIRENS || it.group == PartySounds.RISERS })
        assertTrue(party.any { it.group == PartySounds.VOICES })
        // Its style has a 303 line and a kick.
        val style = StarterWizard.style(LiveStarter.STYLE)!!
        assertNotNull(style.melody.acid)
        assertTrue(style.lines.any { it.role == StarterWizard.Role.Kick })
    }

    @Test
    fun offeredOnceToAnEmptyGridOnly() {
        val empty = Project()
        assertTrue(LiveStarter.wanted(empty))
        assertFalse("a song of the assistant fills its own", LiveStarter.wanted(empty.copy(livePadsPending = true)))
        val one = empty.copy(live = empty.live.with(3, LiveClip("a.wav", "Mon son")))
        assertFalse("a pad already set", LiveStarter.wanted(one))

        val made = mapOf(0 to LiveStarter.Made("k.wav", 4), 2 to LiveStarter.Made("a.wav", 8), 4 to LiveStarter.Made("h.wav", 3))
        val filled = LiveStarter.applied(empty.live.with(4, LiveClip("mine.wav", "Mien")), made)
        assertTrue(filled.starterOffered)
        assertEquals("Boucle de kick", filled.clips[0]?.name)
        assertEquals(4, filled.clips[0]?.beats)
        assertFalse(filled.clips[0]!!.oneShot)
        assertEquals(8, filled.clips[2]?.beats)
        // A pad filled meanwhile keeps its sound; a sound that failed leaves its pad empty.
        assertEquals("Mien", filled.clips[4]?.name)
        assertEquals(null, filled.clips[1])
        assertFalse(LiveStarter.wanted(empty.copy(live = filled.with(0, null).with(2, null).with(4, null))))
    }

    @Test
    fun offeredIsSavedAndOldProjectsReadWithout() {
        val p = Project().let { it.copy(live = LiveStarter.applied(it.live, emptyMap())) }
        val back = ProjectJson.decode(ProjectJson.encode(p))
        assertTrue(back.live.starterOffered)
        // Not written while false: projects saved before read the same.
        assertFalse(ProjectJson.encode(Project()).contains("starterOffered"))
    }

    @Test
    fun everyPartySoundIsShippedAndMadeByTheScript() {
        val assets = File("../app/src/main/assets/party")
        val script = File("../scripts/party/synth_party.py").readText()
        assertEquals(PartySounds.ALL.size, PartySounds.ALL.map { it.id }.toSet().size)
        PartySounds.ALL.forEach { s ->
            val f = File(assets, "${s.id}.ogg")
            assertTrue("${s.id} shipped", f.isFile && f.length() in 1_000L..120_000L)
            assertTrue("${s.id} made by synth_party.py", "\"${s.id}\"" in script)
            assertTrue(s.kind in SoundPacks.KIND_LABELS)
        }
        // Nothing else in the folder (a sound left without its line in the model).
        assertEquals(PartySounds.ALL.map { "${it.id}.ogg" }.toSet(), assets.list()!!.toSet())
    }
}
