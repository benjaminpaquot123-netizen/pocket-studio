package com.pocketstudio.model

import com.pocketstudio.model.StarterWizard.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Benjamin's list n° 7: a line's sound tried in the assistant goes through what the song puts on it. */
class LineEffectsTest {
    @Test
    fun theAcidcoreKickIsSaturatedOnItsPadAsInTheSong() {
        val acidcore = StarterWizard.style("acidcore")!!
        assertTrue(StarterWizard.lineHasEffects(acidcore, Role.Kick))
        val pad = Mixer.padChannel(Role.Kick.lane)
        val distortion = FxPresets.byId("distortion")!!
        val strip = StarterWizard.effectsMixer(acidcore, setOf(Role.Kick), acid = false).channels[pad]
        assertEquals(Project().mixer.channels[pad].withFx(distortion).sanitized().inserts, strip.inserts)
    }

    @Test
    fun everyLineAStyleTreatsHasEffects() {
        StarterWizard.STYLES.forEach { style ->
            style.effects.forEach { fx ->
                style.lines.filter { it.role.id == fx.target || fx.target == "drums" || fx.target == "master" }.forEach { line ->
                    assertTrue("${style.id} ${line.role.id} (${fx.target})", StarterWizard.lineHasEffects(style, line.role))
                }
            }
        }
    }
}
