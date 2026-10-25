package com.pocketstudio.app.library

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.model.BassPatterns
import com.pocketstudio.model.MultiPatch
import com.pocketstudio.model.StarterWizard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** « Motifs de basse » on the bass's step: the grid touched is what the preview and the song play. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h2400dp")
class WizardBassGridTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    private fun vmOnBassStep(styleId: String): SoundLibraryViewModel {
        val vm = SoundLibraryViewModel(app)
        vm.openWizard()
        vm.chooseStyle(styleId)
        vm.setMood(StarterWizard.Mood.Dark)
        vm.stopPreview()
        while (vm.wizard.value.current != StarterWizard.Step.Melodic(StarterWizard.Part.Bass)) vm.next()
        return vm
    }

    /** The bass notes [w] plays (what its preview and its song's bass start from). */
    private fun bassNotes(w: WizardUi) = StarterWizard.startingNotes(
        StarterWizard.Part.Bass, w.style!!, w.mood!!, StarterWizard.harmonyOf(w.style!!, w.mood!!),
        w.parts.getValue(StarterWizard.Part.Bass).sound,
    ).notes

    @Test
    fun aGridTouchedIsTheBassThatPlays() {
        val vm = vmOnBassStep("techno")
        val recipe = bassNotes(vm.wizard.value)
        assertNull(vm.wizard.value.bassGrid)
        // The power chord: three notes at once, on the techno's bass.
        vm.chooseBassGrid("power")
        val w = vm.wizard.value
        assertEquals("power", w.bassGrid)
        assertEquals("power", w.style!!.melody.bassGrid)
        assertTrue(bassNotes(w).groupBy { it.start }.values.any { it.size == 3 })
        // Another grid, then « Celui du style »: the recipe's line again.
        vm.chooseBassGrid("rolling_kbbb")
        assertEquals(BassPatterns.byId("rolling_kbbb"), BassPatterns.of(vm.wizard.value.style!!))
        assertFalse(bassNotes(vm.wizard.value) == recipe)
        vm.chooseBassGrid(null)
        assertNull(vm.wizard.value.bassGrid)
        assertEquals(recipe, bassNotes(vm.wizard.value))
        // A new style forgets the grid.
        vm.chooseBassGrid("power")
        vm.chooseStyle("house")
        assertNull(vm.wizard.value.bassGrid)
        vm.stopPreview()
    }

    @Test
    fun theGridIsHeardOnAPackSoundAndOnAMultizoneInstrument() {
        val vm = vmOnBassStep("techno")
        val sound = vm.wizard.value.parts.getValue(StarterWizard.Part.Bass).sound
        // A sound of the library or of a pack: the grid on that very sound.
        assertEquals(SoundRef.Melodic(StarterWizard.Part.Bass, sound), gridPreview(sound))
        // A multizone instrument's own preview is one sample: the grid is played on its stand-in instead.
        val multi = sound.copy(multi = MultiPatch("bass_guitar", "Basse électrique"))
        assertTrue(previewSource(SoundRef.Melodic(StarterWizard.Part.Bass, multi)) is SoundRef.Instrument)
        val heard = gridPreview(multi)
        assertEquals(heard, previewSource(heard))
        assertEquals(multi.synth, heard.sound.synth)
        vm.stopPreview()
    }

    @Test
    fun aGlidingBassIsNotOfferedThePowerChord() {
        val vm = vmOnBassStep("tech_house")
        val style = vm.wizard.value.style!!
        assertTrue(BassPatterns.offered(style).none { it.power })
        assertTrue(BassPatterns.offered(StarterWizard.style("techno")!!).any { it.power })
        vm.stopPreview()
    }
}
