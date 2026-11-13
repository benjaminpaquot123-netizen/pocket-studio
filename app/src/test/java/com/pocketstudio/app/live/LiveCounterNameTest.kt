package com.pocketstudio.app.live

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.LiveState
import com.pocketstudio.model.Project
import com.pocketstudio.model.Scene
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.PocketStudioTheme
import com.pocketstudio.ui.layoutDirectionOf
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 6, lot 5, R20 (C a #26 « un cercle « 30.2 » sans unité », B c #35 « un rond gris « – » qui ne fait rien »): the
 * Live bar's counter says what it counts, in each of the 15 languages: a word under its figure (« mesure »), and for
 * TalkBack its name and value (« Mesure 30 · temps 2 », stopped « Mesure · Arrêté »).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-xxhdpi")
class LiveCounterNameTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun tearDown() {
        I18n.lang = Lang.FR
    }

    private val project = Project(scenes = listOf(Scene("A", DrumPattern().toggle(0, 0)), Scene("B")), scene = 0).sanitized()

    @Test
    fun theCounterHasANameAndAUnitInEveryLanguage() {
        var lang by mutableStateOf(Lang.FR)
        var clock by mutableIntStateOf(-1)
        compose.setContent {
            I18n.lang = lang
            key(lang) {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirectionOf(lang.rtl)) {
                    PocketStudioTheme {
                        val playing = clock >= 0
                        LiveScreen(LiveScreenUi(project, if (playing) LiveState.on(0) else null, clock = clock, playing = playing), LiveActions())
                    }
                }
            }
        }
        val problems = ArrayList<String>()
        fun said(): String = compose.onNodeWithTag("live_counter").fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" ").orEmpty()
        for (l in Lang.entries) {
            lang = l
            I18n.lang = l
            // Bar 30, beat 2: « 30.2 ».
            clock = 29 * project.stepsPerBar + 4
            compose.waitForIdle()
            val playing = said()
            val bar = tr("Mesure %d", 30)
            val beat = tr("temps")
            if (bar !in playing || !playing.contains(beat, ignoreCase = true) || !playing.trimEnd().endsWith("2")) {
                problems += "${l.code} playing: « $playing » (wanted « $bar », « $beat » and 2)"
            }
            // The unit is written under the figure, in the language.
            val unit = tr("mesure")
            val shown = compose.onAllNodes(hasAnyAncestor(hasTestTag("live_counter")) and hasText(unit), useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
            if (shown.isEmpty()) problems += "${l.code}: no « $unit » under the counter"
            clock = -1
            compose.waitForIdle()
            val stopped = said()
            if (!stopped.contains(unit, ignoreCase = true) || tr("Arrêté") !in stopped) {
                problems += "${l.code} stopped: « $stopped » (wanted « $unit » and « ${tr("Arrêté")} »)"
            }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }
}
