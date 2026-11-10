package com.pocketstudio.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.export.ExportOptions
import com.pocketstudio.app.mixer.ExportCard
import com.pocketstudio.app.mixer.MasterProActions
import com.pocketstudio.app.mixer.MasterProController
import com.pocketstudio.app.mixer.MasterProPanel
import com.pocketstudio.app.mixer.MixerActions
import com.pocketstudio.app.rhythm.ExportUi
import com.pocketstudio.app.rhythm.ExportWhat
import com.pocketstudio.app.rhythm.ExportFormat
import com.pocketstudio.model.MasterPro
import com.pocketstudio.model.Project
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.PocketStudioTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/**
 * Hands-on audit, chantier 2 (G4, rule 13 of docs/DESIGN.md « Un repli montre ce qu'il cache »; recette tel-06: « le
 * repli Avancé n'est pas assez voyant… il cache pourtant un réglage dont dépend l'export »): every fold the audit lists
 * is the shared `Disclosure`. Closed, it says what it hides (its summary is read with its name), it says it is closed
 * (TalkBack: « Développer »), and its summary is never cut, on a small phone at 130 %.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h2400dp")
class DisclosureEverywhereTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @After
    fun end() = RuntimeEnvironment.setFontScale(1f)

    private val project = MutableStateFlow(Project().let { it.copy(mixer = it.mixer.copy(pro = MasterPro(ceilingDb = -0.5f))) })

    @Test
    fun aClosedFoldSaysWhatItHides() {
        RuntimeEnvironment.setFontScale(1.3f)
        val pro = MasterProActions(
            MasterProController(CoroutineScope(Dispatchers.Unconfined), ApplicationProvider.getApplicationContext(), project, { _, _ -> }, {}, { _, _, _ -> }, {}, { "shot" }),
        )
        compose.setContent {
            PocketStudioTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    ExportCard(ExportUi(what = ExportWhat.Song, format = ExportFormat.Wav, parts = 3, songBars = 24, pro = ExportOptions(rate = 96000)), MixerActions(pro = pro), hasSong = true)
                    NeonCard { MasterProPanel(pro) }
                }
            }
        }
        compose.waitForIdle()
        // « Avancé » of the export card, closed: what it holds is said on it (one item changed: the rate, in « Fichier final »).
        closed("export_advanced", "Avancé", listOf("Piste par piste", "FLAC", "MIDI", "96 kHz"))
        // « Avancé : master pro », closed: the ceiling (changed here) said on it.
        closed("master_pro", "Avancé : master pro", listOf("Plafond −0,5 dB"))
        // Opened, « Fichier final » is a fold of its own, its rate and bits said on it.
        compose.onNodeWithTag("export_advanced").performClick()
        compose.waitForIdle()
        assertTrue("« Avancé » touched: it says it is open", actionsOf("export_advanced").let { SemanticsActions.Collapse in it && SemanticsActions.Expand !in it })
        closed("export_final", "Fichier final", listOf("96 kHz", "24 bits"))
        // The master's « Effets : … » (the mixer screen, too big to compose here): the same shared fold.
        val mixer = File("src/main/java/com/pocketstudio/app/mixer/MixerScreen.kt").readText()
        val toggle = mixer.substringAfter("private fun EffectsToggle(").substringBefore("\n}")
        assertTrue("The master's « Effets » fold is not the shared Disclosure", "Disclosure(" in toggle)
    }

    private fun actionsOf(tag: String) = compose.onNodeWithTag(tag).fetchSemanticsNode().config.map { it.key }.toSet()

    /** The fold [tag]: closed, read as its [label] then each of [summary], and none of its words cut. */
    private fun closed(tag: String, label: String, summary: List<String>) {
        val node = compose.onNodeWithTag(tag).fetchSemanticsNode()
        val read = (node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } +
            node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()).joinToString(" | ")
        assertTrue("$tag reads « $read »: not its name « $label » first", read.startsWith(label))
        val missing = summary.filter { it !in read }
        assertTrue("$tag closed reads « $read »: what it hides is not said ($missing)", missing.isEmpty())
        val actions = node.config.map { it.key }.toSet()
        assertTrue("$tag closed: TalkBack is not told it opens (no « expand » action)", SemanticsActions.Expand in actions && SemanticsActions.Collapse !in actions)
        val nodes = compose.onAllNodes(SemanticsMatcher("any") { true }, useUnmergedTree = true).fetchSemanticsNodes()
        val cut = nodes.filter { under(it, node) && it.config.contains(SemanticsProperties.Text) }.mapNotNull { n ->
            layoutOf(n)?.takeIf { it.hasVisualOverflow || (0 until it.lineCount).any { l -> it.isLineEllipsized(l) } }?.let { n.config[SemanticsProperties.Text].joinToString { t -> t.text } }
        }
        assertEquals("$tag at 130 % on a 360 dp phone: words cut", emptyList<String>(), cut)
    }

    private fun under(n: SemanticsNode, a: SemanticsNode): Boolean {
        var p = n.parent
        while (p != null) { if (p.id == a.id) return true; p = p.parent }
        return false
    }

    private fun layoutOf(n: SemanticsNode): TextLayoutResult? {
        val get = n.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action ?: return null
        val list = mutableListOf<TextLayoutResult>()
        return if (get(list)) list.firstOrNull() else null
    }
}
