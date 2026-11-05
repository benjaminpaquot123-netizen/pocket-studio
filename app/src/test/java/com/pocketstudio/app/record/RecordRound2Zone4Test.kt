package com.pocketstudio.app.record

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.pocketstudio.app.audiotracks.AudioTracksActions
import com.pocketstudio.app.audiotracks.AudioTracksScreen
import com.pocketstudio.app.audiotracks.AudioTracksUi
import com.pocketstudio.app.rhythm.AudioRecordingUi
import com.pocketstudio.app.ui.MuteToggle
import com.pocketstudio.model.AudioClip
import com.pocketstudio.model.AudioTrack
import com.pocketstudio.model.Project
import com.pocketstudio.model.chooseTake
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit ultra round 2, zone 4 (« Enregistrement », seat B b: a Korean guitarist on a tablet), on the
 * screens themselves, without the bench:
 * - n° 18: the takes are « Prise 1 », « Prise 2 » in a fixed order (two « 테이크 03:37 » swapped at
 *   each touch); their seconds and « supprimer » in the language (« s », « 빼기 »);
 * - n° 19: the « Son » switch says the sound is on while the track sounds (it said « off »);
 * - n° 20: the microphone's level has a name and says how it is; « Audio 1 » is named in the language.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w800dp-h2400dp")
class RecordRound2Zone4Test {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun french() {
        I18n.lang = Lang.FR
    }

    /** Two takes recorded the same minute, the second one playing. */
    private fun twoTakes(): Project {
        val first = AudioClip("a.wav", "Prise 03:37", 6.2, take = 1)
        val second = AudioClip("b.wav", "Prise 03:37", 7.0, take = 2)
        return Project(audioTracks = listOf(AudioTrack(0, "Audio 1", clip = second, previous = first)))
    }

    private fun left(text: String): Float =
        compose.onNode(hasText(text), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.left

    @Test
    fun takesAreNumberedAndStayInTheirPlaceWhenOneIsChosen() {
        var p by mutableStateOf(twoTakes())
        compose.setContent { PocketStudioTheme { AudioTracksScreen(AudioTracksUi(p.audioTracks), AudioTracksActions()) } }
        compose.waitForIdle()
        assertTrue("Prise 1 before Prise 2", left("Prise 1 · 6 s") < left("Prise 2 · 7 s"))
        compose.onNodeWithTag("audio_take_0_2").assertIsSelected()
        compose.onNodeWithTag("audio_take_0_1").assertIsNotSelected()
        // Only a take not playing can go, said « Supprimer » (it said « Retirer », « 빼기 » in Korean).
        compose.onNode(hasContentDescription("Supprimer Prise 1 · 6 s")).assertExists()
        compose.onNode(hasContentDescription("Supprimer Prise 2 · 7 s")).assertDoesNotExist()

        p = p.chooseTake(0, 0)
        compose.waitForIdle()
        assertTrue("the order did not move", left("Prise 1 · 6 s") < left("Prise 2 · 7 s"))
        compose.onNodeWithTag("audio_take_0_1").assertIsSelected()
        compose.onNodeWithTag("audio_take_0_2").assertIsNotSelected()
        compose.onNode(hasContentDescription("Supprimer Prise 2 · 7 s")).assertExists()
        // The take playing under the waveform: its number too.
        compose.onNodeWithText("Prise 1 · ${String.format(Lang.FR.locale, "%.1f", 6.2)} s").assertExists()
    }

    @Test
    fun inKoreanTheTakesSayTheirSecondsAndDeleteInKorean() {
        I18n.lang = Lang.KO
        compose.setContent { PocketStudioTheme { AudioTracksScreen(AudioTracksUi(twoTakes().audioTracks), AudioTracksActions()) } }
        compose.waitForIdle()
        compose.onNode(hasText("테이크 1 · 6초"), useUnmergedTree = true).assertExists()
        compose.onNode(hasText("테이크 2 · 7.0초"), useUnmergedTree = true).assertExists()
        compose.onNode(hasContentDescription("테이크 1 · 6초 삭제")).assertExists()
        // The fades too: no « s » left alone on the Korean screen.
        val texts = compose.onAllNodes(hasText(" s", substring = true), useUnmergedTree = true).fetchSemanticsNodes()
            .mapNotNull { it.config.getOrNull(SemanticsProperties.Text)?.joinToString() }
        assertTrue("an English « s » in Korean: $texts", texts.none { Regex("""\d s\b""").containsMatchIn(it) })
        // n° 20: the microphone's track named in Korean (« Audio 1 » everywhere).
        compose.onNode(hasText("오디오 1"), useUnmergedTree = true).assertExists()
        compose.onNode(hasContentDescription("오디오 1 옵션")).assertExists()
    }

    @Test
    fun theMicrophonesLevelHasANameAndSaysHowItIs() {
        var level by mutableStateOf(0.5f)
        compose.setContent {
            PocketStudioTheme {
                AudioTracksScreen(
                    AudioTracksUi(twoTakes().audioTracks, recording = AudioRecordingUi(track = 0, waiting = false, seconds = 2f, level = level)),
                    AudioTracksActions(),
                )
            }
        }
        compose.waitForIdle()
        fun state() = compose.onNode(hasContentDescription("Niveau du micro")).fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription)
        compose.onNodeWithText("Niveau du micro").assertExists()
        assertEquals("Bien", state())
        level = 0.02f
        compose.waitForIdle()
        assertEquals("Trop faible", state())
        level = 0.97f
        compose.waitForIdle()
        assertEquals("Trop fort", state())
    }

    /** n° 19 (seat B b, Mix: « 사운드 », « 꺼짐 » while the track sounded): the switch is on while the sound is. */
    @Test
    fun theSoundSwitchIsOnWhileTheTrackSounds() {
        var muted by mutableStateOf(false)
        compose.setContent { PocketStudioTheme { MuteToggle(muted, "Audio 1", "m", caption = true) { muted = !muted } } }
        compose.waitForIdle()
        compose.onNodeWithTag("m").assertIsOn()
        assertEquals("activé", compose.onNodeWithTag("m").fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription))
        compose.onNode(androidx.compose.ui.test.hasTestTag("m") and hasContentDescription("Son de Audio 1")).assertExists()
    }
}
