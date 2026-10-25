package com.pocketstudio.app.rhythm

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.bench.RecordingSink
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.LayerRole
import com.pocketstudio.model.LayerSound
import com.pocketstudio.model.Layers
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.PadVoice
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.SynthPatch
import com.pocketstudio.model.addLayer
import com.pocketstudio.model.addTrack
import com.pocketstudio.model.layersOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.After
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
import kotlin.math.sqrt

/**
 * G1b: a track's layers through the ViewModel. Pushed on load and on each edit (one undo step per
 * layer action), heard alone or without live (the listening mask), in every export (the same sync
 * code: a song's, a frozen track's, the « avec / sans » render, loudness-matched), and in what the
 * live bench records.
 */
@RunWith(RobolectricTestRunner::class)
class LayersVmTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    private class Post(val target: Long, val type: Int, val value: Float, val a: Int, val b: Int)

    private class Sink : CommandSink {
        val posts = ArrayList<Post>()
        val installs = ArrayList<Triple<CommandSink.Install, Int, Long>>()  // live only: kind, index, sample
        @Volatile var renders = 0
        private var next = 1L
        @Synchronized override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) {
            posts += Post(target, type, value, a, b)
        }
        override fun scheduleFrom(target: Long, frame: Long) = Unit
        @Synchronized override fun install(target: Long, kind: CommandSink.Install, index: Int, sample: Long) {
            if (target == 0L) installs += Triple(kind, index, sample)
        }
        @Synchronized override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long = next++
        override fun sampleFrames(handle: Long): Long = 0L
        override fun sampleRelease(handle: Long) = Unit
        @Synchronized override fun exportCreate(): Long = 1000L + next++
        override fun exportRender(target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean, sampleRate: Int, bits: Int) =
            ExportResult.Ok
        // Each render louder than the one before (the « avec » 0.4, the « sans » 0.1): matched after.
        @Synchronized override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray {
            val amp = if (renders++ % 2 == 0) 0.4f else 0.1f
            return FloatArray(2 * 48_000) { amp * kotlin.math.sin(it * 0.03f) }
        }
        override fun exportDestroy(target: Long) = Unit
        @Synchronized fun of(type: Int, live: Boolean = true) = posts.filter { it.type == type && (it.target == 0L) == live }
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

    private fun open(p: Project, with: CommandSink = sink): RhythmViewModel {
        val id = "layers-${System.nanoTime()}"
        ProjectStore(File(app.filesDir, "projects")).save(id, p)
        com.pocketstudio.app.projects.CurrentProject.set(app, id)
        NativeEngine.sink = with
        val vm = RhythmViewModel(app)
        waitUntil("the project never opened") { vm.project.value.tracks.size == p.tracks.size && vm.project.value.song.size == p.song.size }
        return vm
    }

    /** A bass on track 1 (notes, a song) with a sub layer (the 808, an octave down) and an air layer. */
    private fun layered(): Project {
        var p = Project(song = listOf(SongPart(0))).addTrack("Basse")!!
        p = p.copy(tracks = p.tracks.map { it.copy(notes = NoteClip(notes = listOf(Note(0, 48, 40), Note(48, 48, 43)))) })
        p = p.addLayer(1, LayerSound.bass808(PadVoice(PadVoice.BASS_808)), LayerRole.SUB)!!
        p = p.addLayer(1, LayerSound.synth(SynthPatch()), LayerRole.AIR)!!
        return p.copy(tracks = p.tracks.map { t -> t.copy(layers = t.layers.mapIndexed { i, l -> if (i == 0) l.copy(transpose = -12, gainDb = -2f) else l }) })
    }

    private fun layerArg(track: Int, layer: Int, param: Int) = (param and 0xFF) or (layer shl 8) or (track shl 16)

    @Test
    fun aProjectsLayersArePushedOnLoad() {
        // The stream starts (the app then sends the whole project).
        open(layered()).engineStarted()
        val on = sink.of(140).filter { it.a == layerArg(1, 0, NativeEngine.LAYER_ON) || it.a == layerArg(1, 1, NativeEngine.LAYER_ON) }
        assertTrue("both layers on", on.count { it.value == 1f } >= 2)
        // The sub's sound on its own sound index: the 808 (source 4), its transposition and gain.
        val sub = NativeEngine.layerSound(1, 0)
        assertTrue(sink.of(42).any { it.a == ((sub shl 16) or InstrumentTrack.ENGINE_808) })
        assertTrue(sink.of(140).any { it.a == layerArg(1, 0, NativeEngine.LAYER_TRANSPOSE) && it.value == -12f })
        assertTrue(sink.of(140).any { it.a == layerArg(1, 0, NativeEngine.LAYER_GAIN) && kotlin.math.abs(it.value - Layers.linear(-2f)) < 1e-5f })
        // The sub joins after the bass's effects; the air goes through them.
        assertTrue(sink.of(140).any { it.a == layerArg(1, 0, NativeEngine.LAYER_THROUGH) && it.value == 0f })
        assertTrue(sink.of(140).any { it.a == layerArg(1, 1, NativeEngine.LAYER_THROUGH) && it.value == 1f })
        // The air's synth settings to its sound index (8 + 1 * 3 + 1 = 12).
        assertEquals(12, NativeEngine.layerSound(1, 1))
        assertTrue(sink.of(18).any { (it.a shr 16) == 12 })
        // The third layer slot and the other tracks' are off; every track heard as mixed.
        assertTrue(sink.of(140).any { it.a == layerArg(1, 2, NativeEngine.LAYER_ON) && it.value == 0f })
        assertTrue(sink.of(141).any { it.a == 1 && it.value == NativeEngine.LISTEN_ALL.toFloat() })
    }

    @Test
    fun eachLayerActionIsOneUndoStepAndReachesTheEngine() {
        val vm = open(layered())
        sink.posts.clear()
        // A drag of the gain: many values, one undo step; only the gain is sent.
        for (db in listOf(-3f, -4f, -5f, -6f)) vm.setLayer(1, 1, key = "gain", gainDb = db)
        assertEquals(-6f, vm.project.value.layersOf(1)[1].gainDb, 0f)
        assertTrue(sink.of(140).all { it.a == layerArg(1, 1, NativeEngine.LAYER_GAIN) })
        assertEquals(4, sink.of(140).size)
        vm.undo()
        assertEquals(layered().layersOf(1)[1].gainDb, vm.project.value.layersOf(1)[1].gainDb, 0f)
        // Range, transposition, mute: one step each, sent as settings of the layer and its strip.
        sink.posts.clear()
        vm.setLayer(1, 0, low = 30, high = 52)
        vm.setLayer(1, 0, mute = true)
        assertTrue(sink.of(140).any { it.a == layerArg(1, 0, NativeEngine.LAYER_LOW) && it.value == 30f })
        assertTrue(sink.of(22).any { it.a == NativeEngine.layerChannel(1, 0) && it.value == 1f })
        vm.undo()
        assertFalse(vm.project.value.layersOf(1)[0].mute)
        assertEquals(30, vm.project.value.layersOf(1)[0].low)
        vm.undo()
        assertEquals(0, vm.project.value.layersOf(1)[0].low)
        // Removed: one step; the stack sent again (the second layer slot now off), back with undo.
        sink.posts.clear()
        vm.removeLayer(1, 0)
        assertEquals(1, vm.project.value.layersOf(1).size)
        assertTrue(sink.of(140).any { it.a == layerArg(1, 1, NativeEngine.LAYER_ON) && it.value == 0f })
        vm.undo()
        assertEquals(2, vm.project.value.layersOf(1).size)
        // Added: one step; a fourth sound is refused, said.
        assertTrue(vm.addLayer(1, LayerSound.synth(SynthPatch()), LayerRole.BODY))
        assertEquals(3, vm.project.value.layersOf(1).size)
        assertFalse(vm.addLayer(1, LayerSound.synth(SynthPatch())))
        assertTrue(vm.notice.value!!.contains("Quatre sons"))
        vm.undo()
        assertEquals(2, vm.project.value.layersOf(1).size)
        assertFalse(vm.addLayer(6, LayerSound.synth(SynthPatch())))  // no such track
    }

    @Test
    fun aLayerHeardAloneOrWithoutLiveNeverInAnExport() {
        val vm = open(layered())
        sink.posts.clear()
        vm.soloLayer(1, 0)
        assertEquals(Layers.only(0), vm.listeningForTest(1))
        assertTrue(sink.of(141).any { it.a == 1 && it.value == 2f })
        vm.soloLayer(1, Layers.OWN)
        assertTrue(sink.of(141).last().value == 1f)
        vm.soloLayer(1, 1, without = true)
        assertEquals(Layers.without(1), sink.of(141).last().value.toInt())
        // An export made meanwhile hears every sound.
        sink.posts.clear()
        var ab: Pair<FloatArray, FloatArray>? = null
        CoroutineScope(Dispatchers.Main).launch { ab = vm.renderLayerAB(1, 0) }
        waitUntil("the « avec / sans » renders") { ab != null }
        val exports = sink.posts.filter { it.target != 0L }.map { it.target }.distinct()
        assertEquals(2, exports.size)
        // The first render with every sound, the layers on; the second without the sub.
        val first = sink.posts.filter { it.target == exports[0] }
        val second = sink.posts.filter { it.target == exports[1] }
        assertTrue(first.any { it.type == 140 && it.a == layerArg(1, 0, NativeEngine.LAYER_ON) && it.value == 1f })
        assertEquals(NativeEngine.LISTEN_ALL.toFloat(), first.last { it.type == 141 && it.a == 1 }.value)
        assertEquals(Layers.without(0).toFloat(), second.last { it.type == 141 && it.a == 1 }.value)
        // Loudness-matched: the louder one brought down to the other.
        fun rms(x: FloatArray) = sqrt(x.sumOf { it.toDouble() * it } / x.size)
        val (with, without) = ab!!
        assertEquals(rms(without), rms(with), 0.02 * rms(without))
        assertEquals(0.1 / sqrt(2.0), rms(without), 0.01)
        // Live listening still the one chosen; back to every sound.
        assertEquals(Layers.without(1), vm.listeningForTest(1))
        vm.soloLayer(1, null)
        assertEquals(NativeEngine.LISTEN_ALL, vm.listeningForTest(1))
    }

    @Test
    fun aFrozenTrackRendersItsLayers() {
        val vm = open(layered())
        sink.posts.clear()
        vm.freezeTrack(1)
        waitUntil("never frozen (${vm.notice.value})") { vm.freezing.value == null && com.pocketstudio.model.Freeze.isFrozen(vm.project.value, 1) }
        val exported = sink.posts.filter { it.target != 0L }
        assertTrue("the freeze's export plays the layers", exported.any { it.type == 140 && it.a == layerArg(1, 1, NativeEngine.LAYER_ON) && it.value == 1f })
        // Its signature follows the layers: a layer changed, the frozen sound is stale.
        assertFalse(com.pocketstudio.model.Freeze.isStale(vm.project.value, 1))
        vm.setLayer(1, 1, gainDb = -9f)
        assertTrue(com.pocketstudio.model.Freeze.isStale(vm.project.value, 1))
    }

    @Test
    fun aLayersMultizoneInstrumentReachesItOnceRead() {
        var p = layered()
        p = p.addLayer(1, LayerSound.multi(com.pocketstudio.model.MultiPatch("pack-l", "Piano")))!!
        val vm = open(p)
        // The stream starts (the whole project sent, its instruments looked for): their first reading
        // ends (none on this phone) before the instrument appears.
        vm.engineStarted()
        waitUntil("the instruments never stopped loading") { vm.multiLoading.value.isEmpty() }
        val sound = NativeEngine.layerSound(1, 2)
        val manifest = com.pocketstudio.model.PackManifest("pack-l", "Piano",
            instrument = com.pocketstudio.model.MultiInstrument(memoryBytes = 1_000L, zones = listOf(com.pocketstudio.model.MultiZone("z.wav"))))
        sink.posts.clear()
        vm.multiManifest = { _, _ -> manifest }
        vm.multiLoader = { _, pack ->
            com.pocketstudio.app.instruments.MultiBank(pack, 1, manifest.instrument!!, listOf(com.pocketstudio.engine.NativeSample.createCompact(FloatArray(96), 1, 48_000)!!))
        }
        vm.instrumentsChanged()
        waitUntil("the layer's instrument never reached it") { synchronized(sink) { sink.posts.any { it.type == 130 && (it.a shr 16) == sound && it.b == 0 } } }
        assertTrue(synchronized(sink) { sink.installs.any { it.first == CommandSink.Install.TrackSample && it.second == (sound or (1 shl 8)) && it.third != 0L } })
        // The layer removed: its zones let go in the engine (MultiSampler ClearZones), not kept for the next project.
        synchronized(sink) { sink.posts.clear() }
        vm.removeLayer(1, 2)
        assertTrue(sink.of(130).any { (it.a shr 16) == sound && (it.a and 0xFFFF) == 6 && it.b == -1 })
    }

    @Test
    fun aLayerGoneLetsGoOfItsSoundsAndItsListening() {
        var p = layered()
        p = p.addLayer(1, LayerSound.sample(com.pocketstudio.model.SampledPatch(file = "missing.wav", name = "S")))!!
        val vm = open(p)
        vm.engineStarted()
        vm.soloLayer(1, 2)
        assertEquals(Layers.only(2), vm.listeningForTest(1))
        synchronized(sink) { sink.posts.clear(); sink.installs.clear() }
        vm.removeLayer(1, 2)
        val sound = NativeEngine.layerSound(1, 2)
        // Its sample let go, its strip emptied.
        assertTrue(sink.installs.any { it.first == CommandSink.Install.TrackSample && it.second == sound && it.third == 0L })
        assertTrue(sink.of(68).any { it.a == NativeEngine.layerChannel(1, 2) })
        assertEquals(NativeEngine.LISTEN_ALL, vm.listeningForTest(1))
        // Undone: the layer back, heard as mixed (the mask let go: it would land on another layer).
        vm.soloLayer(1, 0)
        vm.undo()
        assertEquals(3, vm.project.value.layersOf(1).size)
        assertEquals(NativeEngine.LISTEN_ALL, vm.listeningForTest(1))
    }

    @Test
    fun theLiveBenchRecordsTheLayers() {
        val dir = File(app.cacheDir, "bench-${System.nanoTime()}")
        val rec = RecordingSink(File(dir, "samples"), null)
        val live = StringBuilder()
        rec.live = live
        val vm = open(layered(), rec)
        assertNotNull(vm)
        vm.engineStarted()
        // ps_live replays these lines through the engine as the phone gets them.
        assertTrue(live.lines().any { it.startsWith("C 140 1.0 ${layerArg(1, 0, NativeEngine.LAYER_ON)} ") })
        assertTrue(live.lines().any { it.startsWith("C 42 0.0 ${(NativeEngine.layerSound(1, 0) shl 16) or InstrumentTrack.ENGINE_808} ") })
        dir.deleteRecursively()
    }
}
