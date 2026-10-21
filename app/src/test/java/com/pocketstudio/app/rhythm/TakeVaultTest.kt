package com.pocketstudio.app.rhythm

import com.pocketstudio.model.PendingTakes
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.addAudioTrack
import com.pocketstudio.model.withTakes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class TakeVaultTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun request(folder: java.io.File, handle: Long = 7L, latency: Int = 0, cut: Int = 0, waiting: Boolean = false) = TakeVault.Request(
        handle, folder, track = 0, name = "Prise 21:14", bpm = 120.0, song = false, start = 0,
        latencyFrames = latency, cutFrames = cut, waiting = waiting,
    )

    @Test
    fun aTakeIsWrittenEvenWhenTheScreenThatStoppedItIsGone() = runBlocking {
        // The engine is slow to hand the take over; meanwhile the screen (its ViewModel) is cleared.
        val release = CountDownLatch(1)
        val vault = TakeVault({ release.await(5, TimeUnit.SECONDS); FloatArray(48000) { 0.5f } })
        val store = ProjectStore(tmp.root)
        store.save("p", Project().addAudioTrack()!!)
        val folder = store.folder("p")
        vault.opened(7L)
        assertEquals(1, vault.busy.value)
        // The take is stopped from the screen's own scope, which is cancelled (onCleared) while
        // the engine still holds the take: the writing must not be cancelled with it.
        val screen = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val handed = kotlinx.coroutines.CompletableDeferred<kotlinx.coroutines.Deferred<TakeVault.Outcome>>()
        screen.launch { handed.complete(vault.finish(request(folder))) }
        val written = handed.await()
        screen.cancel()
        assertFalse(written.isCancelled)
        release.countDown()
        val outcome = written.await()
        assertFalse(written.isCancelled)
        assertTrue(outcome is TakeVault.Outcome.Saved)
        assertTrue(vault.awaitIdle(1000))
        // On disk: the sound and the waiting note; the next opening puts it on its track.
        val waiting = PendingTakes.read(folder)
        assertEquals(1, waiting.size)
        assertTrue(java.io.File(folder, "samples/${waiting[0].file}").length() > 48000 * 4)
        val reopened = store.load("p")!!.withTakes(waiting)
        assertEquals(waiting[0].file, reopened.audioTracks[0].clip?.file)
        assertEquals(1.0, reopened.audioTracks[0].clip!!.seconds, 1e-9)
    }

    @Test
    fun theServiceWaitsForATakeUntilItIsOnDisk() = runBlocking {
        val release = CountDownLatch(1)
        val vault = TakeVault({ release.await(5, TimeUnit.SECONDS); FloatArray(48000) { 0.5f } })
        vault.opened(3L)
        assertFalse(vault.awaitIdle(50))  // recording: the app must stay
        val written = vault.finish(request(tmp.newFolder("p"), handle = 3L))
        assertFalse(vault.awaitIdle(50))  // being written
        release.countDown()
        written.await()
        assertTrue(vault.awaitIdle(1000))
        // A take left with Annuler frees the microphone and is not kept.
        var finished = 0L
        val other = TakeVault({ finished = it; null })
        other.opened(9L)
        other.discard(9L)
        assertTrue(other.awaitIdle(1000))
        assertEquals(9L, finished)
    }

    @Test
    fun latencyAndTheBarsBeforeAKeptStartAreCut() = runBlocking {
        // A ramp: the frame index, so what is kept shows where it was cut.
        val vault = TakeVault({ FloatArray(96000) { i -> i / 96000f + 0.01f } })
        val folder = tmp.newFolder("p")
        val outcome = vault.finish(request(folder, latency = 480, cut = 24000)).await() as TakeVault.Outcome.Saved
        assertEquals(96000 - 24480, outcome.pcm.frames)
        assertEquals(24480 / 96000f + 0.01f, outcome.pcm.data[0], 1e-6f)
        // Stopped during the count-in: nothing, and no error.
        assertEquals(TakeVault.Outcome.Nothing, TakeVault({ FloatArray(10) }).finish(request(folder, waiting = true)).await())
        assertEquals(TakeVault.Outcome.TooShort, TakeVault({ FloatArray(2000) { 0.5f } }).finish(request(folder)).await())
        assertEquals(TakeVault.Outcome.Silent, TakeVault({ FloatArray(48000) }).finish(request(folder)).await())
        assertEquals(TakeVault.Outcome.Lost, TakeVault({ null }).finish(request(folder)).await())
        // Cut down to nothing: too short (the start kept is past what was sung).
        assertNull(TakeVault.keptRange(48000, 0, 47000))
        assertEquals(24000 until 48000, TakeVault.keptRange(48000, 30000, 0))  // latency: at most half
    }

    @Test
    fun theSilenceCheckOfALongTakeAllocatesNothing() {
        // Five minutes at 48 kHz, silent: the check walks every sample without boxing one.
        val audio = FloatArray(48000 * 300)
        val threads = java.lang.management.ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        val id = Thread.currentThread().id
        TakeVault.isSilent(audio, 0..999)  // warmed up
        val before = threads.getThreadAllocatedBytes(id)
        val silent = TakeVault.isSilent(audio, audio.indices)
        val allocated = threads.getThreadAllocatedBytes(id) - before
        assertTrue(silent)
        assertTrue("$allocated bytes allocated", allocated < 1_000_000)
        audio[audio.size - 1] = 0.01f
        assertFalse(TakeVault.isSilent(audio, audio.indices))
        assertTrue(TakeVault.isSilent(audio, 0 until audio.size - 1))
    }
}
