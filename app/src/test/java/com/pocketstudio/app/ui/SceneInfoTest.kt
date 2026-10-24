package com.pocketstudio.app.ui

import com.pocketstudio.model.PadSample
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.copySceneTo
import com.pocketstudio.model.switchScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneInfoTest {
    @Test
    fun theSceneShownSaysWhatItIs() {
        // A song: its sections and their variants (Pré-refrain, Refrain 2…), the next slot free.
        val style = StarterWizard.style("pop")!!
        val p = StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? })
        assertEquals("", sceneInfo(p))
        val free = p.allScenes.size
        val letter = com.pocketstudio.model.Scene.NAMES[free]
        assertTrue(sceneInfo(p.switchScene(free)).startsWith("Partie $letter : nouvelle et vide"))
        assertTrue(sceneInfo(p.copySceneTo(free).switchScene(free)).startsWith("Partie $letter : copie de la partie A"))
    }
}
