package com.pocketstudio.app.audit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.LayoutDirection
import com.pocketstudio.app.midi.LearnUi
import com.pocketstudio.app.midi.MidiActions
import com.pocketstudio.app.midi.MidiScreen
import com.pocketstudio.app.midi.MidiUi
import com.pocketstudio.app.mixer.AutomationActions
import com.pocketstudio.app.mixer.AutomationCard
import com.pocketstudio.app.mixer.AutomationUi
import com.pocketstudio.app.onboarding.PadLiveCard
import com.pocketstudio.app.rhythm.LiveUi
import com.pocketstudio.app.rhythm.RhythmActions
import com.pocketstudio.app.rhythm.RhythmScreen
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.rhythm.TransportUi
import com.pocketstudio.app.session.RenameSceneFields
import com.pocketstudio.app.settings.AppSettings
import com.pocketstudio.app.settings.SettingsActions
import com.pocketstudio.app.settings.SettingsScreen
import com.pocketstudio.app.synth.PianoRoll
import com.pocketstudio.app.synth.PianoRollActions
import com.pocketstudio.app.ui.controlLabel
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.AutomationLane
import com.pocketstudio.model.Channel
import com.pocketstudio.model.Insert
import com.pocketstudio.model.MidiMapping
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.Project
import com.pocketstudio.model.Scene
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 6, lot 5 « langues »: the texts the catalogs corrected (project-model's Tour6LanguagesTest reads them key by
 * key), here read where the person sees them, on the app's own screens, from what the screen draws (its semantics),
 * never from the catalog: the song's parts in the language's own script, the Arabic Mode pro's trade words (« Tous les
 * pads », « Basse en fantôme », « Boucle »), Pad Live's « NOUVEAU » in Japanese and Korean, « 5 zakładek » in Polish,
 * the Spanish song curve's « compases 1 a 20 », and a MIDI knob on Plaits saying it moves a synth.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-xxhdpi")
