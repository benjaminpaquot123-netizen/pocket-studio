package com.pocketstudio.app.nav

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.pocketstudio.app.ui.SimpleTab
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.Project
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.guide.GuideLevels
import com.pocketstudio.model.i18n.Lang
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

/** An acid techno song as the assistant makes it (the 303 on strip 1, the kick on its pad). */
internal fun acidTechnoSong(): Project {
    val style = StarterWizard.style("acid_techno")!!
    return StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? }, style.melody.mood,
        StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() })
}

/** The engine's readings for a song in full swing: each strip lit, the drum sounds, the output « Fort et propre ». */
internal fun swingingMeters(): Pair<FloatArray, FloatArray> {
    val m = FloatArray(NativeEngine.METERS)
    m[0] = 0.75f; m[1] = 0.95f; m[2] = 0.4f
    (5 until 12).forEach { m[it] = 0.3f + 0.08f * (it - 5) }
    (0 until Mixer.PADS).forEach { m[Mixer.padChannel(it)] = 0.25f + 0.1f * (it % 6) }
    m[Mixer.MASTER] = 0.9f
    m[NativeEngine.METER_LIMITER] = 0.9f
    m[NativeEngine.METER_MASTER_IN] = 0.9f
    val l = FloatArray(NativeEngine.LOUDNESS) { NativeEngine.LOUDNESS_NONE }
    l[NativeEngine.LOUDNESS_SHORT_TERM] = -9f
    l[NativeEngine.LOUDNESS_MOMENTARY] = -9f
    return m to l
}

