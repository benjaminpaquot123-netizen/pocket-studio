package com.pocketstudio.app.export

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
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
 * Tour 6, seat A c (E4): in « Mes fichiers » a long file name with no spaces (« Trap-morceau-club-20261112-195612.wav »)
 * was wrapped by the renderer at any character, cutting a word or a number in its middle (« pocketstudio.z / ip »,
 * « 19 / 5558.wav »). A name now stays on one line, shortened with « … » in its middle: its height is a single line's,
 * however long it is.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-port-hdpi", fontScale = 1.0f)
class Tour6ExportsNameWrapTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun french() { I18n.lang = Lang.FR }

    private fun export(name: String, id: Long) =
        MyExport(name, Uri.parse("content://media/external/audio/media/$id"), 2_300_000L, 1_700_000_000L, "audio/wav")

    @Test
    fun aLongNameStaysOnOneLineLikeAShortOne() {
        I18n.lang = Lang.FR
        val shortName = "Trap.wav"
        val longName = "Trap-morceau-club-20261112-195612-final-master.wav"
        compose.setContent {
            PocketStudioTheme {
                // A phone column's width for the name (the « Renvoyer » button and padding take the rest).
                Box(Modifier.width(220.dp)) {
                    MyExportsList(listOf(export(shortName, 1), export(longName, 2))) {}
                }
            }
        }
        compose.waitForIdle()
        val rows: List<SemanticsNode> = compose.onAllNodesWithTag("my_export", useUnmergedTree = true).fetchSemanticsNodes()
        // The name is the first text of each row; its line box is that text node's height.
        val short = rows[0].children.first().size.height.toFloat()
        val long = rows[1].children.first().size.height.toFloat()
        assertTrue("a short name should be one line (got ${short}px)", short > 0f)
        assertTrue(
            "a long name must stay one line like a short one (short=${short}px, long=${long}px)",
            long <= short * 1.5f,
        )
    }
}
