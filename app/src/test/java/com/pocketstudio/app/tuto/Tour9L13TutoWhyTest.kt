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
 * Tour 9, F009 (seats t9-01, t9-22, t9-31, t9-44): « « Pourquoi ? » s'ouvre juste sous son bouton, ne coupe pas la
 * consigne et reste ouvert tant que je ne le ferme pas. » Its reason opened up in the words, far above its button (the
 * button pinned under them), scrolled the instruction away, and went at the step's validation (« Bravo »).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Tour9L13TutoWhyTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun end() {
        I18n.lang = Lang.FR
    }

    private var lang by mutableStateOf(Lang.FR)
    private var scale by mutableStateOf(1f)
    private var at by mutableStateOf(0 to 0)
    private var bravo by mutableStateOf(false)

    @Test
    @Config(qualifiers = "w360dp-h780dp")
    fun theReasonOpensUnderItsButtonAndStaysUntilClosed() = check()

    @Test
    @Config(qualifiers = "w780dp-h360dp")
    fun onItsSideTooTheReasonOpensUnderItsButtonAndStays() = check()

    private fun check() {
        val style = "jump_up"
        val chapters = TutoCourse.of(style)!!.chaptersFor(TutoLevel.Facile)
        val places = chapters.flatMapIndexed { c, ch -> ch.steps.indices.map { c to it } }
            .filter { (c, i) -> chapters[c].steps[i].why != null || chapters[c].why.isNotEmpty() }
        compose.setContent {
            val d = LocalDensity.current
            // The step (not the bravo) keys the panel: « Bravo » is the same step, validated.
            key(lang, scale, at) {
                CompositionLocalProvider(
                    LocalDensity provides Density(d.density, scale),
                    LocalLayoutDirection provides if (lang.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                ) {
                    val (c, i) = at
                    val step = chapters[c].steps[i]
                    val u = TutoUi(TutoProgress(style, TutoLevel.Facile, current = step.id, done = if (bravo) setOf(step.id) else emptySet()), chapters, c, i, bravo = bravo)
                    PocketStudioTheme {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                            TutoPanel(u, canShow = true, actions = TutoActions(), reducedMotion = true)
                        }
                    }
                }
            }
        }
        fun node(tag: String) = compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().firstOrNull()
        val wrong = ArrayList<String>()
        var cases = 0
        for (l in listOf(Lang.FR, Lang.HI)) for (s in listOf(1f, 2f)) for (p in places.take(6)) {
            I18n.lang = l
            lang = l
            scale = s
            bravo = false
            at = p
            compose.waitForIdle()
            val where = "${l.code} ${(s * 100).toInt()} % ${chapters[p.first].steps[p.second].id}"
            if (node("tuto_why") == null) { wrong += "$where: no « Pourquoi ? »"; continue }
            compose.onNode(hasTestTag("tuto_why"), useUnmergedTree = true).performClick()
            compose.waitForIdle()
            cases++
            val button = node("tuto_why")!!
            val reason = node("tuto_why_text") ?: run { wrong += "$where: touched, « Pourquoi ? » showed no reason"; null } ?: continue
            val gap = reason.positionInRoot.y - (button.positionInRoot.y + button.size.height)
            val density = button.layoutInfo.density.density
            if (gap < -1f || gap > 12f * density) wrong += "$where: the reason opens ${(gap / density).toInt()} dp from its button"
            if (reason.boundsInRoot.height < reason.size.height - 1f) wrong += "$where: the reason ${reason.boundsInRoot.height.toInt()} of ${reason.size.height} px in sight"
            // The instruction: its beginning stays in sight, nothing over it.
            val text = node("tuto_text")
            if (text != null) {
                if (text.boundsInRoot.top > text.positionInRoot.y + 1f || text.boundsInRoot.height < 1f) wrong += "$where: opening the reason scrolled the instruction's start away"
                if (text.boundsInRoot.overlaps(reason.boundsInRoot) && text.boundsInRoot.intersect(reason.boundsInRoot).height > 1f) wrong += "$where: the reason over the instruction"
            }
            // The step validated (« Bravo ») : the reason opened stays open.
            bravo = true
            compose.waitForIdle()
            if (node("tuto_why_text") == null) wrong += "$where: the reason went at the validation"
        }
        assertTrue("no « Pourquoi ? » tested", cases > 0)
        assertTrue("$cases cases, ${wrong.size} wrong:\n" + wrong.take(30).joinToString("\n"), wrong.isEmpty())
    }
}
