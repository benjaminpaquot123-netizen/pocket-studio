package com.pocketstudio.app.mixer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.model.MasterPro
import com.pocketstudio.model.Project
import com.pocketstudio.ui.PocketStudioTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Fix 2, n° 4: Master pro's « Libre » from a preset shows its slider (it did nothing), and the
 * slider stays while it slides over a preset's value (it went away at −14).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h1800dp-xxhdpi")
class MasterProFreeTargetTest {
    @get:Rule
    val compose = createComposeRule()

    private val project = MutableStateFlow(Project().let { p -> p.copy(mixer = p.mixer.copy(pro = MasterPro(targetLufs = -14f))) })

    private val controller = MasterProController(
        CoroutineScope(Dispatchers.Unconfined), ApplicationProvider.getApplicationContext(), project,
        { _, t -> project.value = project.value.let { p -> p.copy(mixer = p.mixer.copy(pro = t(p.mixer.pro ?: MasterPro()))) } },
        {}, { _, _, _ -> }, {}, { "free" },
    )

    private fun exists(tag: String) = compose.onAllNodes(androidx.compose.ui.test.hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun freeFromAPresetShowsItsSliderAndKeepsItOverPresets() {
        compose.setContent {
            PocketStudioTheme { Column(Modifier.verticalScroll(rememberScrollState())) { MasterProPanel(-8f, MasterProActions(controller)) {} } }
        }
        compose.onNodeWithTag("master_pro").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("pro_target_SPOTIFY").assertIsSelected()
        assertEquals(false, exists("pro_target_slider"))
        // « Libre » from Spotify: chosen, its slider there, at −14.
        compose.onNodeWithTag("pro_target_free").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("pro_target_free").assertIsSelected()
        compose.onNodeWithTag("pro_target_SPOTIFY").assertIsNotSelected()
        assertEquals(true, exists("pro_target_slider"))
        // Slid to −12, then over −16 (Apple), then to −15: the slider never goes.
        for (v in listOf(-12f, -16f, -15f)) {
            compose.onNodeWithTag("pro_target_slider").performSemanticsAction(SemanticsActions.SetProgress) { it(v) }
            compose.waitForIdle()
            assertEquals(v, project.value.mixer.pro!!.targetLufs!!, 0f)
            assertEquals("slider at $v", true, exists("pro_target_slider"))
            compose.onNodeWithTag("pro_target_free").assertIsSelected()
        }
        // A preset: back to it, the slider gone.
        compose.onNodeWithTag("pro_target_CLUB").performClick()
        compose.waitForIdle()
        assertEquals(-9f, project.value.mixer.pro!!.targetLufs!!, 0f)
        compose.onNodeWithTag("pro_target_CLUB").assertIsSelected()
        assertEquals(false, exists("pro_target_slider"))
    }
}
