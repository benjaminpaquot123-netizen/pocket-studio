package com.pocketstudio.app.audit5

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.pocketstudio.app.live.LiveActions
import com.pocketstudio.app.live.LiveScreen
import com.pocketstudio.app.live.LiveScreenUi
import com.pocketstudio.app.live.TakeUi
import com.pocketstudio.app.onboarding.LaunchActions
import com.pocketstudio.app.onboarding.LaunchScreen
import com.pocketstudio.app.onboarding.LaunchUi
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.LiveState
import com.pocketstudio.model.Project
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.Palettes
import com.pocketstudio.ui.PadWordScrim
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Audit stage 5 (expert usability): the fixes that a test can hold. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h740dp-xxhdpi")
class UsabilityFixesTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun french() {
        I18n.lang = Lang.FR
    }

    /** Every card of « Que veux-tu faire ? » ends with the same arrow (two had none). */
    @Test
    fun everyHomeCardSaysItOpensSomething() {
        compose.setContent { PocketStudioTheme { LaunchScreen(LaunchUi("Mon projet"), LaunchActions()) } }
        for (tag in listOf("launch_new_arrow", "launch_projects_arrow", "launch_remix_arrow", "launch_padlive_arrow")) {
            compose.onNodeWithTag(tag, useUnmergedTree = true).assertExists()
        }
    }

    /**
     * Live, ready to record on a song: « Nouvelle version » and « Ajouter à la suite » both show whole
     * on a 360 dp phone, in French and in German (the second hid behind a fade at 400 dp).
     */
    @Test
    fun bothTakeChoicesShowWholeOnASmallPhone() = takeChoices(Lang.FR)

    @Test
    fun bothTakeChoicesShowWholeOnASmallPhoneInGerman() = takeChoices(Lang.DE)

    private fun takeChoices(lang: Lang) {
        I18n.lang = lang
        val project = Project(scenes = listOf(Scene("A", DrumPattern().toggle(0, 0)), Scene("B", DrumPattern().toggle(1, 4))), scene = 0)
            .copy(song = listOf(SongPart(0), SongPart(1))).sanitized()
        compose.setContent {
            PocketStudioTheme { LiveScreen(LiveScreenUi(project, LiveState.on(0), clock = 5, playing = true, take = TakeUi.Ready), LiveActions()) }
        }
        val screen = compose.onNode(isRoot()).fetchSemanticsNode().size.width
        for (tag in listOf("live_take_replace", "live_take_append")) {
            val node = compose.onNodeWithTag(tag).assertIsDisplayed().fetchSemanticsNode()
            val shown = node.boundsInRoot
            assertTrue("${lang.code} $tag: ${node.size.width} px wide, ${shown.width} shown", shown.width >= node.size.width - 1f)
            assertTrue("${lang.code} $tag inside the screen", shown.right <= screen + 1f)
        }
    }

    /** « coup », « boucle », « tenir » on a pad's colour read at 4.5:1 even on a lit, light pad (2.2:1 before). */
    @Test
    fun aPadsWordReadsOnItsColour() {
        val text = Palettes.Studio.textPrimary
        // The lightest a pad's corner gets: a pale family colour, lit while it plays.
        for (pad in listOf(Color(0xFFDCE1DC), Color(0xFFD3DDD7), Color(0xFF9E9062), Color(0xFF9B7083), Color(0xFF54829C))) {
            val under = PadWordScrim.compositeOver(pad)
            val ratio = (maxOf(text.luminance(), under.luminance()) + 0.05f) / (minOf(text.luminance(), under.luminance()) + 0.05f)
            assertTrue("$pad: $ratio", ratio >= 4.5f)
        }
    }
}
