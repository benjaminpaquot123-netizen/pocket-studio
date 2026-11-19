package com.pocketstudio.app.tuto

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.audit.LayoutScan
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
 * Tour 9, the tutorial's end with steps left (« Presque fini ! »).
 * F006 (seats t9-05, t9-12, t9-42, t9-63): « Revenir à l'étape » se voit en entier, avec son texte, au-dessus de la barre
 * du bas (it was cut at the edge, under « … et 3 de plus », or an arrow alone).
 * F007 (seats t9-12, t9-28, t9-29, t9-30): toucher « il reste N étapes » montre la liste complète des étapes oubliées,
 * chacune touchable (« … et 6 autres (liste ≡) » was words to read, not a way to them).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Tour9L13TutoEndCardTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun end() {
        I18n.lang = Lang.FR
    }

    private val style = "jump_up"
    private val chapters = TutoCourse.of(style)!!.chaptersFor(TutoLevel.Facile)
    private val all = chapters.flatMap { it.steps }

    private var lang by mutableStateOf(Lang.FR)
    private var scale by mutableStateOf(1f)
    private var leftCount by mutableStateOf(9)
    private val went = ArrayList<Pair<Int, Int>>()

    /** The panel as in the app: at the bottom of the screen, over the bottom bar. */
    private fun show() = compose.setContent {
        val d = LocalDensity.current
        key(lang, scale, leftCount) {
            CompositionLocalProvider(
                LocalDensity provides Density(d.density, scale),
                LocalLayoutDirection provides if (lang.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                val left = all.takeLast(leftCount).map { it.id }.toSet()
                val done = all.map { it.id }.filterNot { it in left }.toSet()
                val last = all.last()
                val u = TutoUi(TutoProgress(style, TutoLevel.Facile, current = last.id, done = done), chapters, chapters.lastIndex, chapters.last().steps.lastIndex, atEnd = true)
                PocketStudioTheme {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f).fillMaxWidth().testTag("work"))
                        TutoPanel(u, canShow = true, actions = TutoActions(goTo = { c, i -> went += c to i }), reducedMotion = true)
                        Box(Modifier.fillMaxWidth().height(56.dp).testTag("bottom_bar"))
                    }
                }
            }
        }
    }

    private fun node(tag: String) = compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().firstOrNull()

    @Test
    @Config(qualifiers = "w360dp-h780dp")
    fun backToTheStepIsWholeAboveTheBottomBar() {
        val wrong = ArrayList<String>()
        show()
        for (l in listOf(Lang.FR, Lang.HI, Lang.DE)) for (s in listOf(1f, 1.3f, 2f)) for (n in listOf(3, 9)) {
            I18n.lang = l
            lang = l
            scale = s
            leftCount = n
            compose.waitForIdle()
            val where = "${l.code} ${(s * 100).toInt()} % $n left"
            val back = node("tuto_end_back") ?: run { wrong += "$where: no « Revenir à l'étape »"; null } ?: continue
            val bar = node("bottom_bar")!!.boundsInRoot
            val seen = back.boundsInRoot
            if (seen.height < back.size.height - 1f) wrong += "$where: « Revenir à l'étape » ${seen.height.toInt()} of ${back.size.height} px in sight"
            if (seen.bottom > bar.top + 1f) wrong += "$where: « Revenir à l'étape » under the bottom bar"
            val density = compose.onAllNodes(hasTestTag("work")).fetchSemanticsNodes().first().layoutInfo.density.density
            val cut = LayoutScan.scan(compose, density, setOf(LayoutScan.Kind.CUT)).filter { "Revenir" in it.toString() || "चरण" in it.toString() || "Zurück" in it.toString() }
            if (cut.isNotEmpty()) wrong += "$where: $cut"
        }
        assertTrue("${wrong.size} wrong:\n" + wrong.take(30).joinToString("\n"), wrong.isEmpty())
    }

    @Test
    @Config(qualifiers = "w360dp-h780dp")
    fun theStepsLeftAreAllThereEachOneTouchAway() {
        val wrong = ArrayList<String>()
        show()
        for (l in listOf(Lang.FR, Lang.HI)) for (s in listOf(1f, 2f)) {
            I18n.lang = l
            lang = l
            scale = s
            leftCount = 9
            compose.waitForIdle()
            val where = "${l.code} ${(s * 100).toInt()} %"
            val left = all.takeLast(9)
            // « … et 5 autres »: a touch shows them all.
            val more = node("tuto_end_more")
            if (more == null || more.config.getOrNull(SemanticsActions.OnClick) == null) {
                wrong += "$where: « … et ${left.size - 4} autres » is not touchable"
                continue
            }
            compose.onNode(hasTestTag("tuto_end_more"), useUnmergedTree = true).performScrollTo().performClick()
            compose.waitForIdle()
            for (s2 in left) {
                val row = node("tuto_left_${s2.id}")
                if (row == null) { wrong += "$where: ${s2.id} not in the list"; continue }
                went.clear()
                compose.onNode(hasTestTag("tuto_left_${s2.id}"), useUnmergedTree = true).performScrollTo().performClick()
                compose.waitForIdle()
                val place = chapters.indexOfFirst { s2 in it.steps }.let { c -> c to chapters[c].steps.indexOf(s2) }
                if (went != listOf(place)) wrong += "$where: ${s2.id} touched went to $went"
            }
        }
        assertTrue("${wrong.size} wrong:\n" + wrong.take(30).joinToString("\n"), wrong.isEmpty())
    }
}
