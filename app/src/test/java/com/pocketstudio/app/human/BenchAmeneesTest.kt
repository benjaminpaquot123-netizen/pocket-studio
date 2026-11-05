package com.pocketstudio.app.human

import com.pocketstudio.app.settings.SettingsStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * The scripted starts of the three most frequent missions (Audit ultra §3.1, « amenées », lot P0-A):
 * from the app as just installed to the mission's starting point, as a seat gets it.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class BenchAmeneesTest : HumanTester("amenees") {
    @Before
    fun notOnTheBench() = assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())

    private fun bring(name: String, o: SeatOptions): List<String> {
        begin(o.copy(amenee = name))
        val journal = File.createTempFile("amenee", ".txt").apply { deleteOnExit() }
        amenee(name, journal)
        return journal.readLines()
    }

    @Test
    fun firstSongStartsInAnEmptyProjectInSimpleMode() {
        val j = bring(AMENEE_SONG, SeatOptions(simple = true))
        assertTrue(j.joinToString("\n"), j.last().startsWith("#0 AMENEE prête"))
        assertTrue("Mode simple", SettingsStore.state.value.simpleMode)
        assertNotNull("a project is open", project())
        assertTrue(screenLines().joinToString("\n"), screenLines().any { it.startsWith("onglet") })
    }

    @Test
    fun remixStartsOnTheRemixPage() {
        val j = bring(AMENEE_REMIX, SeatOptions())
        assertTrue(j.joinToString("\n"), j.last().startsWith("#0 AMENEE prête"))
        assertNotNull("« Choisir une chanson »", centerOf("remix_pick"))
    }

    @Test
    fun padLiveStartsWithItsPadsReady() {
        val j = bring(AMENEE_PADLIVE, SeatOptions())
        assertTrue(j.joinToString("\n"), j.last().startsWith("#0 AMENEE prête"))
        assertNotNull(centerOf("padlive"))
        assertNotNull("the ready-to-play pads", project()?.live?.at(4))
        assertEquals(null, centerOf("padlive_busy"))
        // Pad Live covers the project: a person sees its pads, not the drum pads under it.
        val lines = screenLines()
        assertEquals("padlive", screenRoute().substringBefore('/'))
        assertTrue(lines.joinToString("\n"), lines.any { "Klaxon" in it } && lines.none { "Tom médium" in it })
    }

    /**
     * Audit ultra tour 0 (seat c): on a phone the engine lights a pad that plays, ■ silences the pads, the
     * performance take starts on its line and « Enregistrer ce que je joue » records; the bench had none of it
     * (« Prêt » for good, « Pas assez de mémoire », a dark pad, loops going on after ■).
     */
    @Test
    fun padLiveLightsStopsAndRecordsAsOnAPhone() {
        padsAsOnASeat = true
        bring(AMENEE_PADLIVE, SeatOptions())
        val vm = rhythm()!!
        fun tap(tag: String): String = act("touche ${centerOf(tag)!!.first} ${centerOf(tag)!!.second}")
        fun waitFor(what: String, done: () -> Boolean) {
            val until = System.currentTimeMillis() + 60_000
            while (!done()) {
                assertTrue("$what never came", System.currentTimeMillis() < until)
                act("attends 0.5")
            }
        }
        val loop = (0 until 16).first { project()!!.live.at(it)?.oneShot == false }
        val heard = tap("padlive_pad_$loop")
        assertTrue(heard, "joue en boucle" in heard)
        assertEquals("the pad that plays is lit", 2, vm.liveStates.value[loop])
        tap("padlive_play")
        assertTrue(vm.transport.value.playing)
        val stopped = tap("padlive_play")
        assertTrue("■ silences the pads: $stopped", "pad live" !in stopped && "pads live" !in stopped)
        assertEquals(0, vm.liveStates.value[loop])

        // « Enregistrer la performance »: armed, then recording on its line, then written into the song.
        tap("live_record")
        tap("padlive_pad_$loop")
        waitFor("the take recording") { vm.take.value is com.pocketstudio.app.live.TakeUi.Recording }
        act("attends 2")
        tap("live_record")
        waitFor("the take written") { vm.take.value is com.pocketstudio.app.live.TakeUi.Done }
        assertTrue("the song got the take: ${project()!!.song.size}", project()!!.song.isNotEmpty())
        if (vm.transport.value.playing) tap("padlive_play")

        // « Enregistrer ce que je joue »: a pad on, recorded, ended on a bar line.
        tap("padlive_pad_$loop")
        tap("padlive_played")
        assertEquals(null, vm.padLive.played.message.value)
        waitFor("the played take recording") { vm.padLive.played.ui.value is com.pocketstudio.app.padlive.PlayedTakeUi.Recording }
        act("attends 2")
        tap("padlive_played")
        waitFor("the played take done") { vm.padLive.played.ui.value is com.pocketstudio.app.padlive.PlayedTakeUi.Done }
    }

    /** Audit ultra tour 0 (seat c): « Pack… › Techno » on page B left it « vide » (the phone's engine makes the pack). */
    @Test
    fun aPackFillsThePageShown() {
        padsAsOnASeat = true
        bring(AMENEE_PADLIVE, SeatOptions())
        // The ready pads are made on entering (padLive.busy): the pack's chip waits for them; each tag waited for (CI 242: an NPE).
        fun tap(tag: String) {
            val until = System.currentTimeMillis() + 60_000
            while (rhythm()!!.padLive.busy.value != null || centerOf(tag) == null) {
                if (System.currentTimeMillis() > until) error("$tag absent after 60 s (busy ${rhythm()!!.padLive.busy.value}):\n" + screenLines().joinToString("\n"))
                act("attends 0.5")
            }
            val (x, y) = centerOf(tag) ?: error("$tag absent:\n" + screenLines().joinToString("\n"))
            act("touche $x $y")
        }
        tap("padlive_page_B")
        tap("padlive_pack")
        tap("padlive_pack_techno")
        val until = System.currentTimeMillis() + 60_000
        while (rhythm()!!.padLive.busy.value != null) {
            assertTrue("the pack never came", System.currentTimeMillis() < until)
            act("attends 0.5")
        }
        act("attends 0.5")
        val pageB = (0 until com.pocketstudio.model.PadLive.PAGE_SIZE).mapNotNull { project()!!.live.at(com.pocketstudio.model.PadLive.pad(1, it)) }
        assertTrue("page B is empty", pageB.size >= 4)
        assertEquals("techno", project()!!.padLive.packOf(1))
        assertTrue(screenLines().joinToString("\n"), screenLines().none { "Page B · vide" in it })
        assertEquals("the bench let its engine go once the pack was made", null, com.pocketstudio.engine.NativeEngine.sink)
    }
}
