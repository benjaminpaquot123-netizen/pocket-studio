package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FxPresetsTest {
    @Test
    fun presetsAreValidAndRecognised() {
        assertEquals(FxPresets.ALL.size, FxPresets.ALL.map { it.id }.toSet().size)
        val base = Channel(volume = 0.6f, pan = -0.3f, mute = true)
        for (p in FxPresets.ALL) {
            assertTrue(p.name, p.category in FxPresets.CATEGORIES && p.description.isNotBlank())
            assertTrue(p.name, p.inserts.size <= Mixer.INSERTS && p.inserts.all { it.effect != EffectType.NONE && it == it.sanitized() })
            val c = base.withFx(p)
            // The fader, pan and mute stay; the effects are the preset's.
            assertEquals(0.6f, c.volume)
            assertEquals(-0.3f, c.pan)
            assertTrue(c.mute)
            assertEquals(p.inserts, c.inserts.filter { it.effect != EffectType.NONE })
            assertEquals(p, c.matchingFx())
        }
        // "Sans effet" puts everything back at rest; a hand-made mix matches nothing.
        val none = base.withFx(FxPresets.ALL.first()).withFx(FxPresets.NONE)
        assertTrue(none.inserts.all { it.effect == EffectType.NONE } && none.sends.all { it == 0f })
        assertEquals(Macros.DEFAULTS, none.macros)
        assertNull(Channel(macros = listOf(0.5f, 0.33f, 0f, 0f)).matchingFx())
    }

    @Test
    fun macrosAreSavedAndChecked() {
        val p = Project(mixer = Mixer(channels = List(Mixer.CHANNELS) { Channel(macros = listOf(0.1f, 0.2f, 0.3f, 0.4f)) }))
        assertEquals(listOf(0.1f, 0.2f, 0.3f, 0.4f), ProjectJson.decode(ProjectJson.encode(p)).mixer.channels[3].macros)
        assertEquals(Macros.DEFAULTS, ProjectJson.decode(ProjectJson.encode(Project())).mixer.channels[0].macros)
        assertEquals(listOf(0.5f, 1f, 0f, 0f), Macros.sanitized(listOf(Float.NaN, 7f)))
    }
}
