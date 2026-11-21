package com.pocketstudio.app.tuto

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
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
 * Tour 9, F003 (seats t9-11, t9-29, t9-41, t9-56): « La flèche du panneau replie le panneau quand il est ouvert ; jamais
 * de plein écran par surprise. » At 200 % (Hindi, seat t9-29), the arrow ⌃ of the folded panel unfolded it over the whole
 * work area (the piano under it gone). Unfolded, the panel takes at most [MAX_SHARE] of the screen's height,
 * on every step, and its arrow folds it again.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Tour9L13TutoNeverFullScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun end() {
        I18n.lang = Lang.FR
    }

    @Test
    @Config(qualifiers = "w360dp-h780dp")
    fun unfoldedThePanelLeavesTheWorkInSight() {
        val style = "jump_up"
        val chapters = TutoCourse.of(style)!!.chaptersFor(TutoLevel.Facile)
        val places = chapters.flatMapIndexed { c, ch -> ch.steps.indices.map { c to it } }
        var lang by mutableStateOf(Lang.FR)
        var at by mutableStateOf(places.first())
        var open by mutableStateOf(true)
        val toggles = intArrayOf(0)
        compose.setContent {
            val d = LocalDensity.current
            key(lang, at) {
                CompositionLocalProvider(
                    LocalDensity provides Density(d.density, 2f),
                    LocalLayoutDirection provides if (lang.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                ) {
                    val (c, i) = at
                    val shown = TutoUi(TutoProgress(style, TutoLevel.Facile, current = chapters[c].steps[i].id), chapters, c, i, open = open)
                    // As in the app: the work above, the panel at the bottom of the whole screen.
                    PocketStudioTheme {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                            TutoPanel(shown, canShow = true, actions = TutoActions(toggle = { toggles[0]++; open = !open }), reducedMotion = true, map = tutoMap(style))
                        }
                    }
                }
            }
        }
        val wrong = ArrayList<String>()
        fun node(tag: String) = compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().firstOrNull()
        val screen = compose.onRoot().fetchSemanticsNode().size.height.toFloat()
        var tallest = 0f
        for (l in listOf(Lang.FR, Lang.HI)) for (p in places) {
            I18n.lang = l
            lang = l
            at = p
            open = true
            compose.waitForIdle()
            val where = "${l.code} 200 % ${chapters[p.first].steps[p.second].id}"
            val panel = node("tuto_panel") ?: run { wrong += "$where: no panel"; null } ?: continue
            val share = panel.size.height / screen
            tallest = maxOf(tallest, share)
            if (share > MAX_SHARE + 0.005f) wrong += "$where: unfolded, the panel takes ${(share * 100).toInt()} % of the screen"
        }
        // Its arrow folds it (and the folded panel is a line, not a card).
        I18n.lang = Lang.HI
        lang = Lang.HI
        at = places.first { chapters[it.first].steps[it.second].spot.isNotEmpty() }
        open = true
        compose.waitForIdle()
        val before = toggles[0]
        compose.onNode(hasTestTag("tuto_fold")).performClick()
        compose.waitForIdle()
        if (toggles[0] != before + 1 || open) wrong += "the arrow did not fold the open panel"
        assertTrue("tallest ${(tallest * 100).toInt()} %; panel too high:\n" + wrong.take(30).joinToString("\n"), wrong.isEmpty())
    }
}

/** The most of the screen's height the unfolded panel takes. */
private const val MAX_SHARE = 0.6f
