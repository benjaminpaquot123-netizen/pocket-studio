package com.pocketstudio.app.library

import com.pocketstudio.model.StarterWizard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The wizard's word on a style's signature part says where it really plays (phase 28 music audit, m3). */
class SignatureNoteTest {
    private val everywhere = "C'est le son qui fait le style : il joue dans toutes les sections."
    private val short = "C'est le son qui fait le style."

    @Test
    fun aSignatureBassIsNotSaidToPlayEverywhere() {
        val styles = StarterWizard.STYLES.filter { it.melody.signature == StarterWizard.Part.Bass }
        assertTrue(styles.isNotEmpty())
        for (s in styles) {
            val plan = StarterWizard.planFor(s, s.lines.map { it.role }.toSet(), StarterWizard.defaultParts(s) + StarterWizard.Part.Bass, s.melody.acid != null)
            // The bass never plays in the Intro: the badge says « Groove, Drop and Break », the sentence must not say « every section ».
            assertTrue(s.id, plan.parts.getValue(StarterWizard.Part.Bass).size < plan.count)
            val notes = partNotes(s, StarterWizard.Part.Bass, plan)
            assertTrue(s.id, short in notes && everywhere !in notes)
            // Without a plan (the part not chosen yet), the same.
            assertTrue(s.id, short in partNotes(s, StarterWizard.Part.Bass))
        }
        // A signature that does play everywhere (the chords of a style that signs with them) still says so.
        val pad = StarterWizard.STYLES.firstOrNull { it.melody.signature == StarterWizard.Part.Pad }
        if (pad != null) {
            val plan = StarterWizard.planFor(pad, pad.lines.map { it.role }.toSet(), StarterWizard.defaultParts(pad) + StarterWizard.Part.Pad, false)
            assertEquals(plan.count, plan.parts.getValue(StarterWizard.Part.Pad).size)
            assertTrue(everywhere in partNotes(pad, StarterWizard.Part.Pad, plan))
        }
    }
}
