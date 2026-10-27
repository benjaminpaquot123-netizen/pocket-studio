package com.pocketstudio.app.export

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.rhythm.ExportFormat
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.ui.TopBar
import com.pocketstudio.app.ui.TopBarActions
import com.pocketstudio.app.ui.TopBarUi
import com.pocketstudio.app.ui.mixerStrips
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.LoudnessTarget
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.Project
import com.pocketstudio.model.SongPart
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** « Prêt à partager ? »: what it checks, the one-touch fixes, the format kept, and two touches from any tab. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h1600dp")
class ShareSheetTest {
    @get:Rule
    val compose = createComposeRule()

    /** A beat on pad 1, at 120 BPM (a bar lasts 2 s); [parts]: the song. */
    private fun project(vararg parts: SongPart) = Project(name = "Mon tube", drums = DrumPattern().toggle(0, 0), song = parts.toList())

    private fun Project.withChannel(ch: Int, change: (com.pocketstudio.model.Channel) -> com.pocketstudio.model.Channel) =
        copy(mixer = mixer.copy(channels = mixer.channels.mapIndexed { i, c -> if (i == ch) change(c) else c }))

    private fun checks(p: Project, levels: ShareLevels = ShareLevels()) = shareChecks(p, mixerStrips(p), levels, loopBars = 8)

    @Test
    fun aGoodSongHasNothingToSay() {
        // Three minutes of song: no solo, nothing muted, no saturation.
        assertEquals(emptyList<ShareCheck>(), checks(project(SongPart(0, bars = 90))))
    }

    @Test
    fun aSoloLeftOnIsCaughtWithItsTrackName() {
        val p = project(SongPart(0, bars = 90)).withChannel(1) { it.copy(solo = true) }
        assertEquals(listOf(ShareCheck.Solo(listOf("Basse acide"))), checks(p))
        assertTrue(ShareCheck.Solo(listOf("x")).warning)
    }

    @Test
    fun aMutedTrackIsSaidOnlyWhenItHasSomethingToPlay() {
        // The drums play in the song but are muted: said. The acid line is empty: muting it changes nothing.
        val p = project(SongPart(0, bars = 90)).withChannel(0) { it.copy(mute = true) }.withChannel(1) { it.copy(mute = true) }
        assertEquals(listOf(ShareCheck.Muted(listOf("Batterie"), listOf(0))), checks(p))
        assertFalse(ShareCheck.Muted(listOf("x")).warning)
    }

    @Test
    fun anEmptyProjectAOneSceneLoopAndAShortSongGetAHint() {
        assertEquals(listOf(ShareCheck.Empty), checks(Project()))
        // No song: the scene goes out looped, as long as « Partager » makes it.
        assertEquals(listOf(ShareCheck.NoSong(8)), checks(project()))
        // One part of one bar (2 s): too short to be a song.
        assertEquals(listOf(ShareCheck.Short(2)), checks(project(SongPart(0))))
        // One part, but a long one: fine. Two short parts: fine too (the user built a song).
        assertEquals(emptyList<ShareCheck>(), checks(project(SongPart(0, bars = 16))))
        assertEquals(emptyList<ShareCheck>(), checks(project(SongPart(0), SongPart(0))))
    }

    @Test
    fun saturationComesFromTheLimiterWatchOrTheTruePeakHook() {
        val p = project(SongPart(0, bars = 90))
        assertEquals(listOf(ShareCheck.Saturation), checks(p, ShareLevels(limiterHard = true)))
        // The master section's true-peak reading (phase 33 hook): over −1 dBTP saturates, under does not.
        assertEquals(listOf(ShareCheck.Saturation), checks(p, ShareLevels(truePeakDb = -0.2)))
        assertEquals(emptyList<ShareCheck>(), checks(p, ShareLevels(truePeakDb = -1.5)))
        // The fix lowers the master by 6 dB, while there is room for it.
        assertTrue(ShareLevels(masterDb = -6f).canLower)
        assertFalse(ShareLevels(masterDb = Mixer.MASTER_MIN_DB + 3f).canLower)
    }

    /** The master meter's reading with nothing measured yet. */
    private fun noReading() = FloatArray(NativeEngine.LOUDNESS) { NativeEngine.LOUDNESS_NONE }

    private fun Project.withTarget(t: LoudnessTarget) = copy(mixer = mixer.copy(target = t))

    @Test
    fun withATargetTheLimiterWatchIsNotASaturation() {
        val p = project(SongPart(0, bars = 90))
        // No target: the Mixeur's watch means what it says, and the master can go 6 dB down.
        val plain = shareLevels(p, noReading(), limiterHard = true)
        assertEquals(listOf(ShareCheck.Saturation), checks(p, plain))
        assertTrue(plain.canLower)
        // With a target the chain pulls the limiter hard on purpose: no warning, no « Baisser le
        // volume général » (the target's drive would give the 6 dB straight back).
        val aimed = p.withTarget(LoudnessTarget.AUTO)
        val levels = shareLevels(aimed, noReading(), limiterHard = true)
        assertEquals(emptyList<ShareCheck>(), checks(aimed, levels))
        assertFalse(levels.canLower)
        assertFalse(shareLevels(p.withTarget(LoudnessTarget.CLUB), noReading(), limiterHard = false).canLower)
    }

