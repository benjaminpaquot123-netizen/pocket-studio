package com.pocketstudio.app.rhythm

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.human.BenchEngine
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Duration
import kotlin.math.PI
import kotlin.math.sin

/**
 * Audit ultra round 2, zone 1, n° 1 (seats A c and B d): the library's « Poser » left the pad as it was, nothing heard,
 * « Annuler » off. Without the human-tester bench: with an engine that stays, as a phone's, the sound is on the pad (its
 * name, its undo step) and on a live pad; the seats' failure was the bench's (its engine let go before the touch, the
 * sound went to the native library Robolectric has not: UnsatisfiedLinkError). And whatever the failure, the app says
 * it and the pad is free again: a sound the engine cannot take is never silent nor a pad « busy » for good.
 */
@RunWith(RobolectricTestRunner::class)
class PlaceFromLibraryTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val store = ProjectStore(File(app.filesDir, "projects"))

    /** An engine that cannot take a sound, as the native library missing (the bench's engine gone). */
    private class NoNativeSound(private val b: BenchEngine = BenchEngine()) : CommandSink by b {
        override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long =
            throw UnsatisfiedLinkError("'long com.pocketstudio.engine.NativeEngine.sampleCreate(float[], int, int)'")
    }

    @After
    fun end() {
        NativeEngine.sink = null
        CurrentProject.set(app, RhythmViewModel.PROJECT_ID)
    }

    private fun until(what: String, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + 30_000
        while (!ok()) {
            check(System.currentTimeMillis() < end) { what }
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(20))
            Thread.sleep(5)
        }
    }

    private fun horn() = Pcm(FloatArray(24_000 * 2) { i -> (0.4 * sin(2 * PI * 440 * (i / 2) / 48_000.0)).toFloat() }, 2, 48_000)

    private fun vm(): RhythmViewModel {
        val id = "place-lib-" + System.nanoTime()
        store.save(id, Project(name = "Poser"))
        CurrentProject.set(app, id)
        return RhythmViewModel(app).also { v -> until("the project's sounds") { !v.sampleTask.value.let { it is SampleTask.Busy } } }
    }

    @Test
    fun posedFromTheLibraryTheSoundIsOnThePadWithAnEngineThatStays() {
        NativeEngine.sink = BenchEngine()
        val vm = vm()
        vm.selectPad(0)
        assertTrue(vm.placeOnPad(horn(), "Klaxon de DJ", oneShot = true))
        until("never on the pad") { vm.project.value.drums.lanes[0].sample != null }
        assertEquals("Klaxon de DJ", vm.project.value.drums.lanes[0].sample!!.name)
        assertEquals("the pad's name", "Klaxon de DJ", padName(vm.project.value, 0))
        assertTrue("« Annuler » takes it back", vm.session.canUndo.value)
        assertEquals(SampleTask.Idle, vm.sampleTask.value)
        // A live pad (Pad Live's library, seat A c) as well.
        assertTrue(vm.placeOnLivePad(0, horn(), "Klaxon de DJ", oneShot = true))
        until("never on the live pad") { vm.project.value.live.at(0) != null }
        assertEquals("Klaxon de DJ", vm.project.value.live.at(0)!!.name)
    }

    @Test
    fun aSoundTheEngineCannotTakeIsSaidAndThePadIsFreeAgain() {
        NativeEngine.sink = BenchEngine()
        val vm = vm()
        NativeEngine.sink = NoNativeSound()
        vm.selectPad(0)
        assertTrue(vm.placeOnPad(horn(), "Klaxon de DJ", oneShot = true))
        until("the failure never said") { vm.sampleTask.value is SampleTask.Failed }
        assertNull("the pad keeps its sound", vm.project.value.drums.lanes[0].sample)
        val notice = vm.notice.value
        assertTrue("said over the screen (the Mode simple's pad has no line for it): $notice", notice != null && "pad" in notice)
        // Not « busy » for good: the next sound goes on.
        NativeEngine.sink = BenchEngine()
        vm.dismissNotice()
        assertTrue(vm.placeOnPad(horn(), "Klaxon long", oneShot = true))
        until("the next sound never on the pad") { vm.project.value.drums.lanes[0].sample != null }
        // A live pad: its line in Pad Live says it (no window over the pads).
        NativeEngine.sink = NoNativeSound()
        assertTrue(vm.placeOnLivePad(1, horn(), "Sirène dub"))
        until("the live pad's failure never said") { vm.sampleTask.value is SampleTask.Failed }
        assertNull(vm.project.value.live.at(1))
        assertNull("no window over Pad Live", vm.notice.value)
    }
}
