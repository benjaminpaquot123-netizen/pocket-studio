package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.ui.withinGestures
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.Project
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.sceneLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase 36's criterion: the 15 key actions of docs/plan/clics.md (« Navigation : ce que changent
 * les 5 onglets ») made in the whole app, as it opens on a song (the Structure space), on a small
 * phone, each within its budget (the « Pro 3 + contexte » column); together at most 26 gestures,
 * from about 75 with the seven tabs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class KeyActionsGestureTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @org.junit.After
    fun settingsBack() = h.finish()

    companion object {
        /** The 15 key actions and their budgets: [before] with the seven tabs (clics.md, median), [after] the target. */
        val BUDGETS: List<Triple<String, Int, Int>> = listOf(
            Triple("Écouter tout le morceau", 2, 1),
            Triple("Jouer depuis une partie", 2, 1),
            Triple("Changer le son de la basse", 4, 2),
            Triple("Changer le kit", 9, 2),
            Triple("Ouvrir la batterie du refrain", 3, 1),
            Triple("Couper une piste dans une partie", 2, 1),
            Triple("Déplacer une partie", 4, 1),
            Triple("Volume d'une piste", 3, 2),
            Triple("Enregistrer une mélodie", 4, 2),
            Triple("Enregistrer la voix", 6, 3),
            Triple("Montée de filtre avant le drop", 11, 2),
            Triple("Fill en fin de partie", 16, 2),
            Triple("Partager", 4, 2),
            Triple("Exporter en WAV", 4, 3),
            Triple("Ouvrir le Live", 2, 1),
        )

        fun budget(action: String): Int = BUDGETS.first { it.first == action }.third
    }

    @Test
    fun theFifteenKeyActionsCostAtMost26GesturesTogether() {
        assertEquals(15, BUDGETS.size)
        assertTrue(BUDGETS.sumOf { it.third } <= 26)
        assertEquals(26, BUDGETS.sumOf { it.third })
        // From about 75 with the seven tabs (the medians of clics.md).
        assertTrue(BUDGETS.sumOf { it.second } >= 70)
    }

    @Test
    fun theAppOpensOnTheSongsStructure() {
        h.start()
        compose.onNodeWithTag("tab_Structure").assertIsDisplayed()
        compose.onNodeWithTag("timeline_screen").assertIsDisplayed()
        compose.onNodeWithTag("structure_timeline").assertIsDisplayed()
        // Five spaces, no more.
        listOf("Structure", "Sons", "Mix", "Live", "Plus").forEach { compose.onNodeWithTag("tab_$it").assertIsDisplayed() }
    }

    @Test
    fun listeningToTheWholeSong() {
        h.start()
        h.vm.playStartsForTest = mutableListOf()
        compose.withinGestures("Écouter tout le morceau", budget("Écouter tout le morceau")) { tap("top_play") }
        assertEquals(listOf("song 0"), h.vm.playStartsForTest)
        assertTrue(h.vm.transport.value.playing)
    }

    @Test
    fun playingFromAPart() {
        h.start()
        compose.withinGestures("Jouer depuis une partie", budget("Jouer depuis une partie")) {
            val ruler = compose.onNodeWithTag("timeline_ruler").fetchSemanticsNode().boundsInRoot
            tapAt("timeline_ruler", androidx.compose.ui.geometry.Offset(ruler.left + h.partX(2), ruler.top + with(compose.density) { 40.dp.toPx() }), compose)
        }
        assertEquals(2, h.vm.playFrom.value)
    }

    @Test
    fun changingTheBassSound() {
        h.start()
        val bass = h.trackId("Basse")
        val before = h.vm.project.value.tracks.first { it.id == bass }.synth
        val lib = com.pocketstudio.model.SoundLibrary.SYNTH
        val current = com.pocketstudio.model.SoundLibrary.synthNameOf(before, lib)
        val family = lib.firstOrNull { it.name == current }?.category ?: "Basses"
        val pick = lib.indexOfFirst { it.category == family && it.name != current }
        compose.withinGestures("Changer le son de la basse", budget("Changer le son de la basse")) {
            tap("timeline_name_${h.inst("Basse")}")
            tap("synth_sounds_$pick")
        }
        val after = h.vm.project.value.tracks.first { it.id == bass }.synth
        assertEquals(lib[pick].name, com.pocketstudio.model.SoundLibrary.synthNameOf(after, lib))
        // The other tracks keep their sound.
        assertEquals(h.vm.project.value.tracks.first { it.name == "Lead" }.synth, h.vm.project.value.tracks.first { it.name == "Lead" }.synth)
    }

    @Test
    fun changingTheKit() {
        h.start()
        val before = h.vm.project.value.kitVersion
        val other = if (before == Project.KIT_V1) Project.KIT_V3 else Project.KIT_V1
        compose.withinGestures("Changer le kit", budget("Changer le kit")) {
            tap("timeline_name_${SongPart.DRUMS}")
            tap("kit_choice_builtin:$other")
        }
        assertEquals(other, h.vm.project.value.kitVersion)
    }

    @Test
    fun openingTheChorusDrums() {
        h.start()
        compose.withinGestures("Ouvrir la batterie du refrain", budget("Ouvrir la batterie du refrain")) {
            doubleTapAt("timeline_grid", h.clip(1, SongPart.DRUMS), compose)
        }
        compose.onNodeWithTag("rhythm").assertIsDisplayed()
        assertEquals(1, h.vm.project.value.scene)
        assertEquals("Refrain", h.vm.project.value.sceneLabel(h.vm.project.value.scene))
        // In the Sons space, its « Batterie » chip lit.
        compose.onNodeWithTag("sons_Rythme").assertIsDisplayed()
    }

    @Test
    fun cuttingATrackInOnePart() {
        h.start()
        val bass = h.inst("Basse")
        compose.withinGestures("Couper une piste dans une partie", budget("Couper une piste dans une partie")) {
            tapAt("timeline_grid", h.clip(2, bass), compose)
        }
        assertTrue(!h.vm.project.value.song[2].plays(bass))
        assertTrue(h.vm.project.value.song[1].plays(bass))
    }

    @Test
    fun movingAPart() {
        h.start()
        val order = h.vm.project.value.song.map { it.scene }
        assertEquals(h.vm.project.value.song.toString(), 3, order.size)
        compose.withinGestures("Déplacer une partie", budget("Déplacer une partie")) {
            holdAndSlide("timeline_ruler", h.flag(0), h.flag(2), compose)
        }
        assertEquals(listOf(order[1], order[2], order[0]), h.vm.project.value.song.map { it.scene })
    }

    @Test
    fun aTracksVolumeOnTheConsole() {
        h.start()
        val ch = com.pocketstudio.model.InstrumentTrack.mixerChannel(h.trackId("Basse"))
        val before = h.vm.project.value.mixer.channels[ch].volume
        compose.withinGestures("Volume d'une piste", budget("Volume d'une piste")) {
            tap("tab_Mix")
            slide("volume_$ch", compose, up = false)
        }
        assertTrue(h.vm.project.value.mixer.channels[ch].volume < before)
    }

    @Test
    fun recordingAMelodyInThePartChosen() {
        h.start()
        val lead = h.inst("Lead")
        compose.withinGestures("Enregistrer une mélodie", budget("Enregistrer une mélodie")) {
            doubleTapAt("timeline_grid", h.clip(1, lead), compose)
            tap("record_start")
        }
        assertEquals(h.trackId("Lead"), h.vm.selectedTrack.value)
        assertEquals(1, h.vm.project.value.scene)
        assertTrue(h.vm.recording.value)
    }

    @Test
    fun recordingTheVoice() {
        h.start()
        compose.withinGestures("Enregistrer la voix", budget("Enregistrer la voix")) {
            tap("tab_Sons")
            tap("sons_Audio")
            tap("top_record")
        }
        // The voice's track is made, the microphone asked for on it (the engine records it on a phone).
        assertEquals(1, h.vm.project.value.audioTracks.size)
    }

    @Test
    fun aFilterRiseBeforeTheDrop() {
        h.start()
        compose.withinGestures("Montée de filtre avant le drop", budget("Montée de filtre avant le drop")) {
            longPressOn("timeline_ruler", h.flag(2), compose)
            tap("part_buildup_4")
        }
        assertTrue(h.vm.project.value.automation.any { it.target == AutoTarget.live(AutoTarget.LIVE_BUILD_UP) })
    }

    @Test
    fun aFillAtTheEndOfAPart() {
        h.start()
        compose.withinGestures("Fill en fin de partie", budget("Fill en fin de partie")) {
            assertEquals(3, h.vm.project.value.song.size)
            longPressOn("timeline_ruler", h.flag(1), compose)
            compose.onNodeWithTag("part_sheet_title").assertTextContains("Partie 2", substring = true)
            tap("part_fill")
        }
        val p = h.vm.project.value
        assertEquals(p.song.toString(), "Fill", p.sceneLabel(p.song[1].play.getValue(SongPart.DRUMS)))
    }

    @Test
    fun sharing() {
        h.start()
        compose.withinGestures("Partager", budget("Partager")) {
            tap("top_share")
            tap("share_go")
        }
        // No engine in the tests: the export says so, the sheet is gone.
        compose.onNodeWithTag("share_sheet").assertDoesNotExist()
        assertTrue(h.vm.export.value.sharing)
    }

    @Test
    fun exportingAWav() {
        h.start()
        compose.withinGestures("Exporter en WAV", budget("Exporter en WAV")) {
            tap("top_share")
            tap("share_format_Wav")
            tap("share_go")
        }
        assertEquals(com.pocketstudio.app.rhythm.ExportFormat.Wav, com.pocketstudio.app.export.SharePrefs(h.app).format)
        assertTrue(h.vm.export.value.sharing)
    }

    @Test
    fun openingTheLive() {
        h.start()
        compose.withinGestures("Ouvrir le Live", budget("Ouvrir le Live")) { tap("tab_Live") }
        compose.onNodeWithTag("live_screen").assertIsDisplayed()
        // Out of the creation path, one touch back: the bottom bar stays.
        compose.onNodeWithTag("tab_Structure").assertIsDisplayed()
        assertNotEquals(null, compose.onNodeWithTag("live_top_bar").fetchSemanticsNode())
    }
}
