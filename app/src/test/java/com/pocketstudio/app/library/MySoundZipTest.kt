package com.pocketstudio.app.library

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.projects.projectCatalog
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Duration
import kotlin.math.PI
import kotlin.math.sin

/**
 * A sound of « Ma bibliothèque » used in a project is the project's own copy: the project's .zip
 * carries it, the import gets it back whole, and deleting it from the library changes nothing to
 * the project (owner, 2026-10-29).
 */
@RunWith(RobolectricTestRunner::class)
class MySoundZipTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val store = ProjectStore(File(app.filesDir, "projects"))

    @After
    fun end() {
        NativeEngine.sink = null
        File(app.filesDir, "mysounds").deleteRecursively()
        runBlocking { MySoundLibrary.refresh(app) }
        CurrentProject.set(app, RhythmViewModel.PROJECT_ID)
    }

    private fun until(what: String, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + 30_000
        while (!ok()) {
            check(System.currentTimeMillis() < end) { what }
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(20))
            Thread.sleep(5)
        }
    }

    @Test
    fun aProjectUsingAKeptSoundGoesThroughItsZipAndBack() {
        File(app.filesDir, "mysounds").deleteRecursively()
        NativeEngine.sink = com.pocketstudio.app.human.BenchEngine()
        // A voice phrase kept from the Remix: 1 s at 220 Hz, 98 BPM, A minor.
        val voice = Pcm(FloatArray(48_000 * 2) { i -> (0.4 * sin(2 * PI * 220 * (i / 2) / 48_000.0)).toFloat() }, 2, 48_000)
        val kept = runBlocking { MySoundLibrary.keep(app, voice, "Voix · Ma chanson", "vocal", bpm = 98.0, root = 9, minor = true) }

        val id = "mine-zip-" + System.nanoTime()
        store.save(id, Project(name = "Avec ma voix"))
        CurrentProject.set(app, id)
        val vm = RhythmViewModel(app)
        // As « Poser » does: the library's sound, read through its reference, onto pad 6.
        val library = SoundLibraryViewModel(app)
        val pcm = runBlocking { library.loadForTest(SoundRef.Mine(kept.id)) }
        assertNotNull(pcm)
        vm.selectPad(5)
        assertTrue(vm.placeOnPad(pcm!!, kept.name))
        until("never on the pad") { vm.project.value.drums.lanes[5].sample != null }
        val sample = vm.project.value.drums.lanes[5].sample!!
        assertEquals("Voix · Ma chanson", sample.name)
        assertTrue(runBlocking { vm.flushNow() })
        val own = File(store.folder(id), "samples/${sample.file}")
        val bytes = own.readBytes()

        // Gone from the library: the project keeps its sound.
        runBlocking { MySoundLibrary.delete(app, kept.id) }
        assertTrue(MySoundLibrary.sounds.value.isEmpty())
        assertTrue(own.isFile)

        // Sent as a .zip, imported back as another project: the sound comes with it, whole.
        val zip = ByteArrayOutputStream()
        assertEquals("no sound left out", 0, projectCatalog(app).exportZip(id, zip))
        val imported = projectCatalog(app).importZip(ByteArrayInputStream(zip.toByteArray()))
        assertEquals(0, imported.missingSounds)
        val back = store.load(imported.id)!!
        val again = back.drums.lanes[5].sample!!
        assertEquals("Voix · Ma chanson", again.name)
        assertArrayEquals(bytes, File(store.folder(imported.id), "samples/${again.file}").readBytes())
    }
}
