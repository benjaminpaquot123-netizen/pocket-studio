package com.pocketstudio.app.tuto

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.rhythm.kitName
import com.pocketstudio.model.DefaultNames
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.PadVoice
import com.pocketstudio.model.Project
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 9 lot L5a, F061: the ready step said « le kit du disco est chargé » (t9-20), « Já pronto: o kit… minimal »
 * (t9-48), « kick, clap… 909 » (t9-58) while the pads were the studio's and the kit's card still offered the
 * download; once the pack was on the pads, « Kit : Perso » (t9-35, a « Rumble » pad keeps its name).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w392dp-h852dp")
class Tour9TutoL5aKitReadyTest {
    @get:Rule
    val compose = createComposeRule()

    private fun texts(): List<String> = compose.onAllNodesWithTag("tuto_text", useUnmergedTree = true).fetchSemanticsNodes()
        .flatMap { n -> n.config.getOrNull(SemanticsProperties.Text)?.map { it.text } ?: emptyList() }

    @Test
    fun theReadyStepSaysTheKitIsNotLoadedWhileItsCardIsOffered() {
        fun ui(style: String, level: TutoLevel): TutoUi {
            val chapters = TutoCourse.of(style)!!.chaptersFor(level)
            val ch = chapters.indexOfFirst { c -> c.steps.any { it.id == "ready" } }
            val at = chapters[ch].steps.indexOfFirst { it.id == "ready" }
            return TutoUi(TutoProgress(style, level, current = "ready"), chapters, ch, at)
        }
        var shown by mutableStateOf(ui("disco", TutoLevel.Facile))
        compose.setContent {
            PocketStudioTheme {
                TutoPanel(shown, canShow = false, actions = TutoActions(), reducedMotion = true, kit = { Box(Modifier.fillMaxWidth().height(160.dp).testTag("kit_probe")) })
            }
        }
        val bad = ArrayList<String>()
        for (style in listOf("disco", "minimal", "retro_techno", "afro_house", "industrial_techno")) for (level in listOf(TutoLevel.Facile, TutoLevel.Pro)) {
            shown = ui(style, level)
            compose.waitForIdle()
            val t = texts().joinToString(" ")
            if ("Plus tard" !in t || Regex("chargé|déjà celui|Déjà prêt|kit est celui").containsMatchIn(t)) bad += "$style/$level: $t"
        }
        assertTrue("the ready step claims the style's kit while its card is offered:\n" + bad.joinToString("\n"), bad.isEmpty())
    }

    @Test
    fun aLoadedPackIsNamedEvenWithARumblePad() {
        val kit = "Industrial techno"
        val lanes = mapOf(0 to DefaultNames.kitPad(kit, 0), 1 to PadVoice.NAMES[PadVoice.RUMBLE], 3 to DefaultNames.kitPad(kit, 3))
        var p = Project()
        for ((lane, name) in lanes) p = p.copy(drums = p.drums.withSample(lane, PadSample("$lane.wav", name)))
        assertEquals(kit, kitName(p))
    }
}
