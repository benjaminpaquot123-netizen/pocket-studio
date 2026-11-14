package com.pocketstudio.app.padlive

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LivePads
import com.pocketstudio.model.PadLive
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.PocketStudioTheme
import com.pocketstudio.ui.WholeWordsAutoSize
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Lot D2, leftover of B3: Pad Live upright on a 360 dp phone, « Plus » open (the pads at their
 * shortest): the title (« Pad… ») and the pads' names (« Monté/e » split over two lines) fit, words
 * whole, in each of the 14 languages: the shared [com.pocketstudio.ui.FitText] does it.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-xxhdpi")
class PadLiveNarrowWordsTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun tearDown() {
        I18n.lang = Lang.FR
    }

    /** The Techno pack's hits (« Montée » among them) and long names, on page A. */
    private val names = listOf("Laser", "Montée", "Impact", "Sirène", "Boucle de kick", "Charleston ouvert", "Montée courte", "Caisse claire")
    private val pads: LivePads = PadLive.placed(LivePads(), 0, names.map { LiveClip("s.wav", it, 1, oneShot = true) })

    private fun layout(tag: String): TextLayoutResult {
        val node = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()
        val results = ArrayList<TextLayoutResult>()
        node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        check(results.isNotEmpty()) { "$tag: no text laid out" }
        return results.first()
    }

    @Test
    fun titleAndPadNamesFitWithWholeWordsInEveryLanguage() {
        var lang by mutableStateOf(Lang.FR)
        compose.setContent {
            I18n.lang = lang
            androidx.compose.runtime.key(lang) {
                PocketStudioTheme {
                    PadLiveScreen(
                        PadLiveUi(pads, List(LivePads.ALL) { 0 }, page = 0, pack = "techno", playing = false, tempoBpm = 124.0, beatsPerBar = 4, riserBars = 4),
                        PadLiveActions(), clockOf = { -1 }, still = true,
                    )
                }
            }
        }
        val problems = ArrayList<String>()
        for (l in Lang.entries) {
            lang = l
            compose.waitForIdle()
            compose.onNodeWithTag("padlive_more").performClick()
            compose.waitForIdle()
            val title = layout("padlive_title")
            if (!WholeWordsAutoSize.fits(title, tr("Pad Live"))) problems += "${l.code}: title"
            for (pad in names.indices) {
                val r = layout("padlive_name_$pad")
                if (!WholeWordsAutoSize.fits(r, r.layoutInput.text.text)) problems += "${l.code}: pad ${r.layoutInput.text.text}"
            }
            compose.onNodeWithTag("padlive_more").performClick()
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }
}
