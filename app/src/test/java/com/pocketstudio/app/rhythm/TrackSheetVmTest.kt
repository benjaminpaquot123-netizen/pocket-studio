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
 * G3 through the ViewModel, with an engine stand-in: each layer gesture is a line of the track's
 * history in the same undo step (a drag one line); « Ramener » from the sheet puts the layer back
 * in the engine; a chain copied from the sheet reaches the engine's strip; a favourite is kept.
 */
@RunWith(RobolectricTestRunner::class)
class TrackSheetVmTest {
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

    private fun layerArg(track: Int, layer: Int, param: Int) = (param and 0xFF) or (layer shl 8) or (track shl 16)

    @Test
    fun eachLayerGestureIsALineOfTheHistoryAndRamenerReachesTheEngine() {
        val vm = open(song())
        assertTrue(vm.addLayer(1, LayerSound.synth(SynthPatch()), LayerRole.AIR, "Pad doux"))
        for (db in listOf(-7f, -8f, -9f)) vm.setLayer(1, 0, key = "gain", gainDb = db)
        val history = vm.project.value.guide.historyOf(GuideTrack.inst(1))
        // Added, then one drag: two lines, by hand.
        assertEquals(2, GuideHistory.lines(history).size)
        assertTrue(history.all { it.origin == GuideChange.USER })
        // One undo takes the drag and its line together.
        vm.undo()
        assertEquals(1, GuideHistory.lines(vm.project.value.guide.historyOf(GuideTrack.inst(1))).size)
        vm.removeLayer(1, 0)
        assertTrue(vm.project.value.layersOf(1).isEmpty())
        // « Ramener » the removal (the sheet's line): the layer back in the song and in the engine.
        val removal = GuideHistory.lines(vm.project.value.guide.historyOf(GuideTrack.inst(1))).first()
        sink.clear()
        vm.guide.revert(removal.id)
        waitUntil("brought back") { vm.project.value.layersOf(1).size == 1 }
        waitUntil("in the engine") { sink.live(140).any { it.a == layerArg(1, 0, NativeEngine.LAYER_ON) && it.value == 1f } }
        assertTrue(sink.live(140).any { it.a == layerArg(1, 0, NativeEngine.LAYER_TRANSPOSE) && it.value == 12f })
        // And one undo takes it away again.
        vm.undo()
        assertTrue(vm.project.value.layersOf(1).isEmpty())
    }

    @Test
    fun aChainCopiedFromTheSheetReachesTheEngineAndAFavouriteIsKept() {
        val vm = open(song())
        sink.clear()
        vm.copyChain(GuideTrack.inst(1), GuideTrack.inst(2))
        val lead = InstrumentTrack.mixerChannel(2)
        assertEquals(EffectType.COMPRESSOR, vm.project.value.mixer.channels[lead].inserts[0].effect)
        assertEquals(0.3f, vm.project.value.mixer.channels[lead].sends[0], 0f)
        // CMD_INSERT_TYPE (33) on the lead's first slot: the compressor.
        assertTrue(sink.live(33).any { it.a == lead * NativeEngine.INSERTS && it.b == EffectType.COMPRESSOR.ordinal })
        assertTrue(vm.notice.value!!.contains("Chaîne copiée"))
        // Copied again: refused, said, nothing new.
        val now = vm.project.value
        vm.copyChain(GuideTrack.inst(1), GuideTrack.inst(2))
        assertEquals(now, vm.project.value)
        assertTrue(vm.notice.value!!.contains("même chaîne"))
        // « Garder ce son dans mes favoris »: the bass's synth sound, kept.
        val before = vm.favorites.value.synth.size
        vm.saveFavoriteOf(1)
        assertEquals(before + 1, vm.favorites.value.synth.size)
        assertEquals(vm.project.value.tracks.first { it.id == 1 }.synth.sanitized(), vm.favorites.value.synth.last().sound)
    }
}