    @Test
    fun theTruePeakOverMinusOneIsASaturationEvenWithATarget() {
        val aimed = project(SongPart(0, bars = 90)).withTarget(LoudnessTarget.AUTO)
        val hot = noReading().also { it[NativeEngine.LOUDNESS_TRUE_PEAK] = -0.3f }
        assertEquals(listOf(ShareCheck.Saturation), checks(aimed, shareLevels(aimed, hot, limiterHard = false)))
        val fine = noReading().also { it[NativeEngine.LOUDNESS_TRUE_PEAK] = -1.4f }
        assertEquals(emptyList<ShareCheck>(), checks(aimed, shareLevels(aimed, fine, limiterHard = false)))
        // Nothing measured yet (silence): no peak to speak of.
        assertEquals(null, shareLevels(aimed, noReading(), limiterHard = false).truePeakDb)
    }

    @Test
    fun theSheetShowsTheMetersLoudness() {
        val p = project(SongPart(0, bars = 90))
        val reading = noReading().also { it[NativeEngine.LOUDNESS_INTEGRATED] = -8.2f; it[NativeEngine.LOUDNESS_TRUE_PEAK] = -1.1f }
        val levels = shareLevels(p, reading, limiterHard = false)
        assertEquals(-8.2, levels.lufs!!, 0.01)
        assertEquals(-1.1, levels.truePeakDb!!, 0.01)
        assertEquals(null, shareLevels(p, noReading(), limiterHard = false).lufs)
        // Under the duration, once the meter gives it.
        compose.setContent {
            PocketStudioTheme {
                ShareSheetContent(ShareSheetUi("Mon tube", seconds = 180, parts = 3, checks = emptyList(), lufs = levels.lufs), ShareSheetActions())
            }
        }
        compose.onNodeWithTag("share_loudness").assertExists()
    }

    @Test
    fun theDurationIsTheWholeSongOrTheLoop() {
        assertEquals(180, shareSeconds(project(SongPart(0, bars = 90)), loopBars = 8))
        assertEquals(4, shareSeconds(project(SongPart(0, repeats = 2)), loopBars = 8))
        assertEquals(16, shareSeconds(project(), loopBars = 8))
        assertEquals("3:00", shareClock(180))
        assertEquals("0:16", shareClock(16))
    }

    @Test
    fun theNameStartsFromTheProjectsOwnOrASuggestion() {
        assertEquals("Mon tube", initialShareName("Mon tube "))
        assertEquals("Mon morceau", initialShareName("Mon projet 3"))
        assertEquals("Mon morceau", initialShareName(""))
    }

