package com.pocketstudio.app.tuto

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
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
 * Tour 7, control of « fable-repar » (seat c, V4; Korean at 130 %, techno): under « Techno 킷 다운로드 » a long instruction
 * stopped in the middle of a word with no « … »; in Japanese the instruction unfolded by « Lire la suite » stopped
 * clean too (« 次の周の直 »). Whenever the step's words go on under their room, a « … » is shown whole at the end of the
 * last line in sight; never when they are all in sight.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp")
class Tour7TutoCutWordsSaidTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun end() { TutoHints.reset(); I18n.lang = Lang.FR }

    private fun nodes(tag: String) = compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes()

    @Test
    fun wordsThatGoOnSaySo() {
        val style = "techno"
        val chapters = TutoCourse.of(style)!!.chaptersFor(TutoLevel.Facile)
        val starts = chapters.indices.flatMap { c -> chapters[c].steps.indices.map { c to it } }.filter { (c, i) -> chapters[c].steps[i].id != "ready" }
        var lang by mutableStateOf(Lang.KO)
        var scale by mutableStateOf(1.3f)
        var at by mutableStateOf(starts.first())
        var grid by mutableStateOf(false)
        compose.setContent {
            val d = LocalDensity.current
            key(lang, scale, at, grid) {
                TutoHints.gridInSight = grid
                TutoHints.cells = if (grid) 0 to setOf(0, 4) else null
                CompositionLocalProvider(LocalDensity provides Density(d.density, scale)) {
                    val (c, i) = at
                    val shown = TutoUi(TutoProgress(style, TutoLevel.Facile, current = chapters[c].steps[i].id), chapters, c, i, open = true)
                    PocketStudioTheme {
                        TutoPanel(shown, canShow = true, actions = TutoActions(), reducedMotion = true,
                            kit = { TutoKitCard("Techno", "4 Mo", progress = null, error = null, download = {}, later = {}) })
                    }
                }
            }
        }
        val faults = ArrayList<String>()
        var cut = 0
        var unfolded = 0
        var whole = 0
        fun look(where: String) {
            val text = nodes("tuto_text").firstOrNull() ?: return
            val mark = nodes("tuto_text_cut").firstOrNull()
            if (nodes("tuto_text_more").isEmpty()) {
                whole++
                if (mark != null) faults += "$where: « … » under words all in sight"
                return
            }
            cut++
            if (mark == null) { faults += "$where: the words go on under their room, no « … » at the end of the last line in sight"; return }
            val m = mark.boundsInRoot
            val t = text.boundsInRoot
            if (m.height < mark.size.height - 1f || m.width < mark.size.width - 1f) faults += "$where: « … » not whole (${m.height.toInt()} of ${mark.size.height} px)"
            // Over the words' last line in sight: under the instruction's top, above all that is pinned under the words.
            val under = listOf("tuto_kit_download", "tuto_why", "tuto_text_more").flatMap { nodes(it) }.minOf { it.boundsInRoot.top }
            if (m.bottom > under + 1f || m.top < t.top - 1f) faults += "$where: « … » out of the words in sight (mark ${m.top.toInt()}..${m.bottom.toInt()}, words from ${t.top.toInt()}, pinned from ${under.toInt()})"
        }
        for (l in listOf(Lang.KO, Lang.JA)) for (s in listOf(1.3f, 1f)) for (g in listOf(false, true)) for (p in starts) {
            I18n.lang = l; lang = l; scale = s; grid = g; at = p
            compose.waitForIdle()
            val where = "${l.code} ${(s * 100).toInt()} % ${if (g) "grid" else "words"} ${chapters[p.first].steps[p.second].id}"
            look(where)
            // The tester's Japanese path: over the grid's cells, « Lire la suite » unfolds the words.
            if (g && nodes("tuto_text_more").isNotEmpty()) {
                compose.onAllNodes(hasTestTag("tuto_text_more"), useUnmergedTree = true)[0].performClick()
                compose.waitForIdle()
                if (nodes("tuto_text_less").isNotEmpty()) { unfolded++; look("$where, unfolded") }
            }
        }
        assertTrue("${faults.size} of $cut cut ($unfolded unfolded, $whole whole):\n" + faults.take(40).joinToString("\n"), faults.isEmpty())
        assertTrue("too few met: $cut cut, $unfolded unfolded, $whole whole", cut >= 40 && unfolded >= 10 && whole >= 20)
    }
}
