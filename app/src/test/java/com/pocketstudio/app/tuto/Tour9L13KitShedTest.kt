package com.pocketstudio.app.tuto

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 9 (L13, fresh review): at 200 % the kit offer come late over the grid's cells lets the panel's head go (the
 * instruction keeps its line). Once the offer is gone (« Plus tard », a download done), the head comes back on the same
 * step: the chapter's title and the fold button are in sight again.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp", fontScale = 2f)
class Tour9L13KitShedTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun end() = TutoHints.reset()

    private fun has(tag: String) = compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun theHeadComesBackOnceTheLateKitOfferIsGone() {
        val style = "house"
        val chapters = TutoCourse.of(style)!!.chaptersFor(TutoLevel.Facile)
        val starts = chapters.indices.map { it to 0 }.filter { (c, i) -> chapters[c].steps[i].id != "ready" }
        var at by mutableStateOf(starts.first())
        var offer by mutableStateOf(false)
        compose.setContent {
            key(at) {
                TutoHints.gridInSight = true
                TutoHints.cells = 0 to setOf(0, 4)
                val (c, i) = at
                val shown = TutoUi(TutoProgress(style, TutoLevel.Facile, current = chapters[c].steps[i].id), chapters, c, i, open = true)
                PocketStudioTheme {
                    TutoPanel(shown, canShow = true, actions = TutoActions(), reducedMotion = true,
                        kit = if (offer) ({ TutoKitCard("House", "4 Mo", progress = null, error = null, download = {}, later = {}) }) else null)
                }
            }
        }
        var shedSeen = 0
        val stuck = ArrayList<String>()
        for (p in starts) {
            offer = false
            at = p
            compose.waitForIdle()
            offer = true
            compose.waitForIdle()
            if (!has("tuto_fold")) shedSeen++
            offer = false
            compose.waitForIdle()
            if (!has("tuto_fold")) stuck += chapters[p.first].steps[p.second].id
        }
        assertTrue("the late offer never let the head go: this test proves nothing", shedSeen > 0)
        assertTrue("the head stays gone once the offer left: $stuck", stuck.isEmpty())
    }
}
