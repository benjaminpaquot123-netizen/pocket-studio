package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.pocketstudio.app.remix.SongInfo
import com.pocketstudio.app.remix.StemStore
import com.pocketstudio.model.Remix
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Step 5a: « Boys » in the project, found under its name in Sons, Structure and Mix (the voice heard, the rest muted). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class RemixSongScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @org.junit.Before
    fun appViewModels() = h.appViewModelsOfThisTest()

    @After
    fun end() {
        h.finish()
        StemStore.dirFor(h.app, SET).parentFile?.deleteRecursively()
    }

    private fun open(tag: String) {
        val node = compose.onNodeWithTag(tag)
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
        compose.mainClock.advanceTimeBy(800)
        compose.waitForIdle()
    }

    /** A project with its beat and tracks, then « Boys » put in it after « Voix » alone was heard. */
    private fun start(edit: com.pocketstudio.app.rhythm.RhythmViewModel.() -> Unit = {}) {
        val dir = StemStore.dirFor(h.app, SET)
        dir.mkdirs()
        listOf("drums", "bass", "other", "vocals").forEach { File(dir, "$it.wav").writeBytes(ByteArray(44)) }
        val set = StemStore.commit(dir, "Boys", 64.0, SongInfo(bpm = 120.0, firstBeat = 0.0))
        h.start(build = {
            songWithTracks()
            useSong(set, listOf(false, false, false, true))
            edit()
        })
    }

    @Test
    fun sons() {
        start()
        open("tab_Sons")
        open("sons_Remix")
        if (compose.onAllNodesWithTag("remix_project").fetchSemanticsNodes().isNotEmpty()) compose.onNodeWithTag("remix_project").performScrollTo()
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("screenshots/remix_song_sons.png")
    }

    /** Step 5d: « Piocher dans la chanson » under the song's four stems. */
    @Test
    fun matter() {
        start()
        open("tab_Sons")
        open("sons_Remix")
        compose.onNodeWithTag("remix_bass_notes").performScrollTo()
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("screenshots/remix_matter.png")
    }

    @Test
    fun structure() {
        start()
        open("tab_Structure")
        if (compose.onAllNodesWithTag("structure_timeline").fetchSemanticsNodes().isNotEmpty()) open("structure_timeline")
        h.awaitTag("timeline_header_${Remix.stemKey(3)}")
        compose.onRoot().captureRoboImage("screenshots/remix_song_structure.png")
    }

    /** Step 5b: the voice cut on bar 5, its second piece a bar later and faded in; a long press on its line edits it. */
    @Test
    fun structureEdits() {
        start {
            cutStemAt(3, 64)
            moveStemPieceAt(3, 64, 1)
            toggleStemFadeAt(3, 80, true)
        }
        open("tab_Structure")
        if (compose.onAllNodesWithTag("structure_timeline").fetchSemanticsNodes().isNotEmpty()) open("structure_timeline")
        h.awaitTag("timeline_header_${Remix.stemKey(3)}")
        compose.onRoot().captureRoboImage("screenshots/remix_stem_edits.png")
        val row = compose.onNodeWithTag("timeline_header_${Remix.stemKey(3)}").fetchSemanticsNode().boundsInRoot
        val grid = compose.onNodeWithTag("timeline_grid").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("timeline_grid").performTouchInput {
            longClick(androidx.compose.ui.geometry.Offset(grid.width * 0.3f, row.center.y - grid.top))
        }
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("screenshots/remix_stem_edit_menu.png")
    }

    @Test
    fun mix() {
        start()
        open("tab_Mix")
        compose.onNodeWithTag("song_tracks").performScrollTo()
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("screenshots/remix_song_mix.png")
    }

    private companion object {
        const val SET = "song-screenshot"
    }
}
