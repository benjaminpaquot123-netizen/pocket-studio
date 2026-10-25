package com.pocketstudio.model

import com.pocketstudio.model.PadSample
import com.pocketstudio.model.Project
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.switchScene
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Benjamin's jump up kick ended hard left. The model's own writers of a pad's pan (the
 * assistant's mix, its PANS_V2 refresh of every scene, the scene switches that copy the pans)
 * never touch the kick: every style's song, with the kit's voices or with samples, keeps its
 * kick centred in every scene, also once each scene has been shown (what the song's follow does).
 */
class WizardKickPanTest {
    private fun Project.kickPans(): List<Float> = (listOf(drums) + allScenes.map { it.drums }).map { it.lanes[0].pan }

    @Test
    fun everyStyleKeepsItsKickCentredInEveryScene() {
        for (style in StarterWizard.STYLES) {
            val parts = StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() }
            for (samples in listOf(false, true)) {
                val sounds = style.lines.associate { it.role to (if (samples) PadSample("${it.role.id}.wav", it.role.id) else null) }
                var p = StarterWizard.build(style, sounds, style.melody.mood, parts)
                assertEquals("${style.id} samples=$samples", List(p.allScenes.size + 1) { 0f }, p.kickPans())
                for (part in p.song) p = p.switchScene(part.scene)
                assertEquals("${style.id} samples=$samples, the song followed", List(p.allScenes.size + 1) { 0f }, p.kickPans())
            }
        }
    }
}
