package com.pocketstudio.app.library

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.settings.GuidePage
import com.pocketstudio.model.Project
import com.pocketstudio.model.StarterWizard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * « Nouveau projet › Partir d'un style »: one touch makes the whole song the assistant makes with
 * its advised choices (several sections, the song arranged), not a single scene.
 */
@RunWith(RobolectricTestRunner::class)
class QuickCreateTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    private class Made(val project: Project, val files: Map<String, Pcm>, val guide: List<GuidePage>)

    private fun vm() = SoundLibraryViewModel(app).also { vm ->
        // Every kit voice as if heard before (no audio engine here).
        for (v in 0 until 64) vm.kitCache[v] = Pcm(FloatArray(960), 2, 48000)
    }

    /** Runs the main looper until [get] has a value (the packs are read and sounds decoded off it). */
    private fun <T> await(get: () -> T?): T {
        val end = System.currentTimeMillis() + 30_000
        while (true) {
            shadowOf(Looper.getMainLooper()).idle()
            get()?.let { return it }
            check(System.currentTimeMillis() < end) { "nothing made in time" }
            Thread.sleep(2)
        }
    }

    private fun quick(vm: SoundLibraryViewModel, id: String): Made {
        var made: Made? = null
        vm.quickCreate(id) { p, files, guide -> made = Made(p, files, guide) }
        assertEquals(id, vm.quick.value.creating)
        return await { made }
    }

    private fun wizard(vm: SoundLibraryViewModel, id: String): Made {
        var made: Made? = null
        vm.openWizard()
        vm.chooseStyle(id)
        vm.stopPreview()
        vm.create { p, files, guide -> made = Made(p, files, guide) }
        return await { made }.also { vm.closeWizard() }
    }

    @Test
    fun everyStyleMakesTheWizardsWholeSongInOneTouch() {
        assertTrue(StarterWizard.STYLES.size >= 36)
        val vm = vm()
        for (style in StarterWizard.STYLES) {
            val quick = quick(vm, style.id)
            // The assistant stays closed; the touch is still "creating" until the project is written.
            assertFalse(vm.wizard.value.open)
            assertEquals(style.id, vm.quick.value.creating)
            vm.quickDone()
            assertNull(vm.quick.value.creating)
            val p = quick.project
            assertTrue("${style.id}: ${p.scenes.size} scenes", p.scenes.size > 1)
            assertTrue("${style.id}: a song", p.song.size > 1)
            assertTrue("${style.id}: its guide", quick.guide.isNotEmpty())
            // Exactly the song « Créer le morceau » makes with the same advised choices.
            val guided = wizard(vm, style.id).project
            assertEquals(style.id, guided.scenes.size, p.scenes.size)
            assertEquals(style.id, guided.song.map { Triple(it.scene, it.repeats, it.bars) }, p.song.map { Triple(it.scene, it.repeats, it.bars) })
            assertEquals(style.id, songBars(guided), songBars(p))
            assertEquals(style.id, guided.tempoBpm, p.tempoBpm, 0.0)
            assertEquals(style.id, guided.tracks.size, p.tracks.size)
            assertEquals(style.id, guided.automation.size, p.automation.size)
            assertEquals(style.id, guided.drums.lanes.map { it.sample?.name }, p.drums.lanes.map { it.sample?.name })
            assertEquals(style.id, quick.files.size, wizard(vm, style.id).files.size)
        }
    }

    @Test
    fun aSoundThatCannotBeReadIsSaidInPlainWords() {
        // No audio engine and nothing heard before: a style whose line needs a rendered kit voice fails.
        val vm = SoundLibraryViewModel(app)
        val style = StarterWizard.STYLES.first { s -> s.lines.any { !it.off && (it.voice ?: it.role.lane) != it.role.lane || !it.off && StarterWizard.shapesKit(it.role) } }
        var made = false
        vm.quickCreate(style.id) { _, _, _ -> made = true }
        val error = await { vm.quick.value.error }
        assertFalse(made)
        assertNull(vm.quick.value.creating)
        assertTrue(error, error.startsWith("Le morceau n'a pas pu être préparé"))
        vm.dismissQuickError()
        assertNull(vm.quick.value.error)
    }

    @Test
    fun customisingOpensTheAssistantOnThatStyle() {
        val vm = vm()
        vm.openWizard("dnb")
        val w = vm.wizard.value
        assertTrue(w.open)
        assertEquals("dnb", w.styleId)
        assertEquals(1, w.step)
        assertEquals(StarterWizard.style("dnb")!!.melody.mood, w.mood)
    }

    /** The song's length in bars: each part's own length, else its scene's, times its repeats. */
    private fun songBars(p: Project): Int = p.song.sumOf { part ->
        (part.bars ?: (p.scenes[part.scene].steps / 16).coerceAtLeast(1)) * part.repeats
    }
}
