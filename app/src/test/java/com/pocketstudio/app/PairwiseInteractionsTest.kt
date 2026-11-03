package com.pocketstudio.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File

/**
 * All the pairs of [Pairwise.FACTORS] in as few cases as the greedy « all pairs » generator finds. */
object Pairwise {
    enum class Transport { STOPPED, PLAYING, RECORDING }
    enum class Mode { SIMPLE, PRO }
    enum class Event { TEMPO, UNDO, MODE_SWITCH, LANG_SWITCH, MIDI_UNPLUGGED, REOPEN, FOCUS_LOST, UNDO_DURING_SHOW_ME, EDIT_DURING_SHOW_ME }

    val FACTORS: List<List<Any>> = listOf(Transport.entries, Mode.entries, listOf(Lang.FR, Lang.EN, Lang.AR), Event.entries)

    /** Greedy all-pairs (the AETG idea): each new case covers the most pairs still uncovered. */
    fun cases(factors: List<List<Any>> = FACTORS): List<List<Any>> {
        val uncovered = HashSet<List<Any>>()
        for (i in factors.indices) for (j in i + 1 until factors.size) for (a in factors[i]) for (b in factors[j]) uncovered += listOf(i, a, j, b)
        val out = ArrayList<List<Any>>()
        fun pairsOf(c: List<Any>) = buildList { for (i in c.indices) for (j in i + 1 until c.size) add(listOf(i, c[i], j, c[j])) }
        // Every candidate of the full product scored at each round: small factors, so cheap and deterministic.
        val all = factors.fold(listOf(emptyList<Any>())) { acc, f -> acc.flatMap { c -> f.map { c + it } } }
        while (uncovered.isNotEmpty()) {
            val best = all.maxBy { c -> pairsOf(c).count { it in uncovered } }
            uncovered -= pairsOf(best).toSet()
            out += best
        }
        return out
    }
}

