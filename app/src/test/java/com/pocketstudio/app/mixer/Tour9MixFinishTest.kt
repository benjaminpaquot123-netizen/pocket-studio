package com.pocketstudio.app.mixer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import com.pocketstudio.app.audit.CutWords
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.layoutDirectionOf
import org.junit.After
import com.pocketstudio.app.nav.acidTechnoSong
import com.pocketstudio.app.ui.UiScaled
import com.pocketstudio.app.ui.simpleTracks
import androidx.compose.ui.test.onRoot
import org.robolectric.RuntimeEnvironment
import org.junit.Assert.assertTrue
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.Project
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 9, lot L2 (Mix), the finishing touches seen by the seats, each test one acceptance phrase.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-port-hdpi", fontScale = 2.0f)
class Tour9MixFinishTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun tearDown() {
        I18n.lang = Lang.FR
        RuntimeEnvironment.setFontScale(2f)
    }

    /** Five strips, the last one waiting on the right; [lang] switched in place. */
    private val five = BASIC_STRIPS + StripInfo(Mixer.TRACK_STRIPS[5], "Mélodie")
    private val lang = mutableStateOf(Lang.FR)

    private fun showPro(channels: List<com.pocketstudio.model.Channel> = Project().mixer.channels) {
        compose.setContent {
            I18n.lang = lang.value
            key(lang.value) {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirectionOf(lang.value.rtl)) {
                    PocketStudioTheme { Box(Modifier.fillMaxSize()) { MixerScreen(channels, { FloatArray(NativeEngine.METERS) }, MixerActions(), strips = five) } }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun speak(l: Lang) {
        lang.value = l
        I18n.lang = l
        compose.waitForIdle()
    }

    /** F023 « Dans le Mix, chaque tranche garde son nom lisible en entier, même après avoir fait défiler » (« Drum / s »). */
    @Test
    fun f023_eachStripKeepsItsNameWholeAfterAScroll() {
        showPro()
        val problems = ArrayList<String>()
        for (l in Lang.entries) {
            speak(l)
            compose.onNodeWithTag("strip_more_${five.last().ch}").performScrollTo()
            compose.waitForIdle()
            CutWords.under(compose, "mixer").filter { c -> c.tags.any { it.startsWith("strip_more_") } }.forEach { problems += "${l.code}: $it" }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    /** F025 « La feuille ouverte depuis une tranche porte le nom de la piste en titre, lisible en entier » (« XY… » cut at the top). */
    @Test
    fun f025_aStripsSheetIsTitledWithTheTracksWholeName() {
        showPro()
        compose.onNodeWithTag("strip_more_1").performScrollTo().performTouchInput { click() }
        compose.waitForIdle()
        // Scrolled down to its effects: the title is still there, whole.
        compose.onNodeWithTag("effects_box_1", useUnmergedTree = true).performScrollTo()
        compose.waitForIdle()
        val title = compose.onNodeWithTag("strip_sheet_title", useUnmergedTree = true).assertIsDisplayed().fetchSemanticsNode()
        assertEquals(com.pocketstudio.model.DefaultNames.shown(BASIC_STRIPS[1].name), title.config[SemanticsProperties.Text].joinToString { it.text })
        val sheet = compose.onNodeWithTag("strip_sheet").fetchSemanticsNode().boundsInRoot
        assertTrue("the title ${title.boundsInRoot} out of the sheet $sheet", title.boundsInRoot.top >= sheet.top - 0.5f)
        val cut = CutWords.under(compose, "strip_sheet").filter { "strip_sheet_title" in it.tags }
        assertTrue(cut.joinToString(), cut.isEmpty())
    }

    /** F030 « En arabe, les valeurs négatives s'écrivent avec le signe moins du bon côté » (« 12− », « ∞− »). */
    @Test
    fun f030_inArabicTheMinusSignIsOnTheLeft() {
        // No mark of the locale's own in the value (Android's Arabic minus is « ؜- », its digits then read right to left).
        speak(Lang.AR)
        listOf(0f, 0.3f, 0.5f, 0.79f).forEach { v ->
            val s = faderDb(v)
            assertTrue("« $s » for $v", s.none { it == '\u061C' || it == '\u200F' } && s.startsWith("\u2066") && s.endsWith("\u2069"))
        }
        val channels = Project().mixer.channels.mapIndexed { i, c -> if (i == 0) c.copy(volume = 0f) else if (i == 1) c.copy(volume = 0.5f) else c }
        showPro(channels)
        speak(Lang.AR)
        val problems = ArrayList<String>()
        val nodes = compose.onAllNodes(hasAnyAncestor(hasTestTag("mixer")) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Text), useUnmergedTree = true)
            .fetchSemanticsNodes()
        var signs = 0
        for (n in nodes) {
            val text = n.config[SemanticsProperties.Text].joinToString(" ") { it.text }
            val at = text.indexOfFirst { it == '−' || it == '-' }
            // The Console's own values (« −6,0 dB », « −∞ dB »); the loudness chip lives in MasterLoudness.kt, another lot.
            if (at < 0 || !DB_VALUE.matches(text.filterNot { it in "\u2066\u2067\u2068\u2069" }.trim())) continue
            val r = ArrayList<TextLayoutResult>().also { n.config.getOrElseNullable(SemanticsActions.GetTextLayoutResult) { null }?.action?.invoke(it) }.firstOrNull() ?: continue
            val after = (at + 1 until text.length).firstOrNull { text[it].isLetterOrDigit() || text[it] == '∞' } ?: continue
            signs++
            if (r.getBoundingBox(at).left > r.getBoundingBox(after).left) problems += "« $text »: the sign drawn right of « ${text[after]} »"
        }
        assertTrue("no negative value read", signs >= 2)
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    private val DB_VALUE = Regex("[−+-]?(\\p{Nd}+[.,٫]\\p{Nd}|∞)( dB)?")

    /** F141 « Le Mix ne crée rien de neuf à chaque image (liste mémorisée) »: the same channels, drawn again, build nothing. */
    @Test
    fun f141_theMixBuildsNothingNewAtEachFrame() {
        val channels = Project().mixer.channels
        var frame by mutableIntStateOf(0)
        compose.setContent {
            // A new list with the same channels at each frame (as the app's state hands them).
            val now = frame.let { channels.toList() }
            PocketStudioTheme { Box(Modifier.fillMaxSize()) { MixerScreen(now, { FloatArray(NativeEngine.METERS) }, MixerActions()) } }
        }
        compose.waitForIdle()
        val built = fitProbeRestLists
        repeat(5) { frame++; compose.waitForIdle() }
        assertEquals("the channels at rest built again at each frame", built, fitProbeRestLists)
    }

    /**
     * F026 « Dans le Mix simple, tous les noms des tranches ont la même taille de texte » and F027 « Toutes les tranches
     * tiennent dans l'écran, alignées à la même hauteur, même à 130 % » (« Sortie » cut at the right edge, the Batterie
     * card taller): every head one size and one height, « Sortie » whole on screen once scrolled to, in every language.
     */
    @Test
    fun f026_f027_theSimpleMixsHeadsShareOneSizeAndHeight() {
        val problems = ArrayList<String>()
        for ((label, ui, font) in listOf(Triple("Taille 130 %", 1.3f, 1f), Triple("font 130 %", 1f, 1.3f), Triple("font 200 %", 1f, 2f))) {
            RuntimeEnvironment.setFontScale(font)
            val meters = FloatArray(NativeEngine.METERS)
            val loudness = FloatArray(NativeEngine.LOUDNESS) { NativeEngine.LOUDNESS_NONE }
            compose.setContent {
                I18n.lang = lang.value
                key(lang.value, label) {
                    CompositionLocalProvider(LocalLayoutDirection provides layoutDirectionOf(lang.value.rtl)) {
                        UiScaled(ui) {
                            PocketStudioTheme {
                                SimpleMixScreen(
                                    SimpleMixUi("Acid techno", simpleTracks(acidTechnoSong()), emptyMap(), 0f, false, null, null),
                                    { meters }, { loudness }, SimpleMixActions(), words = { OutputWords.GOOD },
                                )
                            }
                        }
                    }
                }
            }
            for (l in Lang.entries) {
                speak(l)
                val texts = compose.onAllNodes(hasAnyAncestor(hasTestTag("mix_head")) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Text), useUnmergedTree = true)
                    .fetchSemanticsNodes()
                val sizes = texts.map { n ->
                    ArrayList<TextLayoutResult>().also { n.config[SemanticsActions.GetTextLayoutResult].action?.invoke(it) }.first().layoutInput.style.fontSize
                }.distinct()
                if (texts.size < 4 || sizes.size != 1) problems += "$label ${l.code}: ${texts.size} heads at $sizes"
                val heads = compose.onAllNodes(hasTestTag("mix_head"), useUnmergedTree = true).fetchSemanticsNodes().map { it.boundsInRoot }
                    .filter { it.height > 0f } // a head past the screen's right edge is clipped to nothing
                if (heads.map { Math.round(it.height) }.distinct().size != 1 || heads.map { Math.round(it.top) }.distinct().size != 1)
                    problems += "$label ${l.code}: heads ${heads.map { "${it.top}+${it.height}" }}"
                compose.onNodeWithTag("mix_out_fader").performScrollTo()
                compose.waitForIdle()
                val out = compose.onNodeWithTag("mix_out").fetchSemanticsNode().boundsInRoot
                val screen = compose.onRoot().fetchSemanticsNode().boundsInRoot
                if (out.right > screen.right + 0.5f || out.left < screen.left - 0.5f) problems += "$label ${l.code}: « Sortie » $out on a screen $screen"
                CutWords.under(compose, "simple_mix").filter { "mix_head" in it.tags }.forEach { problems += "$label ${l.code}: $it" }
            }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }
}
