package com.pocketstudio.app.rhythm

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.PackManifest
import com.pocketstudio.model.PackSound
import com.pocketstudio.model.tuto.TutoChecks
import com.pocketstudio.model.tuto.TutoKit
import com.pocketstudio.model.tuto.TutoTarget
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Duration

/** Lot 6, T2 (its `loadTutoKit` part, seat D b #31): after the tutorial's kit, the rumble's pad keeps the name the step says (« Rumble »). */
@RunWith(RobolectricTestRunner::class)
class Lot6VmTutoKitNameTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    private class Sink : CommandSink {
        private var next = 1L
        override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) = Unit
        override fun scheduleFrom(target: Long, frame: Long) = Unit
        override fun install(target: Long, kind: CommandSink.Install, index: Int, sample: Long) = Unit
        @Synchronized override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long = next++
        override fun sampleFrames(handle: Long): Long = 4_800L
        override fun sampleRelease(handle: Long) = Unit
        @Synchronized override fun exportCreate(): Long = next++
        override fun exportRender(target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean, sampleRate: Int, bits: Int) =
            ExportResult.Cancelled
        override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray = FloatArray(0)
        override fun exportDestroy(target: Long) = Unit
    }

    @After
    fun tearDown() {
        NativeEngine.sink = null
    }

    @Test
    fun theRumblesPadKeepsItsNameAfterTheKit() {
        File(app.filesDir, "projects").deleteRecursively()
        NativeEngine.sink = Sink()
        val target = TutoTarget.of("techno")!!
        val vm = RhythmViewModel(app)
        vm.tutoKitDecodeForTest = { Pcm(FloatArray(4800) { 0.4f * kotlin.math.sin(it * 0.07f) * (1f - it / 4800f) }, 1, 48_000) }
        vm.session.edit { target.blank() }
        val lane = TutoChecks.laneOf("rumble")
        assertEquals("the step's name before the kit", "Rumble", padName(vm.project.value, lane))
        val s = target.style
        val kinds = s.lines.map { it.role.packKind }.distinct()
        val ids = (s.lines.filter { it.voice == null }.mapNotNull { s.kit[it.role] } + s.id).distinct()
        val installed = ids.map { id -> PackManifest(id, "Techno", sounds = kinds.map { k -> PackSound("$k.flac", k, "$k $id") }) }
        val sounds = TutoKit.sounds(target, installed).mapValues { (_, v) -> Triple(v.first, "Techno", v.second) }
        assertTrue("the rumble's pad takes a pack sound", lane in sounds)
        var ok: Boolean? = null
        vm.loadTutoKit(sounds) { ok = it }
        val until = System.currentTimeMillis() + 20_000
        while (ok == null) {
            assertTrue("the kit never came", System.currentTimeMillis() < until)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            Thread.sleep(5)
        }
        assertEquals(true, ok)
        assertTrue(vm.project.value.drums.lanes[lane].sample != null)
        assertEquals("the pad the step names « Rumble »", "Rumble", padName(vm.project.value, lane))
        // A pad the step names by the kit's name keeps the kit's name (the step renames it, tutoPadNames).
        assertEquals("Techno · Kick", padName(vm.project.value, 0))
    }
}
