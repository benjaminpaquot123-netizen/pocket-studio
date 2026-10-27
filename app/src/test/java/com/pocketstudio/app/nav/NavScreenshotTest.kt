package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.isDisplayed
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.pocketstudio.model.i18n.Lang
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Phase 36: the whole app as it shows, each space, the Mode simple's tabs, the first launch's
 * choice, the Guide's first cards (G5) and the « ? » labels, on a song of three parts (images in
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

    /** The assistant's House song (a fixed one: the same capture every time). */
    internal fun houseSong(): com.pocketstudio.model.Project {
        val style = com.pocketstudio.model.StarterWizard.style("house")!!
        return com.pocketstudio.model.StarterWizard.build(style, style.lines.associate { it.role to null as com.pocketstudio.model.PadSample? }, style.melody.mood,
            com.pocketstudio.model.StarterWizard.defaultParts(style).associateWith { com.pocketstudio.model.StarterWizard.soundsFor(it, style).first() })
            .copy(name = "House du soir")
    }

    /** Waits for the Guide to have read the song and shown [tag] (the capture never depends on when the analysis ends). */
    internal fun awaitGuide(tag: String) {
        val end = System.currentTimeMillis() + 60_000
        while (h.vm.guide.ui.value.loading || compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty()) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(100))
            compose.waitForIdle()
            check(System.currentTimeMillis() < end) { "no $tag" }
        }
    }

    private fun tap(tag: String) {
        val node = compose.onNodeWithTag(tag)
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
    }

    @OptIn(ExperimentalRoborazziApi::class)
    protected fun shot(name: String, simple: Boolean = false, firstLaunch: Boolean = false, launchChoice: Boolean = false, prepare: () -> Unit = {}) {
        org.junit.Assume.assumeTrue(wanted(name))
        h.start(simple = simple, firstLaunch = firstLaunch, scale = uiScale, lang = lang, launchChoice = launchChoice)
        // « Que veux-tu faire ? » once it has read the project's last save (« modifié à l'instant »).
        if (launchChoice) h.awaitTag("launch_edited")
        prepare()
        // G5: the Guide's bar says the step once it has read the song: waited for, so that a capture never
        // depends on when the analysis ends (G3 review: `mission_sound` was unstable).
        val end = System.currentTimeMillis() + 60_000
        while (h.vm.guide.ui.value.loading && System.currentTimeMillis() < end) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(100))
            compose.waitForIdle()
        }
        // Saved before the capture: « Sauvegardé » always shows (the wait above let the autosave run or not).
        // Not with runBlocking: the save may need the main thread, which this test runs on.
        var saved = false
        @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) { h.vm.flushNow(); saved = true }
        val until = System.currentTimeMillis() + 30_000
        while (!saved && System.currentTimeMillis() < until) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(50))
            compose.waitForIdle()
        }
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
    @Test fun launch() = shot("launch", launchChoice = true)
    @Test fun launchSimple() = shot("launch_simple", simple = true, launchChoice = true)
    /** G5: « Mon premier morceau » (Plus): audit final, a new song from the assistant (the open one stays as it is). */
    @Test fun guideFirst() = shot("guide_first") {
        tap("tab_Plus"); compose.onNodeWithTag("plus_missions").performScrollTo(); tap("plus_missions")
        awaitGuide("wizard")
    }
    /** G5: a song just made: the Guide opens on « Ton morceau est prêt ». */
    @Test fun guideReady() = shot("guide_ready") {
        h.vm.session.edit { com.pocketstudio.model.guide.GuideStart.begin(houseSong(), com.pocketstudio.model.guide.GuideStart.SONG) }
        awaitGuide("guide_card_start.ready")
    }
    @Test fun helpLabels() = shot("help_labels") { tap("help_labels_button") }
    @Test fun helpLabelsSons() = shot("help_labels_sons") { tap("tab_Sons"); tap("help_labels_button") }
    @Test fun helpLabelsMix() = shot("help_labels_mix") { tap("tab_Mix"); tap("help_labels_button") }
    @Test fun helpLabelsSimple() = shot("help_labels_simple", simple = true) { tap("help_labels_button") }

    /** Lot 2: windows of their own (a sheet, the share sheet) at the interface size. */
    @Test fun sheetPad() = shot("sheet_pad") {
        tap("tab_Sons")
        compose.onNodeWithTag("pad_0").performTouchInput { longClick() }
        compose.waitForIdle()
    }
    @Test fun sheetShare() = shot("sheet_share") { tap("top_share") }

    /** Lot 2 review: Sons › a track after a take (lying down: the take's card beside the keys). */
    @Test fun sonsSynthTake() = shot("sons_synth_take") {
        tap("tab_Sons")
        tap("track_${h.trackId("Lead")}")
        val vm = h.vm
        vm.playingForTest = true
        vm.recordTake(com.pocketstudio.app.record.TakeSource.KEYS)
        for ((pitch, from, to) in listOf(Triple(60, 1.2, 2.9), Triple(64, 3.8, 5.0))) {
            vm.recordPositionForTest = from
            vm.playedClockForTest = from
            vm.synthNoteOn(pitch, 0.8f)
            vm.recordPositionForTest = to
            vm.playedClockForTest = to
            vm.synthNoteOff(pitch)
        }
        vm.recordTake(com.pocketstudio.app.record.TakeSource.KEYS)
        compose.waitForIdle()
    }
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
            "simple_send", "welcome_mode", "guide_first", "guide_ready", "help_labels", "help_labels_simple", "launch", "launch_simple",
            "sheet_pad", "sheet_share",
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
            "simple_send", "welcome_mode", "guide_first", "guide_ready", "help_labels", "launch",
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
    override fun wanted(name: String) = name in setOf("structure", "sons", "sons_synth", "sons_synth_take", "mix", "live", "plus", "simple_song", "simple_keys", "help_labels", "launch")
}

