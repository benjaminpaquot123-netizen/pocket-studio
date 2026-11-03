package com.pocketstudio.app.padlive

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LiveDivision
import com.pocketstudio.model.LivePads
import com.pocketstudio.model.LiveStarter
import com.pocketstudio.model.PadLive
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
import org.robolectric.annotation.GraphicsMode

/**
 * Pad Live's scene (n° 25, lot B2) under the finger: the Filtre held then slid up into its panel
 * (cutoff across, resonance up; let go: everything back), the Roulement slid to a division (1/4 to
 * 1/32, triplets between), a row's ▶, « Enregistrer ce que je joue » and its take's places; and
 * the « règle partout » of the B1 leftover: no held effect's words are cut on a phone, in any
 * language (« souffle qui monte » was).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)  // texts measured with real fonts
@Config(qualifiers = "w360dp-h780dp-xxhdpi")  // a small phone upright
class PadLiveSceneTest {
    @get:Rule
    val compose = createComposeRule()

    private val done = ArrayList<String>()

    private val pads: LivePads = PadLive.placed(LivePads(), 0, LiveStarter.PADS.mapIndexed { i, p ->
        LiveClip("s$i.wav", p.name, if (p.oneShot) 1 else p.beats, p.oneShot, p.gain)
    })

    private var played by mutableStateOf<PlayedTakeUi>(PlayedTakeUi.Ready)

    private fun actions() = PadLiveActions(
        filter = { done += "filter ${it?.let { v -> "%.2f".format(java.util.Locale.ROOT, v) }}" },
        resonance = { done += "resonance ${it?.let { v -> "%.2f".format(java.util.Locale.ROOT, v) }}" },
        roll = { done += "roll ${it?.name}" },
        launchRow = { done += "row $it" },
        played = { done += "played" },
        placeOnPad = { done += "on pad" },
        placeInBrick = { done += "in brick" },
        dropTake = { done += "drop" },
    )

    private fun show() = compose.setContent {
        PocketStudioTheme {
            PadLiveScreen(
                PadLiveUi(pads, List(LivePads.ALL) { 0 }, page = 0, pack = "fete", playing = true, tempoBpm = 124.0, beatsPerBar = 4,
                    played = played, emptyPad = "A13"),
                actions(), clockOf = { 0 }, still = true,
            )
        }
    }

    @After
    fun french() {
        I18n.lang = Lang.FR
    }

    @Test
    fun theFilterSlidUpIntoItsPanelTakesCutoffAndResonance() {
        show()
        val panelTop = compose.onNodeWithTag("padlive_fx_filter").fetchSemanticsNode().boundsInRoot.top
        compose.onNodeWithTag("padlive_fx_filter").performTouchInput {
            down(Offset(width * 0.25f, height / 2f))
            // On its button: the B1 filter, by the finger's place across it (grave side); resonance at rest.
            moveBy(Offset(1f, 0f))
        }
        compose.onNodeWithTag("padlive_held_area").assertIsDisplayed()
        assertTrue(done.toString(), done.first().startsWith("filter 0.2"))
        assertEquals("resonance 0.30", done[1])
        // Slid up into the panel's grid, to its right side, near its top: high-pass, more resonance.
        val area = compose.onNodeWithTag("padlive_held_area").fetchSemanticsNode().boundsInRoot
        val button = compose.onNodeWithTag("padlive_fx_filter").fetchSemanticsNode().boundsInRoot
        assertTrue(area.bottom <= panelTop)
        done.clear()
        compose.onNodeWithTag("padlive_fx_filter").performTouchInput {
            moveTo(Offset(area.left + area.width * 0.9f - button.left, area.top + area.height * 0.1f - button.top))
        }
        compose.waitForIdle()
        val filter = done.last { it.startsWith("filter") }.removePrefix("filter ").toFloat()
        val resonance = done.last { it.startsWith("resonance") }.removePrefix("resonance ").toFloat()
        assertEquals(0.9f, filter, 0.03f)
        assertEquals(HeldFx.resonanceAt(0.9f), resonance, 0.03f)
        compose.onNodeWithTag("padlive_filter_words").assertIsDisplayed()
        // Let go: back to rest, the panel gone.
        done.clear()
        compose.onNodeWithTag("padlive_fx_filter").performTouchInput { up() }
        compose.waitForIdle()
        assertEquals(listOf("filter null", "resonance null"), done)
        assertTrue(compose.onAllNodes(androidx.compose.ui.test.hasTestTag("padlive_held_panel")).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun theRollSlidToADivisionAndItsRowAndTake() {
        show()
        compose.onNodeWithTag("padlive_fx_roll").performTouchInput { down(center) }
        assertEquals("roll SIXTEENTH", done.last())
        val area = compose.onNodeWithTag("padlive_held_area").fetchSemanticsNode().boundsInRoot
        val button = compose.onNodeWithTag("padlive_fx_roll").fetchSemanticsNode().boundsInRoot
        val y = area.top + area.height / 2f - button.top
        // The far left: a quarter; then the eighth's triplet (third of six).
        compose.onNodeWithTag("padlive_fx_roll").performTouchInput { moveTo(Offset(area.left + 4f - button.left, y)) }
        compose.waitForIdle()
        assertEquals("roll QUARTER", done.last())
        compose.onNodeWithTag("padlive_fx_roll").performTouchInput { moveTo(Offset(area.left + area.width * 2.5f / 6f - button.left, y)) }
        compose.waitForIdle()
        assertEquals("roll ${LiveDivision.EIGHTH_TRIPLET.name}", done.last())
        compose.onNodeWithTag("padlive_fx_roll").performTouchInput { up() }
        compose.waitForIdle()
        assertEquals("roll null", done.last())
        // The next press starts where it was left (its button says so).
        compose.onNodeWithTag("padlive_fx_roll").performTouchInput { down(center); up() }
        compose.waitForIdle()
        assertTrue(done.contains("roll ${LiveDivision.EIGHTH_TRIPLET.name}"))
        // A row's ▶: that row, once.
        done.clear()
        compose.onNodeWithTag("padlive_row_1").performClick()
        assertEquals(listOf("row 1"), done)
        // « Enregistrer ce que je joue », then its take: on the empty pad, in a brick, or thrown.
        compose.onNodeWithTag("padlive_played").performClick()
        assertEquals("played", done.last())
        played = PlayedTakeUi.Done(2)
        compose.waitForIdle()
        compose.onNodeWithTag("padlive_take_pad").performClick()
        compose.onNodeWithTag("padlive_take_brick").performClick()
        assertEquals(listOf("on pad", "in brick"), done.takeLast(2))
    }

    /** The words of a text node as laid out (cut: its layout overflows). */
    private fun cut(tag: String): Boolean {
        val node = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()
        val results = ArrayList<TextLayoutResult>()
        node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        check(results.isNotEmpty()) { "$tag: no text laid out" }
        val r = results.first()
        // (didOverflowWidth is no help: a short text laid out narrower than its box says it overflows.)
        return r.multiParagraph.maxIntrinsicWidth > r.size.width + 0.5f || r.didOverflowHeight || r.isLineEllipsized(r.lineCount - 1)
    }

    @Test
    fun noHeldEffectsWordsAreCutOnAPhoneInAnyLanguage() {
        var lang by mutableStateOf(Lang.FR)
        compose.setContent {
            I18n.lang = lang
            androidx.compose.runtime.key(lang) {
                PocketStudioTheme {
                    PadLiveScreen(
                        PadLiveUi(pads, List(LivePads.ALL) { 0 }, page = 0, pack = "fete", playing = false, tempoBpm = 124.0, beatsPerBar = 4, riserBars = 8),
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
            for (fx in listOf("filter", "echo", "roll", "riser", "brake", "stutter")) {
                for (part in listOf("title", "line")) if (cut("padlive_fx_${fx}_$part")) problems += "${l.code}: $fx $part"
            }
            // « Enregistrer ce que je joue » beside « Moins » (« Plus » open: its longest neighbour).
            if (cut("padlive_played_words")) problems += "${l.code}: Enregistrer ce que je joue"
            compose.onNodeWithTag("padlive_more").performClick()
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }
}
