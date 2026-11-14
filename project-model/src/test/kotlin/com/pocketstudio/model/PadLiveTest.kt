package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pad Live (n° 25, lot B1): pages A to D, modes, « un seul à la fois » groups, packs, saved and read back. */
class PadLiveTest {
    private fun clip(n: Int, oneShot: Boolean = false, group: Int = 0) = LiveClip("s$n.wav", "Son $n", 4, oneShot, group = group)

    @Test
    fun fourPagesOfSixteenPadsPageAStaysTheOlderScreensPads() {
        var pads = LivePads()
        pads = pads.withAt(PadLive.pad(0, 3), clip(1)).withAt(PadLive.pad(3, 15), clip(2))
        assertEquals(LivePads.ALL, pads.all.size)
        assertEquals("s1.wav", pads.clips[3]?.file)
        assertEquals(LivePads.MAX, pads.clips.size)  // page A: the older screens see 16 pads, no more
        assertEquals("s2.wav", pads.at(63)?.file)
        assertEquals(1, PadLive.filled(pads, 3))
        assertEquals(3, PadLive.pageOf(63))
        // The first free pad the older screens (Remix « vers un pad ») look for stays on page A.
        assertEquals(0, pads.clips.indexOfFirst { it == null })
        // Out of range: nothing.
        assertEquals(pads, pads.withAt(64, clip(9)))
        // Emptied again: pages B to D leave the file.
        assertTrue(pads.withAt(63, null).more.isEmpty())
    }

    @Test
    fun pagesBToDAreSavedReadBackAndTheirSoundsKept() {
        val p = PadLive.newProject().let { it.copy(live = it.live.withAt(PadLive.pad(1, 0), clip(5, group = 2)).withAt(PadLive.pad(2, 4), PadLive.withMode(clip(6), PadMode.HOLD))) }
        val back = ProjectJson.decode(ProjectJson.encode(p))
        assertEquals(p.live.all, back.live.all)
        assertTrue(back.padLive.home)
        assertEquals(PadLive.PROJECT_NAME, back.name)
        assertEquals(PadMode.HOLD, PadLive.modeOf(back.live.at(PadLive.pad(2, 4))!!))
        assertEquals(2, back.live.at(16)?.group)
        assertTrue("s5.wav" in back.sampleFiles && "s6.wav" in back.sampleFiles)
        // A sound missing from an imported .zip: its pad on page B empties too.
        assertNull(back.withoutSounds(setOf("s5.wav")).live.at(16))
        // An older project's file: no Pad Live field at all, read as before.
        val old = ProjectJson.decode(ProjectJson.encode(Project()))
        assertFalse(old.padLive.home)
        assertFalse("padLive" in ProjectJson.encode(Project()))
        assertFalse("\"more\"" in ProjectJson.encode(Project()))
    }

    @Test
    fun modesSayWhatATouchDoes() {
        val loop = clip(1)
        val shot = PadLive.withMode(loop, PadMode.SHOT)
        val hold = PadLive.withMode(loop, PadMode.HOLD)
        assertEquals(PadMode.LOOP, PadLive.modeOf(loop))
        assertEquals(PadMode.SHOT, PadLive.modeOf(shot))
        assertEquals(PadMode.HOLD, PadLive.modeOf(hold))
        assertEquals(PadMode.LOOP, PadLive.modeOf(PadLive.withMode(shot, PadMode.LOOP)))
        assertEquals(PadLive.LOOP_BEATS, PadLive.withMode(shot, PadMode.LOOP).beats)
        // A loop or a hit: on the touch only. Held: on the touch, off when the finger leaves.
        assertEquals(PadTouch.TOGGLE, PadLive.touch(loop, down = true, sounding = true))
        assertEquals(PadTouch.NONE, PadLive.touch(loop, down = false, sounding = true))
        assertEquals(PadTouch.TOGGLE, PadLive.touch(hold, down = true, sounding = false))
        assertEquals(PadTouch.TOGGLE, PadLive.touch(hold, down = false, sounding = true))
        assertEquals(PadTouch.NONE, PadLive.touch(hold, down = false, sounding = false))  // ended before the finger left
        assertEquals(PadTouch.NONE, PadLive.touch(null, down = true, sounding = false))
        // A one-shot never holds (a file edited by hand).
        assertFalse(loop.copy(oneShot = true, hold = true).sanitized().hold)
    }

