package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Test

/** A new project's master starts 6 dB down (headroom for the limiter); saved projects keep theirs. */
class NewProjectLevelTest {
    @Test
    fun aNewProjectStartsWithHeadroom() {
        assertEquals(Mixer.NEW_PROJECT_DB, Project(name = "Neuf").asNewProject().mixer.masterDb)
        assertEquals(-6f, Mixer.NEW_PROJECT_DB)
        // One that chose its own gain keeps it.
        val chosen = Project(mixer = Mixer(masterDb = -2.5f))
        assertEquals(-2.5f, chosen.asNewProject().mixer.masterDb)
        // The demos too (through the same door as the blank one).
        Demos.ALL.forEach { (_, _, make) -> assertEquals(Mixer.NEW_PROJECT_DB, make().asNewProject().mixer.masterDb) }
    }

    @Test
    fun anOldProjectLoadsAsItWasSaved() {
        // Saved before the master gain existed, and saved at 0 dB: both stay at 0 dB.
        val before = ProjectJson.decode("""{"name":"Ancien","tempoBpm":128.0}""")
        assertEquals(0f, before.mixer.masterDb)
        val atZero = ProjectJson.decode(ProjectJson.encode(Project(name = "Zéro")))
        assertEquals(0f, atZero.mixer.masterDb)
        val trimmed = ProjectJson.decode(ProjectJson.encode(Project(name = "Neuf").asNewProject()))
        assertEquals(Mixer.NEW_PROJECT_DB, trimmed.mixer.masterDb)
    }
}
