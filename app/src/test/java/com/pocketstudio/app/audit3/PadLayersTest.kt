package com.pocketstudio.app.audit3

import android.app.Application
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.human.BenchEngine
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.projects.projectCatalog
import com.pocketstudio.app.rhythm.PadSheetActions
import com.pocketstudio.app.rhythm.PadSheetContent
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.rhythm.padSheetUi
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.PadLayer
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectJson
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.sampleFiles
import com.pocketstudio.model.withoutSounds
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Duration
import java.util.zip.ZipInputStream
import kotlin.math.PI
import kotlin.math.sin

/**
 * Audit stage 3, decision of 2026-11-03: a drum pad's « Couches » (velocity layers, Pro) reach the
 * engine's Sampler layers (NativeEngine.setPadLayer), live and in an export, and live through
 * undo, save, reopen and the project's .zip.
 */
@RunWith(RobolectricTestRunner::class)
class PadLayersTest {
    @get:Rule val compose = createComposeRule()
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val store = ProjectStore(File(app.filesDir, "projects"))

    /** The bench engine, keeping every pad sound install (live: target 0; export: its own). */
    private class Recording(private val inner: CommandSink = BenchEngine()) : CommandSink by inner {
        val installs = java.util.Collections.synchronizedList(ArrayList<Triple<Long, Int, Long>>())
        override fun install(target: Long, kind: CommandSink.Install, index: Int, sample: Long) {
            if (kind == CommandSink.Install.PadSample) installs += Triple(target, index, sample)
            inner.install(target, kind, index, sample)
        }
    }

