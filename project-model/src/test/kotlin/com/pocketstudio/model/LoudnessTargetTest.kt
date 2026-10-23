package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The master's « Cible » (phase 33): per style, saved with the project, off for the projects of before. */
class LoudnessTargetTest {
    @Test
    fun everyStyleHasItsLoudness() {
        StarterWizard.STYLES.forEach { s ->
            val lufs = LoudnessTargets.BY_STYLE[s.id]
            assertTrue("${s.id} has no loudness", lufs != null)
            assertTrue("${s.id}: $lufs", lufs!! in -12f..-6f)
        }
        // son.md 2.4, a few of each family.
        assertEquals(-7f, LoudnessTargets.forStyle("techno"))
        assertEquals(-6f, LoudnessTargets.forStyle("gabber"))
        assertEquals(-8f, LoudnessTargets.forStyle("house"))
        assertEquals(-9f, LoudnessTargets.forStyle("deep_house"))
        assertEquals(-8.5f, LoudnessTargets.forStyle("trap"))
        assertEquals(-10f, LoudnessTargets.forStyle("boom_bap"))
        assertEquals(-12f, LoudnessTargets.forStyle("lofi"))
        // A project made by hand, or a style that does not exist any more.
        assertEquals(LoudnessTargets.DEFAULT_LUFS, LoudnessTargets.forStyle(null))
        assertEquals(LoudnessTargets.DEFAULT_LUFS, LoudnessTargets.forStyle("inconnu"))
    }

    @Test
    fun theTargetGivesTheEngineItsLoudness() {
        assertNull(LoudnessTargets.lufs(LoudnessTarget.OFF, "techno"))
        assertEquals(-7f, LoudnessTargets.lufs(LoudnessTarget.AUTO, "techno"))
        assertEquals(-8f, LoudnessTargets.lufs(LoudnessTarget.CLUB, "lofi"))
        assertEquals(-14f, LoudnessTargets.lufs(LoudnessTarget.STREAMING, "gabber"))
        assertEquals("−8", LoudnessTargets.formatTarget(-8f))
        assertEquals("−8,5", LoudnessTargets.formatTarget(-8.5f))
    }

    @Test
    fun anOldProjectKeepsItsMaster() {
        // Saved before phase 33: no target, no style; and saved again, still nothing written.
        val old = ProjectJson.decode("""{"name":"Ancien","tempoBpm":128.0,"mixer":{"masterDb":-3.0}}""")
        assertEquals(LoudnessTarget.OFF, old.mixer.target)
        assertNull(old.style)
        val text = ProjectJson.encode(old)
        assertFalse(text, "\"target\"" in text)
        assertFalse(text, "\"style\"" in text)
        // A new blank project keeps its master as before too (it has no style to follow).
        assertEquals(LoudnessTarget.OFF, Project(name = "Neuf").asNewProject().mixer.target)
        // A value from a later version falls back to none.
        assertEquals(LoudnessTarget.OFF, ProjectJson.decode("""{"mixer":{"target":"PLUS_FORT"}}""").mixer.target)
    }

    @Test
    fun anAssistantSongFollowsItsStyle() {
        val style = StarterWizard.style("techno")!!
        val song = StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? })
        assertEquals(LoudnessTarget.AUTO, song.mixer.target)
        assertEquals("techno", song.style)
        // Saved and read back, chosen by hand.
        val back = ProjectJson.decode(ProjectJson.encode(song.copy(mixer = song.mixer.copy(target = LoudnessTarget.STREAMING))))
        assertEquals(LoudnessTarget.STREAMING, back.mixer.target)
        assertEquals("techno", back.style)
        assertEquals(-14f, LoudnessTargets.lufs(back.mixer.target, back.style))
    }
}
