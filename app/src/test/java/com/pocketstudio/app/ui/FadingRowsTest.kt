package com.pocketstudio.app.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.audit.edgeDistance
import com.pocketstudio.app.live.LiveActions
import com.pocketstudio.app.live.LiveScreen
import com.pocketstudio.app.live.LiveScreenUi
import com.pocketstudio.app.live.liveColumnsOf
import com.pocketstudio.app.mixer.OutputWords
import com.pocketstudio.app.mixer.SimpleMixActions
import com.pocketstudio.app.mixer.SimpleMixScreen
import com.pocketstudio.app.mixer.SimpleMixUi
import com.pocketstudio.app.nav.acidTechnoSong
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.AcidPattern
import com.pocketstudio.model.AcidStep
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.LiveState
import com.pocketstudio.model.Project
import com.pocketstudio.model.Remix
import com.pocketstudio.model.Scene
import com.pocketstudio.model.addAudioTrack
import com.pocketstudio.model.addTrack
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.PocketStudioTheme
import com.pocketstudio.ui.fadingEdges
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
 * Tour 6, lot 5, R19 (« la ligne G du Live » in Arabic, the Mix's strips, « Ma chanson préférée »: a row cut at the
 * edge with nothing saying it goes on): a row that goes on past the screen fades on the side where more waits, and only
 * there ([fadingEdges]: the end side, right in left-to-right languages, LEFT in Arabic); a row that fits fades nowhere.
 * Read on the pixels: a faded edge shows the ground behind the row.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-xxhdpi")
class FadingRowsTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun tearDown() {
        I18n.lang = Lang.FR
    }

    private val ground = Color(0xFFFF00FF)

    private class Edges(val left: Float, val right: Float) {
        override fun toString() = "left %.3f, right %.3f".format(left, right)
    }

    private fun edges(image: PixelMap, ground: Color) = Edges(edgeDistance(image, 0, ground), edgeDistance(image, image.width - 1, ground))

    /** The scrolled row (its window) that holds the node tagged [tag]. */
    private fun rowHolding(tag: String): PixelMap = compose.onNode(
        SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange) and hasAnyDescendant(hasTestTag(tag)), useUnmergedTree = true,
    ).captureToImage().toPixelMap()

    // A faded edge shows (almost) the ground; content shows well above it.
    private fun faded(d: Float, content: Float) = d < content * 0.25f
    private fun clear(d: Float, content: Float) = d >= content * 0.6f

    @Test
    fun aRowFadesOnlyOnTheSideWhereMoreWaitsInBothDirections() {
        var rtl by mutableStateOf(false)
        var boxes by mutableStateOf(10)
        lateinit var state: ScrollState
        compose.setContent {
            key(rtl, boxes) {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirectionOf(rtl)) {
                    Box(Modifier.background(ground)) {
                        val s = rememberScrollState()
                        state = s
                        Row(Modifier.testTag("row").width(200.dp).fadingEdges(s).horizontalScroll(s)) {
                            repeat(boxes) { Box(Modifier.size(40.dp).background(Color.White)) }
                        }
                    }
                }
            }
        }
        fun read(): Edges {
            compose.waitForIdle()
            return edges(compose.onNodeWithTag("row").captureToImage().toPixelMap(), ground)
        }
        val problems = ArrayList<String>()
        for (r in listOf(false, true)) {
            rtl = r
            boxes = 10
            val dir = if (r) "rtl" else "ltr"
            // At the start: more waits at the end only (right, or left in Arabic).
            read().let { e ->
                val (start, end) = if (r) e.right to e.left else e.left to e.right
                if (!clear(start, 1f) || !faded(end, 1f)) problems += "$dir at the start: $e"
            }
            // At the end: more waits at the start only.
            compose.runOnIdle { state.dispatchRawDelta(10_000f) }
            read().let { e ->
                val (start, end) = if (r) e.right to e.left else e.left to e.right
                if (!faded(start, 1f) || !clear(end, 1f)) problems += "$dir at the end: $e"
            }
            // Five boxes of 40 dp fill the 200 dp row exactly: nothing waits, nothing fades.
            boxes = 5
            read().let { e -> if (!clear(e.left, 1f) || !clear(e.right, 1f)) problems += "$dir fitting: $e" }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    private fun showMix(lang: Lang) {
        I18n.lang = lang
        val song = acidTechnoSong()
        val meters = FloatArray(NativeEngine.METERS)
        val loudness = FloatArray(NativeEngine.LOUDNESS) { NativeEngine.LOUDNESS_NONE }
        compose.setContent {
            I18n.lang = lang
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirectionOf(lang.rtl)) {
                PocketStudioTheme {
                    Box(Modifier.background(ground)) {
                        SimpleMixScreen(
                            SimpleMixUi("Acid techno", simpleTracks(song), emptyMap(), 0f, false, null, null),
                            { meters }, { loudness }, SimpleMixActions(), words = { OutputWords.GOOD },
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    /** The Mix's strips go on past a 360 dp phone: faded at their end, whole at their start (C a, « Chute grave »). */
    private fun mixFadesAtItsEnd(lang: Lang) {
        showMix(lang)
        val e = edges(rowHolding("mix_out"), ground)
        val (start, end) = if (lang.rtl) e.right to e.left else e.left to e.right
        // A strip is see-through (about half): its content reads some 0.3 off the ground.
        assertTrue("${lang.code}: the strips' start is cut or faded ($e)", start > 0.15f)
        assertTrue("${lang.code}: the strips' end is not faded ($e)", end < 0.05f)
    }

    @Test
    fun theMixStripsFadeWhereMoreStripsWait() = mixFadesAtItsEnd(Lang.FR)

    @Test
    fun theMixStripsFadeWhereMoreStripsWaitInArabic() = mixFadesAtItsEnd(Lang.AR)

    /** On a tablet the strips fit: the last one (« Sortie ») is drawn whole to its edge. */
    private fun mixThatFitsFadesNowhere(lang: Lang) {
        showMix(lang)
        val row = compose.onNode(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange) and hasAnyDescendant(hasTestTag("mix_out")), useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val out = compose.onNodeWithTag("mix_out").fetchSemanticsNode().boundsInRoot
        val image = rowHolding("mix_out")
        // A few pixels inside « Sortie »'s end edge (past its outline): no fade over it.
        val x = if (lang.rtl) (out.left - row.left).toInt() + 3 else (out.right - row.left).toInt() - 4
        val d = edgeDistance(image, x, ground)
        assertTrue("${lang.code}: « Sortie » fades at its end ($d)", d > 0.15f)
    }

    @Test
    @Config(qualifiers = "w800dp-h1280dp-hdpi")
    fun theMixStripsThatFitFadeNowhere() = mixThatFitsFadesNowhere(Lang.FR)

    @Test
    @Config(qualifiers = "w800dp-h1280dp-hdpi")
    fun theMixStripsThatFitFadeNowhereInArabic() = mixThatFitsFadesNowhere(Lang.AR)

    /** Eight tracks on a 360 dp phone: the Live grid's columns go on past the edge (B c q9, Arabic: « la ligne G »). */
    private fun liveFadesAtItsEnd(lang: Lang) {
        I18n.lang = lang
        // Batterie, Acid 303, Synthé, Basse, Voix, Remix (LiveDockTest's six) and two more tracks.
        val acid = AcidPattern(steps = List(AcidPattern.MAX_STEPS) { AcidStep(gate = it == 0) })
        var p = Project(
            scenes = listOf(Scene("A", DrumPattern().toggle(0, 0), label = "Intro", acid = acid), Scene("B", DrumPattern().toggle(1, 4))),
            scene = 0, acid = acid, remix = Remix(setId = "x", name = "Chanson"),
        ).addTrack("Basse")!!.addAudioTrack("Voix")!!.sanitized()
        repeat(2) { p = p.addTrack("Piste ${it + 2}")!! }
        check(liveColumnsOf(p).size >= 8) { "${liveColumnsOf(p).size} columns" }
        compose.setContent {
            I18n.lang = lang
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirectionOf(lang.rtl)) {
                PocketStudioTheme { LiveScreen(LiveScreenUi(p, LiveState.on(0), clock = 5, playing = true), LiveActions()) }
            }
        }
        compose.waitForIdle()
        val first = liveColumnsOf(p).first().key
        val e = edges(rowHolding("live_stop_$first"), com.pocketstudio.app.live.GridLook.Ground)
        val (start, end) = if (lang.rtl) e.right to e.left else e.left to e.right
        // A ■'s ground (GridLook.Panel) is some 0.045 off the grid's.
        assertTrue("${lang.code}: the ■ row's start is faded ($e)", start > 0.03f)
        assertTrue("${lang.code}: the ■ row's end is not faded ($e)", end < 0.012f)
    }

    @Test
    fun theLiveColumnsFadeWhereMoreColumnsWait() = liveFadesAtItsEnd(Lang.FR)

    @Test
    fun theLiveColumnsFadeWhereMoreColumnsWaitInArabic() = liveFadesAtItsEnd(Lang.AR)
}
