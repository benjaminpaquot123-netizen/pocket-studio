package com.pocketstudio.app

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.live.LiveAcidParams
import com.pocketstudio.app.live.TakeUi
import com.pocketstudio.app.playback.PlaybackService
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LiveSlots
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.partSteps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Duration

/** The final audit's Live findings: the take, its end, its undo, the 303 and the pads played alone. */
@RunWith(RobolectricTestRunner::class)
class LiveAuditFixesTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    /** Two scenes playing, a take armed at step 5 (it starts on bar 2, step 16), B's drums launched at 32. */
    private fun recording(): RhythmViewModel {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
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
        assertTrue(vm.take.value is TakeUi.Recording)
        return vm
    }

    private fun songSteps(vm: RhythmViewModel): Int = vm.project.value.let { p -> p.song.sumOf { p.partSteps(it) } }

    @Test
    fun leavingTheLivePadsEndsTheTakeAndWritesIt() {
        // 07 #1: the full-screen pads opened from Morceau, closed during a take: the take never goes on unseen.
        val vm = recording()
        vm.takeClockForTest = 40
        vm.leaveLiveEndingTake()
        assertFalse(vm.performanceTaking)
        assertEquals(TakeUi.Ready, vm.take.value)
        assertEquals(32, songSteps(vm))  // bars 2 and 3 of the transport: two bars
        assertTrue(vm.notice.value!!.contains("Prise terminée"))
    }

    @Test
    fun finOnTheOneEndsOnThatLineAndLaterKeepsTheBar() {
        // 07 #3: ■ Fin pressed just after the « one » of bar 4 (step 48) does not add bar 4.
        val vm = recording()
        vm.takeClockForTest = 49
        vm.togglePerformance()
        assertTrue(vm.take.value is TakeUi.Done)
        assertEquals(32, songSteps(vm))
        // Later in the bar (its second beat): the bar being played is kept, as before.
        val late = recording()
        late.takeClockForTest = 53
        late.togglePerformance()
        assertEquals(48, songSteps(late))
    }

    @Test
    fun undoingTheTakeKeepsTheTempoChangedMeanwhileButNotItsGestures() {
        // 07 #5: Annuler takes the take back, not the tempo turned during it; a macro the take moved goes back.
        val vm = recording()
        val macroBefore = vm.project.value.mixer.channels[0].macros[1]
        vm.setTempo(133.0)
        vm.setMacro(0, 1, 0.9f)  // a gesture of the take
        vm.takeClockForTest = 40
        vm.togglePerformance()
        assertTrue(vm.canUndoPerformance())
        vm.undoPerformance()
        val p = vm.project.value
        assertTrue(p.song.isEmpty())
        assertEquals(133.0, p.tempoBpm, 1e-9)
        assertEquals(macroBefore, p.mixer.channels[0].macros[1], 1e-6f)
    }

    @Test
    fun leavingTheLivePageBringsThe303BackToItsSavedSound() {
        // 07 #7: the 303 as the Live page's sliders left it does not outlive the page.
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        vm.liveCommandsForTest = mutableListOf()
        vm.setLiveAcid(LiveAcidParams.CUTOFF, 0f)
        assertEquals(mapOf(LiveAcidParams.CUTOFF to 0f), vm.liveAcid.value)
        vm.leaveLive()
        assertTrue(vm.liveAcid.value.isEmpty())
        val saved = vm.project.value.acid.sound.values()[LiveAcidParams.CUTOFF]
        assertTrue(vm.liveCommandsForTest!!.contains("acid ${LiveAcidParams.CUTOFF} $saved"))
    }

    @Test
    fun livePadsPlayedWithoutPlayKeepTheMusicGoingLikePlay() {
        // 07 #4: a pad started with the transport stopped: the playback service (headphones out,
        // calls) and the background hold it, until every pad is off.
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        vm.session.edit { it.copy(live = it.live.with(0, LiveClip("x.wav", "Batterie", beats = 4))) }
        vm.livePadsSoundingForTest = emptySet()
        assertFalse(vm.keepsRunningInBackground)
        vm.toggleLive(0)
        assertTrue(vm.padsAlone.value)
        assertTrue(vm.keepsRunningInBackground)
        val started = shadowOf(app).nextStartedService
        assertNotNull(started)
        assertEquals(PlaybackService::class.java.name, started.component?.className)
        // Still sounding a while later: still held.
        vm.livePadsSoundingForTest = setOf(0)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(600))
        assertTrue(vm.keepsRunningInBackground)
        // All off (■, or the headphones out: stopLive): let go.
        vm.stopLive()
        vm.livePadsSoundingForTest = emptySet()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(600))
        assertFalse(vm.padsAlone.value)
        assertFalse(vm.keepsRunningInBackground)
    }
}
