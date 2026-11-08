package com.pocketstudio.app.audit

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Round 5 (scan r3): the owner's tablet showed the Sons chips' second line cut at its bottom (« 16 pads »,
 * « Micro »: the lower half of the letters gone) and [LayoutScan] never said so where the chips' row sits in a
 * page that scrolls down (Sons › Synthé's own track bar): a row that scrolls sideways inside a page that scrolls
 * down made both axes « scrolled », and a vertical cut was never judged, even one made by the chip itself (a
 * fixed 44 dp) and not by the page's edge. These chips are that pattern, small: two lines in 44 dp, cut from
 * font 1.25 up.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w600dp-h1006dp-tvdpi")
class LayoutScanClipTest {
    @get:Rule
    val compose = createComposeRule()

    private val density get() = ApplicationProvider.getApplicationContext<android.content.Context>().resources.displayMetrics.density

    @Composable
    private fun Chip(label: String, sub: String) {
        Row(Modifier.height(44.dp).widthIn(max = 160.dp).clip(RoundedCornerShape(10.dp)).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(label, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(sub, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }

    @Composable
    private fun Chips() {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip("Batterie", "16 pads")
            Chip("Voix", "Micro")
            Chip("Remix", "Séparer")
        }
    }

    private fun show(font: Float, content: @Composable () -> Unit) {
        compose.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, d.fontScale * font)) { content() }
        }
    }

    private fun cuts() = LayoutScan.scan(compose, density, setOf(LayoutScan.Kind.CUT))

    @Test
    fun aChipsSecondLineCutInARowThatScrollsSidewaysInAPageThatScrollsDownIsFound() {
        show(1.3f) { Column(Modifier.verticalScroll(rememberScrollState())) { Chips() } }
        val found = cuts()
        for (sub in listOf("16 pads", "Micro", "Séparer")) {
            assertTrue("« $sub » cut at its bottom, not found: $found", found.any { it.what == sub && "down" in it.detail })
        }
    }

    /**
     * The same in a page that really scrolls (its content far taller than its window), the chips in view: a
     * page that can scroll excuses only what lies past its own window, not a cut made inside it.
     */
    @Test
    fun aChipsCutInAPageThatReallyScrollsIsFound() {
        show(1.3f) {
            Column(Modifier.height(300.dp).verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(40.dp))
                Chips()
                Spacer(Modifier.height(1000.dp))
            }
        }
        val found = cuts()
        for (sub in listOf("16 pads", "Micro", "Séparer")) {
            assertTrue("« $sub » cut at its bottom, not found: $found", found.any { it.what == sub && "down" in it.detail })
        }
    }

    @Test
    fun theSameChipsAtFont1AreWhole() {
        show(1.0f) { Column(Modifier.verticalScroll(rememberScrollState())) { Chips() } }
        assertEquals(emptyList<LayoutScan.Finding>(), cuts())
    }

    /** What the page's own edge hides, scrolled away, is not cut: the scan says nothing of it (as before). */
    @Test
    fun chipsHalfScrolledOutOfThePageOrTheRowAreNotCut() {
        show(1.0f) {
            Column(Modifier.height(56.dp).verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(30.dp))
                Row(Modifier.width(150.dp)) { Chips() }
            }
        }
        assertEquals(emptyList<LayoutScan.Finding>(), cuts())
    }
}
