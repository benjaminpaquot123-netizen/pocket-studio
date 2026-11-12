package com.pocketstudio.app.audit

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.nav.AppHarness
import com.pocketstudio.model.i18n.Lang
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 5, lot « langues » (zone « langues et lisibilité », 0 promise kept out of 8): the whole app as a newcomer
 * meets it, in the five languages of tour 6 (French, German's long words, Arabic right to left, Hindi's tall marks,
 * Chinese), with the phone's font at 100 % and at 200 %, on a small phone (360 dp): one case per screen, language
 * and font. Each case reads what the screen draws, never an image:
 * - **CUT / OVERLAP** ([LayoutScan]): a word cut, lines lost, « … » where a word should be whole, a text over another;
 * - **LATIN** (Arabic, Hindi, Chinese, in the Mode simple): an English word (« Snare », « Loop », « Bridge »…) on a
 *   screen promised « everyday words » (promises t5-Aa-4, t5-Bd-1, t5-Fc-ecran-1).
 * What is accepted is in [Allowed] and [LATIN_OK], each with its reason. Not here: the Mix (another team's) and the
 * export screen (another team's).
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-xxhdpi")
class LanguagesScanTest(private val screen: String, private val code: String, private val font: String) {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @After
    fun back() {
        h.finish()
        RuntimeEnvironment.setFontScale(1f)
    }

    private fun tap(tag: String) {
        val node = compose.onNodeWithTag(tag)
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
    }

    /** A screen: how the app opens and the gestures that lead to it. */
    private class Screen(val simple: Boolean = false, val firstLaunch: Boolean = false, val launchChoice: Boolean = false, val go: LanguagesScanTest.() -> Unit = {})

    @Test
    fun screenReadsWhole() {
        val lang = Lang.entries.first { it.code == code }
        val s = SCREENS.getValue(screen)
        if (font != "1.0") RuntimeEnvironment.setFontScale(font.toFloat())
        // The Pro's screens at 200 % on a tablet upright, as tour 5's seat F b (the Mode simple's on the small phone).
        if (font != "1.0" && !s.simple && !s.firstLaunch && !s.launchChoice) RuntimeEnvironment.setQualifiers("w800dp-h1280dp-hdpi")
        h.start(simple = s.simple, firstLaunch = s.firstLaunch, lang = lang, launchChoice = s.launchChoice)
        if (s.launchChoice) h.awaitTag("launch_edited")
        s.go(this)
        h.awaitGuide()
        compose.mainClock.advanceTimeBy(800)
        compose.waitForIdle()
        val density = ApplicationProvider.getApplicationContext<android.content.Context>().resources.displayMetrics.density
        val found = LayoutScan.scan(compose, density, CaptureMatrixTest.LAYOUT)
            .filterNot { f -> Allowed.matches("lang_$screen", f) || ACCEPTED.any { a -> a.containsMatchIn(f.toString()) } ||
                OPEN.any { (where, what) -> where.matches("${screen}_$font") && what.containsMatchIn(f.toString()) } }
            .map { it.toString() }.toMutableList()
        if (s.simple && lang in NON_LATIN) found += latinWords(compose)
        assertTrue("$screen $code font $font:\n" + found.joinToString("\n"), found.isEmpty())
    }

    companion object {
        /**
         * Findings looked at and accepted, each for its reason:
         * - the test project's own name shortened with « … » in the top bar (a user's long name is, by design: a few words kept);
         * - « Sauvegardé » (the saved tick, said by its tick alone when the word does not fit): another team's work (export, « Sauvegardé »);
         * - the drum grid's one-line hint, shortened on purpose in every language (its whole text in « ? »).
         */
        val ACCEPTED = listOf(
            Regex("« (Mon premier rythme|Mein erster Beat|मेरी पहली बीट|我的第一个节奏|إيقاعي الأول)[^»]* » ellipsized"),
            Regex("CUT « (Sauvegardé|Gespeichert|تم الحفظ|सेव हो गया|已保存)[^»]* »"),
            Regex("« (Les pads sonnent|Pads klingen|الـ?[^»]*تُصدر الصوت|पैड बजते हैं|打击垫只发声)[^»]* » ellipsized"),
        )

        /**
         * Left open, looked at and written in docs/REVIEW.md (tour 5, lot « langues »), each a screen (« <screen>_<font> ») and
         * what it shows; a new finding on these screens still fails.
         * - the Mode simple at 200 % on a small phone: the Guide bar's one line (its whole step one touch away), the position's
         *   counter in the play strip, the keyboard's « Vider » and « Octave », the step's header, one pad name in German;
         * - the Pro at 200 % (tablet upright): the play strip's loop chip, the Structure's column heads, the Sons effects tab,
         *   the whole Live grid (a chantier of its own).
         */
        val OPEN: List<Pair<Regex, Regex>> = listOf(
            Regex("(simple_\\w+|welcome_mode)_2\\.0") to Regex("CUT « +· [^»]+ » ellipsized"),
            Regex("(simple_\\w+|welcome_mode)_2\\.0") to Regex("CUT « (mesure|Takt|المازورة|बार|第) ?1/3[^»]* » lines dropped"),
            Regex("simple_keys_2\\.0") to Regex("CUT « (\\s*(Vider|Leeren|مسح|खाली करो|清空)|Octave \\d|Oktave \\d)[^»]* »"),
            Regex("simple_rhythm_2\\.0") to Regex("CUT « (PAS : |Händeklatschen)[^»]* » a word split"),
            Regex("launch_simple_2\\.0") to Regex("CUT « Simple » a word split"),
            Regex("(plus|sons|structure)_2\\.0") to Regex("CUT « (Loop|Boucler|लूप|循环) » drawn past"),
            Regex("structure_2\\.0") to Regex("CUT « [^»]+ » (drawn past|lines cut)"),
            Regex("sons_2\\.0") to Regex("CUT « ([^»]*ثرات|इफ़ेक्ट्स|स्टेप: [^»]*) » (lines dropped|wider)"),
            Regex("live_2\\.0") to Regex("CUT "),
        )

        /** The five languages of tour 6 (docs/decisions/tour-5-langues.md). */
        val LANGS = listOf("fr", "de", "ar", "hi", "zh-CN")
        val FONTS = listOf("1.0", "2.0")

        /** The languages whose script is not Latin: an English word there is a word the person cannot read. */
        val NON_LATIN = setOf(Lang.AR, Lang.HI, Lang.ZH_CN, Lang.JA, Lang.KO)

        private val SCREENS: Map<String, Screen> = linkedMapOf(
            "welcome_mode" to Screen(firstLaunch = true),
            "launch_simple" to Screen(simple = true, launchChoice = true),
            "simple_song" to Screen(simple = true),
            "simple_rhythm" to Screen(simple = true) { tap("tab_simple_Rythme") },
            "simple_keys" to Screen(simple = true) { tap("tab_simple_Clavier") },
            "simple_more" to Screen(simple = true) { tap("top_more") },
            "launch" to Screen(launchChoice = true),
            "structure" to Screen(),
            "sons" to Screen { tap("tab_Sons") },
            "live" to Screen { tap("tab_Live") },
            "plus" to Screen { tap("tab_Plus") },
        )

        @JvmStatic @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}_{2}")
        fun cases(): List<Array<Any>> = SCREENS.keys.flatMap { s -> LANGS.flatMap { l -> FONTS.map { f -> arrayOf<Any>(s, l, f) } } }

        /**
         * Latin words a non-Latin interface may show, each for its reason: units and file kinds written so on every
         * phone (BPM, dB, Hz, MIDI, M4A…), the part letters (A, B, C), the app's and the brands' names, and the test
         * project's own names (a user's name is shown as typed).
         */
        val LATIN_OK = setOf(
            "BPM", "dB", "Hz", "kHz", "ms", "MIDI", "M4A", "WAV", "FLAC", "MP3", "ZIP", "zip", "OK", "LUFS",
            "Pocket", "Studio", "BenStudio", "YouTube", "TikTok", "Instagram", "Spotify", "Apple", "Music", "SoundCloud", "Bluetooth",
            // The harness's song (AppHarness.songWithTracks): its project and track names, typed by the « user ».
            "Lead", "Bass", "Pad", "Chords", "Drums", "Audio", "Synth",
            // The app's features, one name in every language: the 303 (« Acid 303 »), « Remix », « Live », « Pad Live ».
            "Acid", "Remix", "Live",
        )

        private val WORD = Regex("[A-Za-z][A-Za-z'’-]+")

        /** The English words the screen shows, outside [LATIN_OK]. */
        fun latinWords(rule: androidx.compose.ui.test.junit4.ComposeTestRule): List<String> {
            val nodes = rule.onAllNodes(SemanticsMatcher("any") { true }, useUnmergedTree = true).fetchSemanticsNodes(atLeastOneRootRequired = false)
            return nodes.filterNot { LayoutScan.hidden(it) }.flatMap { n ->
                n.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }
            }.flatMap { text -> WORD.findAll(text).map { it.value }.filter { it !in LATIN_OK }.map { "LATIN « $it » in « ${text.take(60)} »" } }
                .distinct()
        }
    }
}
