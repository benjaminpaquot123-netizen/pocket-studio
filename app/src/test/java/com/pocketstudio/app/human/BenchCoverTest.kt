package com.pocketstudio.app.human

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 8, banc9 (point i, seen at t4-26): the bench's finger « touched » a button covered by a drawn element that takes
 * the touch (a card drawn after it, over it); only a `LAYOUT … OVERLAP` line said so. [ScreenText.coverAt] names who
 * covers whom; the finger refuses the touch with that line.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h852dp-port-hdpi")
class BenchCoverTest {
    @get:Rule
    val compose = createComposeRule()

    private fun coverOnTarget(content: @Composable () -> Unit): String? {
        compose.setContent(content)
        compose.waitForIdle()
        val screen = ApplicationProvider.getApplicationContext<android.app.Application>().resources.displayMetrics
        val node = compose.onNodeWithTag("target", useUnmergedTree = true).fetchSemanticsNode()
        val r = BenchWindows.drawnBounds(node, screen)
        val text = ScreenText(compose, screen, reads = true, rtl = false)
        assertTrue("the finger is on the button: ${text.under(r.center.x, r.center.y)}", text.under(r.center.x, r.center.y).startsWith("target"))
        return text.coverAt(r.center.x, r.center.y)
    }

    @Test
    fun aCardDrawnOverTheButtonCoversIt() {
        val line = coverOnTarget {
            Box(Modifier.fillMaxSize()) {
                Button(onClick = {}, modifier = Modifier.testTag("target")) { Text("Valider") }
                Card(Modifier.size(200.dp)) { Text("Fait") }
            }
        }
        assertNotNull(line)
        assertTrue(line!!, line.contains("« Fait » couvre"))
        assertTrue(line, line.contains("Valider"))
    }

    /** Review #1: the finger itself (the lines HumanTester writes, and whether it plays the touch), not only coverAt. */
    private fun fingerOnTarget(content: @Composable () -> Unit): ScreenText.Finger {
        compose.setContent(content)
        compose.waitForIdle()
        val screen = ApplicationProvider.getApplicationContext<android.app.Application>().resources.displayMetrics
        val r = BenchWindows.drawnBounds(compose.onNodeWithTag("target", useUnmergedTree = true).fetchSemanticsNode(), screen)
        return ScreenText(compose, screen, reads = true, rtl = false).finger(7, r.center.x, r.center.y)
    }

    @Test
    fun theFingerFailsOnACoveredButtonAndSaysWhoCoversWhom() {
        val f = fingerOnTarget {
            Box(Modifier.fillMaxSize()) {
                Button(onClick = {}, modifier = Modifier.testTag("target")) { Text("Valider") }
                Card(Modifier.size(200.dp)) { Text("Fait") }
            }
        }
        assertTrue(f.lines.joinToString("\n"), f.refused)
        assertTrue(f.lines.joinToString("\n"), f.lines.none { it.startsWith("#7 DOIGT") })
        assertTrue(f.lines.joinToString("\n"), f.lines.any { it.startsWith("#7 REFUS") && it.contains("« Fait » couvre") && it.contains("Valider") })
    }

    @Test
    fun aBottomBarButtonUnderAFullScreenBodyIsNotCovered() {
        // A Scaffold's body is laid out on the whole screen, under its bars: a full-screen surface is no cover (full-screen
        // layers are ScreenText's overlay rule), so a bottom bar's button is touched.
        val f = fingerOnTarget {
            Scaffold(bottomBar = { Button(onClick = {}, modifier = Modifier.testTag("target")) { Text("Valider") } }) { _ ->
                Surface(Modifier.fillMaxSize()) { Text("Corps") }
            }
        }
        assertTrue(f.lines.joinToString("\n"), !f.refused && f.lines.any { it.startsWith("#7 DOIGT target") })
    }

    @Test
    fun aCardBesideTheButtonCoversNothing() {
        assertNull(coverOnTarget {
            Column(Modifier.fillMaxSize()) {
                Button(onClick = {}, modifier = Modifier.testTag("target")) { Text("Valider") }
                Card(Modifier.size(200.dp)) { Text("Fait") }
            }
        })
    }

    @Test
    fun aRingDrawnOverTheButtonLetsTheTouchThrough() {
        // The tutorial's ring is a canvas: it draws, it does not take the touch.
        assertNull(coverOnTarget {
            Box(Modifier.fillMaxSize()) {
                Button(onClick = {}, modifier = Modifier.testTag("target")) { Text("Valider") }
                Canvas(Modifier.fillMaxSize()) { drawCircle(Color.Yellow, 40f, Offset(60f, 60f)) }
            }
        })
    }
}
