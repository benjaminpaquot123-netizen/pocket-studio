package com.pocketstudio.app.nav

import android.os.Looper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.pocketstudio.app.guide.GuideGlowKey
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.Project
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.StarterWizard
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

/**
 * « Écouter seul » in the app (bug report, APK 224, Mode simple, acid techno): the kick's Guide card
 * plays the kick alone in a loop and gives everything back; leaving the card stops it; « Mes
 * pistes » plays a track (or one drum sound) alone at once; the Guide's window beats softly unless
 * the phone's animations are off. Images: app/screenshots/nav/alone_*.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class GuideAloneTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)
    private val kick = StarterWizard.Role.Kick.lane

    @After
    fun done() = h.finish()

    private fun acidTechno(): Project {
        val style = StarterWizard.style("acid_techno")!!
        return StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? }, style.melody.mood,
            StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() })
    }

    private fun idle(ms: Long = 100) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
        compose.mainClock.advanceTimeBy(ms)
        compose.waitForIdle()
    }

    private fun tap(tag: String) {
        val node = compose.onNodeWithTag(tag)
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
    }

    private fun await(ok: () -> Boolean) {
        val end = System.currentTimeMillis() + 60_000
        while (!ok()) {
            idle()
            check(System.currentTimeMillis() < end) { "not in time" }
            Thread.sleep(5)
        }
    }

    private fun start() {
        h.start(simple = true) { session.edit { acidTechno() } }
        await { !h.vm.guide.ui.value.loading && h.vm.guide.ui.value.simple && h.vm.guide.ui.value.cards.containsKey("sound.kick") }
    }

    /** The Guide open on the kick's card (« Le kick (le « boum ») »). */
    private fun kickCard() {
        tap("guide_bar")
        tap("guide_stage_1")
        tap("guide_item_sound.kick")
        compose.onNodeWithTag("guide_card_sound.kick").assertExists()
    }

    @Test
    fun theKicksCardPlaysTheKickAloneInALoopThenGivesEverythingBack() {
        start()
        kickCard()
        val before = h.vm.project.value
        tap("guide_alone")
        assertEquals(setOf(kick), h.vm.alone.value?.target?.pads)
        // The kick's pad alone inside the kit, not the drums' strip (AloneListenVmTest: what the engine then hears).
        assertTrue(h.vm.alone.value!!.target.channels.isEmpty())
        assertTrue("it loops at once, no ▶ to find", h.vm.transport.value.playing)
        compose.onNodeWithTag("guide_alone").assertIsSelected()
        compose.onNodeWithTag("guide_alone_hint").assertExists()
        idle(300)
        captureScreenRoboImage("screenshots/nav/alone_card.png")

        tap("guide_alone")
        assertNull(h.vm.alone.value)
        assertFalse(h.vm.transport.value.playing)
        // The song as it was: no change, no undo step.
        assertSame(before.drums, h.vm.project.value.drums)
        assertEquals(before.mixer, h.vm.project.value.mixer)
    }

    /** « Autre idée de son » while the kick loops: its pad's sheet opens and the loop goes on, each sound picked heard in it. */
    @Test
    fun anotherSoundIsHeardInTheLoop() {
        start()
        kickCard()
        tap("guide_alone")
        tap("guide_open_other")
        h.awaitTag("pad_sheet")
        idle(RhythmViewModel.ALONE_GRACE_MS + 200)
        assertEquals(setOf(kick), h.vm.alone.value?.target?.pads)
        compose.onNodeWithTag("pad_alone").assertIsSelected()
        tap("pad_sound_0")
        // The sheet stays (the next sound is one touch away) and the loop goes on with the new sound.
        compose.onNodeWithTag("pad_sheet").assertExists()
        assertTrue(h.vm.alone.value != null && h.vm.transport.value.playing)
        idle(300)
        captureScreenRoboImage("screenshots/nav/alone_pad_sheet.png")
        // The sheet closed, the card still shown: it goes on; ■ on the sheet's button stops it.
        tap("pad_alone")
        assertNull(h.vm.alone.value)
    }

    @Test
    fun leavingTheCardStopsIt() {
        start()
        kickCard()
        tap("guide_alone")
        assertTrue(h.vm.alone.value != null)
        // Another card picked in the plan: the kick's loop ends (after the short grace).
        tap("guide_item_sound.snare")
        idle(RhythmViewModel.ALONE_GRACE_MS + 200)
        assertNull(h.vm.alone.value)
        assertFalse(h.vm.transport.value.playing)
        // The Guide folded: the same.
        tap("guide_alone")
        assertTrue(h.vm.alone.value != null)
        tap("guide_bar")
        idle(RhythmViewModel.ALONE_GRACE_MS + 200)
        assertNull(h.vm.alone.value)
    }

    @Test
    fun myTracksPlayATrackOrOneDrumSoundAlone() {
        start()
        compose.onNodeWithTag("simple_tracks").performScrollTo()
        tap("simple_alone_${SongPart.DRUMS}")
        assertEquals(setOf(0), h.vm.alone.value?.target?.channels)
        assertTrue(h.vm.transport.value.playing)
        compose.onNodeWithTag("simple_tracks").performScrollTo()
        idle(300)
        captureScreenRoboImage("screenshots/nav/alone_tracks.png")
        // The Batterie's own sounds, in its sheet: the kick alone.
        tap("simple_volume_${SongPart.DRUMS}")
        tap("simple_pad_alone_$kick")
        assertEquals(setOf(kick), h.vm.alone.value?.target?.pads)
        assertTrue(h.vm.alone.value!!.target.channels.isEmpty())
        assertTrue(h.vm.transport.value.playing)
        idle(300)
        captureScreenRoboImage("screenshots/nav/alone_track_sheet.png")
        tap("simple_pad_alone_$kick")
        assertNull(h.vm.alone.value)
        // The sheet's « Écouter seule » plays the whole track at once.
        tap("simple_track_solo")
        assertEquals(setOf(0), h.vm.alone.value?.target?.channels)
        assertTrue(h.vm.transport.value.playing)
    }

    @Test
    fun theGuidesWindowBeatsUnlessAnimationsAreOff() {
        start()
        tap("guide_bar")
        compose.onNodeWithTag("guide_panel").assert(SemanticsMatcher.expectValue(GuideGlowKey, "beating"))
        tap("guide_bar")
        Settings.Global.putFloat(h.app.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        try {
            tap("guide_bar")
            compose.onNodeWithTag("guide_panel").assert(SemanticsMatcher.expectValue(GuideGlowKey, "still"))
        } finally {
            Settings.Global.putFloat(h.app.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }
        assertTrue(compose.onAllNodesWithTag("guide_next_step").fetchSemanticsNodes().isNotEmpty())
    }
}