/**
 * Audit stage 2, point 3 (« interactions deux à deux »): the app's states (stopped, playing,
 * recording) × the mode × the language × what happens (a tempo change while recording, an undo, Simple
 * ↔ Pro while playing, the language changed with the project open, the MIDI keyboard unplugged again
 * and again, the app rebuilt (a rotation, the app killed), a call taking the sound, an undo or a touch
 * during « Montre-moi »). The pairs generator picks the cases (each pair of values in at least one).
 * Each case checks what must hold: nothing lost, nothing changed that should not, the state consistent.
 * Remix with Live (LiveRemixCellTest), rotation during the cut (CutJourneyTest) and a call during an
 * export ([callDuringAnExportLetsItFinish]) are the pairs of other screens.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
class PairwiseInteractionsTest(
    private val transport: Pairwise.Transport,
    private val mode: Pairwise.Mode,
    private val lang: Lang,
    private val event: Pairwise.Event,
) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "pair{index}")
        fun cases(): List<Array<Any>> = Pairwise.cases().map { it.toTypedArray() }
    }

    private val app: Application = ApplicationProvider.getApplicationContext()

    @After
    fun end() {
        SettingsStore.forgetForTests()
        I18n.lang = Lang.FR
    }

    private fun idle() = shadowOf(android.os.Looper.getMainLooper()).idle()

    private fun RhythmViewModel.hits(lane: Int) = project.value.allScenes[project.value.scene].drums.lanes[lane].hits.keys

    /** Scenes A, B (a kick on 0; 0 and 4), the song A B, opened as the app opens a project. */
    private fun open(): RhythmViewModel {
        File(app.filesDir, "projects").deleteRecursively()
        com.pocketstudio.app.projects.CurrentProject.set(app, "pairs-" + System.nanoTime())
        SettingsStore.forgetForTests()
        SettingsStore.update(app) { it.copy(guideSeen = true, simpleMode = mode == Pairwise.Mode.SIMPLE, language = lang) }
        val vm = RhythmViewModel(app)
        vm.toggleHit(0, 0)
        vm.duplicateScene()
        vm.toggleHit(0, 4)
        vm.selectScene(0)
        listOf(0, 1).forEach(vm::addSongPart)
        when (transport) {
            Pairwise.Transport.STOPPED -> {}
            Pairwise.Transport.PLAYING -> { vm.playingForTest = true; vm.togglePlay() }
            Pairwise.Transport.RECORDING -> { vm.playingForTest = true; vm.togglePlay(); vm.toggleRecord() }
        }
        idle()
        return vm
    }

    private fun stopRequest(why: String) {
        val f = com.pocketstudio.app.playback.PlaybackService::class.java.getDeclaredField("_stopRequests").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        (f.get(null) as kotlinx.coroutines.flow.MutableSharedFlow<String>).tryEmit(why)
    }

    @Test
    fun theyGoTogether() {
        val vm = open()
        val recording = transport == Pairwise.Transport.RECORDING
        val playing = vm.transport.value.playing
        assertEquals(recording, vm.recording.value)
        val before = vm.project.value
        when (event) {
            Pairwise.Event.TEMPO -> {
                vm.setTempo(before.tempoBpm + 7)
                idle()
                assertEquals(before.tempoBpm + 7, vm.project.value.tempoBpm, 1e-9)
                assertEquals("a tempo change keeps the recording going", recording, vm.recording.value)
                if (recording) {
                    // A hit played after the change still lands in the take.
                    vm.recordPositionForTest = 6.0
                    vm.hitPad(7)
                    assertEquals(setOf(6), vm.hits(7))
                    vm.toggleRecord()
                    assertFalse(vm.recording.value)
                }
                vm.undo()
                if (!recording) assertEquals(before.tempoBpm, vm.project.value.tempoBpm, 1e-9)
            }
            Pairwise.Event.UNDO -> {
                vm.toggleHit(5, 3)
                val edited = vm.project.value
                vm.undo()
                // While recording, undo waits for the take to end (one step for the whole take).
                if (recording) assertEquals(edited, vm.project.value) else assertFalse(3 in vm.hits(5))
                assertEquals(recording, vm.recording.value)
            }
            Pairwise.Event.MODE_SWITCH -> {
                SettingsStore.update(app) { it.copy(simpleMode = mode != Pairwise.Mode.SIMPLE) }
                idle()
                assertEquals("Simple ↔ Pro only changes how the project is shown", before, vm.project.value)
                assertEquals(playing, vm.transport.value.playing)
                assertEquals(recording, vm.recording.value)
            }
            Pairwise.Event.LANG_SWITCH -> {
                val other = if (lang == Lang.FR) Lang.DE else Lang.FR
                SettingsStore.update(app) { it.copy(language = other) }
                idle()
                assertEquals(other, I18n.lang)
                assertEquals("the language changes nothing in the project", before, vm.project.value)
                assertEquals(playing, vm.transport.value.playing)
            }
            Pairwise.Event.MIDI_UNPLUGGED -> {
                com.pocketstudio.engine.NativeEngine.sink = com.pocketstudio.app.human.BenchEngine()
                try {
                    val midi = com.pocketstudio.app.midi.MidiViewModel(app)
                    // The app's routing (AppRoot): the controller's keys to the project's keys.
                    val job = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined).launch {
                        midi.commands.collect { c -> if (c is com.pocketstudio.app.midi.MidiCommand.Note) vm.midiNote(c.channel, c.pitch, c.velocity, c.on) }
                    }
                    repeat(3) { i ->
                        vm.recordPositionForTest = 2.0 + i
                        midi.receiveForTest(byteArrayOf(0x90.toByte(), (60 + i).toByte(), 100))
                        midi.disconnect()
                    }
                    job.cancel()
                } finally {
                    com.pocketstudio.engine.NativeEngine.sink = null
                }
                idle()
                // Unplugged three times with a key down: no note left without an end, the state as it was.
                assertTrue(vm.project.value.allScenes.all { s -> s.synthNotes.notes.all { it.length > 0 } })
                assertEquals(recording, vm.recording.value)
                if (recording) vm.toggleRecord()
            }
            Pairwise.Event.REOPEN -> {
                if (recording) vm.toggleRecord()
                val saved = vm.project.value
                runBlocking { vm.flushNow() }
                // Rebuilt (a rotation, the app killed in the background): the same project comes back.
                val again = RhythmViewModel(app)
                assertEquals(saved, again.project.value)
            }
            Pairwise.Event.FOCUS_LOST -> {
                if (recording) { vm.recordPositionForTest = 9.0; vm.hitPad(8) }
                stopRequest("son pris par un appel ou une autre appli")
                idle()
                assertFalse("a call stops the recording", vm.recording.value)
                if (recording) assertEquals("what was recorded before the call is kept", setOf(9), vm.hits(8))
                if (playing) assertFalse("a call stops the music", vm.transport.value.playing)
            }
            Pairwise.Event.UNDO_DURING_SHOW_ME, Pairwise.Event.EDIT_DURING_SHOW_ME -> {
                if (recording) vm.toggleRecord()
                vm.toggleHit(5, 3)
                val mine = vm.project.value
                // « Montre-moi » shows its frame (a hit the user did not make)…
                val frame = mine.copy(drums = mine.drums.toggle(6, 1))
                vm.tutoDemo(frame)
                if (event == Pairwise.Event.UNDO_DURING_SHOW_ME) {
                    vm.undo()
                    vm.tutoDemo(null)
                    assertFalse("the undo is kept after the demonstration", 3 in vm.hits(5))
                    assertFalse("the demonstration is never kept", 1 in vm.hits(6))
                    vm.redo()
                    assertTrue(3 in vm.hits(5))
                    assertFalse("« Rétablir » never brings the demonstration back", 1 in vm.hits(6))
                } else {
                    vm.toggleHit(9, 2)
                    vm.tutoDemo(null)
                    assertTrue("a touch during the demonstration is kept", 2 in vm.hits(9))
                    assertFalse("the demonstration is never kept", 1 in vm.hits(6))
                    vm.undo()
                    assertFalse(2 in vm.hits(9))
                    assertTrue(3 in vm.hits(5))
                    assertFalse(1 in vm.hits(6))
                }
            }
        }
        // Whatever happened: a project the app can save and open again.
        assertEquals(vm.project.value, vm.project.value.sanitized())
        assertTrue(vm.project.value.tempoBpm.isFinite())
    }
}

