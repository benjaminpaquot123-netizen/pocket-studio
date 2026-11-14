package com.pocketstudio.app.tuto

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
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
 * Tour 6, N22 fallout: « Chapitres » written under the list icon took width from the chapter's title and its line
 * (« La vitesse et la couleur du morceau » on two lines at 100 % on a 400 dp phone, « Caisse claire et clap » and
 * « Chapitre 3/15 · Étape 2/2 · Facile » broken in Arabic at 130 %). The title and the chapter's line keep at least
 * the room they had beside the bare 48 dp icon, in many languages, at 100 and 130 %, folded or not.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Tour6TutoPanelFitHeaderTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun end() {
        I18n.lang = Lang.FR
    }

    @Test
    @Config(qualifiers = "w400dp-h800dp")
    fun theTitleKeepsItsRoomOnA400dpPhone() = check(oneLine = { l, s -> l == Lang.FR && s == 1f })

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun theTitleKeepsItsRoomOnA360dpPhone() = check(oneLine = { _, _ -> false })

    /** [oneLine]: where « La vitesse et la couleur du morceau » held on one line before (the capture's case). */
    private fun check(oneLine: (Lang, Float) -> Boolean) {
        val courses = listOf("techno" to 0, "pop" to 2)
        var lang by mutableStateOf(Lang.FR)
        var scale by mutableStateOf(1f)
        var open by mutableStateOf(true)
        var at by mutableStateOf(courses.first())
        compose.setContent {
            val d = LocalDensity.current
            key(lang, scale, open, at) {
                CompositionLocalProvider(LocalDensity provides Density(d.density, scale)) {
                    val chapters = TutoCourse.of(at.first)!!.chaptersFor(TutoLevel.Facile)
                    val c = at.second
                    val i = chapters[c].steps.lastIndex
                    val ui = TutoUi(TutoProgress(at.first, TutoLevel.Facile, current = chapters[c].steps[i].id), chapters, c, i, open = open)
                    PocketStudioTheme { TutoPanel(ui, canShow = true, actions = TutoActions(), reducedMotion = true) }
                }
            }
        }
        val density = compose.density.density
        val wrong = ArrayList<String>()
        for (l in listOf(Lang.FR, Lang.DE, Lang.NL, Lang.AR, Lang.HI, Lang.JA, Lang.ES, Lang.PL)) for (s in listOf(1f, 1.3f)) for (o in listOf(true, false)) for (p in courses) {
            I18n.lang = l
            lang = l
            scale = s
            open = o
            at = p
            compose.waitForIdle()
            val where = "${l.code} ${(s * 100).toInt()} % ${if (o) "open" else "folded"} ${p.first} ${p.second + 1}"
            val panel = node("tuto_panel")
            val toggle = node("tuto_toggle")
            // Before N22: the school icon (22 dp), 8 dp, the title's column, the 48 dp list icon and the 48 dp fold
            // button, in the panel's 14 dp margins.
            val before = panel.size.width - (28 + 22 + 8 + 48 + 48) * density
            if (toggle.size.width < before - 1) {
                wrong += "$where: title's room ${(toggle.size.width / density).toInt()} dp < ${(before / density).toInt()} dp " +
                    "(chapters ${(node("tuto_chapters").size.width / density).toInt()} dp)"
            }
            if (oneLine(l, s) && p.first == "techno") {
                val lines = lines("tuto_chapter_title")
                if (lines != 1) wrong += "$where: title on $lines lines"
            }
        }
        assertTrue("the chapter's title squeezed:\n" + wrong.joinToString("\n"), wrong.isEmpty())
    }

    private fun node(tag: String) = compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().first()

    private fun lines(tag: String): Int {
        val layouts = ArrayList<TextLayoutResult>()
        node(tag).config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
        return layouts.firstOrNull()?.lineCount ?: -1
    }
}
