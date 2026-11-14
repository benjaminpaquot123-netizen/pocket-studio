package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.app.tuto.PrefsTutoStore
import com.pocketstudio.app.tuto.TutoTestHooks
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoTarget
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Audit ultra round 6, lot 3 « guide-et-coquille »: the screens the lot changed, on a 360 dp phone, in Simple and in
 * Pro, at 100 % and at 130 % (images in app/screenshots/tour6/shell_*). States of screens the census already holds
 * (ScreenRegistry), looked at for this lot: not a census class of its own.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-xhdpi")
class Tour6GuideCoquilleLookTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)
    private val shell = Tour6Shell(compose)

    private val ears = object : GuideListenPlayer {
        override fun play(pcm: Pcm, ended: () -> Unit): Boolean = true
        override fun stop() = Unit
    }

    @After
    fun back() {
        TutoTestHooks.kitOffer = true
        TutoTestHooks.player = null
        h.finish()
    }

    private fun start(simple: Boolean, scale: Float, lang: Lang = Lang.FR) {
        h.start(simple = simple, scale = scale, lang = lang)
        h.awaitGuide()
        assertTrue(shell.reached { !h.vm.guide.ui.value.loading })
        shell.idle(300)
    }

    @OptIn(ExperimentalRoborazziApi::class)
    private fun shot(name: String) {
        shell.idle(800)
        captureScreenRoboImage("screenshots/tour6/shell_$name.png")
    }

    private fun mode(simple: Boolean) = if (simple) "simple" else "pro"
    private fun size(scale: Float) = (scale * 100).toInt().toString()

    /** G1: the open panel, its card cut: « ↓ La suite »; then the card's end. */
    private fun panel(simple: Boolean, scale: Float) {
        start(simple, scale)
        shell.tap("guide_bar")
        shot("guide_panel_more_${mode(simple)}_${size(scale)}")
        var touches = 0
        while (shell.has("guide_more_below") && touches++ < 40) { shell.tap("guide_more_below"); shell.idle(600) }
        shot("guide_panel_end_${mode(simple)}_${size(scale)}")
    }

    @Test fun guidePanelSimple100() = panel(true, 1f)
    @Test fun guidePanelSimple130() = panel(true, 1.3f)
    @Test fun guidePanelPro100() = panel(false, 1f)
    @Test fun guidePanelPro130() = panel(false, 1.3f)

    /** G1: the Guide open, then the thumb's « ⋮ Plus »: the panel folded, the Plus page whole. */
    @Test
    fun plusAfterTheOpenGuideSimple100() {
        start(true, 1f)
        shell.tap("guide_bar")
        shell.tap("thumb_more")
        shot("plus_after_guide_simple_100")
    }

    /** G1: the keys' page of a short screen, without the Guide's bar. */
    @Test
    fun keysSimple130() {
        start(true, 1.3f)
        shell.tap("tab_simple_Clavier")
        shot("keys_simple_130")
    }

    @Test
    fun keysSimple100() {
        start(true, 1f)
        shell.tap("tab_simple_Clavier")
        shot("keys_simple_100")
    }

    /** N17: « Passer en mode Simple » asks. */
    @Test
    fun modeAskedFromPlusPro100() {
        start(false, 1f)
        shell.tap("tab_Plus")
        shell.tap("plus_mode")
        shot("mode_asked_pro_100")
    }

    @Test
    fun modeAskedFromPlusSimple130() {
        start(true, 1.3f)
        shell.tap("thumb_more".takeIf { shell.has(it) } ?: "top_more")
        shell.tap("plus_mode")
        shot("mode_asked_simple_130")
    }

    /** R19: the pages of Sons › Voix, the row going on past the edge, then at its end. */
    private fun pages(scale: Float, lang: Lang) {
        start(false, scale, lang)
        shell.tap("tab_Sons")
        shell.tap("sons_Audio")
        shot("audio_pages_${lang.code}_${size(scale)}")
        shell.tap("audio_page_3".takeIf { shell.bounds(it)?.width?.let { w -> w > 1f } == true } ?: "audio_page_2")
        shot("audio_pages_end_${lang.code}_${size(scale)}")
    }

    @Test fun audioPagesFrench100() = pages(1f, Lang.FR)
    @Test fun audioPagesTurkish130() = pages(1.3f, Lang.TR)

    /** T13, N1, U27: a tutorial quit (no Guide's bar), the Guide asked for during a tutorial, the Mix in a tutorial. */
    private fun tuto(simple: Boolean, active: Boolean, scale: Float = 1f) {
        val target = TutoTarget.of("house")!!
        TutoTestHooks.kitOffer = false
        TutoTestHooks.player = ears
        h.start(simple = simple, scale = scale, build = {
            session.edit { target.blank() }
            PrefsTutoStore(h.app).save(projectId, TutoProgress("house", if (simple) TutoLevel.Facile else TutoLevel.Pro, active = active))
        })
        assertTrue(shell.reached { shell.has(if (active) "tuto_panel" else "tuto_resume") })
        shell.reached(3_000) { !h.vm.guide.ui.value.loading }
    }

    @Test
    fun tutoQuitSimple100() {
        tuto(simple = true, active = false)
        shot("tuto_quit_simple_100")
    }

    @Test
    fun guideAskedDuringATutoSimple100() {
        tuto(simple = true, active = true)
        shell.tap("thumb_more")
        shell.tap("tool_Aide")
        shell.tap("help_steps")
        assertTrue(shell.reached(5_000) { shell.has("guide_panel") })
        shot("guide_over_tuto_simple_100")
    }

    @Test
    fun mixDuringATutoPro100() {
        tuto(simple = false, active = true)
        shell.tap("tab_Mix")
        shot("mix_in_tuto_pro_100")
    }

    /** G3: « Fais une deuxième partie »: the sentence names the screen to open, « Plus tard » is a small button. */
    @Test
    fun secondPartCardPro100() {
        h.start(simple = false, build = { toggleHit(0, 0) })
        h.awaitGuide()
        assertTrue(shell.reached { h.vm.guide.ui.value.cards.containsKey("start.parts") })
        shell.tap("guide_bar")
        shell.reached(3_000) { shell.has("guide_stage_-1") || shell.has("guide_item_start.parts") }
        if (shell.has("guide_stage_-1") && !shell.has("guide_item_start.parts")) shell.tap("guide_stage_-1")
        if (shell.has("guide_item_start.parts")) shell.tap("guide_item_start.parts")
        shot("guide_second_part_pro_100")
    }
}