    @Test
    fun oneAtATimeGroupsStopTheOthersOnAnyPage() {
        var pads = LivePads()
        pads = pads.withAt(0, clip(0, group = 1)).withAt(1, clip(1, group = 1)).withAt(20, clip(2, group = 1)).withAt(2, clip(3, group = 2)).withAt(3, clip(4))
        assertEquals(listOf(1, 20), PadLive.choked(pads, 0, setOf(1, 2, 3, 20)))
        // Stopping a pad of the group stops nothing else; a pad without a group neither.
        assertEquals(emptyList<Int>(), PadLive.choked(pads, 0, setOf(0, 1)))
        assertEquals(emptyList<Int>(), PadLive.choked(pads, 3, setOf(0, 1)))
        assertEquals(emptyList<Int>(), PadLive.choked(pads, 2, setOf(0, 1)))
        assertEquals(PadLive.GROUPS, clip(1, group = 99).sanitized().group)
    }

    @Test
    fun packsByStyleFillAPageAndAreAllMadeOfSoundsThatExist() {
        assertEquals("fete", PadLive.DEFAULT_PACK.id)
        val names = PadLive.PACKS.map { it.name }
        assertTrue(names.containsAll(listOf("Fête", "Techno", "Trap")))
        PadLive.PACKS.forEach { pack ->
            pack.style?.let { assertEquals("${pack.id}: its style's name", StarterWizard.style(it)?.name, pack.name) }
            assertTrue(pack.hits.size <= PadLive.PAGE_SIZE - PadLive.HITS_FROM)
            pack.hits.forEach {
                assertTrue("${pack.id}: no party sound ${it.party}", PartySounds.byId(it.party) != null)
                assertTrue("${pack.id}: « ${it.name} » is not shown translated", DefaultNames.isDefault(it.name))
            }
        }
        val hit = PadLive.hitClip(PadLive.pack("trap")!!.hits.last(), "siren.wav")
        assertEquals(PadMode.HOLD, PadLive.modeOf(hit))
        // Placed on page B: page A untouched, page B as given.
        val pads = PadLive.placed(LivePads().withAt(0, clip(0)), 1, listOf(clip(7), null, hit))
        assertEquals("s0.wav", pads.at(0)?.file)
        assertEquals("s7.wav", pads.at(16)?.file)
        assertEquals("siren.wav", pads.at(18)?.file)
        assertEquals(2, PadLive.filled(pads, 1))
        // Page A placed: the 16 pads shown, the ready-to-play set never offered again over them.
        val a = PadLive.placed(LivePads(), 0, listOf(clip(1)))
        assertEquals(LivePads.MAX, a.size)
        assertTrue(a.starterOffered)
    }

    // --- Lot B2 -------------------------------------------------------------------------------

