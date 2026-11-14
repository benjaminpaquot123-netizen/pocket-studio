package com.pocketstudio.app.export

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.text.TextLayoutResult
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
 * Tour 6, seat A c then control seat Ac-2 (E4): in « Mes fichiers » a long file name was cut inside a word or a number
 * (« pocketstudio.z / ip », « 19 / 5558.wav », then « Trap-morceau-clu…61113-144507.wav » with a middle « … »). A name
 * now goes to the next line only between its parts (after « - » or « _ », before « . »), and is shown whole: no line
 * ends inside a word or a number, nothing is shortened.
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

    private val names = listOf(
        "Trap-morceau-club-20261113-144507.wav",
        "Trap-morceau-streaming-20261113-144445.m4a",
        "Trap-20261113-144539.pocketstudio.zip",
    )

    @Test
    fun aLongNameBreaksOnlyBetweenItsPartsAndStaysWhole() {
        I18n.lang = Lang.FR
        compose.setContent {
            PocketStudioTheme {
                // The name's column on a 360 dp phone (the dialog, its padding and « Renvoyer » take the rest).
                Box(Modifier.width(200.dp)) { MyExportsList(names.mapIndexed { i, n -> export(n, i.toLong()) }) {} }
            }
        }
        compose.waitForIdle()
        val wrong = mutableListOf<String>()
        // The name is the first text of each row.
        val nameNodes = compose.onAllNodesWithTag("my_export", useUnmergedTree = true).fetchSemanticsNodes().map { it.children.first() }
        nameNodes.forEach { node ->
            val layouts = mutableListOf<TextLayoutResult>()
            node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
            val t = layouts.single()
            val text = t.layoutInput.text.text
            // Whole: the name's every character is drawn (the lines may be cut by hand, with « \n »), no « … ».
            val drawn = text.filter { it != '\n' && it != '\u200B' }
            if ("…" in drawn || drawn !in names) wrong += "« $text » is shortened"
            for (line in 0 until t.lineCount - 1) {
                val end = t.getLineEnd(line)
                // A line cut by hand ends with « \n »: the character that counts is the one before it.
                val cut = if (text[end - 1] == '\n') end - 1 else end
                val before = text[cut - 1]
                val after = text.getOrNull(end).takeIf { cut == end } ?: text.getOrNull(end)
                val between = !before.isLetterOrDigit() || after == null || !after.isLetterOrDigit()
                if (!between) wrong += "« $text » breaks inside a word or a number: « ${text.substring(0, end)} / ${text.substring(end)} »"
            }
        }
        assertTrue("names checked: ${nameNodes.size}", nameNodes.size == names.size)
        assertTrue(wrong.joinToString("\n"), wrong.isEmpty())
    }
}
