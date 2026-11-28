package com.pocketstudio.app.tuto

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.audit.LayoutScan
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/**
 * Tour 9, F011 (16 seats, t9-02 to t9-74): « Sur la carte des parties, chaque bloc montre son nom et sa durée en entier,
 * la légende se lit et chaque partie a sa propre couleur, même à 200 %. » Narrow blocks showed their bars alone or
 * nothing, and the Intro, the Groove and the Fin (or the Drop and the Drop 2) had the same colour in the legend.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Tour9L13TutoSongMapTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun end() {
        I18n.lang = Lang.FR
    }

    private var lang by mutableStateOf(Lang.FR)
    private var scale by mutableStateOf(1f)
    private var style by mutableStateOf("jump_up")

    @Test
    @Config(qualifiers = "w360dp-h780dp")
    fun everyPartWholeWithItsOwnColourEvenAt200() {
        compose.setContent {
            val d = LocalDensity.current
            key(lang, scale, style) {
                CompositionLocalProvider(
                    LocalDensity provides Density(d.density, scale),
                    LocalLayoutDirection provides if (lang.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                ) {
                    // As in the panel: the screen's width less the panel's sides.
                    PocketStudioTheme { Column(Modifier.width(360.dp).padding(horizontal = 14.dp)) { SongMap(tutoMap(style)) } }
                }
            }
        }
        fun nodes(tag: String) = compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes()
        val wrong = ArrayList<String>()
        var blocks = 0
        val styles = TutoCourse.STYLES.filter { tutoMap(it).isNotEmpty() }
        // Every course's map at 200 %, and three of them in four languages (one right to left), at 100 % and 200 %.
        val cases = styles.map { Triple(it, Lang.FR, 2f) } +
            styles.take(3).flatMap { st -> listOf(Lang.FR, Lang.HI, Lang.KO, Lang.AR).flatMap { l -> listOf(1f, 2f).map { Triple(st, l, it) } } }
        for ((st, l, s) in cases) {
            I18n.lang = l
            lang = l
            scale = s
            style = st
            compose.waitForIdle()
            val map = tutoMap(st)
            val where = "$st ${l.code} ${(s * 100).toInt()} %"
            map.forEachIndexed { i, b ->
                blocks++
                val block = nodes("tuto_map_block_$i").firstOrNull() ?: run { wrong += "$where: no block for ${b.label}"; null } ?: return@forEachIndexed
                for (part in listOf("name", "bars")) {
                    val t = nodes("tuto_map_${part}_$i").firstOrNull()
                    if (t == null) { wrong += "$where: ${b.label} (${b.bars}) shows no $part"; continue }
                    if (t.size.width > block.size.width + 1 || t.size.height > block.size.height + 1) wrong += "$where: ${b.label}'s $part out of its block"
                }
            }
            val density = nodes("tuto_map").first().layoutInfo.density.density
            val cut = LayoutScan.scan(compose, density, setOf(LayoutScan.Kind.CUT))
            if (cut.isNotEmpty()) wrong += "$where: ${cut.take(4)}"
            // Each part its own colour: the legend's swatches, read on the screen.
            val px = compose.onRoot().captureToImage().toPixelMap()
            val colours = map.map { it.label }.distinct().mapNotNull { label ->
                val sw = nodes("tuto_map_swatch_$label").firstOrNull() ?: run { wrong += "$where: no colour for $label in the legend"; null } ?: return@mapNotNull null
                val c = sw.boundsInRoot.center
                label to px[c.x.toInt(), c.y.toInt()]
            }
            for (a in colours.indices) for (b in a + 1 until colours.size) {
                val (la, ca) = colours[a]
                val (lb, cb) = colours[b]
                val apart = abs(ca.red - cb.red) + abs(ca.green - cb.green) + abs(ca.blue - cb.blue)
                if (apart < 0.25f) wrong += "$where: $la and $lb the same colour"
            }
        }
        assertTrue("no map tested", blocks > 0)
        assertTrue("$blocks blocks, ${wrong.size} wrong:\n" + wrong.distinct().take(30).joinToString("\n"), wrong.isEmpty())
    }
}
