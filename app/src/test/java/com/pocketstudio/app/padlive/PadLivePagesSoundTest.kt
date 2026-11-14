package com.pocketstudio.app.padlive

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.rhythm.SampleTask
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
 * Fix 2, n° 3: a sound from the library on Pad Live's pages B to D goes the way of page A: a loop
 * with a tempo follows the project's, a hit stays a hit, a second sound while one is being stored
 * is refused (the library says so), and a phone that cannot store it says why instead of crashing.
 */
@RunWith(RobolectricTestRunner::class)
class PadLivePagesSoundTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val store = ProjectStore(File(app.filesDir, "projects"))

    private fun open(p: Project, id: String = "pages-" + System.nanoTime()): RhythmViewModel {
        com.pocketstudio.engine.NativeEngine.sink = com.pocketstudio.app.human.BenchEngine()
        store.save(id, p.sanitized())
        CurrentProject.set(app, id)
        return RhythmViewModel(app)
    }

    @After
    fun tearDown() {
        com.pocketstudio.engine.NativeEngine.sink = null
        CurrentProject.set(app, RhythmViewModel.PROJECT_ID)
    }

    private fun tone(seconds: Double) = Pcm(FloatArray((seconds * 48_000).toInt() * 2) { i -> (0.3 * sin(2 * PI * 220 * (i / 2) / 48_000.0)).toFloat() }, 2, 48_000)

    private fun until(what: String, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + 30_000
        while (!ok()) {
            check(System.currentTimeMillis() < end) { what }
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(20))
            Thread.sleep(5)
        }
    }

    @Test
    fun aLoopOnB1FollowsTheTempoAndAHitOnC3StaysAHit() {
        val vm = open(Project(name = "Pages", tempoBpm = 120.0))
        // B1 (pad 16): 8 beats at 113 BPM.
        assertTrue(vm.placeOnLivePad(16, tone(8 * 60.0 / 113), "Boys", known = 113.0))
        // A second one while it is stored: refused, the library keeps its place.
        assertFalse(vm.placeOnLivePad(34, tone(0.3), "Clap", oneShot = true))
        until("the loop never reached B1") { vm.project.value.live.at(16) != null && vm.sampleTask.value == SampleTask.Idle }
        val loop = vm.project.value.live.at(16)!!
        assertEquals(113.0, loop.bpm, 0.0)
        assertEquals(8, loop.beats)
        assertFalse(loop.oneShot)
        // C3 (pad 34): a hit.
        assertTrue(vm.placeOnLivePad(34, tone(0.3), "Clap", oneShot = true))
        until("the hit never reached C3") { vm.project.value.live.at(34) != null && vm.sampleTask.value == SampleTask.Idle }
        val hit = vm.project.value.live.at(34)!!
        assertTrue(hit.oneShot)
        assertEquals(0.0, hit.bpm, 0.0)
        // Page A untouched.
        assertNull(vm.project.value.live.at(0))
    }

    /** Fix 2, n° 5: the library's title names a page B to D pad as Pad Live does. */
    @Test
    fun theLibraryNamesThePadAsPadLiveDoes() {
        assertEquals("le pad live 3", libraryTarget(2))
        assertEquals("le pad B1", libraryTarget(16))
        assertEquals("le pad D16", libraryTarget(63))
    }

    @Test
    fun aFullPhoneIsSaidNotACrash() {
        val id = "pages-full-" + System.nanoTime()
        val vm = open(Project(name = "Plein"), id)
        // The samples' folder cannot be made: what a full phone does to the sound's file.
        val samples = File(store.folder(id), "samples")
        samples.deleteRecursively()
        samples.writeText("not a folder")
        assertTrue(vm.placeOnLivePad(17, tone(0.3), "Clap", oneShot = true))
        until("nothing said") { vm.sampleTask.value is SampleTask.Failed }
        assertEquals(
            "Le son n'a pas pu être rangé sur le téléphone : il manque peut-être de la place.",
            (vm.sampleTask.value as SampleTask.Failed).message,
        )
        assertNull(vm.project.value.live.at(17))
    }
}
