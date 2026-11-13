package com.pocketstudio.app.tuto

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
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
 * Tour 6, N31: « Lire la suite » (the fade at the bottom of the step's words) lay over « ▸ Pourquoi ? »: a touch on
 * it read on instead of opening the reason (11 languages at 130 %, 360 dp). No control of the panel under the fade,
 * in any language, at 100 and 130 %.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp")
class Tour6TutoPanelMoreWordsTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun end() {
        I18n.lang = Lang.FR
    }

    @Test
    fun whyIsNeverUnderReadOn() {
        val chapters = TutoCourse.of("techno")!!.chaptersFor(TutoLevel.Facile)
        // The steps with a « Pourquoi ? ».
        val places = chapters.flatMapIndexed { c, ch -> ch.steps.indices.filter { (ch.steps[it].why ?: ch.why).isNotEmpty() }.map { c to it } }
        assertTrue(places.isNotEmpty())
        var lang by mutableStateOf(Lang.FR)
        var scale by mutableStateOf(1f)
        var at by mutableStateOf(places.first())
        compose.setContent {
            val d = LocalDensity.current
            key(lang, scale, at) {
                CompositionLocalProvider(LocalDensity provides Density(d.density, scale)) {
                    val (c, i) = at
                    val ui = TutoUi(TutoProgress("techno", TutoLevel.Facile, current = chapters[c].steps[i].id), chapters, c, i)
                    PocketStudioTheme { TutoPanel(ui, canShow = true, actions = TutoActions(), reducedMotion = true, map = tutoMap("techno")) }
                }
            }
        }
        val under = ArrayList<String>()
        for (l in Lang.entries) for (s in listOf(1f, 1.3f)) for (p in places) {
            I18n.lang = l
            lang = l
            scale = s
            at = p
            compose.waitForIdle()
            val more = compose.onAllNodes(androidx.compose.ui.test.hasTestTag("tuto_text_more"), useUnmergedTree = true).fetchSemanticsNodes().firstOrNull()?.boundsInRoot ?: continue
            val why = compose.onAllNodes(androidx.compose.ui.test.hasTestTag("tuto_why"), useUnmergedTree = true).fetchSemanticsNodes().firstOrNull()?.boundsInRoot ?: continue
            if (why.top < more.bottom && why.bottom > more.top && why.height > 0f) under += "${l.code} ${(s * 100).toInt()} % ${chapters[p.first].steps[p.second].id}"
        }
        assertTrue("« Pourquoi ? » under « Lire la suite »: $under", under.isEmpty())
    }
}
