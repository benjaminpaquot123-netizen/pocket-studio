package com.pocketstudio.app.guide

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.SoundLibrary
import com.pocketstudio.model.SynthPatch
import com.pocketstudio.model.guide.GuideAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Tour 9, lot « textes » (rest of miettesB, same cause as F116): « La fiche « Son » du guide nomme le son que j'ai
 * choisi, même après un bouton tourné, comme l'onglet de la piste ; jamais « Synthé » à la place. »
 */
@RunWith(RobolectricTestRunner::class)
class Tour9TextesGuideSoundNameTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    private fun Float.turned() = if (this < 0.5f) this + 0.3f else this - 0.3f

    private fun sheetSound(vm: RhythmViewModel): Pair<String, String> {
        val t = vm.project.value.tracks.first { it.id == vm.selectedTrack.value }
        val sheet = GuideAnalyzer.analyze(vm.project.value).sheets.first { it.track.key == "inst:${t.id}" }
        return sheet.sound.label to com.pocketstudio.app.synth.trackSoundName(t)
    }

    @Test
    fun theGuideSheetNamesTheChosenSoundLikeTheTab() {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        vm.addTrack()
        val lead = SoundLibrary.SYNTH.first { it.category == "Leads" }
        vm.applySynthPreset(lead.sound.with(SynthPatch.RESONANCE, lead.sound.get(SynthPatch.RESONANCE).turned()))
        val (sheet, tab) = sheetSound(vm)
        assertEquals("the tab's name", lead.name, tab)
        assertEquals("the guide's « Son » names the chosen sound", lead.name, sheet)
    }

    @Test
    fun everyLibrarySoundWithAKnobTurnedReadsLikeTheTab() {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        vm.addTrack()
        val wrong = SoundLibrary.SYNTH.mapNotNull { s ->
            vm.applySynthPreset(s.sound.with(SynthPatch.RESONANCE, s.sound.get(SynthPatch.RESONANCE).turned()))
            val (sheet, tab) = sheetSound(vm)
            "${s.name}: guide « $sheet », tab « $tab »".takeIf { sheet != tab || tab != s.name }
        }
        assertEquals("guide and tab disagree:\n" + wrong.joinToString("\n"), 0, wrong.size)
    }

    @Test
    fun aFreshTracksSoundIsNamedAsBefore() {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        vm.addTrack()
        vm.applySynthPreset(SynthPatch())
        val (sheet, tab) = sheetSound(vm)
        assertEquals(tab, sheet)
    }
}
