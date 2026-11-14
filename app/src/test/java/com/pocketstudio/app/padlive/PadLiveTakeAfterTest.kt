package com.pocketstudio.app.padlive

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.PadLive
import com.pocketstudio.model.SongPart
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Tour 6, lot « padlive2 », P3 (control seat A b, defect #49: a first perf kept, « Live 4 mes. · 0:08 », then a second
 * take of 0:01 and the « Live » project falls to « 1 mes. · 0:02 »). Not the bench: Pad Live's ● wrote each take as a
 * « Nouvelle version » (the Live page's default), the song replaced by the last take. Pad Live has no such choice to
 * show: a take after one already in the song goes after it (« Ajouter à la suite »), so the project keeps its length;
 * the first take still replaces the empty song of a new « Live » project.
 */
@RunWith(RobolectricTestRunner::class)
class PadLiveTakeAfterTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    private class Sink : CommandSink {
        private var next = 1L
        override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) = Unit
        override fun scheduleFrom(target: Long, frame: Long) = Unit
        override fun install(target: Long, kind: CommandSink.Install, index: Int, sample: Long) = Unit
        @Synchronized override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long = next++
        override fun sampleFrames(handle: Long): Long = 48_000L
        override fun sampleRelease(handle: Long) = Unit
        @Synchronized override fun exportCreate(): Long = next++
        override fun exportRender(target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean, sampleRate: Int, bits: Int) = ExportResult.Ok
        override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray = FloatArray(0)
        override fun exportDestroy(target: Long) = Unit
    }

    @Before
    fun setUp() {
        File(app.filesDir, "projects").deleteRecursively()
        NativeEngine.sink = Sink()
    }

    @After
    fun tearDown() {
        NativeEngine.sink = null
    }

    private fun liveVm() = RhythmViewModel(app).also { vm ->
        vm.livePadsSoundingForTest = emptySet()
        vm.session.edit { p -> PadLive.newProject().let { n -> p.copy(name = n.name, tempoBpm = n.tempoBpm, live = n.live, padLive = n.padLive) } }
    }

    @Test
    fun aSecondTakeGoesAfterTheFirst() {
        val vm = liveVm()
        // A first take kept: the « Live » song holds it (four bars).
        vm.session.edit { it.copy(song = List(4) { SongPart() }, liveTake = "take-1") }
        vm.setTakeAppend(false)  // the Live page's default, « Nouvelle version »
        var asked = 0
        vm.padLive.record { asked++ }
        assertEquals("the ● is pressed once", 1, asked)
        assertTrue("the second take goes after the first: the song keeps its length", vm.takeAppend.value)
    }

    @Test
    fun theFirstTakeStillReplacesTheEmptySong() {
        val vm = liveVm()
        vm.session.edit { it.copy(song = listOf(SongPart()), liveTake = "") }
        vm.setTakeAppend(true)
        vm.padLive.record { }
        assertFalse("no take yet: the first one is the song", vm.takeAppend.value)
    }
}
