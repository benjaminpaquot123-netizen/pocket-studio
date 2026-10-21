package com.pocketstudio.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ToolPageTest {
    @Test
    fun toolsOpenOnHelpTheFirstTimeThenOnThePageLastChosen() {
        // Nothing saved yet: Aide, where a lost beginner looks (not Réglages).
        assertEquals(ToolPage.Aide, ToolPage.initial(null))
        assertEquals(ToolPage.Midi, ToolPage.initial("Midi"))
        assertEquals(ToolPage.Reglages, ToolPage.initial("Reglages"))
        // A page that no longer exists (an older version): Aide again.
        assertEquals(ToolPage.Aide, ToolPage.initial("Ancienne"))
    }
}
