package com.pocketstudio.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.live.LiveActions
import com.pocketstudio.app.live.LiveScreen
import com.pocketstudio.app.live.LiveScreenUi
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.LiveState
import com.pocketstudio.model.Project
import com.pocketstudio.model.Scene
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.PocketStudioTheme
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The Live page's bar on a narrow screen (316 dp: a 411 dp phone at « Taille de l'interface 130 % »),
 * in Dutch (longest words): ● REC, the counter, ▶, ← and the tempo stay whole and touchable.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE) // real text widths
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class LiveTopBarTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun french() {
        SettingsStore.update(ApplicationProvider.getApplicationContext()) { it.copy(language = Lang.FR) }
    }

    private val project = Project(scenes = listOf(Scene("A", DrumPattern().toggle(0, 0)), Scene("B")), scene = 0).sanitized()

    private var width by mutableStateOf(316)
    private var lang by mutableStateOf(Lang.NL)
    private var recorded = 0

    private fun show() = compose.setContent {
        PocketStudioTheme {
            key(lang, width) {
                Box(Modifier.width(width.dp)) {
                    LiveScreen(LiveScreenUi(project.copy(liveQuantum = 4), LiveState.on(0), clock = 5, playing = true), LiveActions(record = { recorded++ }))
                }
            }
        }
    }

    private fun assertWhole(tag: String, width: Int) {
        val b = compose.onNodeWithTag(tag).getBoundsInRoot()
        assertTrue("$tag is ${b.right - b.left} wide", b.right - b.left >= 48.dp - 0.5.dp)
        assertTrue("$tag is ${b.bottom - b.top} high", b.bottom - b.top >= 48.dp - 0.5.dp)
        assertTrue("$tag ends at ${b.right}", b.right <= width.dp + 0.5.dp)
    }

    @Test
    fun recStaysOnANarrowScreenInEveryLanguage() {
        show()
        for (l in listOf(Lang.NL, Lang.FR)) for (w in listOf(316, 300)) {
            SettingsStore.update(ApplicationProvider.getApplicationContext()) { it.copy(language = l) }
            lang = l
            width = w
            compose.waitForIdle()
            listOf("live_back", "live_play", "live_counter", "live_record").forEach { assertWhole(it, w) }
            recorded = 0
            compose.onNodeWithTag("live_record").performClick()
            assertEquals("$l $w", 1, recorded)
            // Lot 4d: the tempo keeps a touchable size between ▶ and the counter.
            val t = compose.onNodeWithTag("live_tempo").getBoundsInRoot()
            assertTrue("tempo ${t.right - t.left}", t.right - t.left >= 48.dp - 0.5.dp)
            assertTrue(t.left >= compose.onNodeWithTag("live_play").getBoundsInRoot().right)
            assertTrue(t.right <= compose.onNodeWithTag("live_counter").getBoundsInRoot().left)
            // « Lancer à » went down over the grid, still touchable.
            val q = compose.onNodeWithTag("live_quantum").getBoundsInRoot()
            assertTrue("quantum ${q.right - q.left}", q.right - q.left >= 48.dp)
            assertTrue(q.top >= compose.onNodeWithTag("live_top_bar").getBoundsInRoot().bottom)
        }
    }
}
