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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.performScrollTo
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
import com.pocketstudio.app.settings.SettingsActions
import com.pocketstudio.app.settings.SettingsScreen
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
 * right-to-left language. The sound (or the taps) on the timeline's beats, silent when skipped
 * early or with the animations off; « Son d'ouverture » in Réglages. Key frames in
 * app/screenshots/brand/.
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

    /** What the intro asked of its sound / taps, in order, with the timeline's time for the sound. */
    private val cues = mutableListOf<String>()
    private var soundAt = -1f
    private val cue = object : IntroCue {
        override fun start(atMs: Float) {
            cues += "start"
            soundAt = atMs
        }

        override fun beat(k: Int) {
            cues += "beat$k"
        }

        override fun stop() {
            cues += "stop"
        }
    }

    /** The intro over a stand-in of the app, as MainActivity lays them out. */
    private fun intro(reduced: Boolean) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            PocketStudioTheme {
                var playing by remember { mutableStateOf(true) }
                Box {
                    Text("L'appli", Modifier.testTag("app_under"))
                    if (playing) BrandIntro(onDone = { playing = false; ended++ }, reducedMotion = reduced, cue = cue)
                }
            }
        }
    }

    private fun shown(tag: String) = compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    private fun shot(name: String) = compose.onRoot().captureRoboImage("screenshots/brand/$name.png")

    /** To [ms] after the intro's first frame (the test clock moves by whole frames: within one of it). */
    private var base = -1L
    private fun to(ms: Long) {
        if (base < 0) base = compose.mainClock.currentTime
        val left = base + ms - compose.mainClock.currentTime
        if (left > 0) compose.mainClock.advanceTimeBy(left)
    }

    @Test
    fun theIntroPlaysOverTheAppThenEndsByItself() {
        intro(reduced = false)
        compose.mainClock.advanceTimeByFrame()
        // The app is there from the first frame, under the intro: nothing waits for it.
        assertTrue(shown("app_under"))
        assertTrue(shown("intro"))
        // Key frames: the pads popping in, the two heartbeats (shock wave, ring, sparks), the
        // light over the pads as the mark rises, the light over the name, the fade.
        to(130)
        shot("intro_0_pop")
        to(1150)  // the first beat at its height
        shot("intro_1_beat")
        to(1400)  // the second, softer, the sparks of the first still out
        shot("intro_2_beat")
        to(1800)  // the light runs over the pads, the mark rises
        shot("intro_3_rise")
        to(2750)  // « BenStudio » under the mark, a light over it
        assertTrue(shown("intro"))
        compose.onNodeWithTag("intro_name", useUnmergedTree = true).assertTextContains("BenStudio")
        shot("intro_4_name")
        assertEquals(0, ended)
        to(4600)  // fading
        assertTrue(shown("intro"))
        assertEquals(0, ended)
        // 4.7 s in all, from the splash's start.
        to(IntroTimeline.TOTAL.toLong() + 100)
        compose.waitForIdle()
        assertFalse(shown("intro"))
        assertEquals(1, ended)
        assertTrue(shown("app_under"))
    }

    /**
     * Not a check: with INTRO_FRAMES=<dir> (and roborazzi recording), every frame of the opening
     * at 30 fps, small, for an animated preview (scripts/brand/intro_preview.sh).
     */
    @Test
    @Config(qualifiers = "w360dp-h740dp-mdpi")
    fun previewFrames() {
        val dir = System.getenv("INTRO_FRAMES")
        org.junit.Assume.assumeTrue(dir != null)
        intro(reduced = false)
        compose.mainClock.advanceTimeByFrame()
        var k = 0
        while (k * 1000L / 30 <= IntroTimeline.TOTAL.toLong()) {
            to(k * 1000L / 30)
            compose.onRoot().captureRoboImage("$dir/f%03d.png".format(k))
            k++
        }
    }

    @Test
    fun theSoundStartsOnTheTimelineAndTheBeatsComeOnTheirFrames() {
        intro(reduced = false)
        compose.mainClock.advanceTimeByFrame()
        to(980)
        assertEquals(emptyList<String>(), cues)
        // The sound starts on the frame that reaches its start, told where the timeline is.
        to(1020)
        assertEquals(listOf("start"), cues)
        assertTrue("at $soundAt", soundAt >= IntroTimeline.SOUND_START && soundAt <= IntroTimeline.SOUND_LATEST)
        assertTrue(IntroTimeline.soundFrame(soundAt) in 0 until 48 * 40)
        // The taps (phone on vibrate) on the drawn beats.
        to(1060)
        assertEquals(listOf("start"), cues)
        to(1120)
        assertEquals(listOf("start", "beat0"), cues)
        to(1300)
        assertEquals(listOf("start", "beat0"), cues)
        to(1360)
        assertEquals(listOf("start", "beat0", "beat1"), cues)
        to(IntroTimeline.TOTAL.toLong() + 100)
        compose.waitForIdle()
        // Gone: the sound is let go (it has played out by then).
        assertEquals(listOf("start", "beat0", "beat1", "stop"), cues)
    }

    @Test
    fun itGoesOnFromWhereTheSplashWas() {
        Intro.splashElapsedMs = 900L
        try {
            intro(reduced = false)
            compose.mainClock.advanceTimeByFrame()
            // 900 ms already played by the splash: the rest only, the sound still on time.
            compose.mainClock.advanceTimeBy(130)
            assertEquals("start", cues.first())
            assertTrue("at $soundAt", soundAt >= IntroTimeline.SOUND_START && soundAt <= IntroTimeline.SOUND_LATEST)
            compose.mainClock.advanceTimeBy((IntroTimeline.TOTAL - 900 - 130 + 100).toLong())
            compose.waitForIdle()
            assertFalse(shown("intro"))
            assertEquals(1, ended)
            assertEquals(listOf("start", "beat0", "beat1", "stop"), cues)
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
        // Skipped before its sound: never started, no beat.
        assertFalse("start" in cues)
        assertFalse(cues.any { it.startsWith("beat") })
    }

    @Test
    fun aTouchDuringTheBeatsFadesTheSoundOut() {
        intro(reduced = false)
        compose.mainClock.advanceTimeByFrame()
        to(1200)
        assertEquals(listOf("start", "beat0"), cues)
        compose.onNodeWithTag("intro").performClick()
        compose.mainClock.advanceTimeBy(IntroTimeline.SKIP_FADE_MS + 100L)
        compose.waitForIdle()
        assertFalse(shown("intro"))
        // The sound fades out at the touch; the second beat is not given.
        assertEquals("stop", cues[2])
        assertFalse("beat1" in cues)
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
        // Silent: no sound, no tap.
        assertEquals(emptyList<String>(), cues)
    }

    @Test
    fun theOpeningSoundIsASettingOnByDefault() {
        val fresh = SettingsStore.load(app)
        assertTrue(fresh.introSound)
        compose.setContent {
            val s by SettingsStore.state.collectAsState()
            PocketStudioTheme { SettingsScreen(s, "1", SettingsActions(update = { f -> SettingsStore.update(app, f) })) }
        }
        compose.onNodeWithTag("intro_sound").performScrollTo().assertIsOn().performClick()
        compose.waitForIdle()
        assertFalse(SettingsStore.state.value.introSound)
        compose.onNodeWithTag("intro_sound").assertIsOff()
        // Kept for the next start; « Revenir aux réglages d'origine » puts it back on.
        SettingsStore.forgetForTests()
        assertFalse(SettingsStore.load(app).introSound)
        SettingsStore.reset(app)
        assertTrue(SettingsStore.state.value.introSound)
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
