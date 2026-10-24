package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.isDisplayed
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.pocketstudio.model.i18n.Lang
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Phase 36: the whole app as it shows, each space, the Mode simple's tabs, the first launch's
 * choice, a mission and the « ? » labels, on a song of three parts (images in
 * app/screenshots/nav/; the subclasses below: 130 % in French and Dutch, tablet upright and lying
 * down, a phone lying down).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
open class NavScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    internal val h = AppHarness(compose)
    protected open val prefix = "nav/"
    protected open val uiScale = 1f
    protected open val lang = Lang.FR
    protected open fun wanted(name: String): Boolean = true

    @After
    fun back() = h.finish()

    private fun tap(tag: String) {
        val node = compose.onNodeWithTag(tag)
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
    }

    @OptIn(ExperimentalRoborazziApi::class)
    protected fun shot(name: String, simple: Boolean = false, firstLaunch: Boolean = false, prepare: () -> Unit = {}) {
        org.junit.Assume.assumeTrue(wanted(name))
        h.start(simple = simple, firstLaunch = firstLaunch, scale = uiScale, lang = lang)
        prepare()
        compose.mainClock.advanceTimeBy(800)
        compose.waitForIdle()
        captureScreenRoboImage("screenshots/$prefix$name.png")
    }

    @Test fun structure() = shot("structure")
    @Test fun structureParts() = shot("structure_parts") { tap("structure_parts") }
    @Test fun sons() = shot("sons") { tap("tab_Sons") }
    @Test fun sonsSynth() = shot("sons_synth") { tap("tab_Sons"); tap("track_${h.trackId("Lead")}") }
    @Test fun sonsVoice() = shot("sons_voice") { tap("tab_Sons"); tap("sons_Audio") }
    @Test fun mix() = shot("mix") { tap("tab_Mix") }
    @Test fun mixStrip() = shot("mix_strip") { tap("tab_Mix"); tap("strip_more_2") }
    @Test fun live() = shot("live") { tap("tab_Live") }
    @Test fun plus() = shot("plus") { tap("tab_Plus") }
    @Test fun plusSettings() = shot("plus_settings") { tap("tab_Plus"); tap("tool_Reglages") }
    @Test fun simpleSong() = shot("simple_song", simple = true)
    @Test fun simpleRhythm() = shot("simple_rhythm", simple = true) { tap("tab_simple_Rythme") }
    @Test fun simpleKeys() = shot("simple_keys", simple = true) { tap("tab_simple_Clavier") }
    @Test fun simpleSend() = shot("simple_send", simple = true) { tap("tab_simple_Envoyer") }
    @Test fun simpleMore() = shot("simple_more", simple = true) { tap("top_more") }
    @Test fun welcomeMode() = shot("welcome_mode", firstLaunch = true)
    @Test fun missionListen() = shot("mission_listen") { tap("tab_Plus"); compose.onNodeWithTag("plus_missions").performScrollTo(); tap("plus_missions") }
    @Test fun missionSound() = shot("mission_sound") {
        tap("tab_Plus"); compose.onNodeWithTag("plus_missions").performScrollTo(); tap("plus_missions"); tap("top_play"); h.vm.togglePlay()
    }
    @Test fun helpLabels() = shot("help_labels") { tap("help_labels_button") }
    @Test fun helpLabelsSons() = shot("help_labels_sons") { tap("tab_Sons"); tap("help_labels_button") }
    @Test fun helpLabelsMix() = shot("help_labels_mix") { tap("tab_Mix"); tap("help_labels_button") }
    @Test fun helpLabelsSimple() = shot("help_labels_simple", simple = true) { tap("help_labels_button") }
}

/** Réglages › Taille 130 % on a 411 dp phone, in French (screenshots/scale130/fr_nav_*). */
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
open class NavScale130ScreenshotTest : NavScreenshotTest() {
    override val uiScale = 1.3f
    override val prefix get() = "scale130/${lang.code}_nav_"
    override fun wanted(name: String) = name in SCALED

    companion object {
        val SCALED = setOf(
            "structure", "structure_parts", "sons", "sons_synth", "mix", "live", "plus", "simple_song", "simple_rhythm", "simple_keys",
            "simple_send", "welcome_mode", "mission_listen", "help_labels", "help_labels_simple",
        )
    }
}

/** The same in Dutch (the longest words). */
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class NavScale130DutchScreenshotTest : NavScale130ScreenshotTest() {
    override val lang = Lang.NL
}

/** An 11-inch tablet upright (screenshots/tablet/nav_*). */
@Config(qualifiers = "w800dp-h1280dp-hdpi")
open class NavTabletScreenshotTest : NavScreenshotTest() {
    override val prefix = "tablet/nav_"
    override fun wanted(name: String) = name in TABLET

    companion object {
        val TABLET = setOf(
            "structure", "structure_parts", "sons", "sons_synth", "mix", "live", "plus", "plus_settings", "simple_song", "simple_keys",
            "simple_send", "welcome_mode", "mission_listen", "help_labels",
        )
    }
}

/** The tablet lying down (screenshots/tablet/land_nav_*): the spaces in the rail. */
@Config(qualifiers = "w1280dp-h800dp-land-hdpi")
class NavTabletLandscapeScreenshotTest : NavTabletScreenshotTest() {
    override val prefix = "tablet/land_nav_"
}

/** A phone lying down (screenshots/nav/land_*): the rail, the bars kept low. */
@Config(qualifiers = "w860dp-h400dp-land-xxhdpi")
class NavPhoneLandscapeScreenshotTest : NavScreenshotTest() {
    override val prefix = "nav/land_"
    override fun wanted(name: String) = name in setOf("structure", "sons", "sons_synth", "mix", "live", "plus", "simple_song", "simple_keys", "help_labels")
}
