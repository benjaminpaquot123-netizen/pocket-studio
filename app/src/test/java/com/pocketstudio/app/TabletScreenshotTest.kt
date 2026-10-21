package com.pocketstudio.app

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

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
            "rhythm", "acid", "synth", "mixer", "piano_roll", "wizard_style", "wizard_summary", "projects", "welcome",
            "session", "top_bar", "next_steps", "audio_tracks", "remix",
            "settings", "timeline", "generate_result", "remix_project", "library", "tracks", "instruments",
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
}
