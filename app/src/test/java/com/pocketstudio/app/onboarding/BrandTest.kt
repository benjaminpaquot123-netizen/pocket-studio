package com.pocketstudio.app.onboarding

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.pocketstudio.app.settings.LanguagePicker
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.app.synth.Keyboard
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Locale

/**
 * BenStudio's opening and first start: the intro plays over the app (which starts underneath),
 * ends by itself or at a touch, stays still with the phone's animations off; the language is the
 * phone's at the first start and picked from a list; music canvases stay left to right in a
 * right-to-left language. Key frames in app/screenshots/brand/.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class BrandTest {
    @get:Rule
    val compose = createComposeRule()

    private val app: Application get() = ApplicationProvider.getApplicationContext()

    @Before
    fun fresh() {
        app.getSharedPreferences("settings", 0).edit().clear().commit()
        SettingsStore.forgetForTests()
    }

    @After
    fun french() {
        SettingsStore.forgetForTests()
        I18n.lang = Lang.FR
    }

    private var ended = 0

    /** The intro over a stand-in of the app, as MainActivity lays them out. */
    private fun intro(reduced: Boolean) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            PocketStudioTheme {
                var playing by remember { mutableStateOf(true) }
                Box {
                    Text("L'appli", Modifier.testTag("app_under"))
                    if (playing) BrandIntro(onDone = { playing = false; ended++ }, reducedMotion = reduced)
                }
            }
        }
    }

    private fun shown(tag: String) = compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    private fun shot(name: String) = compose.onRoot().captureRoboImage("screenshots/brand/$name.png")

    @Test
    fun theIntroPlaysOverTheAppThenEndsByItself() {
        intro(reduced = false)
        compose.mainClock.advanceTimeByFrame()
        // The app is there from the first frame, under the intro: nothing waits for it.
        assertTrue(shown("app_under"))
        assertTrue(shown("intro"))
        // Key frames: the pads popping in, the two heartbeats, the name under the mark, the fade.
        compose.mainClock.advanceTimeBy(130)
        shot("intro_0_pop")
        compose.mainClock.advanceTimeBy(420)  // 550 ms: the first beat at its height
        shot("intro_1_beat")
        compose.mainClock.advanceTimeBy(160)  // 710 ms: the second, softer
        shot("intro_2_beat")
        compose.mainClock.advanceTimeBy(690)  // 1.4 s: « BenStudio » under the mark
        assertTrue(shown("intro"))
        compose.onNodeWithTag("intro_name", useUnmergedTree = true).assertTextContains("BenStudio")
        shot("intro_3_name")
        assertEquals(0, ended)
        // 1.8 s in all, from the splash's start.
        compose.mainClock.advanceTimeBy((IntroTimeline.TOTAL - 1400 + 100).toLong())
        compose.waitForIdle()
        assertFalse(shown("intro"))
        assertEquals(1, ended)
        assertTrue(shown("app_under"))
    }

    @Test
    fun itGoesOnFromWhereTheSplashWas() {
        Intro.splashElapsedMs = 900L
        try {
            intro(reduced = false)
            compose.mainClock.advanceTimeByFrame()
            // 900 ms already played by the splash: the rest only.
            compose.mainClock.advanceTimeBy((IntroTimeline.TOTAL - 900 + 100).toLong())
            compose.waitForIdle()
            assertFalse(shown("intro"))
            assertEquals(1, ended)
        } finally {
            Intro.splashElapsedMs = 0L
        }
    }

    @Test
    fun aTouchSkipsIt() {
        intro(reduced = false)
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithTag("intro").performClick()
        compose.mainClock.advanceTimeBy(IntroTimeline.SKIP_FADE_MS + 100L)
        compose.waitForIdle()
        assertFalse(shown("intro"))
        assertEquals(1, ended)
    }

    @Test
    fun withTheAnimationsOffItStaysStillThenFades() {
        intro(reduced = true)
        compose.mainClock.advanceTimeByFrame()
        // Everything in place at once: the mark and the name, nothing moving.
        compose.onNodeWithTag("intro_name", useUnmergedTree = true).assertTextContains("BenStudio")
        shot("intro_still")
        compose.mainClock.advanceTimeBy(IntroTimeline.STILL_MS + IntroTimeline.STILL_FADE_MS + 100)
        compose.waitForIdle()
        assertFalse(shown("intro"))
        assertEquals(1, ended)
    }

    @Test
    fun theFirstStartTakesThePhonesLanguageOnce() {
        SettingsStore.load(app)
        SettingsStore.adoptPhoneLanguage(app, Locale.forLanguageTag("nl-BE"))
        assertEquals(Lang.NL, SettingsStore.state.value.language)
        assertTrue("C D E in Dutch", SettingsStore.state.value.englishNotes)
        // Chosen once: a later start (or the phone changed) leaves it.
        SettingsStore.adoptPhoneLanguage(app, Locale.FRANCE)
        assertEquals(Lang.NL, SettingsStore.state.value.language)
        SettingsStore.forgetForTests()
        assertEquals(Lang.NL, SettingsStore.load(app).language)
    }

    @Test
    fun aFrenchPhoneStartsInFrenchAndAnotherOneInEnglish() {
        SettingsStore.load(app)
        SettingsStore.adoptPhoneLanguage(app, Locale.CANADA_FRENCH)
        assertEquals(Lang.FR, SettingsStore.state.value.language)
        assertFalse(SettingsStore.state.value.englishNotes)
        app.getSharedPreferences("settings", 0).edit().clear().commit()
        SettingsStore.forgetForTests()
        SettingsStore.load(app)
        SettingsStore.adoptPhoneLanguage(app, Locale.forLanguageTag("sw-KE"))
        assertEquals(Lang.EN, SettingsStore.state.value.language)
    }

    @Test
    fun theLanguageListShowsEveryLanguageInItself() {
        SettingsStore.load(app)
        compose.setContent { PocketStudioTheme { LanguagePicker() } }
        compose.onNodeWithTag("lang_button").assertTextContains("Français", substring = true).performClick()
        compose.waitForIdle()
        Lang.entries.forEach { compose.onNodeWithTag("lang_list").performScrollToNode(hasTestTag("lang_${it.code}")) }
        compose.onNodeWithTag("lang_list").performScrollToNode(hasTestTag("lang_fr"))
        compose.onNodeWithTag("lang_fr").assertIsSelected()
        shot("language_list")
        compose.onNodeWithTag("lang_list").performScrollToNode(hasTestTag("lang_en"))
        compose.onNodeWithTag("lang_en").performClick()
        compose.waitForIdle()
        assertEquals(Lang.EN, SettingsStore.state.value.language)
        assertTrue(SettingsStore.state.value.englishNotes)
        assertFalse(shown("lang_dialog"))
    }

    @Test
    fun aKeyboardStaysLeftToRightInARightToLeftLanguage() {
        compose.setContent {
            PocketStudioTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Keyboard(firstNote = 60, octaves = 1, onNoteOn = { _, _ -> }, onNoteOff = {})
                }
            }
        }
        val low = compose.onNodeWithTag("key_60").fetchSemanticsNode().boundsInRoot.left
        val high = compose.onNodeWithTag("key_71").fetchSemanticsNode().boundsInRoot.left
        assertTrue("Do on the left of Si ($low, $high)", low < high)
    }

    @Test
    fun theWelcomeShowsTheBrandAndTheLanguage() {
        SettingsStore.load(app)
        compose.setContent { PocketStudioTheme { WelcomeScreen(WelcomeActions()) } }
        compose.onNodeWithTag("welcome_brand").assertExists()
        compose.onNodeWithTag("lang_button").assertTextContains("Français", substring = true)
        shot("welcome")
    }
}
