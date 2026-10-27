package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.model.SimpleMelody
import com.pocketstudio.model.SongPart
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit final, lot « simple » (reports 05 and 08 above all): the Mode simple's ways in to the voice,
 * its ● where nothing is played, its Envoyer page, its lighter bar and its plainer Rythme and Clavier.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class SimpleModeAuditTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @After
    fun end() = h.finish()

    private fun tap(tag: String) {
        compose.onNodeWithTag(tag).performClick()
        compose.waitForIdle()
    }

    private fun exists(tag: String) = compose.onAllNodes(androidx.compose.ui.test.hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    /** 03 G4, 05 #2, 08 #1: « ⋯ › Enregistrer ma voix » opens the audio tracks (it was forced onto the Remix). */
    @Test
    fun theVoiceHasItsWayIn() {
        h.start(simple = true)
        tap("top_more")
        tap("more_voice")
        compose.onNodeWithTag("simple_audio_title").assertTextContains("Ma voix")
        compose.onNodeWithTag("audio_tracks").assertExists()
        assertFalse(exists("remix_start"))
        assertEquals("a first audio track to sing on", 1, h.vm.project.value.audioTracks.size)
        // The top ● is the microphone's there.
        compose.onNodeWithTag("top_record").assertExists()
        tap("remix_back")
        compose.onNodeWithTag("tab_simple_Morceau").assertIsSelected()
        // And the Remix still opens alone.
        tap("top_more")
        tap("more_remix")
        compose.onNodeWithTag("remix_start").assertExists()
        compose.onNodeWithTag("simple_audio_title").assertTextContains("Remix")
    }

    /** 05 #3: « Mes pistes » › an audio track opens its page, not the Remix. */
    @Test
    fun anAudioTracksSoundOpensTheVoice() {
        h.start(simple = true) { songWithTracks(); addAudioTrack() }
        val key = SongPart.audio(h.vm.project.value.audioTracks.first().id)
        compose.onNodeWithTag("simple_track_$key").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("audio_tracks").assertExists()
        assertFalse(exists("remix_start"))
    }

    /** 05 #11: the top ● on « Mon morceau » records « ma mélodie » on the Clavier, not an empty take of pads. */
    @Test
    fun theTopRecordOnTheSongGoesToTheKeys() {
        h.start(simple = true)
        compose.onNodeWithTag("tab_simple_Morceau").assertIsSelected()
        h.vm.playingForTest = true
        tap("top_record")
        compose.onNodeWithTag("tab_simple_Clavier").assertIsSelected()
        assertTrue(h.vm.recording.value)
        assertEquals(SimpleMelody.trackOf(h.vm.project.value), h.vm.selectedTrack.value)
        assertEquals(com.pocketstudio.app.record.TakeSource.KEYS, h.vm.noteTake.value?.source)
    }

    /** 05 #1: « Envoyer » follows the project's name (a rename elsewhere), and « Partager » does not undo it. */
    @Test
    fun envoyerFollowsTheProjectsName() {
        h.start(simple = true) { songWithTracks(); renameProject("Avant") }
        tap("tab_simple_Envoyer")
        compose.onNodeWithTag("share_name").assertTextContains("Avant")
        tap("tab_simple_Morceau")
        h.vm.renameProject("Après")
        compose.waitForIdle()
        tap("tab_simple_Envoyer")
        compose.onNodeWithTag("share_name").assertTextContains("Après")
    }

    /** 20 M16: one part chip less, no redo, no « ⟳ Partie »; the part to edit is picked in the editor's header. */
    @Test
    fun theTopBarIsLighter() {
        h.start(simple = true)
        tap("tab_simple_Rythme")
        assertFalse(exists("top_scene"))
        assertFalse(exists("top_redo"))
        assertFalse(exists("top_loop_part"))
        tap("part_header_pick")
        tap("part_pick_1")
        assertEquals(1, h.vm.project.value.scene)
    }

    /** 08 #4: the Mode simple's Rythme keeps pads, cells, length and swing; the expert's tools are the full mode's. */
    @Test
    fun theRhythmHasNoExpertTools() {
        h.start(simple = true)
        tap("tab_simple_Rythme")
        compose.onNodeWithTag("step_cells").assertExists()
        listOf("roll_tool", "pan_tool", "pitch_tool", "drum_effects", "pad_effects", "pad_sound_pan", "import_sample", "record_sample", "resample_1").forEach {
            assertFalse("$it in the Mode simple", exists(it))
        }
        compose.onNodeWithTag("open_library").assertExists()
    }

    /** 08 #5, 01 M10: the Clavier has no bend, modulation, synth settings, effects nor freezing; one « Changer le son ». */
    @Test
    fun theKeysHaveNoExpertTools() {
        h.start(simple = true)
        tap("tab_simple_Clavier")
        listOf("play_panel", "bend", "mod", "track_effects", "track_menu_sheet").forEach { assertFalse("$it in the Mode simple", exists(it)) }
        compose.onNodeWithText("Réglages du son").assertDoesNotExist()
        compose.onNodeWithTag("simple_change_sound").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("sound_chooser").assertExists()
    }

    /** 08 #11: on a phone at 130 %, the Clavier's keys are whole above the Guide's bar. */
    @Test
    fun theKeysAreAboveTheGuidesBarAt130() {
        h.start(simple = true, scale = 1.3f)
        tap("tab_simple_Clavier")
        val keys = compose.onNodeWithTag("keyboard").fetchSemanticsNode().boundsInRoot
        val guide = compose.onNodeWithTag("guide_bar").fetchSemanticsNode().boundsInRoot
        assertTrue("the keys end at ${keys.bottom}, the Guide's bar starts at ${guide.top}", keys.bottom <= guide.top + 1f)
        assertTrue(keys.height > 0f && keys.bottom <= compose.onRoot().fetchSemanticsNode().boundsInRoot.bottom)
    }

    private fun requested(): List<String> =
        org.robolectric.Shadows.shadowOf(compose.activity).lastRequestedPermission?.requestedPermissions?.toList().orEmpty()

    /** 03 M5, 15: « Refaire mes. X-Y » without the microphone asks for it (it did nothing). */
    @Test
    fun aPunchWithoutTheMicrophoneAsksForIt() {
        h.start(simple = true) {
            songWithTracks()
            addAudioTrack()
            session.edit { p ->
                p.copy(audioTracks = p.audioTracks.map { it.copy(clip = com.pocketstudio.model.AudioClip("take.wav", "Prise", seconds = 8.0, bpm = p.tempoBpm)) })
            }
        }
        org.robolectric.Shadows.shadowOf(h.app).denyPermissions(android.Manifest.permission.RECORD_AUDIO)
        tap("top_more")
        tap("more_voice")
        val id = h.vm.project.value.audioTracks.first().id
        compose.onNodeWithTag("audio_punch_$id").performScrollTo().performClick()
        compose.waitForIdle()
        assertTrue(requested().toString(), android.Manifest.permission.RECORD_AUDIO in requested())
    }

    /** 02 M4: a Remix (its model's download) asks for the notifications that tell its progress and its end. */
    @Test
    fun theRemixAsksForNotifications() {
        h.start(simple = true)
        // A phone with room for a separation (Robolectric's default reads as too small).
        val am = h.app.getSystemService(android.app.ActivityManager::class.java)
        org.robolectric.Shadows.shadowOf(am).setMemoryInfo(android.app.ActivityManager.MemoryInfo().apply { totalMem = 8L shl 30; availMem = 4L shl 30 })
        tap("top_more")
        tap("more_remix")
        compose.onNodeWithTag("remix_download").performScrollTo().performClick()
        compose.waitForIdle()
        assertTrue(requested().toString(), android.Manifest.permission.POST_NOTIFICATIONS in requested())
    }

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.onNodeWithText(text: String) =
        onNode(androidx.compose.ui.test.hasText(text))
}
