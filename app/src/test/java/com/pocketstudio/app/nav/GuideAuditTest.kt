package com.pocketstudio.app.nav

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.Project
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.guide.GuideLibrary
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
 * The final audit's fixes of the Guide in the whole app: Retour closes it (it no longer leaves the
 * app), the kick's « Autre idée de son » changes the kick only, « Voilà ce que j'ai mis » keeps its
 * choice in sight, the Clavier keeps the track the Guide picked, « Générer une basse acide » makes
 * one, « Mon premier morceau » starts a new song, and lying down the panel sits on the side.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp")
class GuideAuditTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @After
    fun back() = h.finish()

    private fun song(id: String): Project {
        val style = StarterWizard.style(id)!!
        return StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? }, style.melody.mood,
            StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() })
    }

    /** The style's sound cards already decided: the song goes on to its effects. */
    private fun soundsKept(p: Project): Project {
        val ids = GuideLibrary.forProject(p).cards.filter { it.stage == 1 }.map { it.id }
        return p.copy(guide = ids.fold(p.guide) { g, id -> g.withStatus(id, GuideStatus.DONE, System.currentTimeMillis(), "keep") })
    }

    private fun startOn(p: Project, simple: Boolean = false) {
        h.start(simple = simple) { session.edit { p } }
        await("first analysis") { !h.vm.guide.ui.value.loading && h.vm.guide.ui.value.next != null }
    }

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

    private fun pressBack() {
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    /** 20-design M1: Retour folds the Guide's panel and leaves the app open. */
    @Test
    fun backClosesTheGuideInsteadOfLeavingTheApp() {
        startOn(song("jump_up"))
        tap("guide_bar")
        await("panel") { shown("guide_panel") }
        pressBack()
        await("folded") { !shown("guide_panel") }
        compose.activityRule.scenario.onActivity { assertFalse(it.isFinishing) }
        compose.onNodeWithTag("guide_bar").assertIsDisplayed()
    }

    /** 01 M2: the kick's « Autre idée de son » opens the kick's pad, the rest of the kit stays. */
    @Test
    fun theKicksOtherSoundOpensItsPadNotTheKit() {
        startOn(song("house"))
        val lanes = h.vm.project.value.drums.lanes
        h.vm.guide.apply("sound.kick", "other")
        await("pad sheet") { shown("pad_sheet") }
        assertFalse(shown("kit_sheet"))
        assertEquals(lanes, h.vm.project.value.drums.lanes)
    }

    /** 20-design M10: « Voilà ce que j'ai mis » folds each track's effects, « Tout garder » stays in sight. */
    @Test
    fun whatIPutKeepsItsChoiceInSight() {
        startOn(soundsKept(song("jump_up")))
        tap("guide_bar")
        await("card") { shown("guide_card_fx.review") }
        compose.onNodeWithTag("guide_option_keep").assertIsDisplayed()
        val tracks = h.vm.guide.ui.value.next!!.effects.map { it.track }.distinct()
        assertTrue(tracks.size > 1)
        // Folded: no « Retirer » until a track is opened.
        assertFalse(shown("guide_fx_toggle_" + h.vm.guide.ui.value.next!!.effects.first().option))
        tap("guide_fx_track_${tracks.first()}")
        assertTrue(shown("guide_fx_toggle_" + h.vm.guide.ui.value.next!!.effects.first().option))
    }

    /** 05 #4: the Mode simple's Clavier, first visited from the Guide, keeps the track it picked (the bass). */
    @Test
    fun theClavierKeepsTheTrackTheGuidePicked() {
        startOn(song("lofi"), simple = true)
        val bass = com.pocketstudio.app.guide.GuideNav.trackOf(h.vm.project.value, "bass")!!.key.removePrefix("inst:").toInt()
        h.vm.guide.apply("arr.bass", "notes")
        await("on the keyboard") { h.vm.selectedTrack.value == bass }
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500))
        compose.waitForIdle()
        assertEquals(bass, h.vm.selectedTrack.value)
    }

    /** 08 #7: « Générer une basse acide » (a song made by hand, Mode simple) makes a 303 line, then offers its sounds. */
    @Test
    fun generateAnAcidBassMakesOne() {
        startOn(Project(name = "À la main").let { it.copy(drums = it.drums.toggle(0, 0).toggle(0, 8)) }, simple = true)
        assertFalse(com.pocketstudio.model.guide.GuideStart.bassline(h.vm.project.value))
        h.vm.guide.apply("start.bassline", "acid")
        await("acid sheet") { shown("acid_sound_sheet") }
        assertTrue(h.vm.project.value.acid.steps.any { it.gate })
    }

    /** 01 M8: « Mon premier morceau, avec le Guide » with a song open: the assistant, for a new one; the open song stays as it is. */
    @Test
    fun myFirstSongStartsANewSong() {
        startOn(song("house"))
        val before = h.vm.project.value.guide.start
        tap("tab_Plus")
        tap("plus_missions")
        // A new song: « Simple ou Pro ? » first (owner, 2026-10-29).
        h.awaitTag("mode_step")
        tap("mode_card_pro")
        h.awaitTag("wizard")
        assertEquals(before, h.vm.project.value.guide.start)
    }

    /** 20-design G1 / 09: lying down, the panel sits on the side, the screen keeps its height and its rail. */
    @Test
    @Config(qualifiers = "w890dp-h400dp-land")
    fun lyingDownThePanelSitsOnTheSide() {
        startOn(song("jump_up"))
        tap("guide_bar")
        await("panel") { shown("guide_panel") }
        val panel = compose.onNodeWithTag("guide_panel").fetchSemanticsNode().boundsInRoot
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue("$panel in $root", panel.width < root.width * 0.5f)
        assertTrue("$panel in $root", panel.height > root.height * 0.6f)
        compose.onNodeWithTag("tab_Mix").assertIsDisplayed()
    }
}
