package com.pocketstudio.app.human

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
 * PLAN-RESTE R.1: Benjamin's device checklist (2026-11-08) found the app right where the bench reported a defect. Each
 * test replays the seat's route on the bench and checks the bench now sees what the phone does:
 * - tel-09 (T4-16, T5-Ab-14): Mode simple, « Mon morceau », the big ▶ plays the whole song, its counter following it;
 * - tel-13 (T5-Fb-73): Pro, while the song plays, the top part menu's other part takes over at the next bar;
 * - tel-16 (T5-Fc-31): the « Nouveau projet » list left with « Retour » stops the demo heard.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class BenchRecetteTest : HumanTester("recette") {
    @Before
    fun notOnTheBench() = assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())

    private fun tap(tag: String): String {
        val at = reachTag(tag) ?: centerOf(tag)
        assertNotNull("nothing to touch: $tag\n" + screenLines().joinToString("\n"), at)
        return act("touche ${at!!.first} ${at.second}")
    }

    private fun tapText(text: String) {
        val at = centerOfText(text, substring = true)
        assertNotNull("nothing to touch: « $text »\n" + screenLines().joinToString("\n"), at)
        act("touche ${at!!.first} ${at.second}")
    }

    private fun waitFor(what: String, seconds: Int = 60, done: () -> Boolean) {
        val until = System.currentTimeMillis() + seconds * 1_000L
        while (!done()) {
            assertTrue("$what never came\n" + screenLines().joinToString("\n"), System.currentTimeMillis() < until)
            act("attends 0.5")
        }
    }

    private fun playing() = rhythm()!!.transport.value.playing

    /** « mes. B.b / T » of the song's clock (« Mon morceau »), as beats from the song's start, and T; null when not shown. */
    private fun shownBeat(): Pair<Int, Int>? {
        val line = screenLines().firstOrNull { Regex("""mes\. \d+\.\d+ / \d+""").containsMatchIn(it) } ?: return null
        val m = Regex("""mes\. (\d+)\.(\d+) / (\d+)""").find(line)!!
        val perBar = project()!!.beatsPerBar
        return (m.groupValues[1].toInt() - 1) * perBar + m.groupValues[2].toInt() - 1 to m.groupValues[3].toInt()
    }

    /**
     * tel-09 (T4-16, T5-Ab-14): the big ▶ of « Mon morceau » plays the whole song; its clock says where the song is, the
     * engine's song position, part after part (round 5: stuck on « 0:12 / 0:13 · comp. 7.4 / 7 », past the end).
     */
    @Test
    fun simpleSongCounterFollowsTheEnginePastTheFirstPart() {
        begin()
        tapText("Créer mon premier morceau")
        tap("mode_card_simple")
        tap("wizard_family_Techno")
        tap("wizard_style_techno")
        tap("wizard_style_choose_techno")
        tap("wizard_create_now")
        waitFor("the song's tabs") { screenRoute().endsWith("tab_simple_Morceau") }
        val p = project()!!
        assertTrue("a song of several parts: ${p.song.size}", p.song.size >= 2)
        tap("thumb_play")
        assertTrue("▶ plays", playing())
        val vm = rhythm()!!
        waitFor("the song's second part", seconds = 120) { vm.sessionUi.value.songIndex >= 1 }
        act("attends 2")
        assertTrue("still playing", playing())
        reachTag("song_clock")
        val engineBeat = benchEngine.songStep() / 4
        val (beat, total) = shownBeat() ?: error("no song clock\n" + screenLines().joinToString("\n"))
        assertTrue("the counter's bar within the song: beat $beat of $total bars", beat < total * p.beatsPerBar)
        assertTrue(
            "the counter says where the engine's song is (part ${vm.sessionUi.value.songIndex}): beat $beat shown, $engineBeat played",
            kotlin.math.abs(beat - engineBeat) <= 1,
        )
    }

    /**
     * tel-13 (T5-Fb-73): Pro, the song playing, the top bar's part menu › B: B takes over at the next bar (the screens
     * follow it). Round 5: « reste sur A » (the bench's engine never swapped a scene).
     */
    @Test
    fun proPartPickedWhilePlayingTakesOver() {
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
        tap("tab_Sons")
        assertEquals("the song starts on A", 0, project()!!.scene)
        tap("top_play")
        assertTrue("▶ plays", playing())
        act("attends 1")
        val swaps = benchEngine.sceneSwaps()
        tap("top_scene")
        tap("scene_pick_1")
        act("attends 4")
        assertTrue("still playing", playing())
        assertTrue("the engine swapped the part in: ${benchEngine.sceneSwaps()} swaps (were $swaps)", benchEngine.sceneSwaps() > swaps)
        assertEquals("B is the part shown and heard\n" + screenLines().joinToString("\n"), 1, project()!!.scene)
    }

    private fun demoHeard(): Boolean = "un son de la bibliothèque joue" in act("attends 0.2")

    /** « Nouveau projet » › « Autres façons de commencer » › a demo's ▶ until it plays. */
    private fun demoPlaying() {
        begin(SeatOptions(simple = false))
        tap("launch_new")
        waitFor("« Nouveau projet »") { centerOf("new_project") != null }
        tap("start_others")
        tap("demo_play_0")
        waitFor("the demo heard") { demoHeard() }
    }

    /** tel-16 (T5-Fc-31), Benjamin's route: the list left with « Retour », the demo stops (as on his phone). */
    @Test
    fun theDemoStopsWhenTheNewProjectListIsLeft() {
        demoPlaying()
        tap("new_project_back")
        assertFalse("the demo stopped with the list left\n" + screenLines().joinToString("\n"), demoHeard())
    }

    /**
     * T5-Fc-31, the seat's own route (F c #29-#32): a demo playing, « Le beat de départ » touched; the page stays under the
     * « Simple ou Pro ? » step (NewProjectPage's pending), so its DisposableEffect has not run: the code keeps the demo
     * playing there and stops it once a mode is chosen (the page left). Not the bench: what the app's code does on that
     * route, which tel-16 (« Retour ») never took.
     */
    @Test
    fun theDemoGoesOnUnderTheModeStepUntilAModeIsChosen() {
        demoPlaying()
        tap("starter_starter")
        waitFor("« Simple ou Pro ? »") { centerOf("mode_card_simple") != null }
        assertTrue("the app's code keeps the demo under the mode step (T5-Fc-31 fixed in the app if this fails)", demoHeard())
        tap("mode_card_simple")
        assertFalse("the demo stops once the page is left\n" + screenLines().joinToString("\n"), demoHeard())
    }
}
