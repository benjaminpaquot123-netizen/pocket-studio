package com.pocketstudio.app.human

import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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

    /** Touches [tag] (brought into view first) and gives the bench's clock just before the touch (ms). */
    private fun touchAt(tag: String): Long {
        waitFor("« $tag »") { centerOf(tag) != null }
        val at = reachTag(tag) ?: centerOf(tag)!!
        val since = SystemClock.uptimeMillis()
        act("touche ${at.first} ${at.second}")
        return since
    }

    /**
     * PLAN-RESTE 5.7 (control 8): Pad Live's whole ready-to-play set (LiveStarter.PADS: its loops rendered by the engine, the
     * kit's hits, the party sounds) is on the pads after the opening touched at [since] (the bench's clock, the app's own
     * time), in under 10 s of that clock, at every way in; the time taken is measured and said. With the bench's engine
     * there from the start (audit method v2), the app makes the set itself, as a phone does: the engine must render it.
     */
    private fun padsReady(what: String, since: Long) {
        val set = com.pocketstudio.model.LiveStarter.PADS.indices
        fun filled() = rhythm()?.project?.value?.live?.let { live -> set.count { live.at(it) != null } } ?: 0
        waitFor("Pad Live's pads ($what)") { centerOf("padlive") != null && centerOf("padlive_busy") == null && rhythm()?.liveStarting?.value == false && filled() > 0 }
        val took = SystemClock.uptimeMillis() - since
        println("PADLIVE $what : ${filled()} pads sur ${set.count()} prêts $took ms après le toucher (horloge du banc)")
        assertEquals("$what: every pad of the ready-to-play set is filled (${filled()} of ${set.count()})", set.count(), filled())
        assertTrue("$what: the pads took $took ms of the bench's clock (10 s at most)", took < 10_000)
        alive("Pad Live ($what)")
    }

    /** The middle of the first text view showing one of [texts] in a window over the app (the bench's system dialogs), or null. */
    private fun viewText(vararg texts: String): Pair<Float, Float>? = runCatching {
        val screen = ApplicationProvider.getApplicationContext<android.app.Application>().resources.displayMetrics
        BenchWindows.windows()
            .filter { it.params.type != android.view.WindowManager.LayoutParams.TYPE_BASE_APPLICATION && it.view.visibility == android.view.View.VISIBLE }
            .firstNotNullOfOrNull { w ->
                val origin = w.drawn(screen)
                var found: Pair<Float, Float>? = null
                fun visit(v: android.view.View) {
                    if (found != null || v.visibility != android.view.View.VISIBLE) return
                    if (v is android.widget.TextView && v.text?.toString() in texts) {
                        val loc = IntArray(2).also { v.getLocationInWindow(it) }
                        found = (origin.left + loc[0] + v.width / 2f) to (origin.top + loc[1] + v.height / 2f)
                    }
                    if (v is android.view.ViewGroup) for (i in 0 until v.childCount) visit(v.getChildAt(i))
                }
                visit(w.view)
                found
            }
    }.getOrNull()

    /** `fresh=reel`: the system's permission question, if the app asked one, allowed as a person would. */
    private fun allowIfAsked() {
        viewText("Lorsque vous utilisez l'appli", "Autoriser", "While using the app", "Allow")?.let { act("touche ${it.first} ${it.second}") }
    }

    @Test
    fun padLiveTouchedFromTheWelcomeOpensOnItsPads() {
        begin(SeatOptions(device = "tablet11Land"))
        val since = touchAt("launch_padlive")
        padsReady("from « Que veux-tu faire ? » (seat A c, #1)", since)
    }

    /**
     * Control 8, after a song: a song made (the wizard) and played, left for the home screen (Retour), then Pad Live from
     * « Que veux-tu faire ? »: its own « Live » project opens on its pads, not the song's.
     */
    @Test
    fun padLiveOpenedAfterASongHasItsPads() {
        begin(SeatOptions(simple = false))
        tap("launch_new")
        tap("starter_wizard")
        if (centerOf("mode_card_pro") != null) tap("mode_card_pro")
        tap("wizard_family_Techno")
        tap("wizard_style_techno")
        tap("wizard_style_choose_techno")
        tap("wizard_create_now")
        waitFor("the song's spaces") { screenRoute().endsWith("tab_Structure") }
        centerOf("guide_later")?.let { act("touche ${it.first} ${it.second}") }
        val song = rhythm()!!.projectId
        tap("top_play")
        act("attends 1")
        tap("top_play")
        // Retour: the project, then the home screen (a panel open closes first).
        repeat(3) { if (centerOf("launch_padlive") == null) act("retour") }
        waitFor("« Que veux-tu faire ? » again") { centerOf("launch_padlive") != null }
        val since = touchAt("launch_padlive")
        padsReady("after a song", since)
        assertTrue("Pad Live's own « Live » project", rhythm()!!.project.value.padLive.home)
        assertNotEquals("not the song's project", song, rhythm()!!.projectId)
    }

    /** Control 8, `fresh=reel` (nothing set ahead, the welcome played): Pad Live's card on the welcome opens on its pads. */
    @Test
    fun padLiveOpenedOnAFreshPhoneHasItsPads() {
        begin(SeatOptions(fresh = true))
        waitFor("the welcome") { centerOf("welcome") != null }
        val since = touchAt("launch_padlive")
        allowIfAsked()
        padsReady("fresh=reel, from the welcome", since)
    }
}