    @Test
    fun aRowStartsTogetherOrStopsItsLoops() {
        val pads = LivePads()
            .withAt(PadLive.pad(1, 4), LiveClip("a.wav", "Basse", 4))
            .withAt(PadLive.pad(1, 5), LiveClip("b.wav", "Clap", 1, oneShot = true))
            .withAt(PadLive.pad(1, 7), LiveClip("c.wav", "Nappe", 8))
        val first = PadLive.pad(1, 4)
        // Nothing plays: every pad with a sound starts (the empty one is left out).
        assertEquals(PadLive.RowLaunch(listOf(20, 21, 23), emptyList()), PadLive.rowLaunch(pads, first, emptySet(), emptySet()))
        // One loop plays: it goes on, the others start (the hit too).
        assertEquals(PadLive.RowLaunch(listOf(21, 23), emptyList()), PadLive.rowLaunch(pads, first, setOf(20), emptySet()))
        // A loop waits: it moves to the bar line with them.
        assertEquals(PadLive.RowLaunch(listOf(20, 21, 23), emptyList()), PadLive.rowLaunch(pads, first, emptySet(), setOf(20)))
        // Its loops all on (playing or waiting): they stop, the hit is left alone.
        assertEquals(PadLive.RowLaunch(emptyList(), listOf(20, 23)), PadLive.rowLaunch(pads, first, setOf(20), setOf(23)))
        // A row of hits only: they hit again on the bar, each time.
        val hits = LivePads().withAt(0, LiveClip("h.wav", "Hey", 1, oneShot = true))
        assertEquals(PadLive.RowLaunch(listOf(0), emptyList()), PadLive.rowLaunch(hits, 0, setOf(0), emptySet()))
        // An empty row: nothing.
        assertEquals(PadLive.RowLaunch(emptyList(), emptyList()), PadLive.rowLaunch(pads, 0, emptySet(), emptySet()))
    }

    @Test
    fun aPadsOwnSoundIsSavedHeldToItsRangeAndLeftOutAtRest() {
        val clip = LiveClip("a.wav", "Basse", 4, pitch = -5, reverse = true, attack = 0.2f, release = 1.5f)
        val p = Project().copy(live = LivePads().withAt(PadLive.pad(2, 3), clip))
        val back = ProjectJson.decode(ProjectJson.encode(p))
        assertEquals(clip, back.live.at(PadLive.pad(2, 3)))
        // At rest, none of them is written (older files read the same, no new format number).
        val plain = ProjectJson.encode(Project().copy(live = LivePads().withAt(0, LiveClip("b.wav", "Kick", 1))))
        val written = kotlinx.serialization.json.Json.parseToJsonElement(plain).let { root ->
            (root as kotlinx.serialization.json.JsonObject)["live"].let { it as kotlinx.serialization.json.JsonObject }["clips"]
                .let { it as kotlinx.serialization.json.JsonArray }[0] as kotlinx.serialization.json.JsonObject
        }
        for (field in listOf("pitch", "reverse", "attack", "release")) assertFalse(field, field in written.keys)
        // A hand-edited file: held to the ranges.
        val wild = LiveClip("c.wav", "X", 1, pitch = 40, attack = Float.NaN, release = 99f).sanitized()
        assertEquals(LiveClip.PITCH_RANGE, wild.pitch)
        assertEquals(0f, wild.attack, 0f)
        assertEquals(LiveClip.MAX_RELEASE, wild.release, 0f)
    }

    /** Fix 2, n° 6: a pack per page; an older file's single pack stays page A's; the file only grows when used. */
    @Test
    fun eachPageKeepsItsOwnPack() {
        val trapOnA = PadLiveSet().withPack(0, "trap")
        assertEquals("trap", trapOnA.packOf(0))
        assertEquals("", trapOnA.packOf(1))
        val houseOnB = trapOnA.withPack(1, "house")
        assertEquals("trap", houseOnB.packOf(0))
        assertEquals("house", houseOnB.packOf(1))
        assertEquals("", houseOnB.packOf(3))
        assertEquals(houseOnB, ProjectJson.decode(ProjectJson.encode(Project(padLive = houseOnB))).padLive)
        // An older file: « pack » alone is page A's.
        val old = PadLiveSet(pack = "trap")
        assertEquals("trap", old.packOf(0))
        assertEquals("", old.packOf(1))
        assertEquals("trap", old.withPack(1, "house").packOf(0))
        assertEquals("house", old.withPack(0, "house").packOf(0))
        // Never used: not in the file.
        assertFalse(ProjectJson.encode(Project(padLive = PadLiveSet(page = 1))).contains("packs"))
        assertEquals(PadLiveSet(), PadLiveSet().withPack(7, "x"))
    }
}