    @Test
    fun theFormatChosenIsKept() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.getSharedPreferences(SharePrefs.PREFS, 0).edit().clear().commit()
        assertEquals(ExportFormat.M4a, SharePrefs(app).format)
        SharePrefs(app).choose(ExportFormat.Wav)
        assertEquals(ExportFormat.Wav, SharePrefs(app).format)
        // FLAC is not offered here: it reads as M4A.
        SharePrefs(app).choose(ExportFormat.Flac)
        assertEquals(ExportFormat.M4a, SharePrefs(app).format)
    }

    @Test
    fun theSettingsGiveTheFormatToTheExport() {
        assertEquals(ExportFormat.Wav, ShareSong.settings(hasSong = true, sceneBars = 4, format = ExportFormat.Wav).format)
        assertEquals(ExportFormat.M4a, ShareSong.settings(hasSong = true, sceneBars = 4).format)
    }

    /** Audit final (01 M6, 05, 08): the Mode simple has no Mix: no « Voir dans Mix »; a muted track is heard again in one touch, said in its words. */
    @Test
    fun theModeSimpleKeepsItsFixesInItsTabs() {
        var unmuted: List<Int>? = null
        compose.setContent {
            PocketStudioTheme {
                ShareSheetContent(
                    ShareSheetUi("Mon tube", seconds = 180, parts = 3, checks = listOf(ShareCheck.Muted(listOf("Batterie"), listOf(5)), ShareCheck.Saturation), canLower = true),
                    ShareSheetActions(toMixer = null, unmute = { unmuted = it }),
                    page = true,
                )
            }
        }
        compose.onNodeWithTag("share_fix_master").assertExists()
        compose.onNodeWithTag("share_fix_mixer").assertDoesNotExist()
        compose.onNodeWithText("Coupé dans Mes pistes", substring = true).assertExists()
        compose.onNodeWithTag("share_fix_muted").performScrollTo().performClick()
        assertEquals(listOf(5), unmuted)
    }

    @Test
    fun eachFixIsOneTouch() {
        var unsolo = 0
        var lower = 0
        var mixer = 0
        var structure = 0
        var others = 0
        compose.setContent {
            PocketStudioTheme {
                ShareSheetContent(
                    ShareSheetUi(
                        "Mon tube", seconds = 2, parts = 1,
                        checks = listOf(ShareCheck.Solo(listOf("Basse acide")), ShareCheck.Muted(listOf("Batterie")), ShareCheck.Short(2), ShareCheck.Saturation),
                    ),
                    ShareSheetActions(
                        unsolo = { unsolo++ }, lowerMaster = { lower++ }, toMixer = { mixer++ }, toStructure = { structure++ }, otherExports = { others++ },
                    ),
                )
            }
        }
        compose.onNodeWithTag("share_check_muted").assertExists()
        compose.onNodeWithTag("share_fix_solo").performScrollTo().performClick()
        compose.onNodeWithTag("share_fix_structure").performScrollTo().performClick()
        compose.onNodeWithTag("share_fix_master").performScrollTo().performClick()
        compose.onNodeWithTag("share_fix_mixer").performScrollTo().performClick()
        compose.onNodeWithTag("share_other").performScrollTo().performClick()
        assertEquals(listOf(1, 1, 1, 1, 1), listOf(unsolo, structure, lower, mixer, others))
        compose.onNodeWithTag("share_check_ok").assertDoesNotExist()
    }

    @Test
    fun noRoomToLowerLeavesTheMixerAndAnEmptyNameCannotShare() {
        compose.setContent {
            PocketStudioTheme {
                ShareSheetContent(ShareSheetUi(" ", seconds = 180, parts = 3, checks = listOf(ShareCheck.Saturation), canLower = false), ShareSheetActions())
            }
        }
        compose.onNodeWithTag("share_fix_master").assertDoesNotExist()
        compose.onNodeWithTag("share_fix_mixer").assertExists()
        compose.onNodeWithTag("share_go").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun allClearSaysSoAndTheFormatChipsFollowTheChoice() {
        var format = ExportFormat.M4a
        compose.setContent {
            var shown by remember { mutableStateOf(ExportFormat.M4a) }
            PocketStudioTheme {
                ShareSheetContent(
                    ShareSheetUi("Mon tube", seconds = 180, parts = 6, checks = emptyList(), format = shown),
                    ShareSheetActions(setFormat = { shown = it; format = it }),
                )
            }
        }
        compose.onNodeWithTag("share_check_ok").assertExists()
        compose.onNodeWithTag("share_format_M4a").assertIsSelected()
        compose.onNodeWithTag("share_format_Wav").performScrollTo().performClick()
        assertEquals(ExportFormat.Wav, format)
        compose.onNodeWithTag("share_format_Wav").assertIsSelected()
        compose.onNodeWithTag("share_go").performScrollTo().assertIsEnabled()
    }

    /**
     * The budget of plan/clics.md: sharing takes 2 touches from any tab (⤴, then « Partager »),
     * through the real top bar and the real bottom sheet; WAV takes one more, then is kept.
     */
    @Test
    fun sharingTakesTwoTouchesFromTheTopBar() {
        val shared = mutableListOf<ExportFormat>()
        compose.setContent {
            var open by remember { mutableStateOf(false) }
            var format by remember { mutableStateOf(ExportFormat.M4a) }
            PocketStudioTheme {
                Column {
                    Box(Modifier.width(400.dp)) {
                        TopBar(TopBarUi("Mon tube", playing = false, canUndo = true, canRedo = false), TopBarActions(share = { open = true }))
                    }
                    if (open) {
                        ShareSheet(
                            ShareSheetUi("Mon tube", seconds = 180, parts = 6, checks = emptyList(), format = format),
                            ShareSheetActions(setFormat = { format = it }, share = { shared += format; open = false }, close = { open = false }),
                        )
                    }
                }
            }
        }
        var touches = 0
        fun touch(tag: String) {
            compose.onNodeWithTag(tag).performClick()
            touches++
            compose.waitForIdle()
        }
        touch("top_share")
        touch("share_go")
        assertEquals(listOf(ExportFormat.M4a), shared)
        assertTrue("$touches touches", touches <= 2)
        // WAV: ⤴ › WAV › Partager (3), then the choice stays: ⤴ › Partager (2).
        touches = 0
        touch("top_share")
        touch("share_format_Wav")
        touch("share_go")
        assertEquals(3, touches)
        touches = 0
        touch("top_share")
        touch("share_go")
        assertEquals(listOf(ExportFormat.M4a, ExportFormat.Wav, ExportFormat.Wav), shared)
        assertTrue("$touches touches", touches <= 2)
    }

    @Test
    fun tourReactiverClearsEverySoloInOneUndoStep() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        vm.toggleSolo(0)
        vm.toggleSolo(1)
        assertTrue(vm.project.value.mixer.channels.count { it.solo } == 2)
        vm.clearSolos()
        assertTrue(vm.project.value.mixer.channels.none { it.solo })
        vm.undo()
        assertEquals(2, vm.project.value.mixer.channels.count { it.solo })
    }
}
