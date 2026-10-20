package com.pocketstudio.app.ui

import com.pocketstudio.model.PadSample
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.copySceneTo
import com.pocketstudio.model.switchScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneInfoTest {
    @Test
    fun theSceneShownSaysWhatItIs() {
        val style = StarterWizard.style("house")!!
        val p = StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? })
        assertEquals("", sceneInfo(p))
        assertTrue(sceneInfo(p.switchScene(3)).startsWith("Scène D : nouvelle et vide"))
        assertTrue(sceneInfo(p.copySceneTo(3).switchScene(3)).startsWith("Scène D : copie de la scène A"))
    }
}
