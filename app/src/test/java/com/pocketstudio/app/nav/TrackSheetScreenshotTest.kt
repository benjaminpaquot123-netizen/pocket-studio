package com.pocketstudio.app.nav

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.pocketstudio.model.LayerRole
import com.pocketstudio.model.LayerSound
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.Project
import com.pocketstudio.model.SoundLibrary
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.instrumentTracks
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

/**
 * G3 on an assistant's jump up song: the bass's sheet (its five columns, then its history with a
 * layer added by hand), its layers editor with that layer, and « + Ajouter une couche » choosing
 * a sound (images in app/screenshots/nav/sheet_*; the subclasses: 130 % on a phone, the tablet).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
open class TrackSheetScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    internal val h = AppHarness(compose)
    protected open val prefix = "nav/"
    protected open val uiScale = 1f

    @After
    fun done() = h.finish()

    private fun jumpUp(): Project {
        val style = StarterWizard.style("jump_up")!!
        return StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? }, style.melody.mood,
            StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() })
    }

    private fun tap(tag: String) {
        val node = compose.onNodeWithTag(tag)
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
    }

    private fun idle(ms: Long = 800) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
        compose.mainClock.advanceTimeBy(ms)
        compose.waitForIdle()
    }

    private fun await(ok: () -> Boolean) {
        val end = System.currentTimeMillis() + 60_000
        while (!ok()) {
            idle(100)
            check(System.currentTimeMillis() < end) { "the Guide's analysis: not in time" }
            Thread.sleep(5)
        }
    }

    @OptIn(ExperimentalRoborazziApi::class)
    private fun shot(name: String, layer: Boolean = true, prepare: () -> Unit = {}) {
        h.start(scale = uiScale) { session.edit { jumpUp() } }
        await { !h.vm.guide.ui.value.loading }
        val bass = h.vm.project.value.instrumentTracks.first { it.name == "Basse" }.id
        if (layer) {
            val pad = SoundLibrary.SYNTH.first()
            h.vm.addLayer(bass, LayerSound.synth(pad.sound), LayerRole.AIR, pad.name)
            await { h.vm.guide.ui.value.sheets.any { it.lines.isNotEmpty() } }
        }
        tap("tab_Sons")
        compose.onNodeWithTag("track_$bass").performScrollTo().performTouchInput { longClick() }
        compose.waitForIdle()
        tap("track_menu_sheet")
        idle()
        prepare()
        idle()
        captureScreenRoboImage("screenshots/${prefix}sheet_$name.png")
    }

    @Test fun sheet() = shot("bass", layer = false)
    @Test fun history() = shot("history") { compose.onNodeWithTag("track_sheet_history").performScrollTo() }
    @Test fun layers() = shot("layers") { tap("track_sheet_layers") }
    @Test fun addLayer() = shot("layers_add") { tap("track_sheet_layers"); tap("layers_add_body") }
}

/** Réglages › Taille 130 % on a 411 dp phone (screenshots/scale130/fr_sheet_*). */
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class TrackSheetScale130ScreenshotTest : TrackSheetScreenshotTest() {
    override val uiScale = 1.3f
    override val prefix = "scale130/fr_"
}

/** An 11-inch tablet upright (screenshots/tablet/sheet_*). */
@Config(qualifiers = "w800dp-h1280dp-hdpi")
class TrackSheetTabletScreenshotTest : TrackSheetScreenshotTest() {
    override val prefix = "tablet/"
}
