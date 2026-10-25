package com.pocketstudio.app.nav

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.Project
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.guide.GuideActions
import com.pocketstudio.model.guide.GuideStatus
import com.pocketstudio.model.instrumentTracks
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
 * G4 in the whole app: one card of each of the stages 3 to 6, decided from the panel. « Voilà ce
 * que j'ai mis » lists the effects and retires one; a Break made from the Drop; the filter that opens
 * over the Montée (no ▶: a curve cannot be heard in the first Drop); the mono check (heard, said
 * when it cannot here) and its « Resserrer ».
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp")
class GuideStagesPanelTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @After
    fun back() = h.finish()

    private fun jumpUp(): Project {
        val style = StarterWizard.style("jump_up")!!
        return StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? }, style.melody.mood,
            StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() })
    }

    private fun startOn(p: Project = jumpUp()) {
        h.start { session.edit { p } }
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

    private fun open(stage: Int, card: String) {
        tap("guide_bar")
        tap("guide_stage_$stage")
        tap("guide_item_$card")
        compose.onNodeWithTag("guide_card_$card").assertExists()
    }

    private fun status(card: String) = h.vm.project.value.guide.statusOf(card)

    private fun Project.bass() = mixer.channels[InstrumentTrack.mixerChannel(instrumentTracks.first { it.name == "Basse" }.id)]

    @Test
    fun stage3WhatTheAssistantPutIsListedAndOneEffectIsRetired() {
        startOn()
        tap("guide_bar")
        compose.onNodeWithTag("guide_card_fx.review").assertIsDisplayed()
        compose.onNodeWithTag("guide_effects").assertExists()
        val key = "inst:" + h.vm.project.value.instrumentTracks.first { it.name == "Basse" }.id
        val slot = h.vm.project.value.bass().inserts.indexOfFirst { it.effect != EffectType.NONE }
        val option = GuideActions.effectOff(key, slot)
        // Its ▶ (heard with it, then without) and its « Retirer ».
        compose.onNodeWithTag("guide_fx_listen_$option").assertExists()
        tap("guide_fx_toggle_$option")
        await("retired") { h.vm.project.value.bass().inserts[slot].bypass }
        assertEquals(GuideStatus.TODO, status("fx.review"))
        // Now offered back.
        await("put back offered") { shown("guide_fx_toggle_${GuideActions.effectOn(key, slot)}") }
        tap("guide_fx_toggle_${GuideActions.effectOn(key, slot)}")
        await("back") { !h.vm.project.value.bass().inserts[slot].bypass }
    }

    @Test
    fun stage4ABreakIsMadeFromTheDrop() {
        startOn()
        val parts = h.vm.project.value.song.size
        open(4, "arr.subtract")
        // A part added elsewhere in the song: not heard in the first Drop's « Sans / Avec ».
        assertFalse(shown("guide_listen_break"))
        tap("guide_option_break")
        await("break") { h.vm.project.value.song.size == parts + 1 }
        assertEquals(GuideStatus.DONE, status("arr.subtract"))
    }

    @Test
    fun stage5TheFilterOpensOverTheMontee() {
        startOn(jumpUp().copy(automation = emptyList()))
        open(5, "move.build")
        assertFalse(shown("guide_listen_sweep"))
        tap("guide_option_sweep")
        await("curve") { h.vm.project.value.automation.isNotEmpty() }
        await("done") { status("move.build") == GuideStatus.DONE }
    }

    @Test
    fun stage6TheMonoCheckIsHeardAndTheStereoNarrowed() {
        startOn()
        open(6, "mix.mono")
        val before = h.vm.project.value
        // Heard, not applied (no engine here: said).
        tap("guide_option_listen")
        await("said") { shown("guide_listen_failed") }
        assertEquals(before, h.vm.project.value)
        tap("guide_option_narrow")
        await("narrowed") { h.vm.project.value.mixer.monoBassHz >= 150f }
        assertTrue(status("mix.mono").done)
    }
}
