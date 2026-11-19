package com.pocketstudio.app.tuto

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tour 9, lot L14: the tutorial's panel, its kit banner and its end (finitions F012 to F022). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w392dp-h852dp")
class Tour9L14TutoPanelTest {
    @get:Rule
    val compose = createComposeRule()

    private fun nodes(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true)
    private fun has(tag: String) = nodes(tag).fetchSemanticsNodes().isNotEmpty()
    private val chapters = TutoCourse.of("techno")!!.chaptersFor(TutoLevel.Facile)
    private fun ui(ch: Int = 0, at: Int = 0) = TutoUi(TutoProgress("techno", TutoLevel.Facile, current = chapters[ch].steps[at].id), chapters, ch, at)

    /** F013 (t9-02, t9-48): a finger aiming at « Arrêter » during « Montre-moi » hit « Suivant » and got « pas encore faite ». */
    @Test
    fun demoMakesNextAndPreviousInert() {
        var nexts = 0
        var prevs = 0
        compose.setContent {
            PocketStudioTheme {
                TutoPanel(ui(0, 1).copy(demo = TutoDemoPhase.SHOWING), canShow = true, actions = TutoActions(next = { nexts++ }, previous = { prevs++ }), reducedMotion = true)
            }
        }
        compose.waitForIdle()
        assertTrue("« Suivant » is there, in its place", has("tuto_next"))
        nodes("tuto_next")[0].assertIsNotEnabled()
        nodes("tuto_next")[0].performClick()
        nodes("tuto_prev")[0].assertIsNotEnabled()
        nodes("tuto_prev")[0].performClick()
        compose.waitForIdle()
        assertEquals("« Suivant » moved on during the demo", 0, nexts)
        assertEquals("« Précédent » moved back during the demo", 0, prevs)
    }

    /** F017 (t9-05, t9-07, t9-19): while the kit downloads, « Plus tard » went on showing beside the progress. */
    @Test
    fun kitDownloadingShowsOnlyItsProgress() {
        compose.setContent {
            PocketStudioTheme { TutoKitCard("Techno", "3 Mo", progress = 0.4f, error = null, download = {}, later = {}) }
        }
        compose.waitForIdle()
        assertTrue(has("tuto_kit_progress"))
        assertFalse("« Télécharger » during its download", has("tuto_kit_download"))
        assertFalse("« Plus tard » during the download", has("tuto_kit_later"))
    }

    /** F017, the card pinned under the step's words (the panel's own place for it). */
    @Test
    fun kitDownloadingPinnedShowsOnlyItsProgress() {
        compose.setContent {
            PocketStudioTheme {
                TutoPanel(ui(0, 1), canShow = false, actions = TutoActions(), reducedMotion = true, kit = {
                    TutoKitCard("Techno", "3 Mo", progress = 0.4f, error = null, download = {}, later = {})
                })
            }
        }
        compose.waitForIdle()
        assertTrue(has("tuto_kit_progress"))
        assertFalse("« Télécharger » during its download", has("tuto_kit_download"))
        assertFalse("« Plus tard » during the download", has("tuto_kit_later"))
    }

