package com.pocketstudio.app.tuto

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoNeed
import com.pocketstudio.model.tuto.TutoPlay
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoTarget
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 7, P7 (docs/audits/ultra/tour-7-plan-p6-p7.md § 2, § 3.2 (c)): one moment on screen. In Facile, a step whose
 * engine button is still to press shows « À toi : touche « <button> ». » as its words, never the step's own text next to
 * it; in Pro the step's text stays (the button is under it), never the phrase.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class TutoOneMomentShownTest {
    @get:Rule
    val compose = createComposeRule()

    private fun words(): String = compose.onAllNodes(hasTestTag("tuto_text"), useUnmergedTree = true).fetchSemanticsNodes()
        .joinToString(" ") { n -> n.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }.orEmpty() }

    @Test
    fun facileShowsTheNeedAloneAndProKeepsItsText() {
        var shown by mutableStateOf<TutoUi?>(null)
        compose.setContent { PocketStudioTheme { shown?.let { key(it) { TutoPanel(it, canShow = true, actions = TutoActions(), reducedMotion = true) } } } }
        val bad = ArrayList<String>()
        val seen = HashMap<TutoLevel, Int>()
        for (style in TutoCourse.STYLES.filter { TutoTarget.of(it) != null }.take(4)) for (level in TutoLevel.entries) {
            val target = TutoTarget.of(style)!!
            val chapters = TutoCourse.of(style)!!.chaptersFor(level)
            val q = target.blank()
            for ((c, chapter) in chapters.withIndex()) for ((i, s) in chapter.steps.withIndex()) {
                if (s.manual || s.listening) continue
                val need = TutoPlay.button(s, q, target, TutoPlay.arrive(s, q), level) as? TutoNeed ?: continue
                shown = TutoUi(TutoProgress(style, level, current = s.id), chapters, c, i, open = true, need = need)
                compose.waitForIdle()
                val w = words()
                val phrase = tr("À toi : touche « %s ».", needLabel(need, style))
                val text = tr(s.textFor(level))
                seen[level] = (seen[level] ?: 0) + 1
                val case = "$style ${level.name} ${s.id}"
                if (level == TutoLevel.Facile) {
                    if (w != phrase) bad += "$case: the words are « $w », not the need « $phrase »"
                    if (text.isNotBlank() && text in w) bad += "$case: the step's text is shown next to the need"
                } else if (phrase in w) bad += "$case: Pro shows the Facile phrase « $phrase »"
            }
        }
        assertTrue("only $seen steps with a need: the test checks nothing", (seen[TutoLevel.Facile] ?: 0) >= 8 && (seen[TutoLevel.Pro] ?: 0) >= 8)
        assertTrue("${bad.size}:\n" + bad.joinToString("\n"), bad.isEmpty())
    }
}
