package com.pocketstudio.app.human

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.export.ShareCheck
import com.pocketstudio.app.export.ShareLevels
import com.pocketstudio.app.export.shareChecks
import com.pocketstudio.app.export.shareSeconds
import com.pocketstudio.app.live.TakeUi
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.ui.mixerStrips
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.LiveSlots
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.Remix
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.remixEndBars
import com.pocketstudio.model.songBars
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Duration

/**
 * Round 5, phone checks the owner could not do (recette tel-19, tel-15): app or bench? Without the bench
 * (no HumanTester, no BenchEngine), the app's own classes only:
 * - T5-Bb-58: « Annuler » of a Live take gives back the song as it was (a seat saw 572 bars);
 * - T5-Ec-28 (T5-REMIXFIN): « Prêt à partager ? » says so when the remix's song runs past the song's end.
 */
@RunWith(RobolectricTestRunner::class)
class Tour5AppProof2Test {
    /** PLAN-RESTE 4.5: the state inherited from the test before must be pristine (else: red, naming that test). */
    @get:Rule
    val pristine = BenchPristineRule()
    /** See [KnownDefect]. */
    @get:Rule
    val knownDefects: TestRule = KnownDefectRule()

    private val app: Application get() = ApplicationProvider.getApplicationContext()

    @After
    fun clear() {
        NativeEngine.sink = null
        CurrentProject.set(app, RhythmViewModel.PROJECT_ID)
    }

    private fun idle(ms: Long = 20) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    private fun until(what: String, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + 30_000
        while (!ok()) {
            check(System.currentTimeMillis() < end) { what }
            idle(); Thread.sleep(5)
        }
    }

    /** A project of its own, opened as the app opens one (no engine: the take's clock is [RhythmViewModel.takeClockForTest]). */
    private fun open(name: String): RhythmViewModel {
        val id = "tour5b-${System.nanoTime()}"
        ProjectStore(File(app.filesDir, "projects")).save(id, Project(name = name))
        CurrentProject.set(app, id)
        val vm = RhythmViewModel(app)
        until("the project never opened") { vm.project.value.name == name }
        idle(200)
        return vm
    }

    /** Two bricks (A: a kick, B: another hit) and a song of three parts, 2 + 4 + 2 bars: 8 bars. */
    private fun withSong(name: String): RhythmViewModel {
        val vm = open(name)
        vm.toggleHit(0, 0)
        vm.duplicateScene()
        vm.toggleHit(2, 4)
        vm.selectScene(0)
        vm.session.edit { it.copy(song = listOf(SongPart(0, bars = 2), SongPart(1, bars = 4), SongPart(0, bars = 2))) }
        idle(200)
        assertEquals("the song before the take", 8, vm.project.value.songBars)
        return vm
    }

    /** A short take as the Live page plays it (● armed while playing, B's drums launched on bar 3, ■ Fin), then « Annuler ». */
    private fun recordShortTakeThenUndo(vm: RhythmViewModel) {
        val before = vm.project.value
        vm.togglePlay()
        vm.takeClockForTest = 5
        vm.togglePerformance()  // actions.record: armed for the next bar line
        assertTrue("armed: ${vm.take.value}", vm.take.value is TakeUi.Armed)
        vm.takeClockForTest = 16
        vm.tickPerformance()
        assertTrue("recording: ${vm.take.value}", vm.take.value is TakeUi.Recording)
        vm.launchPad(SongPart.DRUMS, 1)
        vm.liveLaunchedForTest(LiveSlots.DRUMS, 1, 32)
        vm.takeClockForTest = 40
        vm.togglePerformance()  // ■ Fin: « Morceau écrit : N parties »
        val done = vm.take.value
        assertTrue("« Morceau écrit » shown: $done", done is TakeUi.Done)
        assertTrue("the take changed the song", vm.project.value.song != before.song)
        idle(200)
        assertTrue("« Annuler » still undoes the take alone", vm.canUndoPerformance())
        vm.undoPerformance()  // actions.undoTake (live_take_undo)
        idle(200)
        val after = vm.project.value
        assertEquals("after « Annuler » the song has ${after.songBars} bars (${after.song.size} parts), it had ${before.songBars} (${before.song.size})",
            before.songBars, after.songBars)
        assertEquals("after « Annuler » the song's parts are not the ones before the take", before.song, after.song)
        assertEquals("after « Annuler » the song's versions changed", before.songVersions, after.songVersions)
        assertEquals(TakeUi.Ready, vm.take.value)
    }

