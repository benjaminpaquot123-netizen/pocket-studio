package com.pocketstudio.app.human

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Audit ultra round 2, zone 1, n° 1 and 2 (seats A c and B d): the library's « Poser » left the pad as it was (the
 * bench let its engine go before the touch: the sound went to the native library Robolectric has not), and Pad Live
 * opened by the tester's own touch showed 16 empty pads (its ready-to-play set is made with the engine). A phone's
 * engine never goes (PlaceFromLibraryTest, PadLiveReadyEveryWayTest): on the bench too, a pad held, a sound « Posé »,
 * the pad has it; Pad Live touched from the welcome opens on its pads.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class BenchPlaceAndPadLiveTest : HumanTester("place-padlive") {
    @Before
    fun notOnTheBench() = assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())

    private fun tap(tag: String): String {
        waitFor("« $tag »") { centerOf(tag) != null }
        val at = centerOf(tag)!!
        return act("touche ${at.first} ${at.second}")
    }

    private fun waitFor(what: String, done: () -> Boolean) {
        val until = System.currentTimeMillis() + 60_000
        while (!done()) {
            assertTrue("$what never came\n" + screenLines().joinToString("\n"), System.currentTimeMillis() < until)
            act("attends 0.5")
        }
    }

    private fun alive(what: String) = assertTrue("$what: the screen's work failed\n${problems.joinToString("\n")}", problems.isEmpty())

    @Test
    fun aPadHeldThenPosedFromTheLibraryHasTheSound() {
        begin()
        tap("welcome_empty")
        tap("mode_card_simple")
        waitFor("Rythme") { screenRoute().endsWith("tab_simple_Rythme") }
        val pad = centerOf("pad_0")
        assertNotNull(pad)
        act("appui-long ${pad!!.first} ${pad.second}")
        waitFor("the library") { centerOf("library") != null }
        tap("library_folder_Sons intégrés")
        // Round 3, n° 33: « Sons intégrés » holds the family's own sounds only (two kicks): the second one, not on the pad yet.
        tap("library_sound_use_1")
        waitFor("the sound on the pad") { rhythm()?.project?.value?.drums?.lanes?.get(0)?.sample != null }
        alive("« Poser » (seat B d, #3)")
        assertTrue("« Annuler » takes it back", rhythm()!!.session.canUndo.value)
    }

    @Test
    fun padLiveTouchedFromTheWelcomeOpensOnItsPads() {
        begin(SeatOptions(device = "tablet11Land"))
        tap("launch_padlive")
        waitFor("Pad Live's pads (seat A c, #1)") { centerOf("padlive") != null && rhythm()?.project?.value?.live?.at(4) != null }
        alive("Pad Live")
    }
}
