package com.pocketstudio.app.tuto

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 6, N31 fallout (capture tuto_step_130_rtl): in Arabic at 130 %, « ? Pourquoi » showed half, cut at the bottom
 * of the step's words above « Lire la suite ». On arriving at a step, « Pourquoi ? » is whole and touchable or wholly
 * out of sight, never cut, never under « Lire la suite », in every language, at 100 and 130 %, on 360 and 400 dp.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Tour6TutoPanelFitWhyTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun end() {
        I18n.lang = Lang.FR
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun whyIsNeverCutOnA360dpPhone() = check()

    @Test
    @Config(qualifiers = "w400dp-h800dp")
    fun whyIsNeverCutOnA400dpPhone() = check()

    /** As the capture tuto_step_130_rtl: the interface at 130 % (each dp bigger), right to left, a dense screen. */
    @Test
    @Config(qualifiers = "w400dp-h890dp-xxhdpi")
    fun whyIsNeverCutWithTheInterfaceAt130PercentRightToLeft() = check("jump_up", ui = 1.3f)

    private fun check(style: String = "techno", ui: Float = 1f) {
        val chapters = TutoCourse.of(style)!!.chaptersFor(TutoLevel.Facile)
        val places = chapters.flatMapIndexed { c, ch -> ch.steps.indices.filter { (ch.steps[it].why ?: ch.why).isNotEmpty() }.map { c to it } }
        assertTrue(places.isNotEmpty())
        var lang by mutableStateOf(Lang.FR)
        var scale by mutableStateOf(1f)
        var at by mutableStateOf(places.first())
        compose.setContent {
            val d = LocalDensity.current
            key(lang, scale, at) {
                val direction = if (lang.rtl || ui != 1f) LayoutDirection.Rtl else LayoutDirection.Ltr
                CompositionLocalProvider(LocalDensity provides Density(d.density * ui, scale), LocalLayoutDirection provides direction) {
                    val (c, i) = at
                    val shown = TutoUi(TutoProgress(style, TutoLevel.Facile, current = chapters[c].steps[i].id), chapters, c, i)
                    PocketStudioTheme { TutoPanel(shown, canShow = true, actions = TutoActions(), reducedMotion = true, map = tutoMap(style)) }
                }
            }
        }
        val cut = ArrayList<String>()
        for (l in Lang.entries) for (s in listOf(1f, 1.3f)) for (p in places) {
            I18n.lang = l
            lang = l
            scale = s
            at = p
            compose.waitForIdle()
            val where = "${l.code} ${(s * 100).toInt()} % ${chapters[p.first].steps[p.second].id}"
            val why = compose.onAllNodes(hasTestTag("tuto_why"), useUnmergedTree = true).fetchSemanticsNodes().firstOrNull()
            if (why == null) { cut += "$where: no « Pourquoi ? »"; continue }
            // Its bounds as seen (clipped by the words' room) against its own height.
            val seen = why.boundsInRoot
            if (seen.height > 0.5f && seen.height < why.size.height - 1f) cut += "$where: ${seen.height.toInt()} of ${why.size.height} px seen"
            val more = compose.onAllNodes(hasTestTag("tuto_text_more"), useUnmergedTree = true).fetchSemanticsNodes().firstOrNull()?.boundsInRoot
            if (more != null && seen.height > 0.5f && seen.top < more.bottom && seen.bottom > more.top) cut += "$where: under « Lire la suite »"
        }
        assertTrue("« Pourquoi ? » cut:\n" + cut.joinToString("\n"), cut.isEmpty())
    }
}