    /**
     * F016 (t9-16, t9-22, t9-28…): « (3 MB) » cut over two lines in « Télécharger le kit ». In the pinned row and in the
     * card, narrow to wide, 100 to 200 % font, short and long style names: the size's « ( » and « ) » on one line.
     */
    @Test
    fun kitSizeStaysOnOneLine() {
        var scale by mutableStateOf(1f)
        var wide by mutableStateOf(392)
        var style by mutableStateOf("Techno")
        var size by mutableStateOf("3 Mo")
        var pinned by mutableStateOf(true)
        compose.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, scale)) {
                PocketStudioTheme {
                    Box(Modifier.width(wide.dp)) {
                        val card = @androidx.compose.runtime.Composable { TutoKitCard(style, size, progress = null, error = null, download = {}, later = {}) }
                        if (pinned) TutoPanel(ui(0, 1), canShow = false, actions = TutoActions(), reducedMotion = true, kit = card) else card()
                    }
                }
            }
        }
        val faults = mutableListOf<String>()
        var seen = 0
        for (p in listOf(true, false)) for (w in listOf(300, 340, 392)) for (f in listOf(1f, 1.3f, 1.6f, 2f))
            for (st in listOf("Techno", "Hard Techno", "Drum and Bass", "Afro House")) for (sz in listOf("3 Mo", "12 Mo", "120 Mo")) {
                pinned = p; wide = w; scale = f; style = st; size = sz
                compose.waitForIdle()
                val node = compose.onAllNodes(hasText("($sz)", substring = true) or hasText("(${sz.replace(' ', '\u00A0')})", substring = true), useUnmergedTree = true)
                    .fetchSemanticsNodes().firstOrNull { it.config.getOrNull(SemanticsProperties.Text) != null }
                if (node == null) { faults += "${if (p) "pinned" else "card"} $w dp ${(f * 100).toInt()} % $st $sz: no label"; continue }
                val layouts = mutableListOf<TextLayoutResult>()
                node.config[SemanticsActions.GetTextLayoutResult].action!!(layouts)
                val l = layouts.first()
                val text = l.layoutInput.text.text
                val open = text.lastIndexOf('(')
                val close = text.lastIndexOf(')')
                seen++
                if (l.getLineForOffset(open) != l.getLineForOffset(close)) faults += "${if (p) "pinned" else "card"} $w dp ${(f * 100).toInt()} % « $text »: its size over two lines"
            }
        assertTrue("no label read", seen > 0)
        assertTrue(faults.joinToString("\n"), faults.isEmpty())
    }

    /** F016: after « Plus tard » (the built-in kit chosen), the kit is offered no more, at the next step or after a reload. */
    @Test
    fun kitNotOfferedAgainAfterLater() {
        val target = com.pocketstudio.model.tuto.TutoTarget.of("techno")!!
        val later = TutoProgress("techno", TutoLevel.Facile, current = chapters[0].steps[2].id, kit = TutoProgress.KIT_BUILT_IN)
        val pack = com.pocketstudio.model.PackEntry(id = "techno", name = "Techno", file = "techno.zip", sha256 = "", sizeBytes = 3_000_000)
        for (p in listOf(later, TutoProgress.decode(later.encode())!!)) {
            val offer = com.pocketstudio.model.tuto.TutoKit.offer(target, p, com.pocketstudio.model.Project(), listOf(pack), emptyList(), asked = false)
            assertEquals("the kit offered again after « Plus tard »", com.pocketstudio.model.tuto.TutoKit.Offer.Wait, offer)
        }
    }

    /**
     * F016 (t9-16 Spanish at 130 %, t9-22 foldable): on a step of the song's map, the kit banner took the map's room
     * (pinned under the words, it took its height from them): the map of parts was cut. The map keeps its whole height.
     */
    @Test
    fun kitLeavesTheMapWhole() {
        val ch = chapters.indexOfFirst { it.map }
        assertTrue("a chapter with the song's map", ch >= 0)
        var kit by mutableStateOf<Int?>(null)
        var scale by mutableStateOf(1f)
        // The step shown: the map's, or the course's first (gone through so that the panel takes its height afresh, as
        // when the user comes to a step with the banner already there).
        var at by mutableStateOf(ui(ch, 0))
        compose.setContent {
            val local = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(local.density, scale)) {
                PocketStudioTheme {
                    TutoPanel(at, canShow = true, actions = TutoActions(), reducedMotion = true, map = tutoMap("techno"), kit = kit?.let { k ->
                        @androidx.compose.runtime.Composable { TutoKitCard("Techno", "3 Mo", progress = if (k == 1) 0.4f else null, error = null, download = {}, later = {}) }
                    })
                }
            }
        }
        fun seen(): Float {
            at = ui(0, 0)
            compose.waitForIdle()
            at = ui(ch, 0)
            compose.waitForIdle()
            val n = nodes("tuto_map").fetchSemanticsNodes()
            assertTrue("the song's map is on the step", n.isNotEmpty())
            return n[0].boundsInRoot.height
        }
        for (s in listOf(1f, 1.3f)) {
            scale = s; kit = null
            val whole = seen()
            assertTrue("the map has a height", whole > 0f)
            for (k in listOf(0, 1)) {
                kit = k
                assertEquals("font $s, kit ${if (k == 1) "downloading" else "offered"}: the map's height seen", whole, seen(), 1f)
            }
        }
    }
    /**
     * F012 (t9-40, 130 %, chapter 10): on a short step done, « Bravo, c'est réussi ! » was cut and its explanation (« Partie
     * « Montée » créée (copie du Groove) : sans … ») lay wholly under « Lire la suite »: the panel kept the step's height.
     * The « Bravo » banner is read whole, nothing under « Lire la suite », at 100 and 130 %.
     */
    @Test
    fun bravoReadsWhole() {
        val made = "Partie « Montée » créée (copie du Groove) : sans Grosse caisse studio, Claquement de mains, Basse, Mélodie. " +
            "Sa caisse claire accélère mesure après mesure."
        // t9-40 (jump_up, Facile, « Construis tes parties »: the steps that make a part, their button says what it made),
        // t9-52 and t9-68 (phonk Facile, techno Pro: a pad's step, « Bravo » and its plain line).
        data class Case(val style: String, val level: TutoLevel, val pick: (String) -> Boolean, val made: String?)
        val cases = listOf(
            Case("jump_up", TutoLevel.Facile, { it.startsWith("make_") }, made),
            Case("phonk", TutoLevel.Facile, { it == "pad_do" }, null),
            Case("techno", TutoLevel.Pro, { it == "pad_do" }, null),
        )
        fun steps(c: Case): List<TutoUi> {
            val chs = TutoCourse.of(c.style)!!.chaptersFor(c.level)
            return chs.withIndex().flatMap { (ci, ch) -> ch.steps.withIndex().filter { c.pick(it.value.id) }.map { (si, st) ->
                TutoUi(TutoProgress(c.style, c.level, current = st.id), chs, ci, si) } }
        }
        val all = cases.flatMap { c -> steps(c).map { it to c.made } }
        assertTrue("the steps of t9-40, t9-52, t9-68 found", all.size >= 3)
        var scale by mutableStateOf(1f)
        var shown by mutableStateOf(ui(0, 0))
        compose.setContent {
            val local = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(local.density, scale)) {
                PocketStudioTheme { TutoPanel(shown, canShow = true, actions = TutoActions(), reducedMotion = true, mine = true) }
            }
        }
        val faults = mutableListOf<String>()
        for (grid in listOf(false, true)) for (s in listOf(1f, 1.3f, 2f)) for ((step, said) in all) {
            scale = s
            // On the pads' screen, the cells to touch (t9-52, t9-68: the words' low room over the grid).
            TutoHints.gridInSight = grid
            TutoHints.cells = if (grid) 0 to setOf(0, 4, 8, 12) else null
            // The step first (its height taken), then ticked: « Bravo » in its words' place.
            shown = ui(0, 0); compose.waitForIdle()
            shown = step; compose.waitForIdle()
            shown = step.copy(progress = step.progress.copy(done = setOf(step.current.id)), bravo = true, made = said); compose.waitForIdle()
            val id = "${step.progress.style} ${step.current.id}${if (grid) " (grid)" else ""}"
            val node = nodes("tuto_bravo").fetchSemanticsNodes().firstOrNull()
            if (node == null) { faults += "font $s $id: no « Bravo »"; continue }
            val seen = node.boundsInRoot.height
            if (seen < node.size.height - 1f) faults += "font $s $id: « Bravo » seen ${seen.toInt()} of ${node.size.height} px"
            if (has("tuto_text_more")) faults += "font $s $id: « Lire la suite » under « Bravo »"
        }
        TutoHints.gridInSight = false
        TutoHints.cells = null
        assertTrue(faults.joinToString("\n"), faults.isEmpty())
    }

    /**
     * F022 (t9-32, hard_techno Facile, Arabic at 200 %, « Construis tes parties »): « Lire la suite » under a « Bravo »
     * whose words the seat read whole (its screen's text, the banner's bottom under the cut). « Lire la suite » only when
     * something is really left to read: never under « Bravo », under a step's words only when they are cut by more than
     * a margin; the steps read first with « Lire la suite » as the seat did. Same cause as F012 (the grid's low room
     * kept for « Bravo »): red on the panel before that fix.
     */
    @Test
    fun moreOnlyWhenMore() {
        val made = "Partie « Drops 2 » créée (copie du Groove) : sans bord de caisse."
        val all = listOf("hard_techno", "jump_up").flatMap { style ->
            val chs = TutoCourse.of(style)!!.chaptersFor(TutoLevel.Facile)
            chs.withIndex().flatMap { (ci, ch) -> ch.steps.indices.map { si ->
                TutoUi(TutoProgress(style, TutoLevel.Facile, current = ch.steps[si].id), chs, ci, si) } }
        }.filter { it.current.id.startsWith("make_") || it.chapter == 7 }
        assertTrue("the steps of t9-32 found", all.size >= 3)
        var scale by mutableStateOf(1f)
        var shown by mutableStateOf(ui(0, 0))
        var lang by mutableStateOf(com.pocketstudio.model.i18n.Lang.FR)
        compose.setContent {
            val local = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(local.density, scale),
                androidx.compose.ui.platform.LocalLayoutDirection provides com.pocketstudio.ui.layoutDirectionOf(lang.rtl)) {
                PocketStudioTheme { TutoPanel(shown, canShow = true, actions = TutoActions(), reducedMotion = true, mine = true) }
            }
        }
        // Less than a line hidden (the banner's own margin and border) is nothing left to read.
        val margin = 12f
        val faults = mutableListOf<String>()
        for (l in listOf(com.pocketstudio.model.i18n.Lang.FR, com.pocketstudio.model.i18n.Lang.AR)) for (grid in listOf(false, true)) for (s in listOf(1f, 1.02f, 1.3f, 1.6f, 2f)) for (step in all) {
            if (lang != l) { lang = l; com.pocketstudio.model.i18n.I18n.lang = l }
            scale = s
            // Over the pads' grid too (its cells to touch: the words' low room).
            TutoHints.gridInSight = grid
            TutoHints.cells = if (grid) 0 to setOf(0, 4, 8, 12) else null
            shown = ui(0, 0); compose.waitForIdle()
            shown = step; compose.waitForIdle()
            val id = "${l.code} font $s ${step.progress.style} ${step.current.id}${if (grid) " (grid)" else ""}"
            nodes("tuto_text").fetchSemanticsNodes().firstOrNull()?.let { t ->
                if (has("tuto_text_more") && t.boundsInRoot.height >= t.size.height - margin) faults += "$id: « Lire la suite » under words seen whole"
            }
            // Read to their end, as the seat did.
            repeat(8) { if (has("tuto_text_more")) { nodes("tuto_text_more").onFirst().performClick(); compose.waitForIdle() } }
            for (said in listOf(made, null)) {
                shown = step.copy(progress = step.progress.copy(done = setOf(step.current.id)), bravo = true, made = said); compose.waitForIdle()
                val b = nodes("tuto_bravo").fetchSemanticsNodes().firstOrNull() ?: continue
                // A few lines, it always has its room: « Lire la suite » under it is never a real « more ».
                if (has("tuto_text_more"))
                    faults += "$id${if (said == null) "" else " (made)"}: « Lire la suite » under « Bravo » seen ${b.boundsInRoot.height.toInt()} of ${b.size.height} px"
            }
        }
        TutoHints.gridInSight = false
        TutoHints.cells = null
        com.pocketstudio.model.i18n.I18n.lang = com.pocketstudio.model.i18n.Lang.FR
        assertTrue(faults.joinToString("\n"), faults.isEmpty())
    }

    /**
     * F014 (t9-20, German at 200 %): « Dein Ergebnis anhören » and « Beispiel anhören » side by side, each label shrunk
     * on its own, in two text sizes. The panel's buttons on one row share one size: a step done (its result and the
     * example) and a step under way (the example and « Montre-moi »), in four languages, narrow to wide, 100 to 200 %.
     */
    @Test
    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun neighboursShareTextSize() {
        val steps = listOf("techno", "disco").flatMap { style ->
            val chs = TutoCourse.of(style)!!.chaptersFor(TutoLevel.Facile)
            chs.withIndex().flatMap { (ci, ch) -> ch.steps.indices.map { ci to it } }
                .filter { (ci, si) -> chs[ci].steps[si].listen.isNotEmpty() && !chs[ci].steps[si].listening }.let { it.take(2) + it.filter { (ci, _) -> ci == 7 }.take(4) }
                .map { (ci, si) -> TutoUi(TutoProgress(style, TutoLevel.Facile, current = chs[ci].steps[si].id), chs, ci, si) }
        }
        assertTrue("steps with an example found", steps.isNotEmpty())
        var scale by mutableStateOf(1f)
        var wide by mutableStateOf(392)
        var shown by mutableStateOf(ui(0, 0))
        compose.setContent {
            val local = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(local.density, scale)) {
                PocketStudioTheme { Box(Modifier.width(wide.dp)) { TutoPanel(shown, canShow = true, actions = TutoActions(), reducedMotion = true, mine = true) } }
            }
        }
        val tags = listOf("tuto_play_mine", "tuto_listen", "tuto_show", "tuto_mark")
        // A label's drawn size: the width of its leading space (every label starts with one), which grows with the font
        // (the layout's style keeps the largest size, the size the words were shrunk to is in its glyphs).
        fun sizeIn(tag: String): Triple<Float, Float, Float>? {
            val button = nodes(tag).fetchSemanticsNodes().firstOrNull() ?: return null
            val text = compose.onAllNodes(androidx.compose.ui.test.hasAnyAncestor(androidx.compose.ui.test.hasTestTag(tag)) and
                androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)
                .fetchSemanticsNodes().firstOrNull() ?: return null
            val layouts = mutableListOf<TextLayoutResult>()
            text.config[SemanticsActions.GetTextLayoutResult].action!!(layouts)
            val lay = layouts.first()
            assertEquals("$tag starts with a space", ' ', lay.layoutInput.text.text.first())
            // The space before the words alone on a first line: the label under an empty line (fr, 160 %: « Montre-moi »).
            val first = lay.layoutInput.text.text.substring(0, lay.getLineEnd(0))
            assertTrue("$tag: a first line of blanks under ${lay.layoutInput.text.text}", lay.lineCount == 1 || first.isNotBlank())
            return Triple(button.boundsInRoot.top, button.boundsInRoot.bottom, lay.getBoundingBox(0).width)
        }
        val faults = mutableListOf<String>()
        var pairs = 0
        for (l in listOf(com.pocketstudio.model.i18n.Lang.DE, com.pocketstudio.model.i18n.Lang.FR, com.pocketstudio.model.i18n.Lang.NL, com.pocketstudio.model.i18n.Lang.IT))
            for (w in listOf(360, 392, 411)) for (s in listOf(1f, 1.3f, 1.6f, 2f)) for (step in steps) for (done in listOf(true, false)) {
                com.pocketstudio.model.i18n.I18n.lang = l
                wide = w; scale = s
                shown = ui(0, 0); compose.waitForIdle()
                shown = if (done) step.copy(progress = step.progress.copy(done = setOf(step.current.id)), bravo = true) else step
                compose.waitForIdle()
                val seen = tags.mapNotNull { t -> sizeIn(t)?.let { t to it } }
                // The buttons on one row: their bounds overlap from top to bottom.
                val rows = mutableListOf<MutableList<Pair<String, Triple<Float, Float, Float>>>>()
                for (b in seen.sortedBy { it.second.first }) {
                    val row = rows.lastOrNull()
                    if (row != null && b.second.first < row.maxOf { it.second.second }) row += b else rows += mutableListOf(b)
                }
                rows.filter { it.size > 1 }.forEach { row ->
                    pairs++
                    val sizes = row.map { it.second.third }
                    if (sizes.max() - sizes.min() > 0.5f)
                        faults += "${l.code} $w dp ${(s * 100).toInt()} % ${step.current.id}${if (done) " done" else ""}: " +
                            row.joinToString { "${it.first} space ${it.second.third} px" }
                }
            }
        com.pocketstudio.model.i18n.I18n.lang = com.pocketstudio.model.i18n.Lang.FR
        assertTrue("no two buttons side by side seen", pairs > 0)
        assertTrue(faults.joinToString("\n"), faults.isEmpty())
    }

    /**
     * F019 (t9-10, t9-18, t9-61, t9-63, t9-72): at the course's end the panel's head still said « Étape 3/3 » under a
     * finished tutorial. At the end: « Tuto fini », « Recommencer » and « Chapitres », no step counter, no « Suivant ».
     */
    @Test
    fun endShowsOnlyFinished() {
        val all = chapters.flatMap { it.steps }
        val last = chapters.lastIndex
        var at by mutableStateOf(TutoUi(TutoProgress("techno", TutoLevel.Facile, all.map { it.id }.toSet(), all.last().id), chapters, last, chapters[last].steps.lastIndex))
        compose.setContent { PocketStudioTheme { TutoPanel(at, canShow = true, actions = TutoActions(), reducedMotion = true) } }
        for (atEnd in listOf(false, true)) {
            at = at.copy(atEnd = atEnd)
            compose.waitForIdle()
            val step = compose.onAllNodes(hasText("Étape", substring = true), useUnmergedTree = true).fetchSemanticsNodes()
                .mapNotNull { it.config.getOrNull(SemanticsProperties.Text)?.joinToString { t -> t.text } }
            assertTrue("atEnd $atEnd: a step's counter at the end: $step", step.isEmpty())
            assertTrue("atEnd $atEnd: « Tuto fini » said", compose.onAllNodes(hasText("Tuto fini", substring = true), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
            assertTrue("atEnd $atEnd: « Recommencer »", has("tuto_restart_finished"))
            assertTrue("atEnd $atEnd: « Chapitres »", has("tuto_chapters"))
            assertFalse("atEnd $atEnd: « Suivant » at the end", has("tuto_next"))
        }
    }

    /**
     * F018 (t9-11, t9-15, t9-54): in the key sheet, nothing is chosen ahead (« G » touched first chose « Dorian » by
     * itself), each chip chosen says so (the seats read only « E · Durowa » above), and the sheet keeps its place under the
     * finger (it rose when « Réussi ! » came).
     */
    @Test
    fun keySheetChoosesNothingAheadAndStaysPut() {
        var harmony by mutableStateOf(com.pocketstudio.model.Harmony())
        val wanted = com.pocketstudio.model.Harmony(root = 7, scale = com.pocketstudio.model.Scale.Dorian)
        compose.setContent {
            PocketStudioTheme {
                Box(androidx.compose.ui.Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.BottomCenter) {
                    TutoKeySheetContent(harmony, wanted, set = { harmony = it }, close = {})
                }
            }
        }
        fun chosen() = compose.onAllNodes(androidx.compose.ui.test.SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
            .fetchSemanticsNodes().mapNotNull { it.config.getOrNull(SemanticsProperties.TestTag) }.filter { it.startsWith("tuto_scale_") || it.startsWith("tuto_root_") }
        fun top(tag: String) = compose.onAllNodesWithTag(tag).onFirst().fetchSemanticsNode().boundsInRoot.top
        val chips = compose.onAllNodes(androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)).fetchSemanticsNodes().size
        assertEquals("every scale and root chip says whether it is chosen", com.pocketstudio.model.Scale.entries.size - 1 + 12, chips)
        assertEquals("nothing chosen when the song has no key", emptyList<String>(), chosen())
        val rootTop = top("tuto_root_7")
        compose.onAllNodesWithTag("tuto_root_7").onFirst().performClick()
        compose.waitForIdle()
        assertEquals("« G » touched: no scale chosen by itself", listOf("tuto_root_7"), chosen())
        assertEquals("the scale stays as the user left it", com.pocketstudio.model.Scale.Off, harmony.scale)
        assertEquals("the sheet stays put after the first touch", rootTop, top("tuto_root_7"), 0.5f)
        val scaleTop = top("tuto_scale_Dorian")
        compose.onAllNodesWithTag("tuto_scale_Dorian").onFirst().performClick()
        compose.waitForIdle()
        assertEquals(wanted.copy(lock = true), harmony)
        assertEquals("both chosen chips marked", setOf("tuto_root_7", "tuto_scale_Dorian"), chosen().toSet())
        compose.onAllNodesWithTag("tuto_key_done").onFirst().fetchSemanticsNode()
        assertEquals("« Réussi ! » comes without moving the chips", scaleTop, top("tuto_scale_Dorian"), 0.5f)
        // Another root touched once the key is set: the scale stays.
        compose.onAllNodesWithTag("tuto_root_2").onFirst().performClick()
        compose.waitForIdle()
        assertEquals(com.pocketstudio.model.Harmony(2, com.pocketstudio.model.Scale.Dorian, true), harmony)
        assertEquals("« Réussi ! » gone, the chips still in place", scaleTop, top("tuto_scale_Dorian"), 0.5f)
        assertTrue("« Revenir au tuto » not offered before the key is reached",
            compose.onAllNodesWithTag("tuto_key_continue").fetchSemanticsNodes().isEmpty())
    }
}
