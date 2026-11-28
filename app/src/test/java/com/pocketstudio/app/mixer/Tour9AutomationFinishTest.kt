package com.pocketstudio.app.mixer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import com.pocketstudio.app.audit.CutWords
import com.pocketstudio.app.audit.LayoutScan
import com.pocketstudio.app.learn.HelpLabelsOverlay
import com.pocketstudio.app.learn.SpotRegistry
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.AutomationLane
import com.pocketstudio.model.Channel
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Insert
import com.pocketstudio.model.Project
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 9, lot L2 (Mix), the curves' finishing touches and the Mix's titles, each test one acceptance phrase.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-port-hdpi")
class Tour9AutomationFinishTest {
    @get:Rule
    val compose = createComposeRule()

    private fun card(ui: AutomationUi, channels: List<Channel> = List(16) { Channel() }, actions: AutomationActions = AutomationActions()) {
        compose.setContent {
            PocketStudioTheme {
                Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) { AutomationCard(ui, channels, emptyList(), actions) }
            }
        }
        compose.waitForIdle()
    }

    /** The texts at or under the node tagged [tag]. */
    private fun texts(tag: String): List<String> =
        compose.onAllNodes((hasTestTag(tag) or hasAnyAncestor(hasTestTag(tag))) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Text), useUnmergedTree = true)
            .fetchSemanticsNodes().map { n -> n.config[SemanticsProperties.Text].joinToString(" ") { it.text } }.filter { it.isNotBlank() }

    /** F128 « Les valeurs de la courbe se lisent en entier (« 18,0 kHz », pas « 18,0 kH »). » */
    @Test
    @Config(fontScale = 1.3f)
    fun f128_theCurvesValuesAreWhole() {
        val channels = List(16) { if (it == 2) Channel().copy(inserts = listOf(Insert.of(EffectType.FILTER))) else Channel() }
        val cutoff = AutoTarget(AutoTarget.INSERT_PARAM, 2, 0, 0)
        card(AutomationUi(listOf(AutomationLane(cutoff))), channels)
        val scale = "auto_scale_${cutoff.pack()}"
        assertTrue("no kHz on the scale: ${texts(scale)}", texts(scale).any { it.endsWith("kHz") })
        val cut = CutWords.under(compose, scale)
        assertTrue("values cut: $cut", cut.isEmpty())
    }

    /** F129 « Une boucle de 4 mesures se compte en mesures sous sa courbe, comme dans l'appli ailleurs. » */
    @Test
    fun f129_aFourBarLoopIsCountedInBars() {
        card(AutomationUi(listOf(AutomationLane(AutoTarget(AutoTarget.VOLUME, 1))), steps = 64, stepsPerBar = 16))
        assertEquals(listOf("mesure"), texts("auto_time_unit_0"))
        assertEquals(listOf("1", "2", "3", "4"), texts("auto_time_0"))
    }

    /** A loop of one bar keeps its beats. */
    @Test
    fun f129_aOneBarLoopKeepsItsBeats() {
        card(AutomationUi(listOf(AutomationLane(AutoTarget(AutoTarget.VOLUME, 1))), steps = 16, stepsPerBar = 16))
        assertEquals(listOf("temps"), texts("auto_time_unit_0"))
    }

    /** F130 « Les choix de longueur d'une courbe disent de quelle piste elle est (« Pour : Synthé »). » */
    @Test
    fun f130_theLoopOptionsNameTheTrack() {
        val lanes = listOf(AutomationLane(AutoTarget(AutoTarget.VOLUME, 2)), AutomationLane(AutoTarget(AutoTarget.VOLUME, 0)))
        card(AutomationUi(lanes, steps = 16, stepsPerBar = 16, partSteps = 64), actions = AutomationActions(setLoopSteps = { _, _ -> }))
        assertEquals(listOf("Pour : Synthé"), texts("auto_len_for_0"))
        assertEquals(listOf("Sur 1 mesure."), texts("auto_len_drums_0"))
        assertEquals(listOf("Pour : Batterie"), texts("auto_len_for_1"))
        assertTrue(texts("auto_len_drums_1").toString(), texts("auto_len_drums_1").single().startsWith("Sur la batterie"))
    }

    /** « Volumes » and « Effets du pad » readable (4.5:1) on the Mix, and under the help labels' veil too. */
    @Test
    fun theMixTitlesReachTheContrast() {
        var veiled by androidx.compose.runtime.mutableStateOf(false)
        compose.setContent {
            PocketStudioTheme {
                Box(Modifier.fillMaxSize()) {
                    MixerScreen(Project().mixer.channels, { FloatArray(NativeEngine.METERS) }, MixerActions())
                    if (veiled) HelpLabelsOverlay(SpotRegistry(), hide = {})
                }
            }
        }
        for (veil in listOf(false, true)) {
            veiled = veil
            compose.waitForIdle()
            val density = compose.density.density
            val shown = texts("console")
            assertTrue("titles missing: $shown", shown.any { it == "Volumes" } && shown.any { "Effets du pad" in it })
            val low = LayoutScan.scan(compose, density, setOf(LayoutScan.Kind.CONTRAST))
                .filter { it.what == "Volumes" || "Effets du pad" in it.what }
            assertTrue("veil $veil: $low", low.isEmpty())
        }
    }
}
