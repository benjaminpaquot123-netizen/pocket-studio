package com.pocketstudio.app.human

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 7, control tuto2 (seat d, screens 16 and 39; seats A/a and B/d before): the screen's text came out empty (only the
 * feedback bubble) while its image showed the studio. The tutorial's ring (« Je le fais », `tuto_spot_ring`) and its finger
 * (« Montre-moi », `tuto_finger`) are canvases over the whole screen that draw a ring and let every touch through; the bench
 * took them for a page drawn over the screen (as Pad Live over the project) and hid everything under them.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h852dp-port-hdpi")
class BenchScreenTextOverlayTest {
    @get:Rule
    val compose = createComposeRule()

    private fun read(overlayTag: String, page: Boolean): ScreenText.Snapshot {
        compose.setContent {
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().testTag("studio")) {
                    Text("Kick sur chaque temps")
                    Button(onClick = {}) { Text("Je le fais") }
                }
                if (page) {
                    // A page drawn over the studio (Pad Live): its own words, the studio hidden under it.
                    Column(Modifier.fillMaxSize().testTag(overlayTag)) { Text("Pad Live") }
                } else {
                    // The tutorial's ring: a drawing over the whole screen, nothing else.
                    Canvas(Modifier.fillMaxSize().testTag(overlayTag)) { drawCircle(Color.Yellow, 20f) }
                }
            }
        }
        compose.waitForIdle()
        val screen = ApplicationProvider.getApplicationContext<android.app.Application>().resources.displayMetrics
        return ScreenText(compose, screen, reads = true, rtl = false).read()
    }

    @Test
    fun aRingDrawnOverTheScreenHidesNothing() {
        for (tag in listOf("tuto_spot_ring", "tuto_finger")) {
            val s = read(tag, page = false)
            assertTrue("$tag: the studio's words\n${s.lines.joinToString("\n")}", s.lines.any { "Kick sur chaque temps" in it } && s.lines.any { "Je le fais" in it })
            assertTrue("$tag: the route is the page under it, not the ring (${s.route})", s.route == "studio")
        }
    }

    /** The rule the ring escapes still holds for a page drawn over the screen: what is under it is not read. */
    @Test
    fun aPageDrawnOverTheScreenStillHidesWhatIsUnder() {
        val s = read("padlive", page = true)
        assertTrue(s.lines.joinToString("\n"), s.lines.any { "Pad Live" in it } && s.lines.none { "Kick sur chaque temps" in it })
        assertTrue(s.route, s.route == "padlive")
    }
}
