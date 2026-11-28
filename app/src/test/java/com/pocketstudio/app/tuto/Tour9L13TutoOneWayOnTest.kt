package com.pocketstudio.app.tuto

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
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
 * Tour 9, F004 (seats t9-12, t9-74): « Le bouton que je vise reste à sa place et garde son nom pendant que je le touche. »
 * The example ended under the finger aimed at « Next → »: the card rebuilt itself and « Écouter l'exemple » (or « Arrêter
 * l'écoute ») took that place, the touch replayed the example.
 * F005 (seats t9-39, t9-40): « Il n'y a jamais deux boutons qui disent « suivant » en même temps ; chaque bouton dit ce qu'il
 * fait. » A step done showed « Étape suivante » and « Suivant → » together (and « C'est fait » on a step already done).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Tour9L13TutoOneWayOnTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun end() {
        I18n.lang = Lang.FR
    }

    private val style = "jump_up"
    private val chapters = TutoCourse.of(style)!!.chaptersFor(TutoLevel.Facile)
    private val places = chapters.flatMapIndexed { c, ch -> ch.steps.indices.map { c to it } }

    private var lang by mutableStateOf(Lang.FR)
    private var scale by mutableStateOf(1f)
    private var shown by mutableStateOf<TutoUi?>(null)

    private fun show() = compose.setContent {
        val d = LocalDensity.current
        val u = shown ?: return@setContent
        key(lang, scale) {
            CompositionLocalProvider(
                LocalDensity provides Density(d.density, scale),
                LocalLayoutDirection provides if (lang.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                PocketStudioTheme {
                    TutoPanel(u, canShow = true, actions = TutoActions(), reducedMotion = true, mine = true)
                }
            }
        }
    }

    private fun ui(p: Pair<Int, Int>, done: Boolean, bravo: Boolean = false): TutoUi {
        val id = chapters[p.first].steps[p.second].id
        val before = places.takeWhile { it != p }.map { chapters[it.first].steps[it.second].id }.toSet()
        return TutoUi(TutoProgress(style, TutoLevel.Facile, current = id, done = if (done) before + id else before), chapters, p.first, p.second, bravo = bravo)
    }

    private fun node(tag: String): SemanticsNode? = compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().firstOrNull()

    /** The action bar's buttons, by tag. */
    private val buttons = listOf("tuto_next_big", "tuto_next", "tuto_prev", "tuto_listen", "tuto_play_mine", "tuto_mark", "tuto_show", "tuto_do", "tuto_need")
    private val nexts = setOf("tuto_next_big", "tuto_next")

    @Test
    @Config(qualifiers = "w360dp-h780dp")
    fun theNextButtonStaysUnderTheFingerWhenTheExampleEnds() {
        val wrong = ArrayList<String>()
        var cases = 0
        show()
        val listening = places.filter { chapters[it.first].steps[it.second].listen.isNotEmpty() }
        for (l in listOf(Lang.FR, Lang.HI)) for (s in listOf(1f, 2f)) for (p in listening) for (bravo in listOf(false, true)) {
            I18n.lang = l
            lang = l
            scale = s
            shown = ui(p, done = false)
            compose.waitForIdle()
            val where = "${l.code} ${(s * 100).toInt()} % ${chapters[p.first].steps[p.second].id}${if (bravo) " bravo" else ""}"
            val aimed = node("tuto_next") ?: continue
            val panel = node("tuto_panel")!!.boundsInRoot
            // The finger's place, from the panel's bottom (where the panel stands on the screen).
            val finger = Offset(aimed.boundsInRoot.center.x, panel.bottom - aimed.boundsInRoot.center.y)
            // The example ends under the finger: the step is done.
            shown = ui(p, done = true, bravo = bravo)
            compose.waitForIdle()
            cases++
            val after = node("tuto_panel")!!.boundsInRoot
            val at = Offset(finger.x, after.bottom - finger.y)
            val under = buttons.filter { t -> compose.onAllNodes(hasTestTag(t), useUnmergedTree = true).fetchSemanticsNodes().any { it.boundsInRoot.contains(at) } }
            if (under.none { it in nexts }) wrong += "$where: under the finger aimed at « Suivant », now ${under.ifEmpty { listOf("nothing") }}"
        }
        assertTrue("no « Suivant » aimed at: nothing tested", cases > 0)
        assertTrue("$cases cases, ${wrong.size} wrong:\n" + wrong.take(30).joinToString("\n"), wrong.isEmpty())
    }

    @Test
    @Config(qualifiers = "w360dp-h780dp")
    fun neverTwoWaysOnAndEachButtonSaysWhatItDoes() {
        val wrong = ArrayList<String>()
        show()
        for (l in listOf(Lang.FR, Lang.HI)) for (s in listOf(1f, 2f)) for (p in places) for (done in listOf(false, true)) for (bravo in listOf(false, true)) {
            if (bravo && !done) continue
            I18n.lang = l
            lang = l
            scale = s
            shown = ui(p, done, bravo)
            compose.waitForIdle()
            val step = chapters[p.first].steps[p.second]
            val where = "${l.code} ${(s * 100).toInt()} % ${step.id}${if (done) " done" else ""}${if (bravo) " bravo" else ""}"
            val ways = nexts.filter { node(it) != null }
            if (ways.size > 1) wrong += "$where: two ways on ($ways)"
            // « C'est fait » on a step already done says nothing it does.
            if (done && node("tuto_mark") != null) wrong += "$where: « C'est fait » on a step done"
        }
        assertTrue("${wrong.size} wrong:\n" + wrong.take(30).joinToString("\n"), wrong.isEmpty())
    }
}
