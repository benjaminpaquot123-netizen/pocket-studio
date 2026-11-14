package com.pocketstudio.app.journey

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.projects.projectCatalog
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.app.tuto.PrefsTutoStore
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectJson
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.Remix
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.cellHasContent
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.time.Duration
import kotlin.math.PI
import kotlin.math.sin

/**
 * Audit stage 2, point 8 (« une semaine dans la vie »), end to end on the app's ViewModel and files
 * (Robolectric has no engine: BenchEngine stands in for it). A synthetic song stands in for
 * Benjamin's (no song of anyone's in the repository). Each day, everything done before is still there:
 *  - Monday: installed (nothing on the phone), a tutorial begun, a song made by the assistant, edited,
 *    a song remixed into it, a loop dropped into a brick of the Live, drums played into another brick
 *    from the controller's pads, a Live performance recorded with the keyboard;
 *  - Thursday: the app opened again (a new process): all of it, the tutorial where it was;
 *  - the project exported as a .zip and imported back (another phone): the same project;
 *  - Sunday: the app updated (its settings read again, the project read by the new version's reader).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class WeekInTheLifeTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    /** The screens' stores of the ViewModels this test opens: cleared at its end, as the app's screens are when they go. */
    private val stores = ArrayList<androidx.lifecycle.ViewModelStore>()

    /** The app's view model, made as a screen's (its store cleared when the test ends). */
    private fun open(): RhythmViewModel {
        val store = androidx.lifecycle.ViewModelStore().also { stores += it }
        val factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = RhythmViewModel(app) as T
        }
        return androidx.lifecycle.ViewModelProvider(store, factory)[RhythmViewModel::class.java]
    }

    @After
    fun end() {
        // Tour 6, lot « stable », follow-up (CI 43531021739): the ViewModels were never cleared; the last one's live-gain
        // estimate, started with the bench's engine, waited on the main looper for its sounds and ran in a later class, the
        // engine gone (UnsatisfiedLinkError, reported by LibraryRound2Zone1Test). Their screens close, and their work ends
        // (the main looper running) before the engine is let go.
        try {
            stores.forEach { it.clear() }
            until("the app's work after the screens closed: ${com.pocketstudio.app.human.BenchPristine.appThreads() + com.pocketstudio.app.human.BenchPristine.appMainCoroutines()}") {
                com.pocketstudio.app.human.BenchPristine.appThreads().isEmpty() && com.pocketstudio.app.human.BenchPristine.appMainCoroutines().isEmpty()
            }
        } finally {
            com.pocketstudio.engine.NativeEngine.sink = null
            SettingsStore.forgetForTests()
        }
    }

    private fun until(what: String, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + 30_000
        while (!ok()) {
            check(System.currentTimeMillis() < end) { what }
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(20))
            Thread.sleep(5)
        }
    }

    /**
     * Tour 6, lot « stable » (a shuffled run of the suite, seed 1: this test hung 20 min in `runBlocking { vm.flushNow() }`,
     * no worker in the app's code): an autosave started by [until]'s clock held the project's save lock, its write done,
     * waiting for the main thread to come back to it; the main thread was blocked waiting for that lock. On a fast disk the
     * autosave was over before; on a slow one, never. The flush runs off the main thread, the main looper running meanwhile.
     */
    private fun flush(vm: RhythmViewModel) {
        val result = java.util.concurrent.CompletableFuture<Unit>()
        kotlin.concurrent.thread(name = "week-flush") { runCatching { runBlocking { vm.flushNow() } }.fold({ result.complete(Unit) }, { result.completeExceptionally(it) }) }
        until("the project's save never ended") { result.isDone }
        result.get()
    }

    /** What must be true of the project every day after Monday. */
    private fun assertMondayIsThere(p: Project, monday: Project, cellTrack: Int) {
        assertEquals("the tempo set on Monday", 150.0, p.tempoBpm, 1e-9)
        assertTrue("the hit added on Monday", 3 in p.allScenes[0].drums.lanes[5].hits.keys)
        assertEquals("the remixed song", "week-song", p.remix.setId)
        assertTrue("the loop in brick B", p.cellHasContent(SongPart.audio(cellTrack), 1))
        assertTrue("the performance's notes", p.allScenes.any { s -> (listOf(s.synthNotes) + s.trackNotes.values).any { c -> c.notes.any { it.pitch == 67 } } })
        assertEquals("the song's parts", monday.song, p.song)
        assertEquals("the tracks", monday.tracks, p.tracks)
    }

    @Test
    fun aWeekWithTheApp() {
        // --- Monday: installed, nothing on the phone.
        listOf("projects", "projects-trash", "projects-backup", "stems").forEach { File(app.filesDir, it).deleteRecursively() }
        listOf("settings", "tuto").forEach { app.getSharedPreferences(it, 0).edit().clear().commit() }
        SettingsStore.forgetForTests()
        SettingsStore.update(app) { it.copy(guideSeen = true, simpleMode = true) }

        // A song by the assistant (acidcore), as « Fais-moi un morceau » makes it.
        val style = StarterWizard.style("acidcore")!!
        val made = StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? }, style.melody.mood,
            StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() })
        val id = "week-" + System.nanoTime()
        ProjectStore(File(app.filesDir, "projects")).save(id, made)
        CurrentProject.set(app, id)
        // A tutorial begun on it: two steps done.
        PrefsTutoStore(app).save(id, TutoProgress(style = "acidcore", level = TutoLevel.Facile, done = setOf("ref", "tempo"), current = "kick"))
        var vm = open()

        // Edited: the tempo, a hit.
        vm.setTempo(150.0)
        vm.toggleHit(5, 3)
        // The synthetic song remixed into it (its separated parts are files of the phone, kept apart).
        vm.session.edit { it.copy(remix = Remix(setId = "week-song", name = "Chanson synthétique")) }
        // A performance recorded on the Live, a key of the MIDI keyboard in it (the take's clock, no engine).
        com.pocketstudio.engine.NativeEngine.sink = null
        vm.togglePlay()
        vm.takeClockForTest = 5
        vm.togglePerformance()
        vm.takeClockForTest = 16
        vm.tickPerformance()
        vm.takeClockForTest = 20; vm.midiNote(0, 67, 0.8f, true)
        vm.takeClockForTest = 22; vm.midiNote(0, 67, 0f, false)
        vm.takeClockForTest = 40
        vm.togglePerformance()
        if (vm.transport.value.playing) vm.togglePlay()
        assertTrue("the take ended", vm.take.value is com.pocketstudio.app.live.TakeUi.Done)
        com.pocketstudio.engine.NativeEngine.sink = com.pocketstudio.app.human.BenchEngine()
        // The Live: a loop dropped into brick B (as from the library), the controller's pads into the drum cell.
        val pcm = Pcm(FloatArray(96_000 * 2) { i -> (0.3 * sin(2 * PI * 220 * (i / 2) / 48_000.0)).toFloat() }, 2, 48_000)
        assertTrue(vm.placeInCell(SongPart.DRUMS, 1, pcm, "Ma boucle"))
        until("the loop never reached its brick") { vm.project.value.audioTracks.any { it.clip != null } }
        val cellTrack = vm.project.value.audioTracks.first { it.clip != null }.id
        val monday = vm.project.value
        assertMondayIsThere(monday, monday, cellTrack)
        flush(vm)

        // --- Thursday: the app opened again (a new process: the ViewModel and the settings read again).
        SettingsStore.forgetForTests()
        SettingsStore.load(app)
        vm = open()
        assertMondayIsThere(vm.project.value, monday, cellTrack)
        assertTrue("the Mode simple kept", SettingsStore.state.value.simpleMode)
        val tuto = PrefsTutoStore(app).load(id)
        assertNotNull(tuto)
        assertEquals(setOf("ref", "tempo"), tuto!!.done)
        assertEquals("kick", tuto.current)
        // An undo on Thursday takes back Thursday's change only, never Monday's.
        vm.toggleHit(6, 7)
        vm.undo()
        assertMondayIsThere(vm.project.value, monday, cellTrack)
        flush(vm)

        // --- Exported as a .zip and imported on another phone: the same project, its loop's sound with it.
        val catalog = projectCatalog(app)
        val zip = java.io.ByteArrayOutputStream().also { catalog.exportZip(id, it) }.toByteArray()
        val other = com.pocketstudio.model.ProjectCatalog(File(app.filesDir, "other-phone"), File(app.filesDir, "other-trash"), File(app.filesDir, "other-stems"))
        val back = other.importZip(zip.inputStream())
        val imported = other.store.load(back.id)!!
        assertMondayIsThere(imported, monday, cellTrack)
        val clip = imported.audioTracks.first { it.id == cellTrack }.clip!!
        assertTrue("the loop's sound travels with the project", File(other.store.folder(back.id), clip.file).exists() || File(other.store.folder(back.id), "samples/" + clip.file).exists() ||
            other.store.folder(back.id).walkTopDown().any { it.isFile && it.name == File(clip.file).name })

        // --- Sunday: the app updated. Its settings and the project read by the new version's reader.
        val file = File(ProjectStore(File(app.filesDir, "projects")).folder(id), ProjectStore.MAIN)
        val reread = ProjectJson.decode(file.readText())
        assertMondayIsThere(reread, monday, cellTrack)
        SettingsStore.forgetForTests()
        SettingsStore.load(app)
        assertTrue(SettingsStore.state.value.simpleMode)
        assertMondayIsThere(open().project.value, monday, cellTrack)
    }
}
