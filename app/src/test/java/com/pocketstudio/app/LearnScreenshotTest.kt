package com.pocketstudio.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.pocketstudio.app.export.video.VideoExportUi
import com.pocketstudio.app.export.video.VideoFrameRenderer
import com.pocketstudio.app.export.video.VideoSheetActions
import com.pocketstudio.app.export.video.VideoSheetContent
import com.pocketstudio.app.export.video.VideoSheetUi
import com.pocketstudio.app.export.video.previewFrame
import com.pocketstudio.app.learn.GlossaryActions
import com.pocketstudio.app.learn.GlossaryPage
import com.pocketstudio.app.learn.GlossaryPlay
import com.pocketstudio.app.midi.KnobHint
import com.pocketstudio.app.midi.KnobHintBar
import com.pocketstudio.app.midi.KnobPages
import com.pocketstudio.app.midi.KnobPanel
import com.pocketstudio.app.midi.KnobPanelActions
import com.pocketstudio.app.midi.KnobScreen
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.model.Project
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.video.VideoLevels
import com.pocketstudio.model.video.VideoPlan
import com.pocketstudio.model.video.VideoSection
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.Palettes
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.PI
import kotlin.math.sin

/**
 * Phase 39: the glossary, the video sheet and one of its pictures, the MIDI knobs' hint and list,
 * on the phone (app/screenshots/learn_*.png); subclasses shoot them again at 130 % and on a tablet.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
open class LearnScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    protected open val prefix = ""
    protected open val uiScale = 1f
    protected open val lang = Lang.FR

    @Before
    fun language() {
        SettingsStore.update(ApplicationProvider.getApplicationContext()) { it.copy(language = lang, englishNotes = lang != Lang.FR) }
    }

    @After
    fun french() {
        SettingsStore.update(ApplicationProvider.getApplicationContext()) { it.copy(language = Lang.FR, englishNotes = false) }
    }

    private fun shoot(name: String, content: @Composable () -> Unit) {
        compose.setContent {
            com.pocketstudio.app.ui.UiScaled(uiScale) {
                PocketStudioTheme(Palettes.Studio) {
                    Box(Modifier.fillMaxSize().background(Neon.Background)) { content() }
                }
            }
        }
        compose.onRoot().captureRoboImage("screenshots/${prefix}learn_$name.png")
    }

    /** Intro 8 s, Couplet 6 s, Drop 16 s: what a song's 30 seconds around its Drop look like. */
    private val plan = VideoPlan(
        "Nuit d'été sur le port", startFrame = 48_000 * 50, frames = 30L * 48_000,
        sections = listOf(VideoSection("Montée", 0.0, 8.0), VideoSection("Drop", 8.0, 24.0), VideoSection("Break", 24.0, 30.0)), part = 3,
    )

    private fun levels(): VideoLevels.Levels {
        val n = plan.videoFrames
        val rms = FloatArray(n) { i -> val t = i / 30.0; (if (t < 8) 0.04 + 0.02 * t else if (t < 24) 0.28 + 0.06 * sin(t * 2 * PI) else 0.06).toFloat() }
        return VideoLevels.Levels(rms, FloatArray(n) { rms[it] * 2.4f })
    }

    @Test
    fun glossary() = shoot("glossary") { GlossaryPage("", null, GlossaryActions()) }

    @Test
    fun glossaryPlaying() = shoot("glossary_playing") {
        GlossaryPage("swing", GlossaryPlay("swing", preparing = false, take = 1), GlossaryActions())
    }

    @Test
    fun videoSheet() = shoot("video_sheet") {
        val preview = previewFrame(plan).asImageBitmap()
        VideoSheetContent(VideoSheetUi(part = "Drop", seconds = 30.0, preview = preview), VideoSheetActions(), Modifier.background(Neon.Surface))
    }

    @Test
    fun videoSheetRunning() = shoot("video_sheet_running") {
        VideoSheetContent(
            VideoSheetUi(part = "Drop", seconds = 30.0, run = VideoExportUi(running = true, stage = VideoExportUi.Stage.Pictures, progress = 0.62f)),
            VideoSheetActions(), Modifier.background(Neon.Surface),
        )
    }

    /** One picture of the video itself, at its full 720 × 1280 (in the Drop, the next part announced). */
    @Test
    fun videoFrame() {
        val bmp = VideoFrameRenderer(plan, levels()).frame((22.4 * 30).toInt())
        bmp.captureRoboImage("screenshots/${prefix}learn_video_frame.png")
    }

    private fun hint(touched: Int, turn: Int, learning: Int? = null): KnobHint {
        val p = Project()
        val slots = KnobPages.slots(KnobScreen.Acide, p, 0, com.pocketstudio.app.ui.mixerStrips(p))
        return KnobHint(
            KnobScreen.Acide, slots, slots.map { KnobPages.valueOf(it.target, p, com.pocketstudio.app.midi.LiveKnobs()) },
            ccs = listOf(74, 71, 76, 77, 93, 18, 19, 16), touched = touched, turn = turn, learning = learning, device = "Arturia MiniLab 3",
        )
    }

    @Test
    fun midiHint() = shoot("midi_hint") {
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
            KnobHintBar(hint(touched = 0, turn = 1), open = {})
            Box(Modifier.padding(6.dp))
            KnobHintBar(hint(touched = 4, turn = 0), open = {})
        }
    }

    @Test
    fun midiPanel() = shoot("midi_panel") {
        KnobPanel(hint(touched = -1, turn = 0, learning = 2), emptyList(), enabled = true, KnobPanelActions(), Modifier.background(Neon.Surface))
    }
}

/** At 130 % on a 411 dp phone (like a 316 dp one). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class LearnScale130ScreenshotTest : LearnScreenshotTest() {
    override val prefix = "scale130/fr_"
    override val uiScale = 1.3f
}

/** The same in Dutch at 130 % (the longest texts). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class LearnDutchScale130ScreenshotTest : LearnScreenshotTest() {
    override val prefix = "scale130/nl_"
    override val uiScale = 1.3f
    override val lang = Lang.NL
}

/** On an 11-inch tablet held upright. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w800dp-h1280dp-hdpi")
class LearnTabletScreenshotTest : LearnScreenshotTest() {
    override val prefix = "tablet/"
}
