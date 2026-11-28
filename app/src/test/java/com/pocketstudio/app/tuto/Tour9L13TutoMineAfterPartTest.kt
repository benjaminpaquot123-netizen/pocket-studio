package com.pocketstudio.app.tuto

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
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
 * Tour 9, F008 (seat t9-06, acidcore, Korean): « Après une étape d'écoute de partie, je trouve aussi « Écouter mon
 * résultat ». » After the Intro and the Break were made (each with its example « vs: » to compare), only « Écouter
 * l'exemple » was there; « Écouter ton résultat » was on the other parts made. Every part made, in every course, has
 * both: the user's own part and the example to compare, the bravo shown or not.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Tour9L13TutoMineAfterPartTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun end() {
        I18n.lang = Lang.FR
    }

    private var lang by mutableStateOf(Lang.FR)
    private var scale by mutableStateOf(1f)
    private var shown by mutableStateOf<TutoUi?>(null)

    @Test
    @Config(qualifiers = "w360dp-h780dp")
    fun everyPartMadeHasMyResultBesideTheExample() {
        compose.setContent {
            val d = LocalDensity.current
            val u = shown ?: return@setContent
            key(lang, scale, u) {
                CompositionLocalProvider(
                    LocalDensity provides Density(d.density, scale),
                    LocalLayoutDirection provides if (lang.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                ) {
                    PocketStudioTheme { TutoPanel(u, canShow = true, actions = TutoActions(), reducedMotion = true, mine = true) }
                }
            }
        }
        fun has(tag: String) = compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        val wrong = ArrayList<String>()
        var cases = 0
        for (style in listOf("acidcore", "jump_up", "jungle")) {
            val chapters = TutoCourse.of(style)!!.chaptersFor(TutoLevel.Facile)
            val places = chapters.flatMapIndexed { c, ch -> ch.steps.indices.map { c to it } }
            for ((c, i) in places) {
                val step = chapters[c].steps[i]
                // A part made by the user, with its example to compare.
                if (!step.check.startsWith("part:") || step.listen.isEmpty()) continue
                val before = places.takeWhile { it != c to i }.map { chapters[it.first].steps[it.second].id }.toSet()
                for (l in listOf(Lang.FR, Lang.KO)) for (s in listOf(1f, 2f)) for (bravo in listOf(true, false)) {
                    I18n.lang = l
                    lang = l
                    scale = s
                    shown = TutoUi(TutoProgress(style, TutoLevel.Facile, current = step.id, done = before + step.id), chapters, c, i, bravo = bravo)
                    compose.waitForIdle()
                    cases++
                    val where = "$style ${step.id} ${l.code} ${(s * 100).toInt()} %${if (bravo) " bravo" else ""}"
                    if (!has("tuto_play_mine")) wrong += "$where: no « Écouter ton résultat »"
                    if (!has("tuto_listen")) wrong += "$where: no « Écouter l'exemple »"
                }
            }
        }
        assertTrue("no part made: nothing tested", cases > 0)
        assertTrue("$cases cases, ${wrong.size} wrong:\n" + wrong.take(30).joinToString("\n"), wrong.isEmpty())
    }
}
