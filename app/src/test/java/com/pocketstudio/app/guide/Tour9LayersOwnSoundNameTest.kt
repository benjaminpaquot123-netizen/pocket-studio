package com.pocketstudio.app.guide

import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.PlaitsPatch
import com.pocketstudio.model.SoundLibrary
import com.pocketstudio.model.SynthPatch
import com.pocketstudio.model.guide.GuideSheets
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tour 9, lot miettesB (rest of L7, same cause as F116, seats t9-58, t9-69): « La feuille des couches d'une piste
 * nomme le son que j'ai choisi, même après un bouton tourné, comme l'onglet de la piste ; jamais « Synth » ou
 * « Plaits » à la place. » The track's tab kept the library sound's name (L7), the layers editor still read the
 * engine's name.
 */
class Tour9LayersOwnSoundNameTest {
    private fun Float.turned() = if (this < 0.5f) this + 0.3f else this - 0.3f

    @Test
    fun theLayersEditorNamesTheChosenSoundLikeTheTab() {
        val lead = SoundLibrary.SYNTH.first { it.category == "Leads" }
        val t = InstrumentTrack(id = 0, name = "Mélodie", synth = lead.sound.with(SynthPatch.RESONANCE, lead.sound.get(SynthPatch.RESONANCE).turned()))
        assertEquals("the engine's name is what the guide gives", t.soundLabel, GuideSheets.soundName(t))
        assertEquals(lead.name, layersOwnSound(t))
        assertEquals(com.pocketstudio.app.synth.trackSoundName(t), layersOwnSound(t))

        val pl = SoundLibrary.PLAITS.first()
        val tp = InstrumentTrack(id = 0, name = "Mélodie", engine = InstrumentTrack.ENGINE_PLAITS, plaits = pl.sound.copy(timbre = pl.sound.timbre.turned()))
        assertEquals(pl.name, layersOwnSound(tp))
    }

    @Test
    fun aFreshTracksSoundIsNamedAsBefore() {
        val fresh = InstrumentTrack(id = 0, name = "Piste 1")
        assertEquals(GuideSheets.soundName(fresh), layersOwnSound(fresh))
        val freshPlaits = fresh.copy(engine = InstrumentTrack.ENGINE_PLAITS, plaits = PlaitsPatch())
        assertEquals(GuideSheets.soundName(freshPlaits), layersOwnSound(freshPlaits))
    }
}
