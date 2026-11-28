package com.pocketstudio.app.guide

import android.app.Application
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.SoundLibrary
import com.pocketstudio.model.SynthPatch
import com.pocketstudio.model.guide.GuideSheets
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Tour 9, lot miettesB (rest of L7, same cause as F116, seats t9-58, t9-69): « La feuille des couches d'une piste
 * nomme le son que j'ai choisi, même après un bouton tourné, comme l'onglet de la piste ; jamais « Synthé » ou
 * « Plaits » à la place. » The track's tab kept the library sound's name (L7); the layers editor still read the
 * engine's name.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class Tour9LayersOwnSoundNameTest {
    @get:Rule
    val compose = createComposeRule()
    private val app: Application = ApplicationProvider.getApplicationContext()

    private fun Float.turned() = if (this < 0.5f) this + 0.3f else this - 0.3f

    private fun shownOwnSound(vm: RhythmViewModel, track: Int) {
        compose.setContent { PocketStudioTheme { LayersEditorRoute(vm, track, close = {}) } }
        compose.waitForIdle()
    }

    @Test
    fun theLayersEditorNamesTheChosenSoundLikeTheTab() {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        vm.addTrack()
        val lead = SoundLibrary.SYNTH.first { it.category == "Leads" }
        // The library sound, then one knob turned (the tutorial's step): no name passed, as the knob does.
        vm.applySynthPreset(lead.sound.with(SynthPatch.RESONANCE, lead.sound.get(SynthPatch.RESONANCE).turned()))
        val t = vm.project.value.tracks.first { it.id == vm.selectedTrack.value }
        assertNotEquals("the engine's name is what the guide gives", lead.name, GuideSheets.soundName(t))
        assertEquals("the tab's name", lead.name, com.pocketstudio.app.synth.trackSoundName(t))
        shownOwnSound(vm, t.id)
        compose.onNode(hasTestTag("layers_own").and(hasAnyDescendant(hasText(lead.name))))
            .assertExists("the layers editor names « ${lead.name} », not « ${GuideSheets.soundName(t)} »")
    }

    @Test
    fun aFreshTracksSoundIsNamedAsBefore() {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        vm.addTrack()
        vm.applySynthPreset(SynthPatch())
        val t = vm.project.value.tracks.first { it.id == vm.selectedTrack.value }
        shownOwnSound(vm, t.id)
        compose.onNode(hasTestTag("layers_own").and(hasAnyDescendant(hasText(GuideSheets.soundName(t)))))
            .assertExists("a fresh synth keeps its usual name")
    }
}
