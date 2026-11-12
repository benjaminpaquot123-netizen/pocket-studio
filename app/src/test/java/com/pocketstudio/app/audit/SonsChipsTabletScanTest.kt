package com.pocketstudio.app.audit

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import com.pocketstudio.app.nav.AppHarness
import com.pocketstudio.app.nav.songWithTracks
import com.pocketstudio.model.TrackKind
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Round 5 (scan r3): the owner's Galaxy Tab A11, upright, Mode Pro, Sons: the track chips' second line cut at
 * its bottom. Hands-on audit, chantier 2 (G7, rule 16 of docs/DESIGN.md « Jamais de hauteur fixe autour d'un texte »):
 * the chips are one shared `TrackChip`, as tall as their two lines need. The real Sons screen on a tablet and on a phone,
 * from font 1.0 to 1.5, read by [LayoutScan] (CUT): no chip's word cut, shortened or dropped at any size (what it finds
 * is written to `app/build/sons-chips-scan/`).
 */
abstract class SonsChipsTabletScanTest(private val device: String, private val font: Float) {
    companion object {
        @JvmStatic @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> = listOf(1.0f, 1.1f, 1.15f, 1.2f, 1.25f, 1.3f, 1.5f).map { arrayOf<Any>(it) }
    }

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @After
    fun end() {
        h.finish()
        RuntimeEnvironment.setFontScale(1f)
    }

    @Test
    fun sonsChips() {
        RuntimeEnvironment.setFontScale(font)
        h.start(build = {
            songWithTracks()
            addTrackOf(TrackKind.Chords)
            renameTrack(project.value.tracks.last().id, "Orgue")
            addTrackOf(TrackKind.Melody)
            renameTrack(project.value.tracks.last().id, "Le grand lead")
            selectTrack(0)
        })
        compose.onNodeWithTag("tab_Sons").performClick()
        compose.waitForIdle()
        val density = h.app.resources.displayMetrics.density
        val nodes = compose.onAllNodes(SemanticsMatcher("any") { true }, useUnmergedTree = true).fetchSemanticsNodes()
        val bar = nodes.first { it.config.getOrNull(SemanticsProperties.TestTag) == "track_bar" }
        val texts = nodes.filter { n -> under(n, bar) && n.config.contains(SemanticsProperties.Text) }
        check(texts.isNotEmpty()) { "no chip text in the track bar" }
        val out = StringBuilder("$device font $font density $density\n")
        for (n in texts) {
            val l = layoutOf(n)
            val b = n.boundsInRoot
            out.append(
                "« %s » node %.1f x %.1f dp at y %.1f, shown y %.1f..%.1f, layout h %.1f, last line bottom %.1f, overflowH %s\n".format(
                    n.config[SemanticsProperties.Text].joinToString { it.text }, n.size.width / density, n.size.height / density,
                    n.positionInRoot.y / density, b.top / density, b.bottom / density, (l?.size?.height ?: 0) / density,
                    (l?.let { it.getLineBottom(it.lineCount - 1) } ?: 0f) / density, l?.didOverflowHeight,
                ),
            )
        }
        val found = LayoutScan.scan(compose, density, setOf(LayoutScan.Kind.CUT))
        val barTexts = texts.mapNotNull { it.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { t -> t.text } }.toSet()
        out.append("CUT findings in the track bar:\n")
        found.filter { it.what in barTexts }.forEach { out.append("  $it\n") }
        val dir = File("build/sons-chips-scan").apply { mkdirs() }
        File(dir, "${device}_$font.txt").writeText(out.toString())
        println(out)
        val cuts = found.filter { it.what in barTexts }
        assertTrue("$device font $font: track chips' words cut:\n$out", cuts.isEmpty())
    }

    private fun under(n: SemanticsNode, a: SemanticsNode): Boolean {
        var p = n.parent
        while (p != null) { if (p.id == a.id) return true; p = p.parent }
        return false
    }

    private fun layoutOf(n: SemanticsNode): TextLayoutResult? {
        val get = n.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action ?: return null
        val list = mutableListOf<TextLayoutResult>()
        return if (get(list)) list.firstOrNull() else null
    }
}

/** The Galaxy Tab A11 upright, as the capture matrix has it. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w600dp-h1006dp-tvdpi")
class SonsChipsTablet8ScanTest(font: Float) : SonsChipsTabletScanTest("tablet8", font)

/** An 800 x 1280 dp tablet upright. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w800dp-h1280dp-hdpi")
class SonsChipsTablet800ScanTest(font: Float) : SonsChipsTabletScanTest("tablet800", font)

/** The owner's Honor phone class: 411 x 914 dp upright. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class SonsChipsPhone411ScanTest(font: Float) : SonsChipsTabletScanTest("phone411", font)

/** A small phone: 360 x 780 dp upright. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-xxhdpi")
class SonsChipsPhone360ScanTest(font: Float) : SonsChipsTabletScanTest("phone360", font)
