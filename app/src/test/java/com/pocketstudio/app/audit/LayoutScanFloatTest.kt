package com.pocketstudio.app.audit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
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
 * Tour 5, lot « moyens » C (review of the home lot, M-b): [LayoutScan] took the scroll hint « ↓ n autres » out of the
 * controls it reads and never said that a floating thing (that chip, or any other) lay over a control. The rule: what
 * is drawn over a control wholly on screen, and is touchable itself, takes the touches meant for that control.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-xhdpi")
class LayoutScanFloatTest {
    @get:Rule
    val compose = createComposeRule()

    private val density get() = ApplicationProvider.getApplicationContext<android.content.Context>().resources.displayMetrics.density

    /** A list of two cards (the second at [secondTop] dp), and a chip floating at the bottom end of the frame. */
    @Composable
    private fun Screen(secondTop: Int, chipTag: String = "floating") {
        Box(Modifier.size(360.dp, 400.dp)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Box(Modifier.fillMaxWidth().height(120.dp).clickable {}.testTag("card_a")) { Text("Créer A") }
                Box(Modifier.padding(top = (secondTop - 120).dp).fillMaxWidth().height(120.dp).clickable {}.testTag("card_b")) { Text("Créer B") }
                Box(Modifier.fillMaxWidth().height(400.dp))
            }
            Box(Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 12.dp).size(120.dp, 48.dp).clickable {}.testTag(chipTag)) {
                Text("↓ 2 autres")
            }
        }
    }

    private fun floats() = LayoutScan.scan(compose, density, setOf(LayoutScan.Kind.OVERLAP)).filter { "floats over" in it.detail }

    @Test
    fun aChipOverACardWhollyOnScreenIsFound() {
        // « Créer B » from 260 to 380 dp, wholly in the 400 dp frame; the chip from 340 to 388 dp lies over its right.
        compose.setContent { Screen(secondTop = 260) }
        val found = floats()
        assertTrue("the chip over « Créer B » is not found: $found", found.any { "Créer B" in it.detail })
    }

    @Test
    fun theScrollHintItselfIsReadToo() {
        // The scroll hint was left out of the controls read: it is a floating thing like any other.
        compose.setContent { Screen(secondTop = 260, chipTag = "scroll_hint") }
        val found = floats()
        assertTrue("the scroll hint over « Créer B » is not found: $found", found.any { "Créer B" in it.detail })
    }

    @Test
    fun aChipOverNothingSaysNothing() {
        // « Créer B » from 150 to 270 dp: nothing under the chip.
        compose.setContent { Screen(secondTop = 150) }
        assertEquals(emptyList<LayoutScan.Finding>(), floats())
    }

    @Test
    fun aPianosBlackKeyOverItsWhiteNeighboursIsTheInstrumentItself() {
        // Laid out together in one list (the synth's keyboard): not a floating thing.
        compose.setContent {
            Box(Modifier.size(360.dp, 200.dp).horizontalScroll(rememberScrollState())) {
                Box(Modifier.size(48.dp, 160.dp).clickable {}.testTag("white_c")) { Text("C") }
                Box(Modifier.padding(start = 48.dp).size(48.dp, 160.dp).clickable {}.testTag("white_d")) { Text("D") }
                Box(Modifier.padding(start = 32.dp).size(32.dp, 100.dp).clickable {}.testTag("black_cs")) { Text("C#") }
            }
        }
        assertEquals(emptyList<LayoutScan.Finding>(), floats())
    }

    @Test
    fun aCardCutByTheListsEdgeIsNotJudgedHere() {
        // « Créer B » from 330 dp, cut by the frame: only partly on screen (the home screens' own test reads that case).
        compose.setContent { Screen(secondTop = 330) }
        assertEquals(emptyList<LayoutScan.Finding>(), floats())
    }
}
