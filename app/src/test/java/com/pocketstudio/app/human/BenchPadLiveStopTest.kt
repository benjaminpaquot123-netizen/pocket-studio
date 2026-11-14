package com.pocketstudio.app.human

import com.pocketstudio.app.ui.DoubleTouchGuard
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Plan of the audits, step 4, n° 7 (« Pad Live »; AUDIT-PRISE-EN-MAIN G8, Benjamin's 4A; T5-Ed-11, recette tel-08): Pad
 * Live's « Tout couper » was behind « ··· Plus », and it cut the pads but left the music, the bars and the time running.
 * It is now in sight as Pad Live opens, and it stops everything: the pads, the song, the tempo's count.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class BenchPadLiveStopTest : HumanTester("padlivestop") {
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
    fun padLivesStopAllIsInSightAndStopsTheTempoToo() {
        padsAsOnASeat = true
        begin()
        tap("launch_padlive")
        waitFor("Pad Live") { centerOf("padlive") != null }
        standInForTheStarter()
        waitFor("the pads ready to play") { centerOf("padlive_busy") == null && project()?.live?.at(4) != null }
        val vm = rhythm()!!
        assertTrue("« Tout couper » is in sight without « ··· Plus »\n" + screenLines().joinToString("\n"), centerOf("padlive_stop_all") != null)
        tap("padlive_play")
        waitFor("the music playing") { vm.transport.value.playing }
        tap("padlive_pad_0")
        act("attends 1")
        val said = tap("padlive_stop_all")
        assertTrue("« Tout couper » went to the engine", "toggleClip -1" in benchEngine.padCommandsHeard())
        assertFalse("« Tout couper » stops the music and the tempo's count too (4A): $said", vm.transport.value.playing)
        val states = IntArray(com.pocketstudio.engine.NativeEngine.LIVE_PADS).also(com.pocketstudio.engine.NativeEngine::clipStatesNow)
        assertTrue("no pad plays or waits: ${states.toList()}", states.all { it == 0 })
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }
}
