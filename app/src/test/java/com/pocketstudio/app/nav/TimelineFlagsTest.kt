package com.pocketstudio.app.nav

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.copySceneTo
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase 36: the ruler's three gestures on the parts' flags, one detector for all (they used to
 * race): a touch plays from the part, a long press then a slide moves it, a long press let go
 * where it was opens its sheet; two touches on a clip open its editor.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class TimelineFlagsTest {
    @get:Rule
    val compose = createComposeRule()

    private val moved = mutableListOf<String>()
    private val played = mutableListOf<Int>()
    private val opened = mutableListOf<String>()

    private fun show() {
        var p = com.pocketstudio.model.Project(drums = DrumPattern().toggle(0, 0)).sanitized().copySceneTo(1).copySceneTo(2)
        p = p.copy(song = listOf(SongPart(0, 1), SongPart(1, 1), SongPart(2, 1))).sanitized()
        compose.setContent {
            PocketStudioTheme {
                com.pocketstudio.app.session.TimelineScreen(
                    com.pocketstudio.app.session.TimelineUi(p, rows = listOf(com.pocketstudio.app.session.TimelineRow(SongPart.DRUMS, "Batterie", androidx.compose.ui.graphics.Color.Blue))),
                    com.pocketstudio.app.session.TimelineActions(
                        movePart = { a, b -> moved += "$a $b" }, playPart = { played += it }, openEditor = { i, key -> opened += "$i $key" },
                    ),
                    barDp = 40.dp,
                )
            }
        }
    }

    /** On the flags' band, over part [part] (each part is one bar, 40 dp). */
    private fun flag(part: Int) = with(compose.density) { Offset((20 + 40 * part).dp.toPx(), 15.dp.toPx()) }

    @Test
    fun aLongPressThenASlideMovesThePart() {
        show()
        compose.onNodeWithTag("timeline_ruler").performTouchInput {
            down(flag(0))
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 200)
            for (i in 1..10) {
                moveTo(Offset(flag(0).x + (flag(2).x - flag(0).x) * i / 10, flag(0).y))
                advanceEventTime(16)
            }
            up()
        }
        compose.waitForIdle()
        assertEquals(listOf("0 2"), moved)
        assertEquals(emptyList<Int>(), played)
    }

    @Test
    fun aLongPressLetGoOpensThePartsSheetAndATouchPlaysFromIt() {
        show()
        compose.onNodeWithTag("timeline_ruler").performTouchInput { longClick(flag(1)) }
        compose.waitForIdle()
        compose.onNodeWithTag("part_sheet").assertExists()
        assertEquals(emptyList<String>(), moved)
        assertEquals(emptyList<Int>(), played)
        compose.onNodeWithTag("part_sheet_close").performTouchInput { click() }
        compose.waitForIdle()
        compose.onNodeWithTag("timeline_ruler").performTouchInput { click(flag(2)) }
        compose.waitForIdle()
        assertEquals(listOf(2), played)
    }

    @Test
    fun twoTouchesOnAClipOpenItsEditor() {
        show()
        val inDrums = with(compose.density) { Offset((40 + 20).dp.toPx(), 32.dp.toPx()) }
        compose.onNodeWithTag("timeline_grid").performTouchInput { doubleClick(inDrums) }
        compose.waitForIdle()
        assertEquals(listOf("1 drums"), opened)
    }
}
