package com.pocketstudio.app

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import androidx.compose.ui.unit.dp

/**
 * The main screens again on an 11-inch tablet held upright (800 x 1280 dp, like a Galaxy Tab);
 * images in app/screenshots/tablet/. Screens with their own phone size are re-run below at the tablet size.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w800dp-h1280dp-hdpi")
class TabletScreenshotTest : ScreenshotTest() {
    override val prefix = "tablet/"
    override fun wanted(name: String) = name in SCREENS

    // Shot at the phone's own (taller) size there; here at the tablet's size (no method @Config).
    @Test override fun session() = super.session()
    @Test override fun settings() = super.settings()
    @Test override fun timeline() = super.timeline()

    companion object {
        val SCREENS = setOf(
            "rhythm", "acid", "synth", "mixer", "piano_roll", "wizard_style", "wizard_style_open", "wizard_acid", "wizard_summary", "projects", "welcome",
            "session", "top_bar", "next_steps", "audio_tracks", "remix",
            "settings", "timeline", "generate_result", "remix_project", "library", "tracks", "instruments", "tuner", "harmony_panel",
            "real_instruments", "song_ready_bar",
        )
    }
}

/** The same screens on the tablet lying down (1280 x 800 dp). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1280dp-h800dp-land-hdpi")
class TabletLandscapeScreenshotTest : ScreenshotTest() {
    override val prefix = "tablet/land_"
    override fun wanted(name: String) = name in TabletScreenshotTest.SCREENS

    @Test override fun session() = super.session()
    @Test override fun settings() = super.settings()
    @Test override fun timeline() = super.timeline()
    @Test override fun whatPlays() = super.whatPlays()
    @Test override fun automationPicker() = super.automationPicker()
    @Test override fun mixerMoves() = super.mixerMoves()
}

/**
 * Benjamin's tablet, a Galaxy Tab A11 (8.7", 1340 x 800 px, about 600 x 1006 dp) held upright.
 * Each screen is drawn in the room the app really gives it there: status bar and top bar (80 dp)
 * above, tab bar (80 dp) and system navigation (48 dp) below, shown black around the screen.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w600dp-h1006dp-tvdpi")
class SmallTabletScreenshotTest : ScreenshotTest() {
    override val prefix = "tablet8/"
    override fun wanted(name: String) = name in TabletScreenshotTest.SCREENS || name in EFFECTS || name in PHASE26 || name in LIVE || name in PHASE29 || name in PHASE30

    companion object {
        /** The Live page (phase 27), full screen: only the status bar and the system navigation around it. */
        val LIVE = setOf(
            "live_grid", "live_fx", "live_acid", "live_rec", "live_rec_done", "live_fx_lock", "live_fx_build", "live_fx_rolls", "live_fx_settings",
            "live_pads_full", "live_pads_full_fx",
        )
        /** The effects box where each sound is chosen (phase 25), on Benjamin's tablet too. */
        val EFFECTS = setOf("effects_box", "effects_picker", "effects_full", "synth_effects", "acid_effects", "effects_scenes", "effects_vibe", "effects_styles")
        /** The 303 made in the assistant and « + Piste »'s choices, on Benjamin's tablet too. */
        val PHASE26 = setOf("wizard_acid_sound", "track_add")
        /** « Nouveau projet »'s styles, each made whole in one touch, on Benjamin's tablet too. */
        val PHASE29 = setOf("projects_new")
        /** « Ce qui joue », the automation picker and curves, the mixer's badges, on Benjamin's tablet too. */
        val PHASE30 = setOf("what_plays", "automation_picker", "automation", "mixer_moves", "library_news", "library_search", "library_queue")
    }
    override val chrome = androidx.compose.foundation.layout.PaddingValues(top = 80.dp, bottom = 128.dp)
    override val fullScreenChrome = androidx.compose.foundation.layout.PaddingValues(top = 24.dp, bottom = 48.dp)

    // « Joue dans » and an ambience's settings at the tablet's own size.
    @Test override fun effectsScenes() = super.effectsScenes()
    @Test override fun effectsVibe() = super.effectsVibe()
    @Test override fun librarySearch() = super.librarySearch()
    @Test override fun session() = super.session()
    @Test override fun settings() = super.settings()
    @Test override fun timeline() = super.timeline()
}

/**
 * The same tablet lying down (1006 x 600 dp): the tabs move to an 80 dp rail on the left, so a
 * screen keeps about 926 x 472 dp once the status, top and navigation bars are taken.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1006dp-h600dp-land-tvdpi")
class SmallTabletLandscapeScreenshotTest : ScreenshotTest() {
    override val prefix = "tablet8/land_"
    override fun wanted(name: String) = name in TabletScreenshotTest.SCREENS || name in SmallTabletScreenshotTest.LIVE
    override val chrome = androidx.compose.foundation.layout.PaddingValues(start = 80.dp, top = 80.dp, bottom = 48.dp)
    override val fullScreenChrome = androidx.compose.foundation.layout.PaddingValues(top = 24.dp, bottom = 48.dp)

    @Test override fun session() = super.session()
    @Test override fun settings() = super.settings()
    @Test override fun timeline() = super.timeline()
}