class Tour6WordsOnScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun back() {
        I18n.everyday = false
        I18n.lang = Lang.FR
    }

    /** A Latin word (two letters or more): a single letter is a part's letter or a note's name. */
    private val latinWord = Regex("[A-Za-z]{2,}")

    /** The language the screen is drawn in: changing it draws the screen again, as the app does when the person picks one. */
    private val language = mutableStateOf(Lang.FR)

    /** [content] on the app's theme, in the language of [language] and its direction (right to left in Arabic). */
    private fun show(lang: Lang, content: @Composable () -> Unit) {
        I18n.lang = lang
        language.value = lang
        compose.setContent {
            val now = language.value
            key(now) {
                CompositionLocalProvider(LocalLayoutDirection provides if (now.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                    PocketStudioTheme { content() }
                }
            }
        }
        compose.waitForIdle()
    }

    /** The same screen, now in [lang]. */
    private fun switchTo(lang: Lang) {
        I18n.lang = lang
        language.value = lang
        compose.waitForIdle()
    }

    private fun texts(node: SemanticsNode): List<String> = node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }

    /** What the node tagged [tag] says (its own text and its children's, as a screen reader reads it). */
    private fun said(tag: String): String = texts(compose.onNodeWithTag(tag).fetchSemanticsNode()).joinToString(" ")

    /** Every text the screen draws, one by one (the unmerged tree: each Text on its own). */
    private fun everyText(): List<String> {
        val out = ArrayList<String>()
        fun walk(n: SemanticsNode) {
            out += texts(n)
            n.children.forEach(::walk)
        }
        walk(compose.onRoot(useUnmergedTree = true).fetchSemanticsNode())
        return out
    }

    private fun report(what: String, wrong: List<String>) = assertTrue("$what (${wrong.size}):\n" + wrong.joinToString("\n"), wrong.isEmpty())

    /**
     * Tutorials' sweep n° 6: naming a part (« Quel moment du morceau est-ce ? »), its sections' chips in Arabic, Hindi,
     * Chinese, Japanese and Korean are said in the language's own script, never « Pre-chorus », « Bridge », « Build-up »,
     * « Fake drop » or « Drop » in Latin letters.
     */
    @Test
    fun thePartChipsAreWrittenInTheLanguagesOwnScript() {
        val wrong = ArrayList<String>()
        show(Lang.AR) { RenameSceneFields(Scene.sectionName("Couplet")) {} }
        for (lang in listOf(Lang.AR, Lang.HI, Lang.ZH_CN, Lang.JA, Lang.KO)) {
            switchTo(lang)
            Scene.SECTIONS.forEach { s ->
                val text = said("section_$s")
                if (text.isBlank() || latinWord.containsMatchIn(text)) wrong += "${lang.code}: chip « $s » shows « $text »"
            }
        }
        report("part chips in Latin letters", wrong)
    }

    /** R15 (seat B c, Arabic, Mode pro: « كل الـPads »): the Rythme tab's « Tous les pads » chip, in Arabic letters. */
    @Test
    fun theArabicRhythmScreenSaysAllPadsInArabic() {
        show(Lang.AR) {
            RhythmScreen(RhythmViewModel.starterProject(), TransportUi(), { LiveUi() }, canUndo = false, canRedo = false, actions = RhythmActions())
        }
        val chip = said("all_pads")
        assertTrue("the « all pads » chip says « $chip »", chip.isNotBlank() && !Regex("[A-Za-z]").containsMatchIn(chip))
    }

    /**
     * R15 (seat B c, Arabic, Mode pro: « Ghost bass », « Loop »): the piano roll's tools (« Basse en fantôme », « Boucle »,
     * « Tout voir »…) and everything else it draws, without a Latin word. A word in brackets after its Arabic one is the
     * trade's name given as a hint (the help's « (velocity) »), not the word itself.
     */
    @Test
    fun theArabicPianoRollHasNoEnglishWord() {
        val notes = listOf(60, 64, 67).mapIndexed { i, p -> Note(i * 24, 24, p, 0.8f) }
        show(Lang.AR) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                PianoRoll(NoteClip(16, notes), ghosts = listOf(Note(0, 48, 36, 0.8f)), playStep = -1, actions = PianoRollActions())
            }
        }
        val shown = everyText()
        assertTrue("the piano roll draws no text", shown.isNotEmpty())
        val hint = Regex("""\([A-Za-z ]+\)""")
        report("Latin words on the Arabic piano roll", shown.filter { latinWord.containsMatchIn(hint.replace(it, " ")) }.map { "« $it »" })
    }

    /** T16 (seats D a, H a: « NEW »): Pad Live's badge on « Que veux-tu faire ? » in Japanese and Korean. */
    @Test
    fun newIsSaidInJapaneseAndKorean() {
        show(Lang.JA) { PadLiveCard {} }
        val ja = said("launch_padlive")
        switchTo(Lang.KO)
        val ko = said("launch_padlive")
        val wrong = ArrayList<String>()
        if ("新機能" !in ja || "NEW" in ja) wrong += "ja: « $ja »"
        if ("새 기능" !in ko || "NEW" in ko) wrong += "ko: « $ko »"
        report("Pad Live's badge", wrong)
    }

    /** R16 (seat F b, Polish: « 5 zakładki »): Réglages, the Mode simple's chip says « 5 zakładek ». */
    @Test
    fun thePolishSettingsCountFiveTabsRight() {
        show(Lang.PL) { SettingsScreen(AppSettings(), "1", SettingsActions()) }
        assertEquals("Prosty: 5 zakładek", said("mode_simple"))
    }

    /**
     * R16 (seat B a, Spanish: « COMP. 2 A 5 »): a song curve of 40 bars, zoomed in as it opens (half of it shown), says
     * which bars it shows in whole words: « compases 1 a 20 ».
     */
    @Test
    fun theSpanishSongCurveSaysWhichBars() {
        val lane = AutomationLane(AutoTarget(AutoTarget.VOLUME, 1), mode = AutomationLane.SONG, songValues = List(40) { 0.5f })
        show(Lang.ES) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                AutomationCard(AutomationUi(listOf(lane), 16, songBars = 40), List(16) { Channel() }, List(4) { Insert() }, AutomationActions())
            }
        }
        val range = everyText().filter { Regex("""\b1\b\D+\b20\b""").containsMatchIn(it) }
        assertEquals("the curve's bars", listOf("compases 1 a 20"), range)
    }

    /**
     * T16 (seat B b: « Plaits »): MIDI › a knob set on Plaits' Timbre says it moves a synth (« Plaitsシンセ » in Japanese),
     * as the app labels it (AppRoot's controlLabel).
     */
    @Test
    fun theMidiKnobOnPlaitsSaysItIsASynth() {
        show(Lang.JA) {
            val label = controlLabel(AutoTarget(MidiMapping.PLAITS, param = 2), Project(), emptyList())
            MidiScreen(MidiUi(), MidiActions(), LearnUi(mappings = listOf(74 to label)))
        }
        val row = everyText().filter { "Plaits" in it }
        assertTrue("the knob's row says « $row »", row.size == 1 && "シンセ" in row[0])
    }
}