/**
 * Lot 2: Benjamin's Galaxy Tab A11 (8.7", 1340 × 800 px, tvdpi) as the app really gets it, the
 * status bar (24 dp) and the navigation bar (48 dp) taken: 600 × 934 dp upright
 * (screenshots/tabA11/nav_*). The 11" classes above are not his tablet.
 */
@Config(qualifiers = "sw600dp-w600dp-h934dp-port-tvdpi")
open class NavTabA11ScreenshotTest : NavScreenshotTest() {
    override val prefix get() = "tabA11/${if (uiScale != 1f) "130_" else ""}nav_"
    override fun wanted(name: String) = name in A11

    companion object {
        val A11 = setOf(
            "structure", "structure_parts", "sons", "sons_synth", "mix", "live", "plus",
            "simple_song", "simple_rhythm", "simple_keys", "simple_send", "launch", "sheet_pad", "sons_synth_take",
        )
    }
}

/** The same tablet lying down: 1006 × 528 dp (screenshots/tabA11/land_nav_*). */
@Config(qualifiers = "sw600dp-w1006dp-h528dp-land-tvdpi")
open class NavTabA11LandscapeScreenshotTest : NavTabA11ScreenshotTest() {
    override val prefix get() = "tabA11/${if (uiScale != 1f) "130_" else ""}land_nav_"
}

/** The A11 upright at 130 % (its owner's size): a sheet open, the keyboard, the five spaces. */
@Config(qualifiers = "sw600dp-w600dp-h934dp-port-tvdpi")
class NavTabA11Scale130ScreenshotTest : NavTabA11ScreenshotTest() {
    override val uiScale = 1.3f
    override fun wanted(name: String) = name in A11_130

    companion object {
        val A11_130 = setOf("structure", "sons", "sons_synth", "mix", "live", "plus", "simple_song", "simple_keys", "launch", "sheet_pad", "sheet_share")
    }
}

/** The A11 lying down at 130 %. */
@Config(qualifiers = "sw600dp-w1006dp-h528dp-land-tvdpi")
class NavTabA11LandscapeScale130ScreenshotTest : NavTabA11LandscapeScreenshotTest() {
    override val uiScale = 1.3f
    override fun wanted(name: String) = name in NavTabA11Scale130ScreenshotTest.A11_130
}
