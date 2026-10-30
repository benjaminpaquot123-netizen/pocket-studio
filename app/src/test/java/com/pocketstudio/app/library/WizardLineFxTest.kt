package com.pocketstudio.app.library

import com.pocketstudio.model.PackEntry
import com.pocketstudio.model.PackManifest
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.StarterWizard.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Benjamin's list n° 7 (APK 233, Acidcore, step Kick): the kick chosen was heard clean while the song
 * saturates it (gabber), and only « Sons intégrés » and a pack already there were offered.
 */
class WizardLineFxTest {
    private val acidcore = StarterWizard.style("acidcore")!!
    private val kickOptions = soundCategories(Role.Kick, acidcore.id, emptyList()).first().options

    @Test
    fun aSoundTouchedIsHeardThroughItsLinesEffectsAsInTheSong() {
        val option = kickOptions.first()
        assertEquals(SoundRef.LineSound(Role.Kick, option), soundOnChoose(null, Role.Kick, option, acidcore))
        assertEquals(SoundRef.LineSound(Role.Kick, option), soundOnChoose(kickOptions.last().ref, Role.Kick, option, acidcore))
        // The beat heard with it goes on with the new sound (it already goes through the effects).
        assertEquals(SoundRef.LineDemo(Role.Kick, option), soundOnChoose(SoundRef.LineDemo(Role.Kick, kickOptions.last()), Role.Kick, option, acidcore))
        // Its tile says it is the one heard.
        assertTrue(tileHeard(SoundRef.LineSound(Role.Kick, option), Role.Kick, option))
    }

    @Test
    fun everyStyleLineWithEffectsIsHeardThroughThem() {
        var lines = 0
        StarterWizard.STYLES.forEach { style ->
            style.lines.forEach { line ->
                val options = soundCategories(line.role, style.id, emptyList()).first().options
                val heard = soundOnChoose(null, line.role, options.first(), style)
                if (StarterWizard.lineHasEffects(style, line.role)) {
                    lines++
                    assertEquals("${style.id} ${line.role.id}", SoundRef.LineSound(line.role, options.first()), heard)
                } else {
                    assertEquals("${style.id} ${line.role.id}: plain, as the song", options.first().ref, heard)
                }
            }
        }
        assertTrue("styles put effects on their lines", lines > 10)
    }

    @Test
    fun theStylesPacksAreOfferedAtTheLineTheMachineOfThatLineFirst() {
        val catalogue = listOf(
            PackEntry("tech_house", "Tech house", "a.zip", "0".repeat(64), 1),
            PackEntry("tr909", "TR-909", "b.zip", "0".repeat(64), 1),
            PackEntry("gabber", "Gabber", "c.zip", "0".repeat(64), 1),
        )
        val installed = listOf(PackManifest("tech_house", "Tech house"))
        assertEquals(listOf("gabber", "tr909"), missingStylePacks(acidcore, catalogue, installed, Role.Kick).map { it.id })
        assertEquals(listOf("tr909", "gabber"), missingStylePacks(acidcore, catalogue, installed, Role.Hat).map { it.id })
        assertEquals(listOf("tr909"), missingStylePacks(acidcore, catalogue, installed + PackManifest("gabber", "Gabber"), Role.Kick).map { it.id })
    }
}
