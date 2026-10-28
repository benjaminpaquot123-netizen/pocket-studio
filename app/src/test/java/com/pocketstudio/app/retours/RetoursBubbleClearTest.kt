package com.pocketstudio.app.retours

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Remix testers (Mehdi): the Mode retours' bubble started over the full app's « Boucler »; it now starts clear of it. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w392dp-h852dp")
class RetoursBubbleClearTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun clear() {
        Retours.keepClear.value = emptyMap()
    }

    private fun bubble(): Rect {
        val b = compose.onNodeWithTag("retours_bubble").getBoundsInRoot()
        return with(compose.density) { Rect(b.left.toPx(), b.top.toPx(), b.right.toPx(), b.bottom.toPx()) }
    }

    @Test
    fun theBubbleStartsUnderAControlItWouldCover() {
        Retours.keepClear.value = emptyMap()
        compose.setContent { PocketStudioTheme { RetoursHost(show = true) { Box(Modifier.fillMaxSize()) } } }
        compose.waitForIdle()
        val first = bubble()
        // « Boucler » right where the bubble starts (the full app's top bar).
        val loop = Rect(first.left + 10f, first.top, first.left + 10f + first.width / 3, first.bottom)
        Retours.keepClear.value = mapOf("top_loop_part" to loop)
        compose.waitForIdle()
        val moved = bubble()
        assertFalse("the bubble still covers « Boucler »: $moved over $loop", moved.overlaps(loop))
        assertTrue(moved.top >= loop.bottom)
        // Gone (the Mode simple has no « Boucler »): back to its first place.
        Retours.keepClear.value = emptyMap()
        compose.waitForIdle()
        assertTrue(bubble().top == first.top)
    }
}
