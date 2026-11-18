package com.pocketstudio.app.tuto

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.settings.SettingsStore
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
 * Tour 7, P6 bloc 0b (« repli du panneau »): on a small phone, in Facile, the panel folds its blocks in the planned order
 * (the examples first) before the words are cut, the kit card stays whole in sight when the step offers it, and
 * « Pourquoi ? » stays whole and touchable, never under « Lire la suite », in Arabic at 130 %.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Tour7TutoPanelFoldTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun end() {
        I18n.lang = Lang.FR
        SettingsStore.update(ApplicationProvider.getApplicationContext()) { it.copy(simpleMode = false) }
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun theBlocksFoldInOrderTheKitStaysAndWhyIsTouchable() {
        SettingsStore.update(ApplicationProvider.getApplicationContext()) { it.copy(simpleMode = true) }
        val style = "techno"
        val chapters = TutoCourse.of(style)!!.chaptersFor(TutoLevel.Facile)
        val places = chapters.flatMapIndexed { c, ch -> ch.steps.indices.filter { ch.steps[it].id != "ready" }.map { c to it } }
        var lang by mutableStateOf(Lang.FR)
        var scale by mutableStateOf(1f)
        var at by mutableStateOf(places.first())
        compose.setContent {
            val d = LocalDensity.current
            key(lang, scale, at) {
                CompositionLocalProvider(
                    LocalDensity provides Density(d.density, scale),
                    LocalLayoutDirection provides if (lang.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                ) {
                    val (c, i) = at
                    val shown = TutoUi(TutoProgress(style, TutoLevel.Facile, current = chapters[c].steps[i].id), chapters, c, i)
                    PocketStudioTheme {
                        TutoPanel(shown, canShow = true, actions = TutoActions(), reducedMotion = true, map = tutoMap(style),
                            kit = { Text("Kit", modifier = Modifier.testTag("tuto_kit")) })
                    }
                }
            }
        }
        val wrong = ArrayList<String>()
        fun node(tag: String) = compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().firstOrNull()
        for (l in listOf(Lang.FR, Lang.AR)) for (s in listOf(1f, 1.3f)) for (p in places) {
            I18n.lang = l
            lang = l
            scale = s
            at = p
            compose.waitForIdle()
            val where = "${l.code} ${(s * 100).toInt()} % ${chapters[p.first].steps[p.second].id}"
            val more = node("tuto_text_more")?.boundsInRoot
            // The examples fold before the words are cut.
            val ex = node("wizard_plain_words")
            if (more != null && ex != null && ex.boundsInRoot.height > 0.5f) wrong += "$where: words cut, examples still in sight"
            // The kit card the step offers: whole in sight.
            val kit = node("tuto_kit")
            if (kit == null) wrong += "$where: no kit card"
            else if (kit.boundsInRoot.height < kit.size.height - 1f) wrong += "$where: kit card ${kit.boundsInRoot.height.toInt()} of ${kit.size.height} px"
            // « Pourquoi ? »: whole, touchable, never under « Lire la suite ».
            val why = node("tuto_why")
            if ((chapters[p.first].steps[p.second].why ?: chapters[p.first].why).isNotEmpty()) {
                if (why == null) wrong += "$where: no « Pourquoi ? »"
                else {
                    val seen = why.boundsInRoot
                    if (seen.height < why.size.height - 1f) wrong += "$where: « Pourquoi ? » ${seen.height.toInt()} of ${why.size.height} px"
                    if (more != null && seen.top < more.bottom && seen.bottom > more.top) wrong += "$where: « Pourquoi ? » under « Lire la suite »"
                }
            }
        }
        assertTrue("Panel fold:\n" + wrong.joinToString("\n"), wrong.isEmpty())
    }
}
