package com.pocketstudio.app.nav

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.Project
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.i18n.Lang
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
 * Le Guide (G2) on an assistant's jump up song: its bar on Structure, its panel on the next card,
 * a card that changes the song (▶ to hear it first), the welcome after days, the Mode simple's
 * panel and « Voir le réglage » (images in app/screenshots/nav/guide_*; the subclasses: 130 %
 * on a phone, the 11-inch tablet and Benjamin's A11).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
open class GuideScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    internal val h = AppHarness(compose)
    protected open val prefix = "nav/"
    protected open val uiScale = 1f
    protected open val lang = Lang.FR

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

    private fun await(ok: () -> Boolean) {
        val end = System.currentTimeMillis() + 60_000
        while (!ok()) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
            compose.waitForIdle()
            check(System.currentTimeMillis() < end) { "the Guide's analysis: not in time" }
            Thread.sleep(5)
        }
    }

    @OptIn(ExperimentalRoborazziApi::class)
    private fun shot(name: String, simple: Boolean = false, project: Project = jumpUp(), prepare: () -> Unit = {}) {
        h.start(simple = simple, scale = uiScale, lang = lang) { session.edit { project } }
        await { !h.vm.guide.ui.value.loading && h.vm.guide.ui.value.next != null && h.vm.guide.ui.value.simple == simple && (!simple || h.vm.guide.ui.value.further.isNotEmpty()) }
        prepare()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(800))
        compose.mainClock.advanceTimeBy(800)
        compose.waitForIdle()
        captureScreenRoboImage("screenshots/${prefix}guide_$name.png")
    }

    @Test fun bar() = shot("bar")
    @Test fun panel() = shot("panel") { tap("guide_bar") }
    @Test fun card() = shot("card") {
        tap("guide_bar"); tap("guide_stage_3"); tap("guide_item_fx.sub")
    }
    @Test fun plan() = shot("plan") {
        tap("guide_bar"); tap("guide_stage_3")
        compose.onNodeWithTag("guide_item_fx.sidechain").performScrollTo()
    }
    @Test fun back() = shot("back", project = jumpUp().let { p -> p.copy(guide = p.guide.copy(step = "fx.sub", lastUsed = System.currentTimeMillis() - 3 * 24 * 3600 * 1000L)) }) {
        tap("guide_bar")
    }
    @Test fun simple() = shot("simple", simple = true) {
        tap("guide_bar"); tap("guide_further_toggle")
    }
    /** G4: « Voilà ce que j'ai mis », effect by effect. */
    @Test fun review() = shot("review") {
        tap("guide_bar"); compose.onNodeWithTag("guide_effects").performScrollTo()
    }
    /** G4: a check of stage 6, the phone's speaker. */
    @Test fun check() = shot("check") {
        tap("guide_bar"); tap("guide_stage_6"); tap("guide_item_mix.phone")
    }
    @Test fun spot() = shot("spot") {
        tap("guide_bar"); tap("guide_stage_0"); tap("guide_item_frame.form"); tap("guide_open_parts")
    }
}

/** Réglages › Taille 130 % on a 411 dp phone (screenshots/scale130/fr_guide_*). */
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class GuideScale130ScreenshotTest : GuideScreenshotTest() {
    override val uiScale = 1.3f
    override val prefix get() = "scale130/${lang.code}_"
}

/** An 11-inch tablet upright (screenshots/tablet/guide_*): the panel takes half the height. */
@Config(qualifiers = "w800dp-h1280dp-hdpi")
class GuideTabletScreenshotTest : GuideScreenshotTest() {
    override val prefix = "tablet/"
}

/** A phone lying down (screenshots/nav/land_guide_*). */
@Config(qualifiers = "w860dp-h400dp-land-xxhdpi")
class GuidePhoneLandscapeScreenshotTest : GuideScreenshotTest() {
    override val prefix = "nav/land_"
}
