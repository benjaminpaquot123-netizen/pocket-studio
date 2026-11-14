package com.pocketstudio.app.journey

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.midi.KnobTarget
import com.pocketstudio.app.midi.rhythmKnobHost
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.record.TakeSource
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.Scene
import com.pocketstudio.model.allScenes
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Duration

/**
 * Benjamin's n° 11, the rest (lot D): the old screens wired like the new Live. On each of Rythme,
 * Synthé (Clavier), Acid 303 and Structure, what the screen records (its « ● Enregistrer » take,
 * played from a MIDI controller as the MiniLab 3 would) or edits goes into the project, is one
 * « Annuler » away and back with « Rétablir », is still there once saved and opened again, and
 * changes what the export renders (undone: the export is as before). The microphone's take of
 * Pistes audio needs a device (Benjamin's list); its clip is checked the same way here.
 */
@RunWith(RobolectricTestRunner::class)
class OldScreensLikeLiveTest {
    private val app: Application get() = ApplicationProvider.getApplicationContext()
    private val t = NoteClip.TICKS_PER_STEP

    /** In the engine's place during an export: the commands each export sends, its clips. */
    private class ExportSink : CommandSink {
        private var next = 1L
        private val sent = HashMap<Long, StringBuilder>()
        @Volatile var rendered = 0
        @Synchronized override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) {
            if (target != 0L) sent.getOrPut(target) { StringBuilder() }.append("C $type $value $a $b\n")
        }
        override fun scheduleFrom(target: Long, frame: Long) = Unit
        @Synchronized override fun install(target: Long, kind: CommandSink.Install, index: Int, sample: Long) {
            if (target != 0L) sent.getOrPut(target) { StringBuilder() }.append("I $kind $index ${sample != 0L}\n")
        }
        @Synchronized override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long = next++
        override fun sampleFrames(handle: Long): Long = 48_000L
        override fun sampleRelease(handle: Long) = Unit
        @Volatile var created = 0
        @Synchronized override fun exportCreate(): Long { created++; return next++ }
        override fun exportRender(target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean, sampleRate: Int, bits: Int): ExportResult {
            rendered++
            return ExportResult.Cancelled
        }
        override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray = FloatArray(0)
        override fun exportDestroy(target: Long) = Unit
        @Synchronized fun all(): String = sent.values.joinToString("|")