    /** T5-Bb-58 (recette tel-19): « Remplacer » (the default), a short take over a song of 3 parts, then « Annuler ». */
    @Test
    fun undoingALiveTakeGivesBackTheSongItReplaced() = recordShortTakeThenUndo(withSong("Annuler remplace"))

    /** T5-Bb-58 (recette tel-19): « Ajouter à la suite », a short take after a song of 3 parts, then « Annuler ». */
    @Test
    fun undoingALiveTakeGivesBackTheSongItWentAfter() {
        val vm = withSong("Annuler suite")
        vm.setTakeAppend(true)
        recordShortTakeThenUndo(vm)
    }

    /** T5-Bb-58 (recette tel-19): ● pressed while stopped (it starts the brick shown), ■ Fin two bars later, « Annuler ». */
    @Test
    fun undoingALiveTakeArmedWhileStoppedGivesBackTheSong() {
        val vm = withSong("Annuler arrêt")
        val before = vm.project.value
        vm.takeClockForTest = -1
        vm.togglePerformance()
        assertTrue("armed: ${vm.take.value}", vm.take.value is TakeUi.Armed)
        // ● stopped launches the brick shown (launchRow): the transport starts with it.
        if (!vm.transport.value.playing) vm.togglePlay()
        vm.takeClockForTest = 0
        vm.tickPerformance()
        assertTrue("recording: ${vm.take.value}", vm.take.value is TakeUi.Recording)
        vm.takeClockForTest = 20
        vm.togglePerformance()
        assertTrue("« Morceau écrit » shown: ${vm.take.value}", vm.take.value is TakeUi.Done)
        idle(200)
        vm.undoPerformance()
        idle(200)
        val after = vm.project.value
        assertEquals("after « Annuler » the song has ${after.songBars} bars (${after.song.size} parts), it had ${before.songBars} (${before.song.size})",
            before.songBars, after.songBars)
        assertEquals(before.song, after.song)
        assertEquals(before.songVersions, after.songVersions)
    }

    /**
     * T5-Ec-28 (recette tel-15, T5-REMIXFIN): a song of 8 bars (16 s at 120 BPM) and a remix's song of 60 s placed on
     * bar 2: the shared file stops with the song's parts (RhythmViewModel.songFrames sums the parts only, the remix's
     * end is not counted), so « Prêt à partager ? » should say the remix's end is cut (chantier 10 of the hands-on
     * audit, G16: it said « Tout est prêt »).
     */
    @Test
    fun shareSaysWhenTheRemixRunsPastTheSongEnd() {
        val p = Project(
            name = "Remix coupé", drums = DrumPattern().toggle(0, 0),
            song = listOf(SongPart(0, bars = 4), SongPart(0, bars = 4)),
            remix = Remix(setId = "s", name = "Boys", songBpm = 120.0, seconds = 60.0, atStep = 16, placed = true),
        )
        assertEquals(8, p.songBars)
        val remixEnd = p.remixEndBars()
        assertTrue("the remix is heard and outlasts the song (it ends on bar $remixEnd)", remixEnd > p.songBars)
        // What the sheet shows as the file's length: the song's parts only (16 s), the remix lasting 60 s from bar 2.
        assertEquals(16, shareSeconds(p, loopBars = 8))
        val checks = shareChecks(p, mixerStrips(p), ShareLevels(), loopBars = 8)
        val known = setOf(ShareCheck.Solo::class, ShareCheck.Muted::class, ShareCheck.Empty::class, ShareCheck.NoSong::class,
            ShareCheck.Short::class, ShareCheck.Saturation::class)
        assertTrue("« Prêt à partager ? » says nothing about the remix's end cut (song ${p.songBars} bars, remix up to bar $remixEnd): " +
            "checks = $checks", checks.any { it::class !in known })
    }
}
