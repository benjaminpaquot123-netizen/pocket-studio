package com.pocketstudio.app.rhythm

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Insert
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.LayerRole
import com.pocketstudio.model.LayerSound
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.SynthPatch
import com.pocketstudio.model.addTrack
import com.pocketstudio.model.guide.GuideChange
import com.pocketstudio.model.guide.GuideHistory
import com.pocketstudio.model.guide.GuideTrack
import com.pocketstudio.model.layersOf
import com.pocketstudio.model.longFxCount
import com.pocketstudio.model.track
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Duration

/**
 * Audit ultra round 1, n° 35 (seat D c, a synth producer): a kept sound comes back with its effects (it came
 * back with « Effets en place : 0 sur 8 »), in one undo step; it is named when kept and can be renamed; kept
 * twice the same, it says so (a silent « Mon son 2 » the same).
 */
@RunWith(RobolectricTestRunner::class)
class KeptSoundTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    private class Sink : CommandSink {
        class Post(val target: Long, val type: Int, val value: Float, val a: Int, val b: Int)
        val posts = ArrayList<Post>()
        private var next = 1L
        @Synchronized override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) {
            posts += Post(target, type, value, a, b)
        }
        override fun scheduleFrom(target: Long, frame: Long) = Unit
        override fun install(target: Long, kind: CommandSink.Install, index: Int, sample: Long) = Unit
        @Synchronized override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long = next++
        override fun sampleFrames(handle: Long): Long = 0L
        override fun sampleRelease(handle: Long) = Unit
        @Synchronized override fun exportCreate(): Long = 1000L + next++
        override fun exportRender(target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean, sampleRate: Int, bits: Int) =
            ExportResult.Ok
        override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray = FloatArray(2 * 48_000)
        override fun exportDestroy(target: Long) = Unit
        @Synchronized fun live(type: Int) = posts.filter { it.type == type && it.target == 0L }
        @Synchronized fun clear() = posts.clear()
    }

    private val sink = Sink()

    @After
    fun tearDown() {
        NativeEngine.sink = null
        com.pocketstudio.app.projects.CurrentProject.set(app, RhythmViewModel.PROJECT_ID)
    }

    private fun waitUntil(what: String, check: () -> Boolean) {
        val until = System.currentTimeMillis() + 60_000
        while (!check()) {
            assertTrue(what, System.currentTimeMillis() < until)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            Thread.sleep(5)
        }
    }

    private fun open(p: Project): RhythmViewModel {
        val id = "sheet-${System.nanoTime()}"
        ProjectStore(File(app.filesDir, "projects")).save(id, p)
        com.pocketstudio.app.projects.CurrentProject.set(app, id)
        NativeEngine.sink = sink
        val vm = RhythmViewModel(app)
        waitUntil("the project never opened") { vm.project.value.tracks.size == p.tracks.size && vm.project.value.song.size == p.song.size }
        return vm
    }

    /** A bass on track 1 and a lead on track 2, with notes and a song part; the bass with a compressor and a reverb send. */
    private fun song(): Project {
        var p = Project(song = listOf(SongPart(0))).addTrack("Basse")!!.addTrack("Lead")!!
        p = p.copy(tracks = p.tracks.map { it.copy(notes = NoteClip(notes = listOf(Note(0, 48, 40)))) })
        val bassCh = InstrumentTrack.mixerChannel(1)
        return p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, c ->
            if (i == bassCh) c.copy(inserts = listOf(Insert(EffectType.COMPRESSOR.ordinal, EffectType.COMPRESSOR.defaults)) + c.inserts.drop(1), sends = listOf(0.3f, 0f)) else c
        }))
    }

    @Test
    fun aKeptSoundComesBackWithItsEffectsNamedAndNeverTwice() {
        val vm = open(song())
        vm.selectTrack(1)
        waitUntil("the bass shown") { vm.selectedTrack.value == 1 }
        vm.saveSynthFavorite("Ma basse")
        val kept = vm.favorites.value.synth.last()
        assertEquals("Ma basse", kept.name)
        assertEquals("its effects kept with it", EffectType.COMPRESSOR, kept.fx?.inserts?.first()?.effect)
        assertTrue(vm.notice.value!!.contains("avec ses effets"))
        // The same again: said, nothing new.
        val count = vm.favorites.value.synth.size
        vm.saveSynthFavorite()
        assertEquals("kept twice the same", count, vm.favorites.value.synth.size)
        assertTrue(vm.notice.value!!, vm.notice.value!!.contains("déjà dans tes favoris") && vm.notice.value!!.contains("Ma basse"))
        assertEquals("Ma basse", vm.keptSynthName(1))
        // On the lead (no effects): its sound and its effects, one undo step.
        vm.selectTrack(2)
        waitUntil("the lead shown") { vm.selectedTrack.value == 2 }
        val lead = InstrumentTrack.mixerChannel(2)
        val before = vm.project.value
        vm.applySynthFavorite(vm.favorites.value.synth.indexOfFirst { it.name == "Ma basse" })
        assertEquals(kept.sound, vm.project.value.track(2).synth.sanitized())
        assertEquals("the effects back with the sound", EffectType.COMPRESSOR, vm.project.value.mixer.channels[lead].inserts[0].effect)
        assertEquals(0.3f, vm.project.value.mixer.channels[lead].sends[0], 0f)
        assertEquals("its volume stays the track's", before.mixer.channels[lead].volume, vm.project.value.mixer.channels[lead].volume, 0f)
        vm.undo()
        assertEquals("one undo takes sound and effects", before.mixer.channels[lead], vm.project.value.mixer.channels[lead])
        assertEquals(before.track(2).synth, vm.project.value.track(2).synth)
        // Renamed; a name another kept sound has is refused, said.
        val i = vm.favorites.value.synth.indexOfFirst { it.name == "Ma basse" }
        vm.renameSynthFavorite(i, "  Basse ronde ")
        assertEquals("Basse ronde", vm.favorites.value.synth[i].name)
        vm.selectTrack(2)
        vm.saveSynthFavorite("Lead")
        vm.renameSynthFavorite(vm.favorites.value.synth.indexOfFirst { it.name == "Lead" }, "Basse ronde")
        assertTrue(vm.favorites.value.synth.any { it.name == "Lead" })
        assertTrue(vm.notice.value!!.contains("déjà"))
    }

    /** Relecture tour 1 (mineur): its effects past the long effects' limit: the kept sound is refused whole, said. */
    @Test
    fun aKeptSoundWhoseEffectsAreRefusedLeavesTheTrackAsItIs() {
        val made = song().let { p -> p.copy(tracks = p.tracks.map { if (it.id == 1) it.copy(synth = it.synth.with(0, 0.91f)) else it }) }
        val vm = open(made)
        vm.selectTrack(1)
        waitUntil("the bass shown") { vm.selectedTrack.value == 1 }
        val bassCh = InstrumentTrack.mixerChannel(1)
        val leadCh = InstrumentTrack.mixerChannel(2)
        vm.setInsertType(bassCh, 1, EffectType.ECHO)
        vm.saveSynthFavorite("Basse écho")
        val limit = EffectType.longLimit(EffectType.ECHO)
        val chans = vm.project.value.mixer.channels.indices.filter { it != leadCh && it != bassCh }
        loop@ for (ch in chans) for (slot in 0 until com.pocketstudio.model.Mixer.INSERTS) {
            if (vm.project.value.mixer.longFxCount(EffectType.ECHO) >= limit) break@loop
            vm.setInsertType(ch, slot, EffectType.ECHO)
        }
        assertEquals(limit, vm.project.value.mixer.longFxCount(EffectType.ECHO))
        vm.dismissNotice()
        vm.selectTrack(2)
        waitUntil("the lead shown") { vm.selectedTrack.value == 2 }
        val before = vm.project.value
        assertTrue("a sound of its own", before.track(2).synth.sanitized() != vm.favorites.value.synth.first { it.name == "Basse écho" }.sound)
        vm.applySynthFavorite(vm.favorites.value.synth.indexOfFirst { it.name == "Basse écho" })
        assertEquals("the lead's sound stays", before.track(2).synth, vm.project.value.track(2).synth)
        assertEquals(before.mixer.channels[leadCh], vm.project.value.mixer.channels[leadCh])
        assertEquals(com.pocketstudio.model.longFxLimitNotice(EffectType.ECHO), vm.notice.value)
    }
}