        /** The live engine's pattern step (a take of knob moves reads it). */
        @Volatile var step = -1
        override fun currentStep(): Int = step
        override fun playPosition(): Double = step.toDouble()
    }

    private var lastExport = ""

    @After
    fun end() {
        NativeEngine.sink = null
    }

    private fun waitFor(what: String, done: () -> Boolean) {
        val until = System.currentTimeMillis() + 20_000
        while (!done()) {
            assertTrue("never: $what ($lastExport)", System.currentTimeMillis() < until)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            Thread.sleep(5)
        }
    }

    /** The export running (its job, null once over: a new one can start). */
    private val jobField = RhythmViewModel::class.java.getDeclaredField("exportJob").apply { isAccessible = true }
    private fun exportJob(vm: RhythmViewModel): kotlinx.coroutines.Job? =
        (jobField.get(vm) as kotlinx.coroutines.Job?)?.takeIf { !it.isCompleted }

    /** What « Exporter » renders now (the loop, or the song when there is one): every command it sends. */
    private fun exported(vm: RhythmViewModel): String {
        val live = NativeEngine.sink
        val sink = ExportSink()
        NativeEngine.sink = sink
        try {
            vm.exportWav()
            waitFor("the export") {
                lastExport = "${sink.rendered} created=${sink.created} job=${exportJob(vm)} ${com.pocketstudio.app.report.AppLog.dump(4).replace("\n", " / ")}"
                sink.rendered > 0 && exportJob(vm) == null
            }
            return sink.all()
        } finally {
            NativeEngine.sink = live
        }
    }

    private fun open(p: Project): RhythmViewModel {
        val id = "old-" + System.nanoTime()
        ProjectStore(File(app.filesDir, "projects")).save(id, p.sanitized())
        CurrentProject.set(app, id)
        return RhythmViewModel(app).also { it.playingForTest = true }
    }

    private fun reopened(vm: RhythmViewModel): Project {
        kotlinx.coroutines.runBlocking { vm.flushNow() }
        return RhythmViewModel(app).project.value
    }

    /** Undo, redo, save and the export, the same way for every screen: [has] says the change is there. */
    private fun connected(screen: String, vm: RhythmViewModel, before: String, has: (Project) -> Boolean) {
        assertTrue("$screen: written", has(vm.project.value))
        val after = exported(vm)
        assertNotEquals("$screen: the export renders it", before, after)
        vm.undo()
        assertFalse("$screen: Annuler", has(vm.project.value))
        assertEquals("$screen: undone, the export as before", before, exported(vm))
        vm.redo()
        assertTrue("$screen: Rétablir", has(vm.project.value))
        assertTrue("$screen: saved and opened again", has(reopened(vm)))
    }

    private fun base() = Project(
        name = "Anciens écrans",
        scenes = listOf(Scene("A", DrumPattern().toggle(0, 0))),
        scene = 0, drums = DrumPattern().toggle(0, 0),
    )

    @Test
    fun rythmeTheControllersPadsInATake() {
        val vm = open(base())
        val before = exported(vm)
        vm.recordTake(TakeSource.PADS)
        vm.recordPositionForTest = 4.05
        vm.midiNote(9, 36 + 2, 0.7f, true)  // the MiniLab's 3rd pad: the snare
        vm.recordTake(TakeSource.PADS)
        vm.keepNoteTake()
        connected("Rythme", vm, before) { p -> p.drums.lanes[2].hits[4]?.velocity?.let { kotlin.math.abs(it - 0.7f) < 1e-3f } == true }
    }

    @Test
    fun clavierTheKeyboardsNotesInATake() {
        // A four-bar phrase already (a one-bar loop would first grow to four, an undo step of its own).
        val vm = open(base().copy(synthNotes = NoteClip(steps = 64)))
        vm.selectTrack(0)
        val before = exported(vm)
        vm.recordTake(TakeSource.KEYS)
        vm.recordPositionForTest = 2.0
        vm.midiNote(0, 67, 0.8f, true)
        vm.recordPositionForTest = 4.0
        vm.midiNote(0, 67, 0f, false)
        vm.recordTake(TakeSource.KEYS)
        vm.keepNoteTake()
        connected("Clavier", vm, before) { p -> p.synthNotes.notes.any { it.pitch == 67 && it.start == 2 * t && it.length == 2 * t } }
    }

    @Test
    fun acidTheControllersKnobInATake() {
        val vm = open(base())
        val before = exported(vm)
        val cutoff = AutoTarget.acid(0)
        // The 303's knobs are written where the engine plays (its pattern step).
        val engine = ExportSink().also { NativeEngine.sink = it }
        engine.step = 0
        vm.recordTake(TakeSource.ACID)
        engine.step = 1
        val host = rhythmKnobHost(vm)
        host.apply(KnobTarget(cutoff), 0.2f)
        engine.step = 9
        host.apply(KnobTarget(cutoff), 0.9f)
        vm.recordTake(TakeSource.ACID)
        vm.keepNoteTake()
        connected("Acid 303", vm, before) { p -> p.automation.any { it.target == cutoff } }
    }

    @Test
    fun structureAPartAddedAndTheControllersTransport() {
        val notes = NoteClip(notes = listOf(Note(0, 24, 60)))
        val vm = open(base().copy(scenes = listOf(Scene("A", DrumPattern().toggle(0, 0)), Scene("B", DrumPattern().toggle(1, 4), synthNotes = notes))))
        vm.addSongPart(0)
        vm.keepNoteTake()
        val before = exported(vm)
        vm.addSongPart(1)
        connected("Structure", vm, before) { p -> p.song.map { it.scene } == listOf(0, 1) }
        // The controller's Start and Stop drive the transport there too.
        vm.playingForTest = false
        vm.midiStart(fromTop = true)
        assertTrue(vm.transport.value.playing)
        vm.togglePlay()
        assertFalse(vm.transport.value.playing)
        assertEquals(2, vm.project.value.allScenes.size)
    }
}
