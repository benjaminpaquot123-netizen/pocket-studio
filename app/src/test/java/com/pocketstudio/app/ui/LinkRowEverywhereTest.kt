package com.pocketstudio.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.pocketstudio.app.export.LoopPick
import com.pocketstudio.app.export.ShareCheck
import com.pocketstudio.app.export.ShareSheetActions
import com.pocketstudio.app.export.ShareSheetContent
import com.pocketstudio.app.export.ShareSheetUi
import com.pocketstudio.app.mixer.ExportCard
import com.pocketstudio.app.mixer.MixerActions
import com.pocketstudio.app.rhythm.ExportUi
import com.pocketstudio.app.rhythm.ExportWhat
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Hands-on audit, chantier 2 (G3, rule 12 of docs/DESIGN.md « Un lien a l'air d'un lien »): what opens something else is
 * the shared `LinkRow` (a filled row, its icon, its title, the chevron ›), never an orange sentence in a bare TextButton.
 * The places the audit lists: « Mes fichiers », « Autres fichiers », « Sauvegarder le morceau en .zip », « Exporter les
 * notes en MIDI » (the export card), « Importer un projet (.zip) » (Mes projets), « Changer le son » (the Mode simple's tracks).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h2400dp")
class LinkRowEverywhereTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val sources = File("src/main/java").walkTopDown().filter { it.extension == "kt" }.toList()

    @Test
    fun noSentenceDisguisedAsALink() {
        // 1. On the screens themselves: each listed line shows the link row's chevron.
        compose.setContent {
            PocketStudioTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    ShareSheetContent(
                        ShareSheetUi("Mon morceau", seconds = 16, parts = 0, checks = listOf(ShareCheck.NoSong(8)), loop = LoopPick(8, LoopPick.BARS)),
                        ShareSheetActions(sendProject = {}),
                    )
                    // « Piste par piste » chosen: « Avancé » opens by itself, the MIDI line in it.
                    ExportCard(ExportUi(what = ExportWhat.Stems, bars = 8, sceneBars = 4), MixerActions(), hasSong = false)
                }
            }
        }
        compose.waitForIdle()
        val nodes = compose.onAllNodes(SemanticsMatcher("any") { true }, useUnmergedTree = true).fetchSemanticsNodes()
        fun tagged(tag: String) = nodes.filter { it.config.getOrNull(SemanticsProperties.TestTag) == tag }
        val missing = listOf("share_other", "share_project", "share_my_exports", "export_midi", "export_my_exports").filter { tag ->
            val row = tagged(tag).singleOrNull() ?: return@filter true
            nodes.none { it.config.getOrNull(SemanticsProperties.TestTag) == "link_chevron" && under(it, row) }
        }
        assertTrue("Lines that open something but are not a LinkRow (no chevron): $missing", missing.isEmpty())

        // 2. In the code: no TextButton whose words are in the accent colour in export/ and mixer/ (the audit's test).
        val bare = sources.filter { "/export/" in it.path || "/mixer/" in it.path }.flatMap { f ->
            val s = f.readText()
            Regex("""\bTextButton\(""").findAll(s).filter { "Neon.Cyan" in callOf(s, it.range.first) }.map { "${f.name}:${s.take(it.range.first).count { c -> c == '\n' } + 1}" }.toList()
        }
        assertTrue("TextButtons drawn as an orange sentence in export/ and mixer/: $bare", bare.isEmpty())

        // 3. The two lines of other screens the audit names: drawn by LinkRow.
        fun drawnByLinkRow(file: String, tag: String): Boolean {
            val s = sources.first { it.name == file }.readText()
            val at = s.indexOf("\"$tag")
            return at > 0 && "LinkRow(" in s.substring((at - 600).coerceAtLeast(0), at)
        }
        val others = listOf("ProjectsScreen.kt" to "project_import", "AppSpaces.kt" to "simple_track_").filterNot { (f, t) -> drawnByLinkRow(f, t) }
        assertTrue("Not a LinkRow: $others", others.isEmpty())
    }

    private fun under(n: SemanticsNode, a: SemanticsNode): Boolean {
        var p = n.parent
        while (p != null) { if (p.id == a.id) return true; p = p.parent }
        return false
    }

    /** The call starting at [start]: its arguments and its trailing lambda, if any. */
    private fun callOf(s: String, start: Int): String {
        var i = s.indexOf('(', start)
        var depth = 0
        while (i < s.length) {
            when (s[i]) { '(' -> depth++; ')' -> { depth--; if (depth == 0) break } }
            i++
        }
        var end = i + 1
        val rest = Regex("""^\s*\{""").find(s.substring(end))
        if (rest != null) {
            var j = end + rest.range.last
            depth = 0
            while (j < s.length) {
                when (s[j]) { '{' -> depth++; '}' -> { depth--; if (depth == 0) break } }
                j++
            }
            end = j + 1
        }
        return s.substring(start, end.coerceAtMost(s.length))
    }
}