/**
 * The Mode simple's « Mix » tab (owner's mock-up 1A): a fifth tab on the project's own mixer, its
 * faders (a slide, two touches = 0 dB), the drums opening on their sounds each heard alone, the
 * « Sortie » in words, one tip with its fix, « Tout à zéro »; all undoable.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class SimpleMixTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)
    private val kick = StarterWizard.Role.Kick.lane

    @After
    fun done() = h.finish()

    private fun idle(ms: Long = 300) {
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

    private fun start() {
        h.start(simple = true) { session.edit { acidTechnoSong() } }
        tap("tab_simple_Mix")
        compose.onNodeWithTag("simple_mix").assertIsDisplayed()
    }

    private fun volume(ch: Int) = h.vm.project.value.mixer.channels[ch].volume

    @Test
    fun theSimpleModeHasAMixTabOnTheSameMixer() {
        start()
        assertEquals(5, SimpleTab.entries.size)
        compose.onNodeWithTag("tab_simple_Mix").assertIsDisplayed()
        compose.onNodeWithTag("mix_hint").assertExists()
        compose.onNodeWithTag("mix_strip_1").assertExists()
        compose.onNodeWithTag("mix_out").performScrollTo().assertIsDisplayed()
        // A slide up on the 303's fader: its volume in the project goes up (the console's own), one undo step.
        val before = volume(1)
        compose.onNodeWithTag("mix_fader_1").performTouchInput { swipeUp() }
        compose.waitForIdle()
        assertTrue("${volume(1)} > $before", volume(1) > before)
        h.vm.undo()
        assertEquals(before, volume(1), 0f)
    }

    @Test
    fun twoTouchesPutAFaderBackAtZeroDecibel() {
        start()
        h.vm.setChannelVolume(1, 0.5f)
        compose.waitForIdle()
        compose.onNodeWithTag("mix_fader_1").performTouchInput { doubleClick() }
        idle()
        assertEquals(Mixer.DEFAULT_VOLUME, volume(1), 0f)
        compose.onNodeWithTag("mix_db_1").assertTextEquals("+0,0 dB")
        h.vm.undo()
        assertEquals(0.5f, volume(1), 0f)
    }

    @Test
    fun theDrumsOpenOnTheirSoundsEachHeardAlone() {
        start()
        // Closed at first (every strip fits), open on its sounds, closed again.
        compose.onNodeWithTag("mix_pad_$kick").assertDoesNotExist()
        tap("mix_drums_expand")
        compose.onNodeWithTag("mix_pad_$kick").assertExists()
        tap("mix_drums_expand")
        compose.onNodeWithTag("mix_pad_$kick").assertDoesNotExist()
        tap("mix_drums_expand")
        // The kick's ▶: the kick alone inside the kit (not the drums' strip), looped at once.
        tap("mix_pad_alone_$kick")
        assertEquals(setOf(kick), h.vm.alone.value?.target?.pads)
        assertTrue(h.vm.alone.value!!.target.channels.isEmpty())
        assertTrue(h.vm.transport.value.playing)
        val before = h.vm.project.value.mixer
        tap("mix_pad_alone_$kick")
        assertNull(h.vm.alone.value)
        assertEquals(before, h.vm.project.value.mixer)
        // Its own level: the pad's strip.
        val padBefore = volume(Mixer.padChannel(kick))
        compose.onNodeWithTag("mix_pad_fader_$kick").performTouchInput { swipeUp() }
        compose.waitForIdle()
        assertTrue(volume(Mixer.padChannel(kick)) > padBefore)
        // A track's ▶: that track alone.
        tap("mix_alone_acid")
        assertEquals(setOf(1), h.vm.alone.value?.target?.channels)
    }

    @Test
    fun aTipsFixIsOneTapAndUndoable() {
        start()
        // The 303 (the bass of an acid song) pushed 3 dB over the assistant's level.
        val start = volume(1)
        h.vm.setChannelVolume(1, GuideLevels.faderOf(GuideLevels.faderDb(start) + 3f))
        val loud = volume(1)
        idle()
        compose.onNodeWithTag("mix_tip_text").assertTextEquals("⚠️ La basse et le kick se gênent.")
        tap("mix_tip_fix")
        assertEquals(GuideLevels.faderDb(loud) - 2f, GuideLevels.faderDb(volume(1)), 0.01f)
        h.vm.undo()
        assertEquals(loud, volume(1), 0f)
        // « Tout à zéro »: the assistant's levels again, one undo step.
        h.vm.setChannelVolume(0, 0.3f)
        tap("mix_reset")
        assertEquals(start, volume(1), 0f)
        assertEquals(h.vm.project.value.mixer.startVolume(0), volume(0), 0f)
        h.vm.undo()
        assertEquals(0.3f, volume(0), 0f)
    }

    @Test
    fun theOutputSaysInWordsHowLoudItIs() {
        start()
        compose.onNodeWithTag("mix_out_words").performScrollTo().assertTextEquals("Lance ▶")
        val (m, l) = swingingMeters()
        h.vm.showMetersForTest(m, l)
        idle()
        compose.onNodeWithTag("mix_out_words").assertTextEquals("Fort et propre")
        m[NativeEngine.METER_LIMITER] = 0.3f
        h.vm.showMetersForTest(m, l)
        idle()
        compose.onNodeWithTag("mix_out_words").assertTextEquals("Ça sature")
        // Clipping: its tip first, the master 3 dB lower.
        val db = h.vm.project.value.mixer.masterDb
        tap("mix_tip_fix")
        assertEquals(db - 3f, h.vm.project.value.mixer.masterDb, 0.001f)
    }

    @Test
    fun theLoopPillPlaysThePartInALoop() {
        start()
        tap("mix_loop")
        assertTrue(h.vm.transport.value.playing && h.vm.loopPart.value)
        tap("mix_loop")
        assertTrue(!h.vm.transport.value.playing)
    }
}

/**
 * The tab's images (screenshots/nav/simple_mix.png and its variants): the meters lit as in a song
 * in full swing, to see their pixel tops; at 130 %, on a tablet, in Arabic (right to left).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
open class SimpleMixScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    internal val h = AppHarness(compose)
    protected open val prefix = "nav/"
    protected open val uiScale = 1f
    protected open val lang = Lang.FR

    @After
    fun done() = h.finish()

    @Test
    fun mix() {
        h.start(simple = true, scale = uiScale, lang = lang) { session.edit { acidTechnoSong() } }
        compose.onNodeWithTag("tab_simple_Mix").performClick()
        // The tab's ripple over before the images.
        compose.mainClock.advanceTimeBy(3000)
        compose.waitForIdle()
        // The Guide's bar settled (its analysis done): the same image each time.
        val end = System.currentTimeMillis() + 60_000
        while (h.vm.guide.ui.value.loading || h.vm.guide.ui.value.next == null) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
            compose.waitForIdle()
            check(System.currentTimeMillis() < end) { "the Guide's analysis: not in time" }
            Thread.sleep(5)
        }
        val (m, l) = swingingMeters()
        // The 303 pushed up: the tip about the bass and the kick shows.
        h.vm.setChannelVolume(1, GuideLevels.faderOf(GuideLevels.faderDb(h.vm.project.value.mixer.channels[1].volume) + 3f))
        h.vm.showMetersForTest(m, l)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(800))
        compose.mainClock.advanceTimeBy(800)
        compose.waitForIdle()
        captureScreenRoboImage("screenshots/${prefix}simple_mix.png")
        // The drums open on their sounds, each with its meter.
        compose.onNodeWithTag("mix_drums_expand").performClick()
        h.vm.showMetersForTest(m, l)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(800))
        compose.mainClock.advanceTimeBy(800)
        compose.waitForIdle()
        // The tab alone (the bar's indicator is in the first image).
        compose.onNodeWithTag("simple_mix").captureRoboImage("screenshots/${prefix}simple_mix_drums.png")
    }
}

@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class SimpleMixScale130ScreenshotTest : SimpleMixScreenshotTest() {
    override val uiScale = 1.3f
    override val prefix get() = "scale130/${lang.code}_"
}

@Config(qualifiers = "w800dp-h1280dp-hdpi")
class SimpleMixTabletScreenshotTest : SimpleMixScreenshotTest() {
    override val prefix = "tablet/"
}

class SimpleMixArabicScreenshotTest : SimpleMixScreenshotTest() {
    override val lang = Lang.AR
    override val prefix get() = "i18n/${lang.code}_"
}
