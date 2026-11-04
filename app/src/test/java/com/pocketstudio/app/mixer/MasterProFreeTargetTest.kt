package com.pocketstudio.app.mixer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.model.LoudnessTarget
import com.pocketstudio.model.LoudnessTargets
import com.pocketstudio.model.MasterPro
import com.pocketstudio.model.Project
import com.pocketstudio.model.TargetChoice
import com.pocketstudio.ui.PocketStudioTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit ultra round 1 (n° 14, seats A c, E d, F c): « Volume visé » and the master pro's « Cible de
 * volume » were two rows of targets, both selected at once (Club −8 above, Spotify −14 below, the
 * export taking the second) and « Club » read −8 in one and −9 in the other. Now one row is the one
 * target: a project saved with « Spotify −14 » in the pro row reads « Streaming · −14 LUFS », every
 * chip says LUFS, « Libre » keeps its slider over the others' values (fix 2, n° 4), a chip clears the
 * pro value, and the master pro panel has no target row of its own.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h1800dp-xxhdpi")
class MasterProFreeTargetTest {
    @get:Rule
    val compose = createComposeRule()

    private val project = MutableStateFlow(
        Project(style = "house").let { p -> p.copy(mixer = p.mixer.copy(target = LoudnessTarget.CLUB, pro = MasterPro(targetLufs = -14f))) },
    )

    private fun choose(c: TargetChoice, free: Float?) {
        project.value = project.value.let { p -> p.copy(mixer = LoudnessTargets.choose(p.mixer, c, free)) }
    }

    private val controller = MasterProController(
        CoroutineScope(Dispatchers.Unconfined), ApplicationProvider.getApplicationContext(), project,
        { _, t -> project.value = project.value.let { p -> p.copy(mixer = p.mixer.copy(pro = t(p.mixer.pro ?: MasterPro()))) } },
        {}, { _, _, _ -> }, {}, { "free" },
    )

    private fun exists(tag: String) = compose.onAllNodes(androidx.compose.ui.test.hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun oneRowOfTargetsWithLufsAndTheFreeSlider() {
        compose.setContent {
            PocketStudioTheme {
                val p by project.collectAsState()
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    MasterLoudnessPanel({ FloatArray(0) }, LoudnessTargets.choiceOf(p.mixer), p.mixer.pro?.targetLufs,
                        LoudnessTargets.forStyle(p.style, p.mixVersion), ::choose)
                    MasterProPanel(MasterProActions(controller))
                }
            }
        }
        // The project of before: Club in one row, Spotify −14 in the other. One choice is shown: the one the export used.
        compose.onNodeWithTag("loudness_target_STREAMING").assertIsSelected().assertTextContains("−14 LUFS", substring = true)
        compose.onNodeWithTag("loudness_target_CLUB").assertIsNotSelected().assertTextContains("−8 LUFS", substring = true)
        compose.onNodeWithTag("loudness_target_APPLE").assertTextContains("−16 LUFS", substring = true)
        assertEquals(false, exists("pro_target_slider"))
        // The master pro panel opened: no second row of targets.
        compose.onNodeWithTag("master_pro").performClick()
        compose.waitForIdle()
        assertEquals(false, exists("pro_target_SPOTIFY"))
        assertEquals(false, exists("pro_target_CLUB"))
        // « Libre » from Streaming: chosen, its slider there, at −14.
        compose.onNodeWithTag("pro_target_free").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("pro_target_free").assertIsSelected()
        compose.onNodeWithTag("loudness_target_STREAMING").assertIsNotSelected()
        assertEquals(true, exists("pro_target_slider"))
        // Slid to −12, then over −16 (Apple) and −14 (Streaming), then to −15: the slider never goes.
        for (v in listOf(-12f, -16f, -14f, -15f)) {
            compose.onNodeWithTag("pro_target_slider").performSemanticsAction(SemanticsActions.SetProgress) { it(v) }
            compose.waitForIdle()
            assertEquals(v, project.value.mixer.pro!!.targetLufs!!, 0f)
            assertEquals("slider at $v", true, exists("pro_target_slider"))
        }
        // Club: the one target, the pro value cleared, the slider gone, −8 LUFS everywhere (never −9 any more).
        compose.onNodeWithTag("loudness_target_CLUB").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("loudness_target_CLUB").assertIsSelected()
        assertNull(project.value.mixer.pro?.targetLufs)
        assertEquals(LoudnessTarget.CLUB, project.value.mixer.target)
        assertEquals(-8f, LoudnessTargets.of(project.value)!!, 0f)
        assertEquals(false, exists("pro_target_slider"))
        compose.onNodeWithTag("loudness_target_hint").assertTextContains("LUFS", substring = true)
    }
}
