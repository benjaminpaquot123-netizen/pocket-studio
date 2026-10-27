package com.pocketstudio.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.live.TakeUi
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.AutomationLane
import com.pocketstudio.model.LiveSlots
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.LiveSlot
import com.pocketstudio.model.liveColumns
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.LoudnessTarget
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.StarterWizard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * The Live page's grid through the ViewModel (no audio engine here: the launches the engine would
 * report are fed in by hand, as its event queue does).
 */
@RunWith(RobolectricTestRunner::class)
class LiveVmTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    /** Two scenes: A (shown) with a kick, B with another drum hit. */
    private fun twoScenes(): RhythmViewModel {
        File(app.filesDir, "projects").deleteRecursively()
        app.filesDir.listFiles()?.filter { it.name.startsWith("live-take") }?.forEach { it.delete() }
        val vm = RhythmViewModel(app)
        vm.toggleHit(0, 0)
        vm.duplicateScene()  // B, shown
        vm.toggleHit(2, 4)
        vm.selectScene(0)
        assertEquals(0, vm.project.value.scene)
        return vm
    }

    @Test
    fun aPadWaitsForItsBarThenOnlyThatTrackPlaysAnotherScene() {
        val vm = twoScenes()
        vm.togglePlay()
        assertTrue(vm.transport.value.playing)
        assertEquals(0, vm.liveGrid.value!!.row)
        vm.launchPad(SongPart.DRUMS, 1)
        val armed = vm.liveGrid.value!!
        assertEquals(1, armed.slot(LiveSlots.DRUMS).pending)
        assertEquals(0, armed.slot(LiveSlots.DRUMS).playing)
        assertFalse(vm.waitsForTest(LiveSlots.DRUMS))  // until the bar, the drums still play the scene edited
        vm.liveLaunchedForTest(LiveSlots.DRUMS, 1, 16)
        val g = vm.liveGrid.value!!
        assertEquals(1, g.slot(LiveSlots.DRUMS).playing)
        assertEquals(16, g.slot(LiveSlots.DRUMS).start)
        assertTrue(g.diverged)
        // Editing scene A: the drums (playing B) wait, the acid (playing A) hears it at once.
        assertTrue(vm.waitsForTest(LiveSlots.DRUMS))
        assertFalse(vm.waitsForTest(LiveSlots.ACID))
        assertEquals(0, vm.project.value.scene)  // the screens stay on the scene shown
        // A whole row: once every track plays B, B is the scene shown and nothing waits.
        vm.launchRow(1)
        assertEquals(1, vm.liveGrid.value!!.pendingRow)
        for (k in 0 until LiveSlots.COUNT) vm.liveLaunchedForTest(k, 1, 32)
        assertEquals(1, vm.project.value.scene)
        assertFalse(vm.liveGrid.value!!.diverged)
        assertFalse(vm.waitsForTest(LiveSlots.DRUMS))
        // A track stopped: its edits wait (it must stay silent).
        vm.stopLiveTrack(SongPart.ACID)
        assertEquals(-1, vm.liveGrid.value!!.slot(LiveSlots.ACID).pending)
        vm.liveLaunchedForTest(LiveSlots.ACID, -1, 48)
        assertTrue(vm.waitsForTest(LiveSlots.ACID))
        // Stop: the grid is gone, edits reach the engine again.
        vm.togglePlay()
        assertNull(vm.liveGrid.value)
        assertFalse(vm.waitsForTest(LiveSlots.ACID))
    }

    @Test
    fun aPadTouchedWhileStoppedStartsItsTrackAlone() {
        val vm = twoScenes()
        vm.launchPad(SongPart.ACID, 1)
        assertTrue(vm.transport.value.playing)
        assertEquals(1, vm.project.value.scene)  // the pad's scene is shown
        val g = vm.liveGrid.value!!
        assertNull(g.slot(LiveSlots.ACID).pending)
        assertEquals(-1, g.slot(LiveSlots.DRUMS).pending)  // the others stop on the first step
        for (k in 0 until LiveSlots.COUNT) if (k != LiveSlots.ACID) vm.liveLaunchedForTest(k, -1, 0)
        assertEquals(1, vm.liveGrid.value!!.slot(LiveSlots.ACID).playing)
        assertTrue(vm.liveGrid.value!!.diverged)
        assertFalse(vm.liveGrid.value!!.hasPending)
        // « ■ Tout » then a row while stopped: the row starts the scene whole.
        vm.stopLiveAll()
        assertEquals(-1, vm.liveGrid.value!!.slot(LiveSlots.ACID).pending)
        vm.togglePlay()
        vm.launchRow(0)
        assertEquals(0, vm.project.value.scene)
        assertFalse(vm.liveGrid.value!!.hasPending)
        assertFalse(vm.liveGrid.value!!.diverged)
    }

    @Test
    fun muteQuantumAndGesturesAreNotWrittenAndEndWithStop() {
        val vm = twoScenes()
        val depth = vm.session.undoDepth
        vm.setLiveQuantum(2)
        assertEquals(2, vm.project.value.liveQuantum)
        vm.setLiveQuantum(3)  // not a choice
        assertEquals(2, vm.project.value.liveQuantum)
        vm.togglePlay()
        vm.toggleLiveMute(SongPart.DRUMS)
        assertEquals(setOf(SongPart.DRUMS), vm.liveMuted.value)
        assertFalse(vm.project.value.mixer.channels[0].mute)
        val cutoff = vm.project.value.acid.sound.cutoff
        vm.setLiveAcid(0, 0.9f)
        vm.setLiveAcid(5, 0.9f)  // tuning: not on the Live page
        assertEquals(mapOf(0 to 0.9f), vm.liveAcid.value)
        assertEquals(cutoff, vm.project.value.acid.sound.cutoff)
        vm.resetLiveAcid(0)  // double tap: back to the saved setting
        assertTrue(vm.liveAcid.value.isEmpty())
        vm.setLiveAcid(1, 0.2f)
        vm.setLiveReturn(0, 0.8f)
        assertEquals(listOf(0.8f, 0.5f), vm.liveReturns.value)
        assertEquals(depth, vm.session.undoDepth)  // nothing of it is an undo step
        vm.togglePlay()
        assertTrue(vm.liveMuted.value.isEmpty())
        assertTrue(vm.liveAcid.value.isEmpty())
        assertEquals(listOf(0.5f, 0.5f), vm.liveReturns.value)
    }

    @Test
    fun aTakeIsWrittenIntoTheSongInOneUndoStep() {
        val vm = twoScenes()
        val depth = vm.session.undoDepth
        vm.togglePlay()
        vm.takeClockForTest = 5
        vm.togglePerformance()  // armed for the next bar
        assertEquals(TakeUi.Armed(16, 11), vm.take.value)
        assertTrue(vm.performanceTaking)
        vm.takeClockForTest = 16
        vm.tickPerformance()
        assertEquals(TakeUi.Recording(16, 0, 1), vm.take.value)
        // The drums go to B on bar 3; the 303's cutoff moves on beat 5 of the take.
        vm.launchPad(SongPart.DRUMS, 1)
        vm.liveLaunchedForTest(LiveSlots.DRUMS, 1, 32)
        vm.takeClockForTest = 36
        vm.setLiveAcid(0, 0.9f)
        vm.livePerformance(AutoTarget.LIVE_CHOP, 1f)
        vm.takeClockForTest = 44
        vm.livePerformance(AutoTarget.LIVE_CHOP, 0f)
        vm.tickPerformance()
        assertEquals(TakeUi.Recording(16, 3, 2), vm.take.value)  // 28 steps at 120 BPM, bar 2 of the song
        // Annuler waits for the take's end.
        val during = vm.project.value
        vm.undo()
        assertEquals(during, vm.project.value)
        vm.togglePerformance()  // ■ Fin: the take ends on the next bar line (step 48)
        assertEquals(TakeUi.Done(2, 2, 0), vm.take.value)
        val song = vm.project.value.song
        assertEquals(listOf(SongPart(0, 1), SongPart(0, 1, play = mapOf(SongPart.DRUMS to 1), bars = 1)), song)
        val lanes = vm.project.value.automation
        assertEquals(setOf(AutoTarget.acid(0), AutoTarget.live(AutoTarget.LIVE_CHOP)), lanes.map { it.target }.toSet())
        assertTrue(lanes.all { it.mode == AutomationLane.SONG && it.songPerBeat })
        assertTrue(lanes.first { it.target.kind == AutoTarget.LIVE }.hold)
        assertEquals(0.9f, lanes.first { it.target.kind == AutoTarget.ACID }.songValues[5])
        assertFalse(vm.performanceTaking)
        // One undo step for the whole take.
        assertEquals(depth + 1, vm.session.undoDepth)
        vm.undoPerformance()
        assertTrue(vm.project.value.song.isEmpty())
        assertTrue(vm.project.value.automation.isEmpty())
        assertEquals(TakeUi.Ready, vm.take.value)
    }

    @Test
    fun aTakeGoesAfterTheSongOrEndsWithStop() {
        val vm = twoScenes()
        vm.addSongPart(1)
        vm.setTakeAppend(true)
        vm.togglePlay()
        vm.takeClockForTest = 3
        vm.togglePerformance()
        vm.takeClockForTest = 16
        vm.tickPerformance()
        // The song is one bar long already: the take starts on its bar 2.
        assertEquals(TakeUi.Recording(16, 0, 2), vm.take.value)
        vm.takeClockForTest = 40
        vm.togglePlay()  // Stop: the take ends at the bar being played (step 48)
        assertFalse(vm.transport.value.playing)
        assertEquals(TakeUi.Done(1, 0, 1), vm.take.value)
        assertEquals(listOf(SongPart(1, 1), SongPart(0, 2)), vm.project.value.song)
        // Armed then cancelled: nothing written.
        vm.togglePlay()
        vm.takeClockForTest = 3
        vm.togglePerformance()
        vm.togglePerformance()
        assertEquals(TakeUi.Ready, vm.take.value)
        assertEquals(2, vm.project.value.song.size)
    }

    /** The engine's slot states after a song swap on [step]: every slot on the row's scene from there. */
    private fun swappedOn(step: Int) = IntArray(LiveSlots.COUNT * 3) { i -> when (i % 3) { 0 -> LiveSlots.ROW_TAG; 1 -> LiveSlots.NO_LAUNCH; else -> step } }

    @Test
    fun aTakeArmedDuringASongPartStartsFromWhatThePartPlays() {
        val vm = twoScenes()
        // A part written by an earlier take: row A, the drums muted, the acid on B.
        val part = SongPart(0, 1, setOf(SongPart.DRUMS), play = mapOf(SongPart.ACID to 1), bars = 4)
        vm.session.edit { it.copy(song = listOf(SongPart(1), part)) }
        vm.togglePlay()
        // The engine swapped the part in on step 64; the 40 ms watch saw it on step 67.
        vm.takeClockForTest = 67
        vm.songPartSwappedForTest(1, swappedOn(64))
        val g = vm.liveGrid.value!!
        assertEquals(LiveSlot(-1, null, 64), g.slot(LiveSlots.DRUMS))
        assertEquals(LiveSlot(1, null, 64), g.slot(LiveSlots.ACID))
        // Edits of the drums do not reach the muted track; the acid's per-scene curves are B's.
        assertTrue(vm.waitsForTest(LiveSlots.DRUMS))
        assertEquals(1, vm.laneSceneForTest(AutoTarget.acid(0)))
        assertEquals(0, vm.laneSceneForTest(AutoTarget.live(AutoTarget.LIVE_FILTER)))
        assertNull(vm.laneSceneForTest(AutoTarget(AutoTarget.VOLUME, 0)))  // the drums stopped: left alone
        // ● armed there: it starts on the next bar (80) with the part as it plays.
        vm.togglePerformance()
        assertEquals(TakeUi.Armed(80, 13), vm.take.value)
        vm.takeClockForTest = 80
        vm.tickPerformance()
        vm.setLiveAcid(0, 0.9f)
        // Held in the engine to the take's end (no lane pulls it back between two gestures).
        assertEquals(setOf(AutoTarget.acid(0).pack()), vm.takeHeldForTest)
        vm.takeClockForTest = 100
        vm.togglePerformance()  // ends on the bar line 112
        assertTrue(vm.takeHeldForTest.isEmpty())
        assertEquals(listOf(SongPart(0, 1, setOf(SongPart.DRUMS), play = mapOf(SongPart.ACID to 1), bars = 2)), vm.project.value.song)
    }

    /**
     * Audit 28 live M6: a pad launched plays what the song's part would: the effects of every strip
     * of its track (the drum pads' own ones too), and the 303's knobs no lane drives in the scene
     * launched go back to the project's settings (else they stay where the last scene's curve left them).
     */
    @Test
    fun aPadLaunchedPlaysItsTracksEffectsAndThe303AsTheSongDoes() {
        val vm = twoScenes()
        val pad = com.pocketstudio.model.Mixer.padChannel(2)
        vm.setInsertType(pad, 0, com.pocketstudio.model.EffectType.EQ)
        assertEquals(listOf(0, pad), vm.slotStrips(vm.project.value, LiveSlots.DRUMS))
        assertEquals(listOf(1), vm.slotStrips(vm.project.value, LiveSlots.ACID))
        // The cutoff driven by a per-scene lane in A only.
        val lane = AutomationLane(AutoTarget.acid(0), mode = AutomationLane.PER_SCENE, sceneValues = mapOf(0 to List(AutomationLane.STEPS) { 0.1f }))
        vm.session.edit { it.copy(automation = listOf(lane)) }
        val cutoff = vm.project.value.acid.sound.values()[0]
        vm.togglePlay()
        vm.liveCommandsForTest = mutableListOf()
        vm.launchPad(SongPart.ACID, 1)
        vm.liveLaunchedForTest(LiveSlots.ACID, 1, 16)
        val sent = vm.liveCommandsForTest!!
        assertTrue(sent.toString(), "acid 0 $cutoff" in sent)
        // Back on A: its lane drives the cutoff, left to it.
        sent.clear()
        vm.launchPad(SongPart.ACID, 0)
        vm.liveLaunchedForTest(LiveSlots.ACID, 0, 32)
        assertFalse(sent.toString(), sent.any { it.startsWith("acid 0 ") })
    }

    /** Audit 28 live m1: « Lancer à » changed while a launch waits: its ring counts to the line it was armed for. */
    @Test
    fun aWaitingLaunchKeepsItsQuantumOnScreen() {
        val vm = twoScenes()
        vm.setLiveQuantum(4)
        vm.togglePlay()
        vm.launchPad(SongPart.DRUMS, 1)
        vm.setLiveQuantum(1)
        val g = vm.liveGrid.value!!
        assertEquals(64, g.slot(LiveSlots.DRUMS).quantum)
        // On step 20: the launch is 44 steps away (step 64), not 12 (the next bar).
        assertEquals(64, com.pocketstudio.app.live.nextLaunchQuantum(g, 20, vm.project.value.stepsPerBar))
        assertEquals(1f - 20f / 64f, com.pocketstudio.app.live.launchWaitLeft(20, 64), 1e-6f)
    }

    /** Audit 28 live m2: « Ajouter à la suite » chosen while armed with a full song: it stays « Remplacer » and says why. */
    @Test
    fun appendChosenWhileArmedWithAFullSongStaysReplace() {
        val vm = twoScenes()
        vm.session.edit { it.copy(song = List(SongPart.MAX_PARTS) { SongPart(it % 2) }) }
        vm.togglePlay()
        vm.takeClockForTest = 3
        vm.togglePerformance()
        assertTrue(vm.take.value is TakeUi.Armed)
        vm.setTakeAppend(true)
        assertFalse(vm.takeAppend.value)
        assertTrue(vm.notice.value!!.contains("Nouvelle version"))
        vm.takeClockForTest = 16
        vm.tickPerformance()
        vm.takeClockForTest = 40
        vm.togglePerformance()
        assertEquals(TakeUi.Done(1, 0, 0), vm.take.value)  // written, in place of the song
    }

    /** Audit 28 live m3: every column brought to B pad by pad makes B the row (the slots without a track follow). */
    @Test
    fun padsLaunchedOneByOneOnAnotherSceneMakeItTheRow() {
        val vm = twoScenes()
        vm.togglePlay()
        vm.liveCommandsForTest = mutableListOf()
        var step = 16
        for (key in vm.project.value.liveColumns) {
            vm.launchPad(key, 1)
            vm.liveLaunchedForTest(LiveSlots.of(key)!!, 1, step)
            step += 16
        }
        val g = vm.liveGrid.value!!
        assertEquals(1, g.row)
        assertFalse(g.diverged)
        assertEquals(1, vm.project.value.scene)  // the screens follow
        assertTrue(vm.liveCommandsForTest.toString(), "master 1" in vm.liveCommandsForTest!!)  // the master's part of B too
    }

    /** Audit 28 données m1: « Lancer à » and the live effects' settings are not undone by ↶ of something else. */
    @Test
    fun undoOfSomethingElseKeepsTheLiveSettings() {
        val vm = twoScenes()
        vm.toggleHit(3, 3)
        vm.setLiveQuantum(4)
        vm.setLiveFxSettings(vm.project.value.liveFx.copy(riserBars = 8))
        vm.undo()
        assertFalse(vm.project.value.drums.lanes[3].hits.containsKey(3))
        assertEquals(4, vm.project.value.liveQuantum)
        assertEquals(8, vm.project.value.liveFx.riserBars)
        vm.redo()
        assertTrue(vm.project.value.drums.lanes[3].hits.containsKey(3))
        assertEquals(4, vm.project.value.liveQuantum)
        assertEquals(8, vm.project.value.liveFx.riserBars)
    }

    /**
     * Audit 28 données G1 / live m6: the app closed (or killed) during a take: the take kept on
     * disk is written into the song when the project opens again.
     */
    @Test
    fun aTakeTheAppCouldNotFinishIsWrittenAtTheNextOpening() {
        val vm = twoScenes()
        vm.togglePlay()
        vm.takeClockForTest = 5
        vm.togglePerformance()
        vm.takeClockForTest = 16
        vm.tickPerformance()
        vm.launchPad(SongPart.DRUMS, 1)
        vm.liveLaunchedForTest(LiveSlots.DRUMS, 1, 32)
        vm.takeClockForTest = 36
        vm.setLiveAcid(0, 0.9f)
        vm.takeClockForTest = 45
        vm.keepTakeJournalForTest()  // what the app keeps while it records (and when it closes)
        kotlinx.coroutines.runBlocking { vm.flushNow() }
        // A new screen on the same project (the app opened again): the take is in the song.
        val again = RhythmViewModel(app)
        val song = again.project.value.song
        assertEquals(listOf(SongPart(0, 1), SongPart(0, 1, play = mapOf(SongPart.DRUMS to 1), bars = 1)), song)
        assertTrue(again.project.value.automation.any { it.target == AutoTarget.acid(0) })
        assertTrue(again.notice.value!!.contains("2"))
        // Written once: the next opening has nothing more to add.
        val third = RhythmViewModel(app)
        assertEquals(song, third.project.value.song)
    }

    /** A take recorded up to step 45 (a launch, a 303 knob moved), its clock there. */
    private fun recording(): RhythmViewModel {
        val vm = twoScenes()
        vm.togglePlay()
        vm.takeClockForTest = 5
        vm.togglePerformance()
        vm.takeClockForTest = 16
        vm.tickPerformance()
        vm.launchPad(SongPart.DRUMS, 1)
        vm.liveLaunchedForTest(LiveSlots.DRUMS, 1, 32)
        vm.takeClockForTest = 36
        vm.setLiveAcid(0, 0.9f)
        vm.takeClockForTest = 45
        assertTrue(vm.take.value is TakeUi.Recording)
        return vm
    }

    private fun waitFor(what: String, condition: () -> Boolean) {
        val until = System.currentTimeMillis() + 10_000
        while (!condition()) {
            check(System.currentTimeMillis() < until) { "waited too long for $what" }
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
    }

    /**
     * Audit 28 review G2: the 5 s journal write is under way when « ■ Fin » is tapped. It lands
     * after the end before the fix, and the next opening wrote the take again over the song (the
     * edits made after the take lost). Now it never lands, and the song stays as it was saved.
     */
    @Test
    fun aJournalWriteUnderWayAtTheEndIsNeverWrittenAgain() {
        val vm = recording()
        val journals = vm.journalsForTest
        val reached = java.util.concurrent.CountDownLatch(1)
        val go = java.util.concurrent.CountDownLatch(1)
        journals.beforeMove = { reached.countDown(); go.await(5, java.util.concurrent.TimeUnit.SECONDS) }
        try {
            vm.keepTakeJournalForTest(wait = false)  // the 5 s write, encoded, about to replace the journal
            assertTrue(reached.await(5, java.util.concurrent.TimeUnit.SECONDS))
            vm.togglePerformance()  // ■ Fin
            assertTrue(vm.take.value is TakeUi.Done)
        } finally {
            go.countDown()
            journals.beforeMove = null
        }
        assertTrue(journals.awaitIdle(5000))
        assertFalse("the late write never lands", journals.file(vm.projectId).exists())
        // The take saved (on the main thread's scope: let it run rather than block it), then the
        // song edited after the take, saved.
        waitFor("the take saved") { vm.session.settled }
        val song = vm.project.value.song
        assertEquals(2, song.size)
        vm.removeSongPart(1)
        val edited = vm.project.value.song
        kotlinx.coroutines.runBlocking { vm.flushNow() }
        val again = RhythmViewModel(app)
        assertEquals(edited, again.project.value.song)
        assertNull(again.notice.value)
    }

    /**
     * G2 / m1: the journal stays until the song holding the take is saved; found again after
     * that (the app killed in between), it is not written a second time.
     */
    @Test
    fun aJournalFoundAgainAfterItsTakeWasSavedIsNotWrittenTwice() {
        val vm = recording()
        vm.keepTakeJournalForTest()
        val file = vm.journalsForTest.file(vm.projectId)
        val kept = file.readText()
        vm.togglePerformance()  // ■ Fin: written into the song, saved, then the journal goes
        waitFor("the journal dropped once saved") { !file.exists() }
        val song = vm.project.value.song
        assertEquals(song, com.pocketstudio.model.ProjectStore(File(app.filesDir, "projects")).load(vm.projectId)!!.song)
        // Killed just before the journal went: it is still there at the next opening.
        file.writeText(kept)
        val again = RhythmViewModel(app)
        assertEquals(song, again.project.value.song)
        assertNull(again.notice.value)
        // The opening drops it on the journal writer (its own thread): checked once that is done.
        assertTrue(again.journalsForTest.awaitIdle(5000))
        assertFalse(again.journalsForTest.file(again.projectId).exists())
        // Recovered once at an opening, then found again (killed before it went): once too.
        val recovered = recording()
        recovered.keepTakeJournalForTest()
        val text = recovered.journalsForTest.file(recovered.projectId).readText()
        kotlinx.coroutines.runBlocking { recovered.flushNow() }
        val first = RhythmViewModel(app)
        val firstSong = first.project.value.song
        assertTrue(first.notice.value!!.contains("gardée"))
        // Its drop done first (else it could delete the journal written back below).
        assertTrue(first.journalsForTest.awaitIdle(5000))
        assertFalse(first.journalsForTest.file(first.projectId).exists())
        first.journalsForTest.file(first.projectId).writeText(text)
        val second = RhythmViewModel(app)
        assertEquals(firstSong, second.project.value.song)
        assertNull(second.notice.value)
        assertTrue(second.journalsForTest.awaitIdle(5000))
        assertFalse(second.journalsForTest.file(second.projectId).exists())
    }

    /** m9: the screen closed (the app swiped away) while a take records: its journal is written, and the next opening has it. */
    @Test
    fun aScreenClosedDuringATakeKeepsItsJournal() {
        File(app.filesDir, "projects").deleteRecursively()
        app.filesDir.listFiles()?.filter { it.name.startsWith("live-take") }?.forEach { it.delete() }
        val owner = androidx.lifecycle.ViewModelStore()
        val vm = androidx.lifecycle.ViewModelProvider(owner, androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory(app))[RhythmViewModel::class.java]
        vm.toggleHit(0, 0)
        vm.duplicateScene()
        vm.toggleHit(2, 4)
        vm.selectScene(0)
        vm.togglePlay()
        vm.takeClockForTest = 5
        vm.togglePerformance()
        vm.takeClockForTest = 16
        vm.tickPerformance()
        vm.launchPad(SongPart.DRUMS, 1)
        vm.liveLaunchedForTest(LiveSlots.DRUMS, 1, 32)
        vm.takeClockForTest = 45
        owner.clear()  // onCleared, Recording
        val again = RhythmViewModel(app)
        assertEquals(2, again.project.value.song.size)
        assertTrue(again.notice.value!!.contains("gardée"))
    }

    /**
     * Audit 28 review m6: two fingers on two columns, the first one then slides (the grid
     * scrolls): its launch is taken back, the second one's stays.
     */
    @Test
    fun theFirstOfTwoFingersSlidingTakesBackItsOwnLaunch() {
        val vm = twoScenes()
        vm.togglePlay()
        assertTrue(vm.pressLive(SongPart.DRUMS, 1))
        assertTrue(vm.pressLive(SongPart.ACID, 1))
        assertEquals(1, vm.liveGrid.value!!.slot(LiveSlots.DRUMS).pending)
        assertEquals(1, vm.liveGrid.value!!.slot(LiveSlots.ACID).pending)
        vm.retractLive(SongPart.DRUMS)
        assertNull(vm.liveGrid.value!!.slot(LiveSlots.DRUMS).pending)
        assertEquals(1, vm.liveGrid.value!!.slot(LiveSlots.ACID).pending)
        // Its press is used up: a second retract does nothing more.
        vm.retractLive(SongPart.DRUMS)
        assertEquals(1, vm.liveGrid.value!!.slot(LiveSlots.ACID).pending)
    }

    @Test
    fun aTakeThatFillsTheSongEndsAndSaysSo() {
        val vm = twoScenes()
        vm.togglePlay()
        vm.takeClockForTest = 3
        vm.togglePerformance()
        vm.takeClockForTest = 16
        vm.tickPerformance()
        // The acid changes scene on every bar: a new part each time.
        var b = 1
        while (vm.take.value is TakeUi.Recording && b < 200) {
            vm.launchPad(SongPart.ACID, b % 2)
            vm.liveLaunchedForTest(LiveSlots.ACID, b % 2, 16 + b * 16)
            vm.takeClockForTest = 16 + b * 16 + 1
            vm.tickPerformance()
            b++
        }
        assertTrue(vm.take.value is TakeUi.Done)
        assertEquals(SongPart.MAX_PARTS, vm.project.value.song.size)
        assertTrue(b < 140)  // it ended there, not 70 bars later
        assertTrue(vm.notice.value!!.contains("128"))
    }

    /**
     * A sink in the engine's place that keeps the loudness targets sent (CMD_MASTER, b = 4): to the
     * live engine (target 0) and to each private render engine. Renders give nothing back.
     */
    private class TargetSink : CommandSink {
        val live = ArrayList<Float>()
        val renders = ArrayList<Float>()
        private var next = 1L

        @Synchronized
        override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) {
            if (type == 67 && a == 4) (if (target == 0L) live else renders) += value  // NativeEngine.CMD_MASTER, setLoudnessTarget
        }

        override fun scheduleFrom(target: Long, frame: Long) = Unit
        override fun install(target: Long, kind: CommandSink.Install, index: Int, sample: Long) = Unit
        @Synchronized override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long = next++
        override fun sampleFrames(handle: Long): Long = 0L
        override fun sampleRelease(handle: Long) = Unit
        @Synchronized override fun exportCreate(): Long = next++
        override fun exportRender(target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean, sampleRate: Int, bits: Int) = ExportResult.Ok
        override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray = FloatArray(0)
        override fun exportDestroy(target: Long) = Unit
    }

    /**
     * The pads of a song the assistant made are rendered one by one on a private engine: none of
     * those renders carries the song's loudness target (the pad is brought to it once, live, in
     * the master chain), while the live engine does get it.
     */
    @Test
    fun padRendersCarryNoLoudnessTarget() {
        File(app.filesDir, "projects").deleteRecursively()
        val style = StarterWizard.style("techno")!!
        val parts = StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() }
        val song = StarterWizard.build(style, style.lines.associate { it.role to null }, style.melody.mood, parts).copy(livePadsPending = true)
        assertEquals(LoudnessTarget.AUTO, song.mixer.target)
        ProjectStore(File(app.filesDir, "projects")).save("p33", song)
        com.pocketstudio.app.projects.CurrentProject.set(app, "p33")
        val sink = TargetSink()
        NativeEngine.sink = sink
        try {
            val vm = RhythmViewModel(app)
            assertTrue(vm.project.value.livePadsPending)
            vm.engineStarted()  // as the audio stream's start does: the whole project goes to the live engine
            val until = System.currentTimeMillis() + 60_000
            while (vm.project.value.livePadsPending) {
                assertTrue("the pads were never prepared", System.currentTimeMillis() < until)
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(50))
                Thread.sleep(5)
            }
        } finally {
            NativeEngine.sink = null
            com.pocketstudio.app.projects.CurrentProject.set(app, RhythmViewModel.PROJECT_ID)
        }
        // One render per pad, each told « no target »; the live engine was told the style's. S6 (G5): one
        // more render may carry it, the live gain's estimate (the fullest Drop, at the song's target).
        val pads = sink.renders.filter { it == 0f }
        val targeted = sink.renders.filter { it != 0f }
        assertTrue("${pads.size} pad renders", pads.size >= 2)
        assertTrue("render targets ${sink.renders}", targeted.size <= 1 && targeted.all { t -> sink.live.any { it == t } })
        assertTrue("live targets ${sink.live}", sink.live.any { it != 0f })
    }
}
