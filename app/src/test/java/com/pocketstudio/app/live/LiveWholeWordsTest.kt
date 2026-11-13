package com.pocketstudio.app.live

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import com.pocketstudio.app.audit.CutWords
import com.pocketstudio.app.nav.acidTechnoSong
import com.pocketstudio.app.ui.UiScaled
import com.pocketstudio.model.LiveState
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.PocketStudioTheme
import com.pocketstudio.ui.layoutDirectionOf
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Tour 6, lot 5, R1: the Live page (Briques, the tools folded, playing) on a 360 dp phone, the phone's font at 130 %
 * and 200 % or Réglages › Taille 130 %, in each of the 15 languages: every text drawn by LiveScreen.kt is whole (no
 * word over two lines, no « … », no line lost). The tempo (LiveDock.kt), « Lancer à » (LiveDock.kt) and « Cases »
 * (LiveGridLook.kt) and the « ? » of the help dots (ui-components' Help.kt) belong to other files: what they cut is
 * written to build/reports/lot5/live-others.txt, not judged here.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-xxhdpi")
class LiveWholeWordsTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun tearDown() {
        I18n.lang = Lang.FR
        RuntimeEnvironment.setFontScale(1f)
    }

    private val song = acidTechnoSong()

    /** Bar 30, beat 2 (a counter « 30.2 »). */
    private val clock = 29 * song.stepsPerBar + 4

    private fun cuts(
        label: String, uiScale: Float, fontScale: Float, project: com.pocketstudio.model.Project = song,
        tab: LivePanelTab = LivePanelTab.Briques, judged: (CutWords.Cut) -> Boolean = { true },
    ): List<String> {
        if (fontScale != 1f) RuntimeEnvironment.setFontScale(fontScale)
        var lang by mutableStateOf(Lang.FR)
        compose.setContent {
            I18n.lang = lang
            key(lang) {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirectionOf(lang.rtl)) {
                    UiScaled(uiScale) {
                        PocketStudioTheme { LiveScreen(LiveScreenUi(project, LiveState.on(0), clock = clock, playing = true), LiveActions(), initialTab = tab) }
                    }
                }
            }
        }
        val mine = ArrayList<String>()
        val others = ArrayList<String>()
        for (l in Lang.entries) {
            lang = l
            I18n.lang = l
            compose.waitForIdle()
            for (cut in CutWords.under(compose, "live_screen")) {
                val line = "$label ${l.code}: $cut"
                if (cut.tags.any { it in OTHER_FILES || it.startsWith("help_") } || !judged(cut)) others += line else mine += line
            }
        }
        File("build/reports/lot5").mkdirs()
        File("build/reports/lot5/live-others.txt").appendText(others.distinct().joinToString("\n", postfix = "\n"))
        return mine.distinct()
    }

    @Test
    fun everyTextIsWholeWithThePhonesFontAt130() {
        val problems = cuts("font 130 %", uiScale = 1f, fontScale = 1.3f)
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun everyTextIsWholeWithThePhonesFontAt200() {
        val problems = cuts("font 200 %", uiScale = 1f, fontScale = 2f)
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun everyTextIsWholeAtTaille130() {
        val problems = cuts("Taille 130 %", uiScale = 1.3f, fontScale = 1f)
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    /** « Cases − 4 + »: rows tall enough for each brick's part name under its letter (« Cou… », « Refr… » at 100 %). */
    @Test
    fun everyBrickNameIsWholeInTallRows() {
        val problems = cuts("4 cases", uiScale = 1f, fontScale = 1f, project = song.copy(liveRows = 4))
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun everyBrickNameIsWholeInTallRowsWithThePhonesFontAt130() {
        val problems = cuts("4 cases, font 130 %", uiScale = 1f, fontScale = 1.3f, project = song.copy(liveRows = 4))
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    /** The tools open: the panel's own tabs (« Blokk… », « Acid 3… » at Taille 130 % in Dutch); its pages are other files'. */
    private fun panelTabs(label: String, uiScale: Float, fontScale: Float) =
        cuts(label, uiScale, fontScale, tab = LivePanelTab.Effets, judged = { c -> c.tags.any { it.startsWith("live_tab_") } })

    @Test
    fun thePanelsTabsAreWholeAtTaille130() {
        val problems = panelTabs("panel, Taille 130 %", uiScale = 1.3f, fontScale = 1f)
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun thePanelsTabsAreWholeWithThePhonesFontAt130() {
        val problems = panelTabs("panel, font 130 %", uiScale = 1f, fontScale = 1.3f)
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun thePanelsTabsAreWholeWithThePhonesFontAt200() {
        val problems = panelTabs("panel, font 200 %", uiScale = 1f, fontScale = 2f)
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    private companion object {
        /** Drawn on the Live page by other files (other lots): reported, not judged here. */
        val OTHER_FILES = setOf("live_tempo_box", "live_quantum", "live_cases")
    }
}
