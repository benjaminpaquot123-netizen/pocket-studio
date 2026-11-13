package com.pocketstudio.app.synth

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Lot 6, N18 (seat C b, screen 10): « Changer le son » was a ♪ alone beside the tracks, its words only for TalkBack.
 * The button shows a word (« Son »), in Simple and in Pro, and TalkBack still hears whose sound it changes.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-port-hdpi", fontScale = 1.3f)
class Lot6UiTrackSoundWordTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun french() { I18n.lang = Lang.FR }

    private fun saysAWord(simple: Boolean) {
        I18n.lang = Lang.FR
        val tracks = listOf(InstrumentTrack(0, "Basse"), InstrumentTrack(1, "Lead"))
        compose.setContent { PocketStudioTheme { TrackBar(TracksUi(tracks, 0, changeSound = {}, simple = simple)) } }
        compose.waitForIdle()
        val button = compose.onAllNodesWithTag("track_sound_0").fetchSemanticsNodes().single()
        val words = button.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text }.orEmpty()
        assertEquals("a visible word on the button", tr("Son"), words.trim())
        val said = button.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" ").orEmpty()
        assertTrue("TalkBack hears whose sound: « $said »", said.contains("Basse"))
        // As tall as the track chips beside it (rule 16: a touch target of 48 dp at least).
        assertTrue(button.size.height >= 48 * compose.density.density - 1f)
    }

    @Test
    fun simple() = saysAWord(simple = true)

    @Test
    fun pro() = saysAWord(simple = false)
}