/** The pairs of other screens and the generator itself. */
@RunWith(org.robolectric.RobolectricTestRunner::class)
class PairwiseOthersTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun everyPairIsInACase() {
        val cases = Pairwise.cases()
        val f = Pairwise.FACTORS
        for (i in f.indices) for (j in i + 1 until f.size) for (a in f[i]) for (b in f[j]) {
            assertTrue("$a with $b", cases.any { it[i] == a && it[j] == b })
        }
        // Far fewer than the whole product (3 × 2 × 3 × 9 = 162).
        assertTrue("${cases.size} cases", cases.size <= 30)
    }

    /** A call (the sound taken) during an export: the export goes on to its end, it is not cancelled. */
    @Test
    fun callDuringAnExportLetsItFinish() {
        File(app.filesDir, "projects").deleteRecursively()
        com.pocketstudio.app.projects.CurrentProject.set(app, "pairs-export-" + System.nanoTime())
        val vm = RhythmViewModel(app)
        var cancelled = 0
        com.pocketstudio.app.export.ExportRun.begin(app, "Export") { cancelled++ }
        try {
            val f = com.pocketstudio.app.playback.PlaybackService::class.java.getDeclaredField("_stopRequests").apply { isAccessible = true }
            @Suppress("UNCHECKED_CAST")
            (f.get(null) as kotlinx.coroutines.flow.MutableSharedFlow<String>).tryEmit("son pris par un appel ou une autre appli")
            shadowOf(android.os.Looper.getMainLooper()).idle()
            assertEquals(0, cancelled)
            assertTrue(com.pocketstudio.app.export.ExportRun.state.value != null)
            assertFalse(vm.transport.value.playing)
        } finally {
            com.pocketstudio.app.export.ExportRun.end(null)
        }
    }
}
