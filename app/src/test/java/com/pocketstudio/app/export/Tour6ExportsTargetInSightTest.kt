package com.pocketstudio.app.export

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.mixer.TargetPick
import com.pocketstudio.model.Project
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 6, seat A c (E1), replayed on the bench in its conditions (French, phone, 100 %, Mode simple): the « Partager »
 * tab of a finished song (nothing to fix) has about 500 dp between the app's bars. « Streaming » and « Club » must be
 * whole in sight when it opens: one touch on the chip, one on « Partager », no scroll.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-port-hdpi", fontScale = 1.0f)
class Tour6ExportsTargetInSightTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun streamingAndClubAreInSightWhenThePageOpens() {
        I18n.lang = Lang.FR
        val ui = ShareSheetUi(
            name = "Trap", seconds = 178, parts = 26, checks = emptyList(), projectName = "Trap", lufs = -13.0, exportLufs = -8.5,
            target = TargetPick.of(Project(style = "trap")),
        )
        compose.setContent {
            PocketStudioTheme { Box(Modifier.fillMaxWidth().height(2000.dp)) { ShareSheetContent(ui, ShareSheetActions(), page = true, simple = true) } }
        }
        compose.waitForIdle()
        // In sight when the page opens (checked on the bench in the seat's conditions): the target comes before
        // « Tout est prêt », right under the level, not under the checks at the bottom of what scrolls.
        fun top(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().single().positionInRoot.y
        val chips = top("share_target_CLUB")
        val ready = top("share_check_ok")
        assertTrue("« Club » ($chips px) comes before « Tout est prêt » ($ready px)", chips < ready)
    }

    /**
     * Bench replay after the « ? » was added (screen 8): the label's row grew to the « ? »'s 48 dp and the target no longer
     * fitted whole above the pinned row on a phone, so the page opened without « Streaming » and « Club ». The target
     * (its words, its chips and its « ? ») keeps to two rows' room: at most 110 dp on a 360 dp phone.
     */
    @Test
    fun theSimpleTargetStaysCompact() {
        I18n.lang = Lang.FR
        val ui = ShareSheetUi(
            name = "Trap", seconds = 178, parts = 26, checks = emptyList(), projectName = "Trap", exportLufs = -8.5,
            target = TargetPick.of(Project(style = "trap")),
        )
        compose.setContent {
            PocketStudioTheme { Box(Modifier.fillMaxWidth().height(2000.dp)) { ShareSheetContent(ui, ShareSheetActions(), page = true, simple = true) } }
        }
        compose.waitForIdle()
        val n = compose.onAllNodesWithTag("share_target", useUnmergedTree = true).fetchSemanticsNodes().single()
        val dp = n.size.height / compose.density.density
        assertTrue("the Simple target takes $dp dp (at most 110)", dp <= 110f)
    }
}