    @After
    fun end() {
        NativeEngine.sink = null
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

    private fun tone(hz: Double) = Pcm(FloatArray(24_000 * 2) { i -> (0.5 * sin(2 * PI * hz * (i / 2) / 48_000.0)).toFloat() }, 2, 48_000)

    @Test
    fun aLayerIsHeardSavedUndoneZippedAndExported() {
        val sink = Recording()
        NativeEngine.sink = sink
        val id = "layers-" + System.nanoTime()
        store.save(id, Project(name = "Couches"))
        CurrentProject.set(app, id)
        val vm = RhythmViewModel(app)
        until("never opened") { vm.project.value.name == "Couches" }
        vm.selectPad(2)
        // No own sound: no layer (the kit's synthesized voice has none).
        assertFalse(vm.addPadLayer(2, tone(220.0), "Doux"))
        assertTrue(vm.placeOnPad(tone(220.0), "Doux", oneShot = true))
        until("never on the pad") { vm.project.value.drums.lanes[2].sample != null }
        val base = vm.project.value.drums.lanes[2].sample!!
        until("pad free") { vm.sampleTask.value is com.pocketstudio.app.rhythm.SampleTask.Idle }

        sink.installs.clear()
        assertTrue(vm.addPadLayer(2, tone(880.0), "Fort"))
        until("never layered") { vm.project.value.drums.lanes[2].sample?.layers?.size == 1 }
        val layered = vm.project.value.drums.lanes[2].sample!!
        assertEquals(base.file, layered.file)
        val layer = layered.layers.single()
        assertEquals("Fort", layer.name)
        assertEquals(PadLayer.DEFAULT_FROM, layer.from)
        // The engine: base sound, then layer 0 (soft hits, the base again) and layer 1 (hard hits).
        val live = sink.installs.filter { it.first == 0L && (it.second and 0xFF) == 2 }.map { it.second shr 8 and 0xFF }
        assertTrue("layers sent live: $live", 1 in live && 2 in live)
        val hard = sink.installs.last { it.first == 0L && (it.second shr 8 and 0xFF) == 2 }.second
        assertEquals(PadLayer.DEFAULT_FROM, hard shr 16 and 0xFF)
        assertEquals(127, hard ushr 24)
        val soft = sink.installs.last { it.first == 0L && (it.second shr 8 and 0xFF) == 1 }.second
        assertEquals(0, soft shr 16 and 0xFF)
        assertEquals(PadLayer.DEFAULT_FROM - 1, soft ushr 24)
        assertTrue(layer.file in vm.project.value.sampleFiles)

        // The slider: one undo step for a slide, kept between its neighbours.
        vm.setPadLayerFrom(2, 0, 90)
        vm.setPadLayerFrom(2, 0, 80)
        assertEquals(80, vm.project.value.drums.lanes[2].sample!!.layers.single().from)
        vm.undo()
        assertEquals(PadLayer.DEFAULT_FROM, vm.project.value.drums.lanes[2].sample!!.layers.single().from)
        vm.undo()
        assertTrue("undo takes the layer away", vm.project.value.drums.lanes[2].sample!!.layers.isEmpty())
        vm.redo()
        assertEquals(layered, vm.project.value.drums.lanes[2].sample)

        // Saved and read back the same; its file on disk.
        assertTrue(runBlocking { vm.flushNow() })
        assertEquals(layered, store.load(id)!!.drums.lanes[2].sample)
        assertTrue(File(store.folder(id), "samples/${layer.file}").isFile)

        // The .zip carries the layer's sound; imported back whole.
        val zip = ByteArrayOutputStream()
        assertEquals("no sound left out", 0, projectCatalog(app).exportZip(id, zip))
        val names = ArrayList<String>()
        ZipInputStream(ByteArrayInputStream(zip.toByteArray())).use { z -> generateSequence { z.nextEntry }.forEach { names += it.name } }
        assertTrue("layer's sound in the zip: $names", names.any { it.endsWith(layer.file) })
        val imported = projectCatalog(app).importZip(ByteArrayInputStream(zip.toByteArray()))
        assertEquals(0, imported.missingSounds)
        assertEquals(listOf("Fort"), store.load(imported.id)!!.drums.lanes[2].sample!!.layers.map { it.name })

        // An export describes the layers to its own engine too.
        sink.installs.clear()
        vm.exportWav()
        until("export never ended: ${vm.export.value}") { sink.installs.any { it.first != 0L } && !vm.export.value.running }
        val exported = sink.installs.filter { it.first != 0L && (it.second and 0xFF) == 2 }.map { it.second shr 8 and 0xFF }
        assertTrue("layers in the export: $exported", 2 in exported)

        // Removed: back to the pad's own sound alone.
        vm.removePadLayer(2, 0)
        assertTrue(vm.project.value.drums.lanes[2].sample!!.layers.isEmpty())
    }

    @Test
    fun layersAreCleanedFromAFile() {
        val raw = PadSample(
            "a.wav", "A",
            layers = listOf(PadLayer("../evil.wav", "x", 100), PadLayer("b.wav", "B", 500), PadLayer("c.wav", "C", 127), PadLayer("d.wav", "D", -4), PadLayer("e.wav", "E", 60)),
        ).sanitized()
        // Unsafe file gone; starts in range, apart, sorted; three at most.
        assertEquals(listOf("d.wav" to 2, "e.wav" to 60, "b.wav" to 127), raw.layers.map { it.file to it.from })
        assertEquals(listOf(0..1, 2..59, 60..126, 127..127), raw.layerRanges())
        // Round trip, and an old file (no layers) reads the same.
        val p = Project().let { it.copy(drums = it.drums.withSample(0, raw)) }
        assertEquals(raw, ProjectJson.decode(ProjectJson.encode(p)).drums.lanes[0].sample)
        assertFalse(ProjectJson.encode(Project().let { it.copy(drums = it.drums.withSample(0, PadSample("a.wav", "A"))) }).contains("layers"))
        // A missing layer's sound: the layer goes, the pad keeps its own sound.
        val without = p.withoutSounds(setOf("e.wav"))
        assertEquals(listOf("d.wav", "b.wav"), without.drums.lanes[0].sample!!.layers.map { it.file })
        assertNull(p.withoutSounds(setOf("a.wav")).drums.lanes[0].sample)
    }

    @Test
    fun theSheetShowsLayersInProOnly() {
        val p = Project().let { it.copy(drums = it.drums.withSample(1, PadSample("a.wav", "A", layers = listOf(PadLayer("b.wav", "Fort", 100))))) }
        var added = -1
        var removed = -1
        var simple by androidx.compose.runtime.mutableStateOf(true)
        compose.setContent {
            PadSheetContent(
                padSheetUi(p, 1, listOf("Kick 1", "Kick 2"), pro = !simple),
                PadSheetActions(addLayer = { added = it }, removeLayer = { removed = it }),
                close = {},
            )
        }
        compose.onNodeWithTag("pad_layers").assertDoesNotExist()
        simple = false
        compose.onNodeWithTag("pad_layers").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("pad_layer_from_0").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("pad_layer_remove_0").performScrollTo().performClick()
        assertEquals(0, removed)
        compose.onNodeWithTag("pad_layer_add").performScrollTo().performClick()
        compose.onNodeWithTag("pad_layer_sound_1").performScrollTo().performClick()
        assertEquals(1, added)
    }
}
