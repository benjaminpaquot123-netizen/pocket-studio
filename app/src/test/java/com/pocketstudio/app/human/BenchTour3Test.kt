package com.pocketstudio.app.human

import com.pocketstudio.app.ui.DoubleTouchGuard
import com.pocketstudio.model.PadLive
import com.pocketstudio.model.SongPart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Audit ultra round 3 (lot banc): what the testers saw do nothing, on the bench this time (the app's side:
 * Tour3AppProofTest, Tour3RenameIdleTest).
 * - Seat C b: Pad Live's row ▶ started nothing, its ■ and « Tout couper » stopped nothing (the bench's pads never
 *   heard the engine's row and stop-all commands).
 * - Seats A b, C a: a part touched, « Jouer depuis… », « ▶ Depuis ma prise » played nothing, then ▶ the part's loop
 *   (the bench had let its engine go before the touch).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class BenchTour3Test : HumanTester("tour3") {
    @Before
    fun notOnTheBench() {
        assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())
        DoubleTouchGuard.reset()
    }

    private fun tap(tag: String): String {
        waitFor("« $tag »") { centerOf(tag) != null }
        val at = reachTag(tag) ?: centerOf(tag)!!
        return act("touche ${at.first} ${at.second}")
    }

    private fun waitFor(what: String, done: () -> Boolean) {
        val until = System.currentTimeMillis() + 60_000
        while (!done()) {
            assertTrue("$what never came\n" + screenLines().joinToString("\n"), System.currentTimeMillis() < until)
            act("attends 0.5")
        }
    }

    @Test
    fun padLiveRowsStartAndStopAndStopAllCutsThem() {
        padsAsOnASeat = true
        begin()
        tap("launch_padlive")
        waitFor("Pad Live") { centerOf("padlive") != null }
        standInForTheStarter()
        waitFor("the pads ready to play") { centerOf("padlive_busy") == null && project()?.live?.at(4) != null }
        val vm = rhythm()!!
        // A row of page A with loops (the ready-to-play set has some).
        val row = (0 until PadLive.ROWS).first { r -> (0 until PadLive.ROW_SIZE).any { vm.project.value.live.at(r * PadLive.ROW_SIZE + it)?.oneShot == false } }
        val loops = (0 until PadLive.ROW_SIZE).map { row * PadLive.ROW_SIZE + it }.filter { vm.project.value.live.at(it)?.oneShot == false }
        // A.16 (G2): what the engine received, and what it plays (the bench's engine, as ClipLauncher): on the next bar line
        // if a pad already plays (the one Pad Live may start as it opens), at once otherwise.
        fun states() = IntArray(com.pocketstudio.engine.NativeEngine.LIVE_PADS).also(com.pocketstudio.engine.NativeEngine::clipStatesNow)
        val first = row * PadLive.ROW_SIZE
        benchEngine.padCommandsHeard()
        tap("padlive_row_$row")
        assertTrue("the row went to the engine", "launchClipRow $first ${PadLive.ROW_SIZE}" in benchEngine.padCommandsHeard())
        waitFor("the row's loops playing") { loops.all { states()[it] == 2 } }
        assertTrue("and are lit", loops.all { vm.liveStates.value[it] == 2 })
        // Its ■: they stop.
        tap("padlive_row_$row")
        assertTrue("the row's ■ went to the engine", "launchClipRow $first ${PadLive.ROW_SIZE}" in benchEngine.padCommandsHeard())
        assertTrue("the row's loops stopped: ${states().toList()}", loops.none { states()[it] != 0 })
        assertTrue(loops.all { vm.liveStates.value[it] == 0 })
        // Again, then « Plus » › « Tout couper »: nothing plays.
        tap("padlive_row_$row")
        waitFor("the row's loops playing again") { loops.all { states()[it] == 2 } }
        tap("padlive_more")
        tap("padlive_stop_all")
        assertTrue("« Tout couper » went to the engine", "toggleClip -1" in benchEngine.padCommandsHeard())
        assertTrue("nothing plays: ${states().toList()}", states().all { it == 0 })
        assertTrue(vm.liveStates.value.all { it == 0 })
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun theSongPlaysFromAPartAndFromItsStart() {
        begin()
        tap("welcome_empty")
        tap("mode_card_simple")
        waitFor("Rythme") { screenRoute().endsWith("tab_simple_Rythme") }
        rhythm()!!.session.edit { it.copy(song = listOf(SongPart(0), SongPart(0), SongPart(0))) }
        act("attends 1")
        // « Jouer depuis… » on top: part 2.
        tap("top_position")
        tap("play_from_1")
        val vm = rhythm()!!
        assertTrue("the song plays (${vm.transport.value.playing}, part ${vm.sessionUi.value.songIndex})", vm.transport.value.playing && vm.sessionUi.value.songIndex == 1)
        // Another gesture: the song goes on (never the part's loop alone).
        act("attends 1")
        assertTrue(vm.transport.value.playing && vm.sessionUi.value.songIndex >= 1)
        // « Tout le morceau, du début ».
        tap("top_position")
        tap("play_from_whole")
        assertTrue("the whole song (${vm.transport.value.playing}, part ${vm.sessionUi.value.songIndex})", vm.transport.value.playing && vm.sessionUi.value.songIndex == 0)
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }
}
