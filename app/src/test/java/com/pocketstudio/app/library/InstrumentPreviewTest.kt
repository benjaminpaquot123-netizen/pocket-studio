package com.pocketstudio.app.library

import com.pocketstudio.model.MultiInstruments
import com.pocketstudio.model.MultiPatch
import com.pocketstudio.model.StarterWizard
import org.junit.Assert.assertEquals
import org.junit.Test

/** Phase 38: ▶ on an instrument in the assistant plays the instrument, not its stand-in synth sound. */
class InstrumentPreviewTest {
    @Test
    fun anInstrumentPartPlaysItsOwnPreview() {
        val piano = StarterWizard.PartSound("Piano à queue", multi = MultiPatch(MultiInstruments.PIANO_SALAMANDER, "Piano à queue"))
        assertEquals(
            SoundRef.Instrument(MultiInstruments.PIANO_SALAMANDER),
            previewSource(SoundRef.Melodic(StarterWizard.Part.Pad, piano)),
        )
        // A synth part plays itself, in the song's context.
        val synth = SoundRef.Melodic(StarterWizard.Part.Pad, StarterWizard.PartSound("Nappe"))
        assertEquals(synth, previewSource(synth))
        assertEquals(SoundRef.Kit(3), previewSource(SoundRef.Kit(3)))
    }
}
