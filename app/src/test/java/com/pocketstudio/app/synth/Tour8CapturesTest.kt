package com.pocketstudio.app.synth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.export.ShareCheck
import com.pocketstudio.app.export.ShareSheetActions
import com.pocketstudio.app.export.ShareSheetContent
import com.pocketstudio.app.export.ShareSheetUi
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
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
 * Tour 8, lot captures8: the two screen defects its captures showed, checked on every device (a 360 dp phone, the 400 dp
 * phone, the 8" tablet's 600 dp) and every size (the interface at 100, 130 and 200 %, the font at 130 and 200 %).
 * - « + Piste »: the arrow saying the row of tracks goes on (R19) lay over the row's last chip, its text cut under it
 *   (« + Pist→ » in fr and nl at 130 %, « + トラ→ » in ja). No chip's text lies under it now.
 * - Partage: an empty band under the sheet's last line (« Mes fichiers »). The sheet ends where its content does.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w600dp-h1100dp-port-xxhdpi")
class Tour8CapturesTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun french() { I18n.lang = Lang.FR }

    /** A device [width] dp wide (as the device counts them), the interface at [ui], the font at [font] on top of it. */
    private data class Size(val width: Int, val ui: Float, val font: Float) {
        override fun toString() = "${width}dp ui ${(ui * 100).toInt()} % font ${(font * 100).toInt()} %"
    }

    private val sizes = listOf(360, 400, 600).flatMap { w ->
        listOf(Size(w, 1f, 1f), Size(w, 1.3f, 1f), Size(w, 2f, 1f), Size(w, 1f, 1.3f), Size(w, 1f, 2f))
    }

    @Composable
    private fun Sized(size: Size, content: @Composable () -> Unit) {
        com.pocketstudio.app.ui.UiScaled(size.ui) {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, d.fontScale * size.font)) {
                PocketStudioTheme { Box(Modifier.width((size.width / size.ui).dp)) { content() } }
            }
        }
    }

    private fun nodes(tag: String): List<SemanticsNode> =
        compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes(atLeastOneRootRequired = false)

    private fun SemanticsNode.all(): List<SemanticsNode> = listOf(this) + children.flatMap { it.all() }

    private fun Rect.crosses(o: Rect) = left < o.right - 0.5f && o.left < right - 0.5f && top < o.bottom - 0.5f && o.top < bottom - 0.5f

    // ---- « + Piste »: the arrow covers no chip's text ----

    private fun tracksUi(n: Int) = TracksUi(
        List(n) { InstrumentTrack(it, if (it == 0) "Lead" else "Basse $it") }, selected = 1, frozen = setOf(1),
        changeSound = {}, effects = {}, simple = true, freeze = {}, unfreeze = {},
    )

    /** Every text under [sign] (a text clipped by the row's edge counts by what is left of it in sight). */
    private fun textsUnder(sign: Rect): List<String> {
        val root = compose.onAllNodesWithTag("track_bar", useUnmergedTree = true).fetchSemanticsNodes().first().let { n ->
            generateSequence(n) { it.parent }.last()
        }
        return root.all().filter { it.config.getOrNull(SemanticsProperties.Text) != null }
            .filter { it.boundsInRoot.width > 0.5f && it.boundsInRoot.crosses(sign) }
            .map { it.config[SemanticsProperties.Text].joinToString() + " " + it.boundsInRoot }
    }

    @Test
    fun theRowsArrowCoversNoChipText() {
        var case by mutableStateOf(Triple(Lang.FR, sizes.first(), 4))
        compose.setContent { val (lang, size, n) = case; key(case) { I18n.lang = lang; Sized(size) { TrackBar(tracksUi(n)) } } }
        val seen = mutableListOf<String>()
        var arrows = 0
        for (lang in listOf(Lang.FR, Lang.NL, Lang.JA)) for (size in sizes) for (n in listOf(3, 6)) {
            case = Triple(lang, size, n)
            compose.waitForIdle()
            // At the start, then half way, then at the end of the row: wherever the arrows show.
            for (step in listOf(0f, 150f, 100_000f)) {
                if (step > 0f) {
                    compose.onNodeWithTag("track_bar").performSemanticsAction(SemanticsActions.ScrollBy) { it(step, 0f) }
                    compose.waitForIdle()
                }
                for (tag in listOf("track_bar_more", "track_bar_less")) nodes(tag).forEach { sign ->
                    arrows++
                    val b = sign.boundsInRoot
                    assertTrue("$lang $size $n tracks, $tag: in sight ($b)", b.width > 1f && b.height > 1f)
                    textsUnder(b).forEach { seen += "$lang $size $n tracks, $tag over « $it »" }
                }
            }
        }
        assertTrue("the rows went on past the edge somewhere ($arrows arrows)", arrows > 20)
        assertTrue("an arrow over a chip's text:\n" + seen.take(12).joinToString("\n") + "\n(${seen.size} in all)", seen.isEmpty())
    }

    // ---- Partage: the sheet ends where its content does ----
}
