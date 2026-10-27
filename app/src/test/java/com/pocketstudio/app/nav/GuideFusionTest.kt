package com.pocketstudio.app.nav

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.Project
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.guide.GuideStart
import com.pocketstudio.model.guide.GuideStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * G5 (docs/PLAN-GUIDE.md §5): one guidance surface instead of four. A song just made opens the Guide
 * on « Ton morceau est prêt » (Écouter 8 bars before the Drop, Depuis le début, its guide); « Mon
 * premier morceau » is the Guide's first cards (the old Missions); the « Prochaine étape » strip and the
 * Missions' card are gone; a user mid-mission lands on the step reached; the Mode simple keeps it simple.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp")
class GuideFusionTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @After
    fun back() = h.finish()

    private fun house(): Project {
        val style = StarterWizard.style("house")!!
        return StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? }, style.melody.mood,
            StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() })
    }

    /** As the app makes it: the assistant's song, started in the Guide ([mode]), its guide kept with it. */
    private fun made(mode: String = GuideStart.SONG) = GuideStart.begin(house().copy(name = "House du soir"), mode,
        listOf(com.pocketstudio.model.guide.GuidePageText("Ton morceau House est prêt", listOf("Intro, Groove, Drop."))))

    private fun await(what: String, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + 60_000
        while (!ok()) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
            compose.waitForIdle()
            check(System.currentTimeMillis() < end) { "$what: not in time" }
            Thread.sleep(5)
        }
        compose.waitForIdle()
    }

    private fun shown(tag: String) = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    private fun tap(tag: String) {
        val node = compose.onNodeWithTag(tag)
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
    }

    private fun status(card: String) = h.vm.project.value.guide.statusOf(card)

    /** None of the old surfaces is on screen. */
    private fun noOldSurface() {
        listOf("next_step_strip", "next_steps", "mission_overlay", "mission_card", "song_ready", "song_ready_bar").forEach { assertFalse(it, shown(it)) }
    }

    @Test
    fun aNewSongOpensTheGuideOnItsReadyCard() {
        h.start { session.edit { made() } }
        h.vm.playStartsForTest = ArrayList()
        await("ready card") { shown("guide_card_start.ready") }
        noOldSurface()
        compose.onNodeWithTag("guide_panel").assertIsDisplayed()
        compose.onNodeWithText("Ton morceau est prêt !").assertIsDisplayed()
        compose.onNodeWithText("House du soir · ", substring = true).assertIsDisplayed()
        // « Depuis le début » is there (« Écouter » starts later, before the Drop), and the song's guide.
        compose.onNodeWithTag("guide_option_start").assertExists()
        tap("guide_open_guide")
        await("song guide") { shown("song_guide") }
        compose.onNodeWithText("Ton morceau House est prêt").assertExists()
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        // « Écouter »: about 8 bars before the first Drop; the card is decided, the plan goes on.
        tap("guide_option_drop")
        val start = com.pocketstudio.model.FirstListen.startPart(h.vm.project.value)
        assertTrue(start > 0)
        assertEquals(listOf("listen $start"), h.vm.playStartsForTest)
        await("decided") { status("start.ready") == GuideStatus.DONE }
        await("next card") { !shown("guide_card_start.ready") }
        // Only once: the next visit does not open the panel by itself again.
        assertFalse(GuideStart.welcomes(h.vm.project.value))
    }

    @Test
    fun aFirstSongFollowsTheMissionsAsGuideCards() {
        h.start(simple = true) { session.edit { made(GuideStart.FIRST) } }
        await("ready card") { shown("guide_card_start.ready") }
        noOldSurface()
        // « Pour commencer », in the Mode simple's plan, and the cards of the old Missions.
        val ui = h.vm.guide.ui.value
        assertEquals(listOf("start.ready", "start.sound", "start.melody", "start.arrange"), ui.plan.first().items.map { it.id })
        assertTrue(ui.plan.all { it.stage in com.pocketstudio.model.guide.GuideStages.SIMPLE })
        tap("guide_option_start")
        await("listened") { status("start.ready") == GuideStatus.DONE }
        // « Change un son »: the melody's sounds, in one touch; the card ticks itself once one is picked.
        await("sound card") { shown("guide_card_start.sound") }
        tap("guide_open_melody")
        await("chooser") { shown("sound_chooser") }
        val lead = com.pocketstudio.app.guide.GuideNav.mainTrackOf(h.vm.project.value)!!
        h.vm.session.edit { p -> p.copy(tracks = p.tracks.map { if (it.id == lead) it.copy(synth = it.synth.with(0, (it.synth.get(0) + 0.3f) % 1f)) else it }) }
        await("sound changed") { h.vm.guide.ui.value.cards["start.sound"]?.status == GuideStatus.DONE_ELSEWHERE }
        // The Remix closes the plan (the Missions' last card said it).
        assertTrue(h.vm.guide.ui.value.plan.last().items.last().id == "share.remix")
    }

    @Test
    fun monPremierMorceauFromTheLaunchStartsTheGuideNotTheMissions() {
        h.start(launchChoice = true)
        h.awaitTag("launch_missions")
        tap("launch_missions")
        assertFalse(shown("launch"))
        assertEquals(GuideStart.FIRST, h.vm.project.value.guide.start)
        await("panel") { shown("guide_panel") }
        noOldSurface()
        // A song made by hand with three parts: « Choisis un style » is done there, its first steps follow.
        await("analysis") { h.vm.guide.ui.value.cards["start.style"] != null }
        assertEquals(GuideStatus.DONE_ELSEWHERE, h.vm.guide.ui.value.cards.getValue("start.style").status)
    }

    @Test
    fun aSongMadeByHandHasItsFirstStepsInTheGuideAndNoStrip() {
        h.start()
        await("analysis") { !h.vm.guide.ui.value.loading }
        noOldSurface()
        compose.onNodeWithTag("guide_bar").assertIsDisplayed()
        // The old « Prochaine étape »: a beat and a bass line are there (ticked), the melody is next (the Lead has no notes).
        val ui = h.vm.guide.ui.value
        assertEquals(GuideStatus.DONE_ELSEWHERE, ui.cards.getValue("start.beat").status)
        assertEquals(GuideStatus.DONE_ELSEWHERE, ui.cards.getValue("start.bassline").status)
        assertEquals("start.tune", ui.next!!.id)
        compose.onNodeWithTag("guide_bar_step", useUnmergedTree = true).assertTextContains("Ajoute une mélodie", substring = true)
        // Its word, heard before deciding: « Lead » and its example.
        tap("guide_bar")
        compose.onNodeWithTag("guide_word").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("guide_word_listen").assertIsDisplayed()
        compose.onNodeWithText("Le mot : Lead").assertIsDisplayed()
    }

    @Test
    fun aUserMidMissionLandsOnTheStepReached() {
        h.start {
            session.edit { house() }
            // The old settings: Missions on, at « Change un son » (Style and Listen done).
            h.app.getSharedPreferences("missions", 0).edit().putBoolean("active", true).putInt("step", 2).commit()
        }
        await("sound card") { shown("guide_card_start.sound") }
        noOldSurface()
        assertEquals(GuideStart.FIRST, h.vm.project.value.guide.start)
        assertEquals(GuideStatus.DONE, status("start.ready"))
        assertTrue(h.app.getSharedPreferences("missions", 0).all.isEmpty())
    }

    @Test
    fun theModeSimpleStaysSimple() {
        h.start(simple = true) { session.edit { made() } }
        await("ready card") { shown("guide_card_start.ready") }
        noOldSurface()
        // The Mode simple's four tabs stay, the Guide over them takes its own room.
        compose.onNodeWithTag("tab_simple_Morceau").assertIsDisplayed()
        val ui = h.vm.guide.ui.value
        assertTrue(ui.plan.all { it.stage in com.pocketstudio.model.guide.GuideStages.SIMPLE })
        assertTrue(ui.further.isNotEmpty())
        // Folded in one touch.
        tap("guide_bar")
        assertFalse(shown("guide_panel"))
    }
}
