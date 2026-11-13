package com.pocketstudio.app.export

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.text.TextLayoutResult
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.pocketstudio.app.ui.UiScaled
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.PocketStudioTheme
import com.pocketstudio.ui.layoutDirectionOf
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.TimeZone

/**
 * Tour 6, R14 (seats A c #43, fr, and E b #16, nl, small phone): « Mes fichiers » broke a file's name inside a word
 * (« pocketstudio.z / ip ») and lost its end to the closing « … » (« …21361… »): which one is the .wav, the .m4a, the
 * .zip? Each name keeps its extension in sight and its words whole: the name itself, or its start and its end around
 * a « … » when two lines are not enough. On a small phone (360 dp), in five languages (« Renvoyer » is longer in
 * German and Dutch, Arabic right to left, Hindi's tall marks), at 100 %, at 130 % (the interface's size) and at 200 %
 * (the phone's font).
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-xxhdpi")
class MyFilesNamesTest(private val code: String, private val scale: String, private val font: String) {
    @get:Rule
    val compose = createComposeRule()

    private var zone: TimeZone? = null

    @Before
    fun utc() {
        zone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun back() {
        I18n.lang = Lang.FR
        RuntimeEnvironment.setFontScale(1f)
        zone?.let(TimeZone::setDefault)
    }

    @Test
    fun eachNameKeepsItsExtension() {
        val lang = Lang.entries.first { it.code == code }
        I18n.lang = lang
        if (font != "1.0") RuntimeEnvironment.setFontScale(font.toFloat())
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirectionOf(lang.rtl)) {
                UiScaled(scale.toFloat()) {
                    PocketStudioTheme { Box(Modifier.fillMaxSize().background(Neon.Background)) { MyExportsDialog(shown = files) {} } }
                }
            }
        }
        compose.waitForIdle()
        // Images to look at (never committed): under -Proborazzi.test.record=true, build/lot5-captures/.
        if (System.getProperty("roborazzi.test.record") == "true") captureScreenRoboImage("build/lot5-captures/my_files_${code}_${scale}_$font.png")
        val problems = files.indices.flatMap { i ->
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(i)
            compose.waitForIdle()
            problemsOf(files[i].name)
        }
        assertTrue("$code, interface $scale, font $font:\n" + problems.joinToString("\n"), problems.isEmpty())
    }

    /** What is wrong with how « Mes fichiers » shows [name] (nothing: its words whole, its extension in sight). */
    private fun problemsOf(name: String): List<String> {
        val ext = name.substring(name.lastIndexOf('.'))
        // A project's .zip and its video share their start: the text closest to the name (its start and its end).
        fun near(t: String): Int {
            val flat = t.replace("\n", "")
            return flat.commonPrefixWith(name).length + flat.commonSuffixWith(name).length
        }
        val node = compose.onAllNodes(hasAnyAncestor(hasTestTag("my_export")), useUnmergedTree = true).fetchSemanticsNodes()
            .filter { textOf(it).replace("\n", "").startsWith(name.take(4)) }.maxByOrNull { near(textOf(it)) } ?: return listOf("« $name »: not shown")
        val shown = textOf(node)
        val layout = mutableListOf<TextLayoutResult>().also { node.config[SemanticsActions.GetTextLayoutResult].action!!(it) }.first()
        val out = mutableListOf<String>()
        val last = layout.lineCount - 1
        if ((0..last).any { layout.isLineEllipsized(it) } || layout.getLineEnd(last, visibleEnd = true) < shown.length) {
            out += "« $shown »: its end hidden (« … » or lines dropped)"
        }
        val flat = shown.replace("\n", "")
        if (!flat.endsWith(ext)) out += "« $shown »: does not end with $ext"
        else if (layout.getLineForOffset(shown.length - ext.length) != layout.getLineForOffset(shown.length - 1)) out += "« $shown »: $ext split over two lines"
        // Its own words: the name whole, or its start and its end around « … ».
        val cut = flat.indexOf('…')
        if (flat != name && (cut < 1 || !name.startsWith(flat.substring(0, cut)) || !name.endsWith(flat.substring(cut + 1)))) {
            out += "« $shown »: neither « $name » nor its start and its end around « … »"
        }
        // No word split: a line starts after « - », « _ » or a space (a line break of its own aside), or at a « . ».
        for (line in 1..last) {
            val at = layout.getLineStart(line)
            val before = shown.substring(0, at.coerceIn(0, shown.length)).trimEnd('\n').lastOrNull()
            if (at < shown.length && before != null && before !in "-_ " && shown[at] != '.') out += "« $shown »: a word split at line ${line + 1}"
        }
        // Each line in the name's own direction, whatever the interface's (here Latin names, left to right): a line's
        // last letter at its right end (in Arabic, « -Nuit-sur-la-ville » and « zip….20261108 » were not).
        for (line in 0..last) {
            val end = (layout.getLineEnd(line, visibleEnd = true) - 1 downTo layout.getLineStart(line)).firstOrNull { !shown[it].isWhitespace() } ?: continue
            if (layout.getBoundingBox(end).right < layout.getLineRight(line) - 1f) out += "« $shown »: line ${line + 1} not written left to right"
        }
        return out
    }

    private fun textOf(n: SemanticsNode): String = n.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }.orEmpty()

    private fun file(name: String, bytes: Long, at: Long, kind: MyExport.Kind) = MyExport(name, Uri.parse("content://media/$at"), bytes, at, null, kind)

    /** A project's .zip (A c #43), long names, the Dutch seat's (E b #16), a short one. */
    private val files = listOf(
        file("Nuit-sur-la-ville-20261108-174901.pocketstudio.zip", 3_400_000, 1_794_160_141, MyExport.Kind.Project),
        file("Jump-up-DnB-bootleg-Mehdi-morceau-club-20261108-174512.wav", 44_090_000, 1_794_159_912, MyExport.Kind.Sound),
        file("Mijn-eerste-beat-20261112-213615.m4a", 1_200_000, 1_794_159_000, MyExport.Kind.Sound),
        file("Nuit-sur-la-ville-20261108-171502.mp4", 9_800_000, 1_794_158_102, MyExport.Kind.Video),
        file("Beat-1.m4a", 900_000, 1_794_046_502, MyExport.Kind.Sound),
    )

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}_{2}")
        fun cases(): List<Array<Any>> = listOf("fr", "de", "nl", "ar", "hi").flatMap { l ->
            listOf("1.0" to "1.0", "1.3" to "1.0", "1.0" to "2.0").map { (s, f) -> arrayOf<Any>(l, s, f) }
        }
    }
}
