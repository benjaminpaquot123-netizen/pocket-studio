package com.pocketstudio.app.remix

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.app.nav.AppHarness
import com.pocketstudio.app.nav.appViewModelsOfThisTest
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectJson
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.Remix
import com.pocketstudio.model.SongPart
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Step 5a (n° 15): a song put in a project from inside it is a track of the project, found under
 * its name on every screen: Sons (« Boys · Chanson », « Chanson : Boys » with its four stems),
 * Structure (its folder and four lines on the bars), Mix (its strip and its stems' faders) and
 * Live (its column). The parts not heard while listening come in muted; ▶ and reopening keep it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class RemixSongEverywhereTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @Before
    fun appViewModels() = h.appViewModelsOfThisTest()

    @After
    fun end() {
        h.finish()
        StemStore.dirFor(h.app, SET).parentFile?.deleteRecursively()
    }

    private fun exists(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun tap(tag: String) {
        val node = compose.onNodeWithTag(tag)
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
    }

    /** « Boys », separated already: 64 s at 120 BPM. */
    private fun song(): StemSet {
        val dir = StemStore.dirFor(h.app, SET)
        dir.mkdirs()
        listOf("drums", "bass", "other", "vocals").forEach { File(dir, "$it.wav").writeBytes(ByteArray(44)) }
        return StemStore.commit(dir, "Boys", 64.0, SongInfo(bpm = 120.0, firstBeat = 0.0))
    }

    private val store get() = ProjectStore(File(h.app.filesDir, "projects"))

    @Test
    fun fromAProjectTheVoiceHeardAloneComesInAndTheSongIsFoundEverywhere() {
        song()
        // The Remix's ViewModel was made before the song was on the phone: it reads the songs again.
        androidx.lifecycle.ViewModelProvider(com.pocketstudio.app.projects.AppViewModels.of(h.app))[RemixViewModel::class.java].refreshSets()
        h.start(build = { })
        // The Remix page of this project: « Voix » alone heard, then « Utiliser cette chanson ».
        SeparationJob.requestOpen()
        compose.waitForIdle()
        h.awaitTag("stem_${SET}_vocals")
        tap("stem_${SET}_vocals")
        tap("use_$SET")
        val p = h.vm.project.value
        assertEquals(SET, p.remix.setId)
        // All four stems in the project, the voice heard, the others muted.
        assertEquals(Remix.STEMS, p.remix.stems.size)
        assertEquals(VOICE_ONLY, p.remix.stems.map { !it.mute })
        // In the song's structure: a part as long as the song (64 s at 120 BPM: 32 bars of 4/4).
        assertEquals(1, p.song.size)
        assertTrue(p.remix.placed)

        // Sons: its chip under its name, its card « Chanson : Boys » with the four stems, voice first.
        tap("tab_Sons")
        compose.onNode(hasTestTag("sons_Remix") and hasText("Boys", substring = true)).assertExists()
        tap("sons_Remix")
        h.awaitTag("remix_project")
        compose.onNode(hasText("Chanson : Boys", ignoreCase = true), useUnmergedTree = true).assertExists()
        (0 until Remix.STEMS).forEach { assertTrue(exists("stem_mute_$it")) }

        // Structure: its folder and four lines; a touch on a stem's M mutes that stem only.
        tap("tab_Structure")
        if (exists("structure_timeline")) tap("structure_timeline")
        h.awaitTag("timeline_fold_${SongPart.REMIX}")
        (0 until Remix.STEMS).forEach { assertTrue("stem $it", exists("timeline_header_${Remix.stemKey(it)}")) }
        tap("timeline_mute_${Remix.stemKey(3)}")
        assertTrue(h.vm.project.value.remix.stems[3].mute)
        tap("timeline_mute_${Remix.stemKey(3)}")
        assertFalse(h.vm.project.value.remix.stems[3].mute)
        // Folded: the four lines away, the folder still there.
        tap("timeline_fold_${SongPart.REMIX}")
        assertFalse(exists("timeline_header_${Remix.stemKey(0)}"))
        assertTrue(exists("timeline_header_${SongPart.REMIX}"))

        // Mix: « Les 4 pistes de Boys », each with its fader, Solo and Muet.
        tap("tab_Mix")
        h.awaitTag("song_tracks")
        tap("song_stem_mute_0")
        assertFalse(h.vm.project.value.remix.stems[0].mute)

        // Live: its column under its name.
        assertEquals("Boys", com.pocketstudio.app.live.liveColumnsOf(h.vm.project.value).last().name)

        // ▶ then ■: the choice stays; saved and read back, it stays too.
        h.vm.togglePlay()
        compose.waitForIdle()
        h.vm.togglePlay()
        kotlinx.coroutines.runBlocking { h.vm.flushNow() }
        val heard = listOf(true, false, false, true)
        assertEquals(heard, h.vm.project.value.remix.stems.map { !it.mute })
        assertEquals(heard, store.load(CurrentProject.get(h.app))!!.remix.stems.map { !it.mute })
    }

    @Test
    fun anOldProjectWithTheHiddenRemixStripOpensWithItsSongInTheStructure() {
        // Saved before step 5a: the song on the « Remix » strip and the Live column only, no part.
        val old = Project(tempoBpm = 120.0, remix = Remix(SET, "Boys", 120.0, seconds = 64.0, stems = Remix.stemsHeard(VOICE_ONLY)))
        val json = ProjectJson.encode(old)
        assertFalse(json.contains("placed"))
        val opened = ProjectJson.decode(json)
        assertEquals(1, opened.song.size)
        assertEquals(VOICE_ONLY, opened.remix.stems.map { !it.mute })
        // The timeline rows: the folder then the four stems, voice first.
        val rows = com.pocketstudio.app.ui.timelineRows(opened)
        assertEquals(
            listOf(SongPart.REMIX) + Remix.SHOWN_ORDER.map { Remix.stemKey(it) },
            rows.map { it.key }.filter { it == SongPart.REMIX || Remix.stemOfKey(it) != null },
        )
        assertEquals("Boys", rows.first { it.key == SongPart.REMIX }.name)
        assertTrue(rows.first { it.key == Remix.stemKey(3) }.spans!!.single().isNotEmpty())
        assertEquals("Boys", com.pocketstudio.app.ui.mixerStrips(opened).first { it.ch == Remix.STRIP }.name)
    }

    private companion object {
        const val SET = "song-everywhere"
        val VOICE_ONLY = listOf(false, false, false, true)
    }
}
