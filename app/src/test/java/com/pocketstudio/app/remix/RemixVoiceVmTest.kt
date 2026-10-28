package com.pocketstudio.app.remix

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.rhythm.SampleTask
import com.pocketstudio.app.rhythm.kitName
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.RemixVoice
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.songStepOf
import com.pocketstudio.model.remixEndBars
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Duration
import kotlin.math.sin

/**
 * Remix lot 1, R2, through the ViewModel with an engine stand-in that holds a separated song (a
 * voice singing three phrases between silences, 120 BPM): the song comes in where it is placed
 * (live from the song's start or from the drop, and in the song's export), « → Pad » takes a free
 * pad and never the kick (asking when every pad is used), « Découper la voix en phrases » puts each
 * phrase on its own free pad in one undo step, and « Voix » alone stays alone in the project.
 */
@RunWith(RobolectricTestRunner::class)
class RemixVoiceVmTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val store = ProjectStore(File(app.filesDir, "projects"))

    /** The song: 20 s at 48 kHz, the voice (stem 3) sings 2-4 s phrases from 2 s, 8 s and 14 s. */
    private class Sink : CommandSink {
        private var next = 100L
        val frames = 48_000L * 20
        val phrases = listOf(2.0 to 4.5, 8.0 to 11.0, 14.0 to 16.0)
        /** The last stem start (frame) each engine got (0: the live one). */
        val stemStart = HashMap<Long, Int>()
        val extracted = ArrayList<Pair<Long, Long>>()

        fun voice(frame: Long): Float {
            val t = frame / 48_000.0
            return if (phrases.any { (a, b) -> t >= a && t < b }) (0.5 * sin(2 * Math.PI * 220 * t)).toFloat() else 0f
        }

        @Synchronized override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) {
            if (type == 32) stemStart[target] = a  // CMD_STEM_START
        }
        override fun scheduleFrom(target: Long, frame: Long) = Unit
        override fun install(target: Long, kind: CommandSink.Install, index: Int, sample: Long) = Unit
        @Synchronized override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long = next++
        override fun sampleFrames(handle: Long): Long = 4800L
        override fun sampleRelease(handle: Long) = Unit
        @Synchronized override fun exportCreate(): Long = next++
        override fun exportRender(target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean, sampleRate: Int, bits: Int): ExportResult =
            ExportResult.Cancelled
        override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray = FloatArray(0)
        override fun exportDestroy(target: Long) = Unit

        override fun stemsLoad(paths: Array<String>): Long = 7L
        override fun stemsFrames(handle: Long): Long = frames
        @Synchronized override fun stemsExtract(handle: Long, stem: Int, start: Long, count: Long): FloatArray {
            extracted += start to count
            val n = maxOf(0L, minOf(count, frames - start)).toInt()
            return FloatArray(n * 2) { i -> if (stem == 3) voice(start + i / 2) else 0.1f }
        }
        override fun stemsPeaks(handle: Long, stem: Int, buckets: Int): FloatArray = FloatArray(buckets) { k ->
            val from = frames * k / buckets
            val to = frames * (k + 1) / buckets
            var peak = 0f
            var f = from
            while (f < to) { peak = maxOf(peak, kotlin.math.abs(if (stem == 3) voice(f) else 0.1f)); f += 24 }
            peak
        }
        override fun stemsStretched(handle: Long, ratio: Double): Long = 8L
        override fun stemsRelease(handle: Long): Boolean = true
        override fun stemPosition(): Long = -1L
    }

    private val sink = Sink()

    @After
    fun tearDown() {
        NativeEngine.sink = null
        com.pocketstudio.app.projects.CurrentProject.set(app, RhythmViewModel.PROJECT_ID)
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))

    private fun waitUntil(what: String, check: () -> Boolean) {
        val until = System.currentTimeMillis() + 60_000
        while (!check()) {
            assertTrue(what, System.currentTimeMillis() < until)
            idle()
            Thread.sleep(5)
        }
    }

    /** A separated song on the phone, as the separation leaves it. */
    private fun stemSet(): StemSet {
        val dir = StemStore.dirFor(app, "song-r2")
        dir.mkdirs()
        listOf("drums", "bass", "other", "vocals").forEach { File(dir, "$it.wav").writeBytes(ByteArray(44)) }
        return StemStore.commit(dir, "Ma chanson", 20.0, SongInfo(bpm = 120.0, firstBeat = 0.0, root = 9, minor = true))
    }

    /** Intro (8 bars, the kick on pad 1 and the snare on pad 3), Drop (8 bars, the same beat). */
    private fun beat(): Project {
        val drums = DrumPattern(steps = 16).toggle(0, 0).toggle(0, 8).toggle(2, 4).toggle(2, 12)
        return Project(
            tempoBpm = 120.0, drums = drums,
            scenes = listOf(Scene("A", drums, label = "Intro"), Scene("B", drums, label = "Drop")), scene = 0,
            song = listOf(SongPart(0, repeats = 8), SongPart(1, repeats = 8)),
        ).sanitized()
    }

    private fun open(p: Project): RhythmViewModel {
        val id = "remix-voice-${System.nanoTime()}"
        store.save(id, p.copy(name = id))
        com.pocketstudio.app.projects.CurrentProject.set(app, id)
        NativeEngine.sink = sink
        val vm = RhythmViewModel(app)
        waitUntil("the project never opened") { vm.project.value.name == id }
        return vm
    }

    private fun withSong(vm: RhythmViewModel, heard: List<Boolean>? = null): StemSet {
        val set = stemSet()
        vm.useSong(set, heard)
        waitUntil("the stems never loaded") { vm.stemOverview.value != null }
        return set
    }

    @Test
    fun theVoiceComesInOnTheDropLiveAndInTheExport() {
        val vm = open(beat())
        withSong(vm)
        val drop = vm.project.value.songStepOf(1)
        assertEquals(8 * 16, drop)
        vm.setRemixAt(drop)
        assertEquals(drop, vm.project.value.remix.atStep)
        // One undo step for the place.
        vm.undo()
        assertEquals(0, vm.project.value.remix.atStep)
        vm.setRemixAt(drop)
        // ▶ from the song's start: 8 bars (16 s at 120 BPM) of silence before the song's first frame.
        vm.playSongPart(0)
        assertEquals(-16 * 48_000, sink.stemStart[0L])
        vm.togglePlay()
        // From the drop: the song's first frame at once.
        vm.playSongPart(1)
        assertEquals(0, sink.stemStart[0L])
        vm.togglePlay()
        // The drop looped on its own (« ⟳ Partie »): heard as in the song, from the song's start.
        vm.selectScene(1)
        vm.setLoopPart(true)
        vm.togglePlay()
        assertEquals(0, sink.stemStart[0L])
        vm.togglePlay()
        // The intro looped on its own: the song waits for the drop (it never comes in the loop's 8 bars).
        vm.selectScene(0)
        vm.togglePlay()
        assertEquals(-16 * 48_000, sink.stemStart[0L])
        vm.togglePlay()
        // The whole song's export: the same 16 s of silence, then the voice.
        vm.shareSong(com.pocketstudio.app.rhythm.ExportFormat.Wav)
        waitUntil("the song was never described to an export engine") { sink.stemStart.keys.any { it != 0L } }
        assertEquals(-16 * 48_000, sink.stemStart.filterKeys { it != 0L }.values.last())
        // And it lasts to the song's end: 8 bars after the drop's start (20 s = 10 bars).
        assertEquals(18, vm.project.value.remixEndBars())
    }

    @Test
    fun aCutNeverCoversTheKick() {
        val vm = open(beat())
        withSong(vm)
        vm.selectPad(0)  // the kick, selected
        vm.setRemixStartBar(1)
        vm.stemToPad(3, 2)
        waitUntil("the cut never landed") { vm.remixCut.value != null }
        val p = vm.project.value
        // The kick and the snare are as they were; the cut is on pad 2, the first one the beat never plays.
        assertNull(p.drums.lanes[0].sample)
        assertEquals(2, p.drums.lanes[0].hits.size)
        assertNull(p.drums.lanes[2].sample)
        val cut = p.drums.lanes[1].sample
        assertNotNull(cut)
        assertEquals("Voix 1", cut!!.name)
        assertEquals(RemixVoice.CUT_RELEASE, cut.release)
        assertEquals(1, vm.transport.value.selectedPad)
        assertEquals(1, vm.remixCut.value!!.lane)
        assertTrue(vm.remixCut.value!!.message, vm.remixCut.value!!.message.contains("pad 2"))
        // Two bars from bar 2 of the song.
        assertEquals(96_000L to 192_000L, sink.extracted.last())
        // The kit keeps its name (no « Kit : Voix »).
        assertEquals(kitName(beat()), kitName(p))
        // A second cut: the next free pad, the next number.
        vm.stemToPad(3, 1)
        waitUntil("the second cut never landed") { vm.project.value.drums.lanes[3].sample != null }
        assertEquals("Voix 2", vm.project.value.drums.lanes[3].sample!!.name)
        // Its start, end and fade, from the Remix page.
        vm.editCut("start") { it.copy(start = 0.25f) }
        vm.editCut(null) { it.copy(attack = 0.05f, release = 0.3f) }
        val trimmed = vm.project.value.drums.lanes[3].sample!!
        assertEquals(0.25f, trimmed.start)
        assertEquals(0.3f, trimmed.release)
    }

    @Test
    fun everyPadUsedAsksFirst() {
        var p = beat()
        var d = p.drums
        for (lane in 0 until DrumPattern.PAD_COUNT) if (d.lanes[lane].hits.isEmpty()) d = d.toggle(lane, 15)
        p = p.copy(drums = d)
        val vm = open(p)
        withSong(vm)
        vm.selectPad(0)
        vm.stemToPad(3, 1)
        idle()
        // Nothing done yet: the question.
        assertEquals(0, vm.stemPadAsk.value?.lane)
        assertNull(vm.project.value.drums.lanes[0].sample)
        vm.dismissStemPadAsk()
        assertNull(vm.stemPadAsk.value)
        assertNull(vm.project.value.drums.lanes[0].sample)
        // « Remplacer »: on the selected pad after all.
        vm.stemToPad(3, 1)
        vm.confirmStemPad()
        waitUntil("the cut never landed") { vm.project.value.drums.lanes[0].sample != null }
        assertEquals("Voix 1", vm.project.value.drums.lanes[0].sample!!.name)
        // And « Découper la voix en phrases » says why it cannot.
        vm.splitVoice()
        waitUntil("no word") { vm.sampleTask.value is SampleTask.Failed }
    }

    @Test
    fun theVoiceIsCutIntoItsPhrasesOnFreePads() {
        val vm = open(beat())
        withSong(vm)
        vm.selectPad(0)
        val depth = vm.project.value
        vm.splitVoice()
        waitUntil("the phrases never landed") { vm.remixCut.value != null }
        val lanes = vm.project.value.drums.lanes
        // Three phrases on pads 2, 4, 5 (pads 1 and 3 play the beat), named Voix 1..3.
        assertEquals(listOf("Voix 1", "Voix 2", "Voix 3"), listOf(1, 3, 4).map { lanes[it].sample?.name })
        assertNull(lanes[0].sample)
        assertNull(lanes[2].sample)
        assertNull(lanes[5].sample)
        // Each cut holds its phrase: from just before it starts to just after it ends.
        val cuts = sink.extracted.takeLast(3)
        sink.phrases.zip(cuts).forEach { (phrase, cut) ->
            val (from, to) = phrase
            val start = cut.first / 48_000.0
            val end = (cut.first + cut.second) / 48_000.0
            assertTrue("$phrase -> $start..$end", start in (from - 0.1)..from && end in to..(to + 0.2))
        }
        assertTrue(vm.remixCut.value!!.message, vm.remixCut.value!!.message.contains("3 phrases"))
        // One undo step takes them all away.
        vm.undo()
        assertEquals(depth.drums.lanes.map { it.sample }, vm.project.value.drums.lanes.map { it.sample })
    }

    @Test
    fun theVoiceAloneStaysAloneInTheProject() {
        val vm = open(beat())
        withSong(vm, heard = listOf(false, false, false, true))
        assertEquals(listOf(true, true, true, false), vm.project.value.remix.stems.map { it.mute })
        // « Vers le pad : 2 mes. » is kept while the user goes to the drums and back.
        vm.setRemixPadBars(2)
        assertEquals(2, vm.remixPadBars.value)
    }
}
