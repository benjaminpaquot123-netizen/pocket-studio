package com.pocketstudio.app.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.PocketStudioTheme
import com.pocketstudio.ui.layoutDirectionOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 5, lot « langues » (promise t5-Bd-3, seat B d in Arabic, Mode simple: after a « تراجع », « إعادة » showed exactly
 * where « تراجع » was and « تراجع » slid away: her second touch brought the part back). ↶ Annuler stays under the finger
 * when ↷ Rétablir comes, in a right-to-left language as in French, on a phone and on a tablet.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1280dp-h800dp-land-hdpi")
class ThumbUndoStaysTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun back() {
        I18n.lang = Lang.FR
    }

    private fun undoMoves(lang: Lang): Pair<Float, Float> {
        I18n.lang = lang
        var canRedo by mutableStateOf(false)
        val actions = ThumbBarActions(undo = {}, redo = {}, togglePlay = {}, tempo = {}, more = {}, share = {})
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirectionOf(lang.rtl)) {
                PocketStudioTheme { ThumbBar(ThumbBarUi(playing = false, canUndo = true, canRedo = canRedo, tempoBpm = 84, redoAlways = false), actions) }
            }
        }
        val before = compose.onNodeWithTag("thumb_undo").getBoundsInRoot().left.value
        canRedo = true
        compose.waitForIdle()
        val after = compose.onNodeWithTag("thumb_undo").getBoundsInRoot().left.value
        return before to after
    }

    @Test
    fun undoStaysWhereItWasInArabic() {
        val (before, after) = undoMoves(Lang.AR)
        assertEquals("↶ moved when ↷ came", before, after, 0.5f)
    }

    @Test
    fun undoStaysWhereItWasInFrench() {
        val (before, after) = undoMoves(Lang.FR)
        assertEquals("↶ moved when ↷ came", before, after, 0.5f)
    }
}
